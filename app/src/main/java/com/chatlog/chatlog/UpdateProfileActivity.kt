package com.chatlog.chatlog

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.HttpsURLConnection

class UpdateProfileActivity : AppCompatActivity() {
    var nameField: EditText? = null
    var surnameField: EditText? = null
    var dateField: EditText? = null
    var emailField: EditText? = null
    var countryField: EditText? = null
    var cityField: EditText? = null
    var aboutMe: EditText? = null

    var userData: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_profile)

        nameField = findViewById(R.id.name_field)
        surnameField = findViewById(R.id.surname_field)
        dateField = findViewById(R.id.date_field)
        emailField = findViewById(R.id.email_field)
        countryField = findViewById(R.id.country_field)
        cityField = findViewById(R.id.city_field)
        aboutMe = findViewById(R.id.about_me)

        val addAvatar = findViewById<Button>(R.id.update_profile_add_avatar)
        val addBanner = findViewById<Button>(R.id.update_profile_add_banner)
        val updateButton = findViewById<Button>(R.id.update_profile_button)

        val util = Utils()
        val user = JSONObject(util.readUserFile(File(filesDir, util.userFileName))).getJSONObject("user")
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        nameField?.setText(user.getString("name"))
        surnameField?.setText(user.getString("surname"))
        dateField?.setText(user.getString("age"))
        emailField?.setText(user.getString("email"))
        countryField?.setText(user.getString("country"))
        cityField?.setText(user.getString("city"))
    }
    fun update(view: View) {
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
        Log.e("TAG", "Проверка прошла")
        updateInBackground()
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
    private fun updateInBackground() {
        Thread {
            try {
                sendData()
            } catch (e: InterruptedException) {
                Log.e("TAG", "AAA")
            }
        }.start()
    }
    private fun sendData() {
        val token = userData?.getString("token")
        val url = URL(Constants().SITE_NAME + "editprofile")
        val connection = url.openConnection() as HttpsURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer $token")
        val json = "{\"name\": \"${nameField?.text.toString()}\", " +
                "\"surname\": \"${surnameField?.text.toString()}\", " +
                "\"age\": \"${dateField?.text.toString()}\", " +
                "\"email\": \"${emailField?.text.toString()}\"," +
                "\"country\": \"${countryField?.text.toString()}\"," +
                "\"aboutMe\": \"${aboutMe?.text.toString()}\"," +
                "\"city\": \"${cityField?.text.toString()}\"}"
        connection.outputStream.write(json.toByteArray())
        var data: Int = connection.inputStream.read()
        var result = ""
        while(data != -1) {
            result += data.toChar().toString()
            data = connection.inputStream.read()
        }
        Utils().clearUserData(filesDir)
        runLoginActivity()
    }

    private fun runLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
}