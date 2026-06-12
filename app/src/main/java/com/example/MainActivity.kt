package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.GHPlayerMain
import com.example.ui.MainViewModel
import com.example.ui.theme.GHPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            GHPlayerTheme(skin = viewModel.currentSkin) {
                GHPlayerMain(viewModel = viewModel)
            }
        }
    }
}

