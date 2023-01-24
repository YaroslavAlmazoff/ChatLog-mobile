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
        val url = URL(Constants().SITE_NAME + "admin/sendmessage")
        val connection = url.openConnection() as HttpsURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept-Charset", "utf-8")
        val json = "{\"message\": \"${supportField?.text.toString()}\"}"
        connection.outputStream.write(json.toByteArray())
        var data: Int = connection.inputStream.read()
        var result = ""
        while(data != -1) {
            result += data.toChar().toString()
            data = connection.inputStream.read()
        }
        Log.e("TAG", result)
    }
}