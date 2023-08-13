package com.chatlog.chatlog

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class SupportActivity : AppCompatActivity() {
    var supportField: EditText? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_support)

        supportField = findViewById(R.id.support_field)
    }
    fun send(view: View) {
        sendInBackground()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
    private fun sendInBackground() {
        Thread {
            try {
                sendSupportMessage()
            } catch (e: InterruptedException) {
                Log.e("TAG", "AAA")
            }
        }.start()
    }
    private fun sendSupportMessage() {
        val json = "{\"message\": \"${supportField?.text.toString()}\"}"
        val result = Utils.request(this, "admin/sendmessage", "POST", false, json)
        Log.e("TAG", result)
    }
}