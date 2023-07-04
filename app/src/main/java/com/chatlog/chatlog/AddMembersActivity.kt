package com.chatlog.chatlog

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.File
import java.net.URL

class AddMembersActivity : AppCompatActivity() {
    var userData: JSONObject? = null
    var usersList: RecyclerView? = null
    var addButton: com.sanojpunchihewa.glowbutton.GlowButton? = null
    var usersArray: ArrayList<User> = ArrayList()
    var members: ArrayList<String> = ArrayList()
    var pb: ProgressBar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_members)

        var util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        addButton = findViewById(R.id.add_button)
        usersList = findViewById(R.id.friends_list)
        pb = findViewById(R.id.pb)

        findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.back_button).setOnClickListener {
            val myIntent = Intent(this, DiscussionSettingsActivity::class.java)
            myIntent.putExtra("id", intent.getStringExtra("id")!!)
            startActivity(myIntent)
        }

        getMembers()

        val adapter = AddMembersAdapter(usersArray, intent.getStringExtra("id")!!, members, userData, this)
        usersList?.adapter = adapter
        usersList?.layoutManager = LinearLayoutManager(this)

        Log.e("TAG", "before getUsersInBackground")

        getUsersInBackground(adapter)

        adapter?.filter("")
    }

    private fun getUsersInBackground(adapter: AddMembersAdapter) {
        Thread {
            try {
                Log.e("TAG", "in getUsersInBackground")
                getUsers(adapter)
                runOnUiThread {
                    adapter?.notifyDataSetChanged()
                    pb?.visibility = View.GONE
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "все плохо")
            }
        }.start()
    }
    private fun getUsers(adapter: AddMembersAdapter) {
        Log.e("TAG", "in getUsers")
        val usersData = URL(Constants().SITE_NAME + "userfriends/${userData?.getJSONObject("user")?.getString("_id")}").readText(Charsets.UTF_8)
        val usersDataArray = JSONObject(usersData).getJSONArray("friends")
        Log.e("TAG", usersData)
        for(i in 0 until usersDataArray.length()) {
            usersArray.add(User(
                usersDataArray.getJSONObject(i).getString("name"),
                usersDataArray.getJSONObject(i).getString("surname"),
                usersDataArray.getJSONObject(i).getString("age"),
                usersDataArray.getJSONObject(i).getString("country"),
                usersDataArray.getJSONObject(i).getString("city"),
                usersDataArray.getJSONObject(i).getString("avatarUrl"),
                usersDataArray.getJSONObject(i).getString("_id")
            ))
        }
        runOnUiThread {
            adapter?.filter("")
        }
    }
    private fun getMembers() {
        Thread {
            try {
                val membersData = URL(Constants().SITE_NAME + "members/${intent.getStringExtra("id")!!}").readText(Charsets.UTF_8)
                Log.e("TAG", "before membersData")
                Log.e("TAG", membersData)
                val jsonMembers = JSONObject(membersData).getJSONArray("members")
                for(i in 0 until jsonMembers.length()) {
                    members.add(jsonMembers.getString(i))
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "Interrupted Exception")
            }
        }.start()
    }
}