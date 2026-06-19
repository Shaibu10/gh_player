package com.example

import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.GHPlayerMain
import com.example.ui.MainViewModel
import com.example.ui.theme.GHPlayerTheme
import com.example.service.PlaybackService

class MainActivity : ComponentActivity() {
    private var mainViewModel: MainViewModel? = null

    private val playbackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val vm = mainViewModel ?: return
            when (intent?.action) {
                PlaybackService.ACTION_PLAY_PAUSE -> {
                    if (vm.isPlaying) {
                        vm.pauseAudio()
                    } else {
                        vm.resumePlayback()
                    }
                }
                PlaybackService.ACTION_PREVIOUS -> {
                    vm.playPreviousAudio()
                }
                PlaybackService.ACTION_NEXT -> {
                    vm.playNextAudio()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val filter = IntentFilter().apply {
            addAction(PlaybackService.ACTION_PLAY_PAUSE)
            addAction(PlaybackService.ACTION_PREVIOUS)
            addAction(PlaybackService.ACTION_NEXT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(playbackReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(playbackReceiver, filter)
        }

        setContent {
            val vm: MainViewModel = viewModel()
            mainViewModel = vm
            GHPlayerTheme(skin = vm.currentSkin) {
                GHPlayerMain(viewModel = vm)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(playbackReceiver)
        } catch (e: Exception) {
            // Unregister safety
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val vm = mainViewModel
        if (vm != null && vm.activeVideoItem != null && vm.isPlaying) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val params = PictureInPictureParams.Builder().build()
                    enterPictureInPictureMode(params)
                } catch (e: Exception) {
                    // Fallback or ignore if not supported by current screen settings
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        mainViewModel?.isInPipMode = isInPictureInPictureMode
    }
}

