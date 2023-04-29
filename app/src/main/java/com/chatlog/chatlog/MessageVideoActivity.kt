package com.chatlog.chatlog

import android.R.attr.path
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity


class MessageVideoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_video)

        val video = findViewById<VideoView>(R.id.video)
        val button = findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.go_back_button)

        button.setOnClickListener {
            onBackPressed()
        }
        val isUri = intent.getBooleanExtra("isUri", false)
        Log.e("TAG", isUri.toString())
        if(isUri) {
            val uri: Uri = Uri.parse(intent.getStringExtra("videoUri"))
            Log.e("TAG", uri.toString())
            Log.e("TAG", video?.toString()!!)
            val mediaController = MediaController(this)
            mediaController.setAnchorView(video)
            mediaController.setMediaPlayer(video)
            video?.setMediaController(mediaController)
            video?.setVideoURI(uri)
            video?.requestFocus()
            video?.start()
        } else {
            val url = Constants().SITE_NAME_FILES + "/messagevideos/${intent.getStringExtra("videoUrl")}"
            val uri: Uri = Uri.parse(url)
            Log.e("TAG", uri.toString())
            Log.e("TAG", video?.toString()!!)
            val mediaController = MediaController(this)
            mediaController.setAnchorView(video)
            mediaController.setMediaPlayer(video)
            video?.setMediaController(mediaController)
            video?.setVideoURI(uri)
            video?.requestFocus()
            video?.start()
        }
    }
}