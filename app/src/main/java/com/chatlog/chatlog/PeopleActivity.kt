package com.chatlog.chatlog

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.net.URL


class PeopleActivity : AppCompatActivity() {
    var usersList: RecyclerView? = null
    var pb: ProgressBar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_people)
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
        usersList = findViewById(R.id.users_list)
        var usersArray: ArrayList<User> = ArrayList()

        pb = findViewById(R.id.pb)

        usersArray.add(User("ChatLog", "Admin", "28.02.2022", "Russia", "Alexeevka", "user.png"))

        getUsersInBackground(usersArray)

        usersList?.adapter = PeopleAdapter(usersArray)
        usersList?.layoutManager = LinearLayoutManager(this)
    }
    private fun getUsersInBackground(users: ArrayList<User>) {
        Thread {
            try {
                getUsers(users)
                runOnUiThread {
                    usersList?.adapter?.notifyDataSetChanged()
                    pb?.visibility = View.GONE
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "все плохо")
            }
        }.start()
    }
    private fun getUsers(users: ArrayList<User>) {
        val usersData = URL(Constants().SITE_NAME + "users").readText(Charsets.UTF_8)
        val usersArray = JSONObject(usersData).getJSONArray("users")
        Log.e("TAG", usersData)
        for(i in 0 until usersArray.length()) {
            users.add(User(
                usersArray.getJSONObject(i).getString("name"),
                usersArray.getJSONObject(i).getString("surname"),
                usersArray.getJSONObject(i).getString("age"),
                usersArray.getJSONObject(i).getString("country"),
                usersArray.getJSONObject(i).getString("city"),
                usersArray.getJSONObject(i).getString("avatarUrl"),
                usersArray.getJSONObject(i).getString("_id")
            ))
        }
    }
}