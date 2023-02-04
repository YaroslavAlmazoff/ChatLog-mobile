package com.chatlog.chatlog

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class NavigationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        val home = findViewById<View>(R.id.link_home)
        val profile = findViewById<View>(R.id.link_profile)
        val messenger = findViewById<View>(R.id.link_messenger)
        val people = findViewById<View>(R.id.link_people)
        val games = findViewById<View>(R.id.link_games)
        val videos = findViewById<View>(R.id.link_videos)
        val ads = findViewById<View>(R.id.link_ads)
        val adverts = findViewById<View>(R.id.link_adverts)
        val cloud = findViewById<View>(R.id.link_cloud)
        val quit = findViewById<View>(R.id.link_quit)

        home.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
        profile.setOnClickListener {
            val intent = Intent(this, MyProfileActivity::class.java)
            startActivity(intent)
        }
        messenger.setOnClickListener {
            val intent = Intent(this, MessengerActivity::class.java)
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
        ads.setOnClickListener {
            val intent = Intent(this, AdsActivity::class.java)
            startActivity(intent)
        }
        adverts.setOnClickListener {
            val intent = Intent(this, AdvertsActivity::class.java)
            startActivity(intent)
        }
        cloud.setOnClickListener {
            val intent = Intent(this, CloudActivity::class.java)
            startActivity(intent)
        }
        quit.setOnClickListener {
            Utils().clearUserData(filesDir)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}