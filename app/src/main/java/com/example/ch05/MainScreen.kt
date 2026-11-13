package com.example.ch05

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick  = {
                            navController.navigate(item.route) {
                                popUpTo(Routes.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon  = {
                            Icon(
                                imageVector =
                                    if (currentRoute == item.route)
                                        item.iconSelected
                                    else
                                        item.iconUnselected,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Routes.Home.route,
            modifier         = Modifier.padding(innerPadding),
            // Default untuk semua destinasi — dipakai saat berpindah tab
            // Bottom Navigation (Home/Explore/Profile), fade halus karena
            // ketiganya sederajat, bukan hubungan induk-anak.
            enterTransition = { fadeIn(tween(200)) },
            exitTransition  = { fadeOut(tween(200)) }
        ) {
            composable(Routes.Home.route)    { HomeScreen(navController) }
            composable(Routes.Explore.route) { ExploreScreen() }
            composable(Routes.Profile.route) { ProfileScreen() }

            composable(
                route     = Routes.Detail.route,
                arguments = listOf(
                    navArgument("itemId") { type = NavType.IntType }
                ),
                // Tantangan deep link: myapp://article/{itemId} dicocokkan
                // ke placeholder {itemId} yang sama dengan route biasa —
                // baik dibuka lewat navigate() maupun dari luar app,
                // DetailScreen menerima itemId dengan cara yang identik.
                deepLinks = listOf(
                    navDeepLink { uriPattern = "myapp://article/{itemId}" }
                ),
                // Detail adalah navigasi "masuk lebih dalam" — beda dari
                // pindah tab, jadi dibuat geser horizontal supaya terasa
                // seperti membuka halaman baru, bukan sekadar berganti isi.
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300)
                    )
                }
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getInt("itemId") ?: 0
                DetailScreen(
                    itemId = itemId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
