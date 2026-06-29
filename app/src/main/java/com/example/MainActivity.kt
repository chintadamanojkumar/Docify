package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.PdfViewModel
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PdfReaderScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.android.gms.ads.MobileAds

enum class AppScreen {
    Dashboard,
    Reader,
    AiChat
}

class MainActivity : ComponentActivity() {
    private val pdfViewModel: PdfViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize AdMob on app startup to guarantee active delivery of academic ads
        MobileAds.initialize(this) {}
        
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.Dashboard) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                            when (screen) {
                                AppScreen.Dashboard -> {
                                    DashboardScreen(
                                        viewModel = pdfViewModel,
                                        onNavigateToReader = { currentScreen = AppScreen.Reader },
                                        onNavigateToAiChat = { currentScreen = AppScreen.AiChat }
                                    )
                                }
                                AppScreen.Reader -> {
                                    PdfReaderScreen(
                                        viewModel = pdfViewModel,
                                        onNavigateBack = { currentScreen = AppScreen.Dashboard }
                                    )
                                }
                                AppScreen.AiChat -> {
                                    AiChatScreen(
                                        onNavigateBack = { currentScreen = AppScreen.Dashboard }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Inline wrapper Box helper to avoid complex layout imports
@Composable
fun Box(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        content()
    }
}
