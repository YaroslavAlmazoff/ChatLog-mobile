package com.chatlog.chatlog

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import org.json.JSONObject
import java.io.File

class ForgotPassword : AppCompatActivity() {
    var forgotPasswordText: TextView? = null
    var returnAccessText: TextView? = null
    var emailField: EditText? = null
    var sendButton: com.sanojpunchihewa.glowbutton.GlowButton? = null
    var backButton: com.sanojpunchihewa.glowbutton.GlowButton? = null
    var pb: ProgressBar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        forgotPasswordText = findViewById(R.id.forgot_password_text)
        returnAccessText = findViewById(R.id.return_access_text)
        emailField = findViewById(R.id.email_field)
        sendButton = findViewById(R.id.send_button)
        backButton = findViewById(R.id.back_button)
        pb = findViewById(R.id.pb)

        sendButton?.setOnClickListener {
            pb?.visibility = View.VISIBLE
            sendMail()
        }
        backButton?.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun String.isEmailValid(): Boolean {
        return !TextUtils.isEmpty(this) && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
    }

    fun sendMail() {
        if(!emailField?.text.toString().isEmailValid()) {
            returnAccessText?.text = "Введите корректный email"
            return
        }
        Thread {
            try {
                val json = "{\"email\": \"${emailField?.text}\"}"
                Utils.request(this, "send-return-mail", "POST", false, json)
                runOnUiThread {
                    returnAccessText?.visibility = View.GONE
                    emailField?.visibility = View.GONE
                    sendButton?.visibility = View.GONE
                    forgotPasswordText?.visibility = View.VISIBLE
                    backButton?.visibility = View.VISIBLE
                }
            } catch (e: InterruptedException) {
                Log.e("TAG", "ERROR")
            }
        }.start()
    }
}