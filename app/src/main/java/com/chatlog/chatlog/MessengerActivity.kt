package com.chatlog.chatlog

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

class MessengerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messenger)
        Log.e("TAG", intent.getStringExtra("id")!!)
    }
}