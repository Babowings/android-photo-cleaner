package com.puregallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.puregallery.navigation.AppNavGraph
import com.puregallery.ui.theme.PureGalleryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PureGalleryTheme {
                AppNavGraph()
            }
        }
    }
}
