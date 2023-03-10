package com.chatlog.chatlog

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class MainActivity : AppCompatActivity() {
    var user: String? = null
    var userData: JSONObject? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val util = Utils()
        user = util.readUserFile(File(filesDir, Utils().userFileName))
        if(user?.isNotEmpty() == true) {
            userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))
        }
        if(util.readUserFile(File(filesDir, util.userFileName)).isNotEmpty()) {
            try {
                if(JSONObject(user).getString("token") != null) {
                    runHomeActivity()
                }
            } catch(e: JSONException) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }

        var listView = findViewById<ListView>(R.id.capabilitiesList)
        var capabilitiesArr: ArrayList<Capability> = ArrayList()
        listView.adapter = CapabilitiesAdapter(applicationContext, capabilitiesArr)
        capabilitiesArr.add(Capability("Игры", R.drawable.games, "#9A38D6"))
        capabilitiesArr.add(Capability("Новости для геймеров", R.drawable.news, "#4D4DFF"))
        capabilitiesArr.add(Capability("Удобный мессенджер", R.drawable.messenger, "#B71DDE"))
        capabilitiesArr.add(Capability("Облачное хранилище", R.drawable.cloud, "#86ED26"))
        capabilitiesArr.add(Capability("Реклама игр", R.drawable.adverts, "#FF760D"))

        val appName = findViewById<TextView>(R.id.appName)
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.2f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.2f)
        val anim: ObjectAnimator = ObjectAnimator.ofPropertyValuesHolder(appName, scaleX, scaleY)
        anim.repeatCount = ValueAnimator.INFINITE
        anim.repeatMode = ValueAnimator.REVERSE
        anim.duration = 2000
        anim.start()
        Log.e("TAG", user!!)
    }
    fun runLoginActivity(view: View) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
    fun runRegisterActivity(view: View) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
    fun runSupportActivity(view: View) {
        val intent = Intent(this, SupportActivity::class.java)
        startActivity(intent)
    }
    fun runHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
    }
}