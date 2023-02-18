package com.chatlog.chatlog

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class UserActivity : AppCompatActivity() {
    var userName: TextView? = null
    var userDate: TextView? = null
    var userPlace: TextView? = null
    var userAvatar: ImageView? = null
    var userAboutMe: TextView? = null
    var userHead: View? = null

    var isOwner = false
    var userData: JSONObject? = null

    var isFriends = false
    var isRoomExists = false
    var friendsNotifications = false
    var isNotificationsDisplay = false

    var makeFriendsButton: Button? = null
    var writeMessageButton: Button? = null
    var alreadyInFriends: TextView? = null

    val friendsArray: ArrayList<Friend> = ArrayList()
    var notificationsList: RecyclerView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
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
        val userId = intent.getStringExtra("id")
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))
        userName = findViewById(R.id.user_name)
        userDate = findViewById(R.id.user_date)
        userPlace = findViewById(R.id.user_place)
        userAboutMe = findViewById(R.id.user_about_me)
        userAvatar = findViewById(R.id.user_avatar)
        userHead = findViewById(R.id.user_head)

        makeFriendsButton = findViewById(R.id.user_friends_button)
        writeMessageButton = findViewById(R.id.user_message_button)
        alreadyInFriends = findViewById(R.id.already_in_friends)

        val notificationIcon = findViewById<ImageView>(R.id.notification_icon)
        notificationsList = findViewById(R.id.user_notifications)
        val updateButton = findViewById<Button>(R.id.user_update_button)

        updateButton.setOnClickListener {
            val intent = Intent(this, UpdateProfileActivity::class.java)
            startActivity(intent)
        }

        notificationIcon.visibility = View.VISIBLE

        notificationIcon.setOnClickListener {
            if(isNotificationsDisplay) {
                notificationsList?.visibility = View.GONE
                isNotificationsDisplay = false
            } else {
                notificationsList?.visibility = View.VISIBLE
                isNotificationsDisplay = true
            }
        }

        makeFriendsButton?.setOnClickListener {
            if (userId != null) {
                makeFriendsInBackground(userId)
            }
        }
        writeMessageButton?.setOnClickListener {
            if (userId != null) {
                checkRoomsInBackground(userId)
            }
        }

        val notOwnerOptions = findViewById<View>(R.id.not_owner_options)
        val ownerOptions = findViewById<View>(R.id.owner_options)
        if (userId != null) {
            getUserInBackground(userId)
        }
        isOwner = JSONObject(util.readUserFile(File(filesDir, util.userFileName))).getJSONObject("user").getString("_id") == userId
        if(isOwner) {
            notOwnerOptions.visibility = View.GONE
            ownerOptions.visibility = View.VISIBLE
        } else {
            notOwnerOptions.visibility = View.VISIBLE
            ownerOptions.visibility = View.GONE
        }


        val friendsList = findViewById<RecyclerView>(R.id.friends_list)
        val c = Constants()
        friendsArray.add(Friend(c.HIDDEN_ITEM, "user.png", c.HIDDEN_ITEM))

        val lm = LinearLayoutManager(this)
        lm.orientation = LinearLayoutManager.HORIZONTAL
        friendsList?.layoutManager = lm
        val adapter = FriendsAdapter(friendsArray)
        friendsList?.adapter = adapter

        var notificationsArr: ArrayList<Notification> = ArrayList()

        notificationsList?.adapter = NotificationsAdapter(notificationsArr)
        notificationsList?.layoutManager = LinearLayoutManager(this)

        if (userId != null) {
            getFriendsInBackground(userId, friendsArray)
            checkFriendsInBackground(userId)
            getNotificationsInBackground(userId, notificationsArr)
            adapter.notifyDataSetChanged()
        }
    }
    private fun getUserInBackground(id: String) {
        Thread {
            try {
                getUser(id)
            } catch(e: InterruptedException){
                Log.e("TAG", "Не удалось загрузить пользователя")
            }
        }.start()
    }
    private fun getUser(id: String) {
        val userString = URL(Constants().SITE_NAME + "user/$id").readText(Charsets.UTF_8)
        Log.e("TAG", userString)
        if(userString == "{\"user\":null}") {
            Log.e("TAG", "backpressed")
            runOnUiThread {
                onBackPressed()
                finish()
            }
            return
        }
        val user = JSONObject(userString).getJSONObject("user")
        val bitmap = BitmapFactory.decodeStream(URL(Constants().SITE_NAME_FILES + "/userbanners/${user.getString("bannerUrl")}").content as InputStream)
        val banner: Drawable = BitmapDrawable(resources, bitmap)
        runOnUiThread {
            userName?.text = user.getString("name")
            userDate?.text = user.getString("age")
            if(user.getString("city") != "") {
                val unicode = 0x1F3E0
                userPlace?.text = Utils().getEmojiByUnicode(unicode) + user?.getString("city").toString()
            }
            if(user.getString("aboutMe").isNotEmpty()) {
                userAboutMe?.visibility = View.VISIBLE
                userAboutMe?.text = user.getString("aboutMe")
            }
            if(user.getString("avatarUrl") != null && user.getString("avatarUrl") != "") {
                Picasso.get().load(Constants().SITE_NAME_FILES + "/useravatars/${user.getString("avatarUrl")}").into(userAvatar)
                userAvatar?.scaleType = ImageView.ScaleType.CENTER_CROP
            }
            userHead?.background = banner
        }
    }
    private fun getFriendsInBackground(id: String, friends: ArrayList<Friend>) {
        Thread {
            try {
                getFriends(id, friends)
            } catch(e: InterruptedException){
                Log.e("TAG", "Не удалось загрузить друзей пользователя")
            }
        }.start()
    }
    private fun getFriends(id: String, friends: ArrayList<Friend>) {
        val friendsArray = URL(Constants().SITE_NAME + "userfriends/$id").readText(Charsets.UTF_8)
        val jsonFriends = JSONObject(friendsArray).getJSONArray("friends")
        for(i in 0 until jsonFriends.length()) {
            friends.add(Friend(jsonFriends.getJSONObject(i).getString("name"),
                jsonFriends.getJSONObject(i).getString("avatarUrl"),
                jsonFriends.getJSONObject(i).getString("_id")))
        }
        runOnUiThread {
            val friendsCount = findViewById<TextView>(R.id.user_friends_count)
            friendsCount.text = "Друзья " + jsonFriends.length().toString()
        }
    }
    private fun checkFriendsInBackground(id: String) {
        Thread {
            try {
                checkNotifications(id)
                checkFriends(id)
            } catch(e: InterruptedException){
                Log.e("TAG", "Не удалось загрузить друзей пользователя")
            }
        }.start()
    }
    private fun checkNotifications(id: String) {
        val token = userData?.getString("token")
        val url = URL(Constants().SITE_NAME + "checknotifications/$id")
        val connection = url.openConnection() as HttpsURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept-Charset", "utf-8")
        connection.setRequestProperty("Authorization", "Bearer $token")

        var data: Int = connection.inputStream.read()
        var result = ""
        while(data != -1) {
            result += data.toChar().toString()
            data = connection.inputStream.read()
        }
        Log.e("TAG", JSONObject(result).getBoolean("message").toString())
        friendsNotifications = JSONObject(result).getBoolean("message")
        if(friendsNotifications) {
            runOnUiThread {
                makeFriendsButton?.visibility = View.GONE
                alreadyInFriends?.visibility = View.VISIBLE
                alreadyInFriends?.setText(R.string.waiting_for_friends)
            }
        }
    }
    private fun checkFriends(id: String) {
        val token = userData?.getString("token")
        val url = URL(Constants().SITE_NAME + "checkfriends/$id")
        val connection = url.openConnection() as HttpsURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept-Charset", "utf-8")
        connection.setRequestProperty("Authorization", "Bearer $token")

        var data: Int = connection.inputStream.read()
        var result = ""
        while(data != -1) {
            result += data.toChar().toString()
            data = connection.inputStream.read()
        }
        Log.e("TAG", JSONObject(result).getBoolean("message").toString())
        isFriends = JSONObject(result).getBoolean("message")
        if(isFriends) {
            runOnUiThread {
                makeFriendsButton?.visibility = View.GONE
                alreadyInFriends?.visibility = View.VISIBLE
            }
        }
    }
    private fun makeFriendsInBackground(id: String) {
        Thread {
            try {
                makeFriends(id)
            } catch(e: InterruptedException){
                Log.e("TAG", "Не удалось загрузить друзей пользователя")
            }
        }.start()
    }
    private fun makeFriends(id: String) {
        val token = userData?.getString("token")
        val url = URL(Constants().SITE_NAME + "makefriends/$id")
        val connection = url.openConnection() as HttpsURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept-Charset", "utf-8")
        connection.setRequestProperty("Authorization", "Bearer $token")

        var data: Int = connection.inputStream.read()
        var result = ""
        while(data != -1) {
            result += data.toChar().toString()
            data = connection.inputStream.read()
        }
        runOnUiThread {
            Toast.makeText(this, R.string.friends_request, Toast.LENGTH_SHORT).show()
        }
    }
    private fun checkRoomsInBackground(id: String) {
        Thread {
            try {
                checkRooms(id)
            } catch(e: InterruptedException){
                Log.e("TAG", "Не удалось загрузить друзей пользователя")
            }
        }.start()
    }
    private fun checkRooms(id: String) {
        val token = userData?.getString("token")
        val url = URL(Constants().SITE_NAME + "checkrooms/$id")
        val connection = url.openConnection() as HttpsURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept-Charset", "utf-8")
        connection.setRequestProperty("Authorization", "Bearer $token")

        var data: Int = connection.inputStream.read()
        var result = ""
        while(data != -1) {
            result += data.toChar().toString()
            data = connection.inputStream.read()
        }
        isRoomExists = JSONObject(result).getBoolean("exists")
        if(isRoomExists) {
            val intent = Intent(this, MessengerActivity::class.java)
            intent.putExtra("id", JSONObject(result).getString("room"))
            startActivity(intent)
        } else {
            createRoom(id)
        }
    }
    private fun createRoom(id: String) {
        val token = userData?.getString("token")
        val url = URL(Constants().SITE_NAME + "createroom/$id")
        val connection = url.openConnection() as HttpsURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept-Charset", "utf-8")
        connection.setRequestProperty("Authorization", "Bearer $token")

        var data: Int = connection.inputStream.read()
        var result = ""
        while(data != -1) {
            result += data.toChar().toString()
            data = connection.inputStream.read()
        }
        getRoom(id, result)
    }
    private fun getRoom(id: String, resultData: String) {
        val token = userData?.getString("token")
        val url = URL(Constants().SITE_NAME + "getroom/$id")
        val connection = url.openConnection() as HttpsURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept-Charset", "utf-8")
        connection.setRequestProperty("Authorization", "Bearer $token")

        var data: Int = connection.inputStream.read()
        var result = ""
        while(data != -1) {
            result += data.toChar().toString()
            data = connection.inputStream.read()
        }
        val err = JSONObject(resultData).getInt("err")
        if(err == 1) {
            val intent = Intent(this, MessengerActivity::class.java)
            intent.putExtra("id", JSONObject(resultData).getJSONObject("room").getString("_id"))
            startActivity(intent)
        } else {
            val intent = Intent(this, MessengerActivity::class.java)
            intent.putExtra("id", JSONObject(result).getJSONObject("room").getString("_id"))
            startActivity(intent)
        }
    }
    private fun getNotificationsInBackground(id: String, notifications: ArrayList<Notification>) {
        Thread {
            try {
                getNotifications(id, notifications)
            } catch(e: InterruptedException) {
                Log.e("TAG", "все плохо")
            }
        }.start()
    }
    private fun getNotifications(id: String, notifications: ArrayList<Notification>) {
        val notificationsData = URL(Constants().SITE_NAME + "getnotifications/$id").readText(Charsets.UTF_8)
        Log.e("TAG", notificationsData)
        val notificationsArray = JSONObject(notificationsData).getJSONArray("notifications")
        Log.e("TAG", notificationsArray.toString())
        for(i in 0 until notificationsArray.length()) {
            notifications.add(Notification(
                notificationsArray.getJSONObject(i).getString("title"),
                notificationsArray.getJSONObject(i).getString("type"),
                notificationsArray.getJSONObject(i).getString("from"),
                notificationsArray.getJSONObject(i).getString("to"),
                notificationsArray.getJSONObject(i).getString("postType"),
                notificationsArray.getJSONObject(i).getString("postID")))
        }

    }
}