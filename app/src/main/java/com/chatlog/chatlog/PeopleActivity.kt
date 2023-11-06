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
import java.io.File
import java.net.URL


class PeopleActivity : AppCompatActivity() {
    var userData: JSONObject? = null
    var usersList: RecyclerView? = null
    var savedUsersList: RecyclerView? = null
    var savedUsersArray: ArrayList<User> = ArrayList()
    var pb: ProgressBar? = null
    var dbHelper = DatabaseHelper(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_people)

        var util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        savedUsersList = findViewById(R.id.saved_users_list)

        savedUsersList?.layoutManager = LinearLayoutManager(this)
        savedUsersList?.adapter = ShortPeopleAdapter(savedUsersArray, this)

        getCacheInBackground()

        initialize()
    }

    private fun getCacheInBackground() {
        val usersArr = dbHelper.getUsers()
        usersArr.reverse()
        for(i in 0 until usersArr.size) {
            val item = usersArr[i]
            savedUsersArray.add(
                User(
                    item.name,
                    item.surname,
                    item.birthDate,
                    item.country,
                    item.city,
                    item.avatarUrl,
                )
            )
        }

        savedUsersList?.adapter?.notifyDataSetChanged()
        pb?.visibility = View.GONE
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

        usersList?.layoutManager = LinearLayoutManager(this)
        usersList?.adapter = PeopleAdapter(usersArray, userData!!, this)
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
        val usersData = Utils.request(this, "users", "GET", false, null)
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
        runOnUiThread {
            usersList?.adapter?.notifyDataSetChanged()
            savedUsersList?.visibility = View.GONE
            usersList?.visibility = View.VISIBLE
        }
    }
}