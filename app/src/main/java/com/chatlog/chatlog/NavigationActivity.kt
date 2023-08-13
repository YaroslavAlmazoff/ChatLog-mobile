package com.chatlog.chatlog

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import org.json.JSONObject
import java.io.File

class NavigationActivity : AppCompatActivity() {
    var user: JSONObject? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        val home = findViewById<View>(R.id.link_home)
        val profile = findViewById<View>(R.id.link_profile)
        val messenger = findViewById<View>(R.id.link_messenger)
        val people = findViewById<View>(R.id.link_people)
        val games = findViewById<View>(R.id.link_games)
        val videos = findViewById<View>(R.id.link_videos)
//        val ads = findViewById<View>(R.id.link_ads)
//        val adverts = findViewById<View>(R.id.link_adverts)
        val cloud = findViewById<View>(R.id.link_cloud)
        val quit = findViewById<View>(R.id.link_quit)
        val publics = findViewById<View>(R.id.link_publics)
        val settings = findViewById<View>(R.id.link_settings)

        val util = Utils()
        user = JSONObject(util.readUserFile(File(filesDir, util.userFileName))).getJSONObject("user")

        home.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
        profile.setOnClickListener {
            val intent = Intent(this, UserActivity::class.java)
            intent.putExtra("id", user?.getString("_id"))
            startActivity(intent)
        }
        messenger.setOnClickListener {
            val intent = Intent(this, MessengerListActivity::class.java)
            startActivity(intent)
        }
        people.setOnClickListener {
            val intent = Intent(this, PeopleActivity::class.java)
            startActivity(intent)
        }
        games.setOnClickListener {
            val intent = Intent(this, GamesActivity::class.java)
            startActivity(intent)
        }
        videos.setOnClickListener {
            val intent = Intent(this, VideohostActivity::class.java)
            startActivity(intent)
        }
//        ads.setOnClickListener {
//            val intent = Intent(this, AdsActivity::class.java)
//            startActivity(intent)
//        }
//        adverts.setOnClickListener {
//            val intent = Intent(this, AdvertsActivity::class.java)
//            startActivity(intent)
//        }
        cloud.setOnClickListener {
            val intent = Intent(this, CloudActivity::class.java)
            startActivity(intent)
        }
        quit.setOnClickListener {
            Utils().clearUserData(filesDir)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        publics.setOnClickListener {
            val intent = Intent(this, PublicsActivity::class.java)
            startActivity(intent)
        }
        settings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }


        findViewById<TextView>(R.id.delete_profile).setOnClickListener {
            val url = "https://chatlog.ru"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }
}