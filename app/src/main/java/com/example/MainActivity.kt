package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.MainStudioScreen
import com.example.ui.theme.MuseCraftTheme
import com.example.viewmodel.MusicStudioViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MusicStudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuseCraftTheme {
                MainStudioScreen(viewModel = viewModel)
            }
        }
    }
}
