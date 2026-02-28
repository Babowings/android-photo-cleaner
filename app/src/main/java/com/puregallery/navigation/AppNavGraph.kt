package com.puregallery.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.puregallery.ui.gallery.GalleryScreen
import com.puregallery.ui.review.ReviewScreen

object Routes {
    const val GALLERY = "gallery"
    const val REVIEW = "review"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.GALLERY
    ) {
        composable(Routes.GALLERY) {
            GalleryScreen(
                onOpenReview = { navController.navigate(Routes.REVIEW) }
            )
        }
        composable(Routes.REVIEW) {
            ReviewScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
