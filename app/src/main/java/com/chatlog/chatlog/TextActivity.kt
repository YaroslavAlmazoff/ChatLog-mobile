package com.chatlog.chatlog

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class TextActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text)

        findViewById<TextView>(R.id.text).text = intent.getStringExtra("text")
        findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.go_back).setOnClickListener {
            onBackPressed()
        }
    }
}