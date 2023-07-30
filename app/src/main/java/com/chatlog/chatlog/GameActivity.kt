package com.chatlog.chatlog

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Im
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.io.File
import java.net.URL

class GameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        val id = intent.getStringExtra("id")!!
        val title = intent.getStringExtra("title")!!
        val description = intent.getStringExtra("description")!!
        val version = intent.getStringExtra("version")!!
        val previewUrl = intent.getStringExtra("previewUrl")!!
        val downloadUrl = intent.getStringExtra("downloadUrl")!!

        findViewById<TextView>(R.id.game_title).text = title
        findViewById<TextView>(R.id.game_description).text = description
        findViewById<TextView>(R.id.game_version).text = "Версия $version"

        val preview = findViewById<ImageView>(R.id.game_avatar)

        if(previewUrl != "") {
            Picasso.get().load(Constants().SITE_NAME_FILES + "/gamepreviews/${previewUrl}").into(preview)
            preview?.scaleType = ImageView.ScaleType.CENTER_CROP
        }

        findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.download_button).setOnClickListener {
            Utils.downloadFile(title, Constants().SITE_NAME_FILES + "/gamedownloads/$downloadUrl", this, "") {
                Toast.makeText(this, R.string.downloaded, Toast.LENGTH_LONG).show()
                it.visibility = View.GONE
                findViewById<TextView>(R.id.game_downloaded).visibility = View.VISIBLE
            }
        }
    }
}