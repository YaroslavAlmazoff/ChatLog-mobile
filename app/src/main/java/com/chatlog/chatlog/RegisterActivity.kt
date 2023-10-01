package com.chatlog.chatlog

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.HttpsURLConnection

class RegisterActivity : AppCompatActivity() {
    var nameField: EditText? = null
    var surnameField: EditText? = null
    var dateField: EditText? = null
    var emailField: EditText? = null
    var countryField: EditText? = null
    var cityField: EditText? = null
    var password1Field: EditText? = null
    var password2Field: EditText? = null

    var pb: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        pb = findViewById(R.id.pb)
    }
    private fun String.isEmailValid(): Boolean {
        return !TextUtils.isEmpty(this) && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
    }
    private fun isDateValid(dateStr: String): Boolean {
        try {
            var formatter = SimpleDateFormat("dd.mm.yyyy", Locale.getDefault())
            val date = formatter.parse(dateStr)
            println(date)
        } catch (e: ParseException) {
            return false
        }
        return true
    }
    fun register(view: View) {
        pb?.visibility = View.VISIBLE
        nameField = findViewById(R.id.nameField)
        surnameField = findViewById(R.id.surnameField)
        dateField = findViewById(R.id.dateField)
        emailField = findViewById(R.id.emailField)
        countryField = findViewById(R.id.countryField)
        cityField = findViewById(R.id.cityField)
        password1Field = findViewById(R.id.password1Field)
        password2Field = findViewById(R.id.password2Field)

        if(nameField?.text.toString() == "") {
            Toast.makeText(this, R.string.register_name_error, Toast.LENGTH_SHORT).show()
            return
        }
        if(dateField?.text.toString().isNotEmpty()) {
            if(!isDateValid(dateField?.text.toString())) {
                Toast.makeText(this, R.string.register_date_error, Toast.LENGTH_SHORT).show()
                return
            }
        }
        if(!emailField?.text.toString().isEmailValid()) {
            Toast.makeText(this, R.string.register_email_error, Toast.LENGTH_SHORT).show()
            return
        }
        if(password1Field?.text.toString().length < 8) {
            Toast.makeText(this, R.string.register_password_small_error, Toast.LENGTH_SHORT).show()
            return
        }
        if(password1Field?.text.toString() != password2Field?.text.toString()) {
            Toast.makeText(this, R.string.register_passwords_not_match_error, Toast.LENGTH_SHORT).show()
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
        val json = "{\"name\": \"${nameField?.text.toString()}\", " +
                "\"surname\": \"${surnameField?.text.toString()}\", " +
                "\"age\": \"${dateField?.text.toString()}\", " +
                "\"email\": \"${emailField?.text.toString()}\"," +
                "\"country\": \"${countryField?.text.toString()}\"," +
                "\"city\": \"${cityField?.text.toString()}\"," +
                "\"password\": \"${password1Field?.text.toString()}\"}"
        val result = Utils.request(this, "register", "POST", false, json)
        Utils().saveUserData(result, filesDir)
        Utils.init(this, JSONObject(result).getString("userId"))
        runHomeActivity()
    }

    private fun runHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
    }
}