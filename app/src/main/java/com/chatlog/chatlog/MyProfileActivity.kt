package com.chatlog.chatlog

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import org.json.JSONObject

class MyProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)

        val json = intent.getStringExtra("user_data")
        Log.e("TAG", json!!)
        val user = JSONObject(json)



        val userInfo = findViewById<TextView>(R.id.user_info)
        userInfo.text = user.getString("name") + " ," + user.getString("surname") + ", " + user.getString("age")
    }
}