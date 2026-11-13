package com.example.ch05

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

// Data dummy — nantinya bisa diganti sumber foto asli (API/galeri lokal).
// Warna berbeda per item hanya untuk membedakan sel grid secara visual
// karena belum ada aset foto sungguhan.
data class Photo(val id: Int, val color: Color)

private val palette = listOf(
    Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF5C6BC0),
    Color(0xFF29B6F6), Color(0xFF26A69A), Color(0xFF9CCC65),
    Color(0xFFFFCA28), Color(0xFFFF7043)
)

val dummyPhotos = (1..21).map { i ->
    Photo(id = i, color = palette[(i - 1) % palette.size])
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Eksplorasi") }) }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dummyPhotos, key = { it.id }) { photo ->
                PhotoGridItem(photo)
            }
        }
    }
}

@Composable
private fun PhotoGridItem(photo: Photo) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(photo.color),
        contentAlignment = Alignment.Center
    ) {
        // Placeholder gambar dari painterResource — bukan Icons bawaan,
        // supaya polanya sama dengan cara memuat foto sungguhan nanti.
        Image(
            painter = painterResource(id = R.drawable.ic_photo_placeholder),
            contentDescription = "Foto ${photo.id}",
            colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.85f)),
            modifier = Modifier.size(32.dp)
        )
    }
}
