package com.example.ch12.data.photo

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.example.ch12.util.PROFILE_PHOTO_QUALITY
import com.example.ch12.util.PROFILE_PHOTO_SIZE
import com.example.ch12.util.calculateInSampleSize
import com.example.ch12.util.centerSquare
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject

// Foto 4000x3000 -> JPEG persegi 400x400, sekitar 30-60 KB
class AndroidImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context
) : ImageCompressor {

    // Decode dan kompres JPEG adalah kerja CPU + I/O: jangan di main thread
    override suspend fun compress(uri: String): CompressedImage = withContext(Dispatchers.Default) {
        val source = Uri.parse(uri)

        // 1. Baca ukuran saja (tanpa mengalokasikan piksel)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(source).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw ImageDecodeException("Berkas yang dipilih bukan gambar.")
        }

        // 2. Dekode versi yang sudah diperkecil (stream BARU: stream lama sudah habis dibaca)
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, PROFILE_PHOTO_SIZE)
        }
        val sampled = open(source).use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw ImageDecodeException("Berkas yang dipilih bukan gambar.")

        // 3. Potong persegi di tengah, perkecil ke 400 px, dan putar sesuai EXIF
        val crop = centerSquare(sampled.width, sampled.height)
        val matrix = Matrix().apply {
            val scale = minOf(1f, PROFILE_PHOTO_SIZE.toFloat() / crop.size)
            postScale(scale, scale)
            postRotate(readRotation(source).toFloat())
        }
        val result = Bitmap.createBitmap(sampled, crop.left, crop.top, crop.size, crop.size, matrix, true)

        // 4. Kompres ke JPEG kualitas 80
        val bytes = ByteArrayOutputStream().use { out ->
            result.compress(Bitmap.CompressFormat.JPEG, PROFILE_PHOTO_QUALITY, out)
            out.toByteArray()
        }

        // 5. Lepas memori piksel secepatnya
        val width = result.width
        val height = result.height
        if (result !== sampled) result.recycle()
        sampled.recycle()

        CompressedImage(bytes, bounds.outWidth, bounds.outHeight, width, height)
    }

    // content:// (Photo Picker) maupun file:// (CameraX) dibuka lewat ContentResolver
    private fun open(uri: Uri): InputStream =
        context.contentResolver.openInputStream(uri)
            ?: throw ImageDecodeException("Gambar tidak dapat dibuka.")

    // Kamera menyimpan piksel apa adanya + tag orientasi. BitmapFactory
    // mengabaikan tag itu, sehingga tanpa ini foto potret bisa tampil miring.
    private fun readRotation(uri: Uri): Int = runCatching {
        open(uri).use { ExifInterface(it).rotationDegrees }
    }.getOrDefault(0)
}
