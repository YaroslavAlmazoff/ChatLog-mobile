package com.chatlog.chatlog

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.File
import java.net.URL

class PublicsActivity : AppCompatActivity() {
    var userData: JSONObject? = null
    var publicsList: RecyclerView? = null
    var pb: ProgressBar? = null
    var searchField: SearchView? = null
    var adapter: PublicsAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publics)

        searchField = findViewById(R.id.search_field)

        val initWidth = searchField?.width

        searchField?.setOnSearchClickListener {
            val layoutParams = searchField?.layoutParams
            layoutParams?.width = initWidth!! + 500 // change this value to adjust the width
            searchField?.layoutParams = layoutParams
        }
        searchField?.setOnCloseListener {
            val layoutParams = searchField?.layoutParams
            layoutParams?.width = 140 // change this value to adjust the width
            searchField?.layoutParams = layoutParams
            return@setOnCloseListener false
        }

        searchField?.setOnQueryTextListener(object : SearchView.OnQueryTextListener, SearchView.OnSuggestionListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    adapter?.filter(it)
                }
                return true
            }

            override fun onSuggestionClick(position: Int): Boolean {
                Log.e("TAG", "chatlog")
                return true
            }

            override fun onSuggestionSelect(position: Int): Boolean {
                Log.e("TAG", "chatlog")
                return true
            }
        })

        var util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        publicsList = findViewById(R.id.publics_list)
        var publicsArray: ArrayList<Public> = ArrayList()

        pb = findViewById(R.id.pb)

        getPublicsInBackground(publicsArray)
        adapter = PublicsAdapter(publicsArray, userData!!, this, this)
        publicsList?.adapter = adapter
        publicsList?.layoutManager = LinearLayoutManager(this)

        findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.create_button).setOnClickListener {
            startActivity(Intent(this, CreatePublicActivity::class.java))
        }
    }
    private fun getPublicsInBackground(publics: ArrayList<Public>) {
        Thread {
            try {
                getPublics(publics)
                runOnUiThread {
                    publicsList?.adapter?.notifyDataSetChanged()
                    pb?.visibility = View.GONE
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "все плохо")
            }
        }.start()
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun getPublics(publics: ArrayList<Public>) {
        val publicsData = Utils.request(this, "public/all/${userData?.getJSONObject("user")?.getString("_id")}", "GET", false, null)
        val publicsArray = JSONObject(publicsData).getJSONArray("publics")
        Log.e("TAG", publicsData)
        for(i in 0 until publicsArray.length()) {
            publics.add(Public(
                publicsArray.getJSONObject(i).getString("name"),
                publicsArray.getJSONObject(i).getString("description"),
                publicsArray.getJSONObject(i).getString("avatarUrl"),
                publicsArray.getJSONObject(i).getString("bannerUrl"),
                publicsArray.getJSONObject(i).getJSONArray("subscribers"),
                publicsArray.getJSONObject(i).getJSONArray("posts"),
                publicsArray.getJSONObject(i).getString("admin"),
                publicsArray.getJSONObject(i).getBoolean("isSubscriber"),
                publicsArray.getJSONObject(i).getString("_id"),
            ))
        }

        runOnUiThread {
            publicsList?.adapter?.notifyDataSetChanged()
            adapter?.filter("")
        }
    }
}