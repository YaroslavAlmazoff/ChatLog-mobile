package com.chatlog.chatlog

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PublicPhotosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photos)
        initialize()
    }
    override fun onRestart() {
        super.onRestart()
        initialize()
    }
    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
        initialize()
    }
    private fun initialize() {
        val photosArrayData = intent.getStringArrayListExtra("photos")
        Log.e("TAG", photosArrayData.toString())
        val photosArray: ArrayList<Photo> = ArrayList()

        for(i in 0 until photosArrayData?.size!!) {
            photosArray.add(Photo(photosArrayData[i]))
        }

        val photosList = findViewById<RecyclerView>(R.id.photos_list)
        val goBackButton = findViewById<Button>(R.id.go_back)
        photosList.adapter = PhotosAdapter(photosArray, "publicposts")
        photosList.layoutManager = LinearLayoutManager(this)

        goBackButton.setOnClickListener {
            onBackPressed()
        }
    }
}