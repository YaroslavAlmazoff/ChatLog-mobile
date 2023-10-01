package com.chatlog.chatlog

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        if(!Utils.isInternetAvailable(this)) {
            startActivity(Intent(this, NoInternetConnectionActivity::class.java))
        } else {
            // Получение данных уведомления
            val extras = intent.extras
            if (extras != null) {
                val clickAction = extras.getString("click_action")

                if (clickAction == "MESSENGER") {
                    // Запуск нужной активности
                    val intent = Intent(this, MessengerActivity::class.java)
                    intent.putExtra("id", extras.getString("id"))
                    startActivity(intent)
                } else if (clickAction == "CHAT") {
                    // Запуск нужной активности
                    val intent = Intent(this, ChatMessengerActivity::class.java)
                    intent.putExtra("id", extras.getString("id"))
                    startActivity(intent)
                } else if (clickAction == "USER") {
                    // Запуск нужной активности
                    val intent = Intent(this, UserActivity::class.java)
                    intent.putExtra("id", extras.getString("id"))
                    startActivity(intent)
                } else if (clickAction == "POST") {
                    // Запуск нужной активности
                    val intent: Intent = if(extras.getString("type") == "public") {
                        Intent(this, PublicActivity::class.java)
                    } else {
                        Intent(this, UserActivity::class.java)
                    }
                    intent?.putExtra("id", extras.getString("id"))
                    startActivity(intent)
                }
                else if (clickAction == "CLOUD") {
                    val intent = Intent(this, CloudActivity::class.java)
                    startActivity(intent)
                }
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }


    }
}