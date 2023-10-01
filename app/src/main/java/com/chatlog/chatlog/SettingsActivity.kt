package com.chatlog.chatlog

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject

class SettingsActivity : AppCompatActivity() {
    var saveButton: com.sanojpunchihewa.glowbutton.GlowButton? = null
    var changePassword: View? = null
    var deleteProfile: View? = null
    var oldPassword: EditText? = null
    var newPassword: EditText? = null
    var currentMode: String? = "change"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        saveButton = findViewById(R.id.save_button)
        deleteProfile = findViewById(R.id.settings_delete_profile_view)
        changePassword = findViewById(R.id.settings_change_password_view)
        oldPassword = findViewById(R.id.old_password)
        newPassword = findViewById(R.id.new_password)

        changePassword?.setOnClickListener {
            saveButton?.text = "Сохранить изменения"
            oldPassword?.visibility = View.VISIBLE
            newPassword?.visibility = View.VISIBLE
            saveButton?.visibility = View.VISIBLE
            currentMode = "change"
        }

        deleteProfile?.setOnClickListener {
            saveButton?.text = "Удалить профиль"
            oldPassword?.visibility = View.VISIBLE
            saveButton?.visibility = View.VISIBLE
            currentMode = "delete"
        }

        saveButton?.setOnClickListener {
            if(currentMode == "change") {
                if(oldPassword?.text?.toString() == newPassword?.text?.toString()) {
                    Toast.makeText(this, "Пароли НЕ должны совпадать", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                Thread {
                    try {
                        val json = "{\"oldPassword\": \"${oldPassword?.text.toString()}\", " +
                                "\"newPassword\": \"${newPassword?.text.toString()}\"}"
                        val result = Utils.request(this, "change-password", "POST", true, json)
                        if(JSONObject(result).getString("error") != "") {
                            runOnUiThread {
                                oldPassword?.setText("")
                                Toast.makeText(this, JSONObject(result).getString("error"), Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Utils().clearUserData(filesDir)
                            startActivity(Intent(this, LoginActivity::class.java))
                        }
                    } catch (e: InterruptedException) {
                        Log.e("TAG", "Error")
                    }
                }.start()
            } else {
                Thread {
                    try {
                        val json = "{\"password\": \"${oldPassword?.text.toString()}\"}"
                        val result = Utils.request(this, "delete-profile-mobile", "POST", true, json)
                        if(JSONObject(result).getString("error") != "") {
                            runOnUiThread {
                                oldPassword?.setText("")
                                Toast.makeText(this, JSONObject(result).getString("error"), Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Utils().clearUserData(filesDir)
                            startActivity(Intent(this, MainActivity::class.java))
                        }
                    } catch (e: InterruptedException) {
                        Log.e("TAG", "Error")
                    }
                }.start()
            }
        }
    }
}