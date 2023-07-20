package com.chatlog.chatlog

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class CloudVideoActivity : AppCompatActivity() {
    private lateinit var videoView: VideoView
    private var isFullScreen = false
    private var originalSystemUiVisibility = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud_video)

        findViewById<TextView>(R.id.go_back).setOnClickListener {
            onBackPressed()
        }

        videoView = findViewById(R.id.video)

        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)

        videoView.setMediaController(mediaController)
        videoView.requestFocus()

        videoView.setVideoPath(intent.getStringExtra("path"))

        videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.setOnVideoSizeChangedListener { _, _, _ ->
                if (videoView.isPlaying && !isFullScreen) {
                    toggleFullscreen()
                }
            }
        }

        videoView.start()
    }

    fun toggleFullscreen() {
        if (!isFullScreen) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            supportActionBar?.hide()
            originalSystemUiVisibility = window.decorView.systemUiVisibility
            setSystemUiVisibility()
            isFullScreen = true
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            supportActionBar?.show()
            window.decorView.systemUiVisibility = originalSystemUiVisibility
            isFullScreen = false
        }
    }

    private fun setSystemUiVisibility() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }
}