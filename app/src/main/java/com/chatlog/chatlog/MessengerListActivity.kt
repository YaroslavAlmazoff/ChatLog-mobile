package com.chatlog.chatlog

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class MessengerListActivity : AppCompatActivity() {
    var roomsList: RecyclerView? = null
    var chatRoomsList: RecyclerView? = null
    var userData: JSONObject? = null
    var pb: ProgressBar? = null
    var pb2: ProgressBar? = null
    var searchField: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messenger_list)
        searchField = findViewById(R.id.search_field)

        val cancelIcon = searchField?.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        cancelIcon?.setColorFilter(Color.WHITE)
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
        val util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))
        roomsList = findViewById(R.id.rooms_list)
        chatRoomsList = findViewById(R.id.chatrooms_list)
        var roomsArray: ArrayList<Room> = ArrayList()
        var chatRoomsArray: ArrayList<ChatRoom> = ArrayList()

        pb = findViewById(R.id.pb)
        pb2 = findViewById(R.id.pb2)


        getRoomsInBackground(roomsArray, chatRoomsArray)
        val roomsAdapter = RoomsAdapter(roomsArray, applicationContext)
        val chatRoomsAdapter = ChatRoomsAdapter(chatRoomsArray, applicationContext)

        roomsList?.adapter = roomsAdapter
        roomsList?.layoutManager = LinearLayoutManager(this)

        chatRoomsList?.adapter = chatRoomsAdapter
        chatRoomsList?.layoutManager = LinearLayoutManager(this)

        searchField?.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                roomsAdapter.filter.filter(newText)
                return true
            }

        })
    }
    private fun getRoomsInBackground(rooms: ArrayList<Room>, chatRooms: ArrayList<ChatRoom>) {
        Thread {
            try {
                getRooms(rooms)
                runOnUiThread {
                    roomsList?.adapter?.notifyDataSetChanged()
                    pb?.visibility = View.GONE
                }
                getChatRooms(chatRooms)
                runOnUiThread {
                    chatRoomsList?.adapter?.notifyDataSetChanged()
                    pb2?.visibility = View.GONE
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "все плохо")
            }
        }.start()
    }
    private fun getRooms(rooms: ArrayList<Room>) {
        val token = userData?.getString("token")
        val url = URL(Constants().SITE_NAME + "getrooms-mobile")
        val connection = url.openConnection() as HttpsURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("Accept-Charset", "utf-8")
        connection.setRequestProperty("Authorization", "Bearer $token")

        val inputAsString = connection.inputStream.bufferedReader().use { it.readText() }
        Log.e("TAG", inputAsString)
        val roomsArray = JSONObject(inputAsString).getJSONArray("rooms")
        for(i in 0 until roomsArray.length()) {
            rooms.add(Room(
                roomsArray.getJSONObject(i).getString("name"),
                roomsArray.getJSONObject(i).getString("avatar"),
                roomsArray.getJSONObject(i).getString("lastMessage"),
                roomsArray.getJSONObject(i).getBoolean("read"),
                roomsArray.getJSONObject(i).getString("_id"),
            ))
        }
    }
    private fun getChatRooms(rooms: ArrayList<ChatRoom>) {
        val token = userData?.getString("token")
        val url = URL(Constants().SITE_NAME + "getchatrooms-mobile")
        val connection = url.openConnection() as HttpsURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("Accept-Charset", "utf-8")
        connection.setRequestProperty("Authorization", "Bearer $token")

        val inputAsString = connection.inputStream.bufferedReader().use { it.readText() }
        Log.e("TAG", inputAsString)
        val roomsArray = JSONObject(inputAsString).getJSONArray("rooms")
        Log.e("TAG", inputAsString)
        for(i in 0 until roomsArray.length()) {
            rooms.add(ChatRoom(
                roomsArray.getJSONObject(i).getString("name"),
                roomsArray.getJSONObject(i).getString("avatar"),
                roomsArray.getJSONObject(i).getString("lastMessage"),
                roomsArray.getJSONObject(i).getBoolean("read"),
                roomsArray.getJSONObject(i).getString("_id"),
            ))
        }
    }
}