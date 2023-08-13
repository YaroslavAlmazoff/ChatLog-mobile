package com.chatlog.chatlog

import android.R.attr.duration
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.nio.charset.Charset
import javax.net.ssl.HttpsURLConnection


class LoginActivity : AppCompatActivity() {
    var emailField: EditText? = null
    var passwordField: EditText? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }
    private fun String.isEmailValid(): Boolean {
        return !TextUtils.isEmpty(this) && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
    }
    fun login(view: View) {
        emailField = findViewById(R.id.emailField)
        passwordField = findViewById(R.id.passwordField)

        if(!emailField?.text.toString().isEmailValid()) {
            Toast.makeText(this, R.string.login_email_error, Toast.LENGTH_SHORT).show()
            return
        }
        if(passwordField?.text.toString() == "") {
            Toast.makeText(this, R.string.login_password_error, Toast.LENGTH_SHORT).show()
            return
        }
        Log.e("TAG", "Проверка прошла")
        sendInBackground()
    }

    private fun sendInBackground() {
        Thread {
            try {
                sendData()
            } catch (e: InterruptedException) {
                Log.e("TAG", "AAA")
            }
        }.start()
    }
    private fun sendData() {
        val json = "{\"email\": \"${emailField?.text.toString()}\", " +
                "\"password\": \"${passwordField?.text.toString()}\"}"
        val result = Utils.request(this, "auth/login-mobile", "POST", false, json)
        if(JSONObject(result).getJSONArray("errors").length() > 0) {
            runOnUiThread {
                Toast.makeText(this, R.string.login_error, Toast.LENGTH_SHORT).show()
            }
            return
        }
        Utils().saveUserData(result, filesDir)
        runHomeActivity()
    }
    private fun runHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
    }
}