package com.chatlog.chatlog

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import org.json.JSONArray
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

    var pb: ProgressBar? = null

    var isFriends = false
    var isRoomExists = false
    var friendsNotifications = false
    var isNotificationsDisplay = false

    var makeFriendsButton: Button? = null
    var writeMessageButton: Button? = null
    var alreadyInFriends: TextView? = null
    var createPostButton: com.sanojpunchihewa.glowbutton.GlowButton? = null
    var removeFromFriends: com.sanojpunchihewa.glowbutton.GlowButton? = null

    var postsList: RecyclerView? = null

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
        val userFile = File(filesDir, util.userFileName)
        if(Utils().readUserFile(userFile) != "") {
            userData = JSONObject(util.readUserFile(userFile))
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        pb = findViewById(R.id.pb)
        userName = findViewById(R.id.user_name)
        userDate = findViewById(R.id.user_date)
        userPlace = findViewById(R.id.user_place)
        userAboutMe = findViewById(R.id.user_about_me)
        userAvatar = findViewById(R.id.user_avatar)
        userHead = findViewById(R.id.user_head)

        makeFriendsButton = findViewById(R.id.user_friends_button)
        writeMessageButton = findViewById(R.id.user_message_button)
        alreadyInFriends = findViewById(R.id.already_in_friends)
        removeFromFriends = findViewById(R.id.user_delete_friend)

        val createButton = findViewById<Button>(R.id.user_create_button)
        createButton.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            startActivity(intent)
        }


        val notificationIcon = findViewById<ImageView>(R.id.notification_icon)
        notificationsList = findViewById(R.id.user_notifications)
        val updateButton = findViewById<Button>(R.id.user_update_button)

        updateButton.setOnClickListener {
            val intent = Intent(this, UpdateProfileActivity::class.java)
            startActivity(intent)
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
        createPostButton?.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            startActivity(intent)
        }

        val notOwnerOptions = findViewById<View>(R.id.not_owner_options)
        val ownerOptions = findViewById<View>(R.id.owner_options)
        isOwner = JSONObject(util.readUserFile(File(filesDir, util.userFileName))).getJSONObject("user").getString("_id") == userId
        if(isOwner) {
            notOwnerOptions.visibility = View.GONE
            ownerOptions.visibility = View.VISIBLE
        } else {
            notOwnerOptions.visibility = View.VISIBLE
            ownerOptions.visibility = View.GONE
        }

        if (isOwner) {
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
        }


        val friendsList = findViewById<RecyclerView>(R.id.friends_list)
        val c = Constants()
        friendsArray.add(Friend(c.HIDDEN_ITEM, "user.png", c.HIDDEN_ITEM))
        var notificationsArr: ArrayList<Notification> = ArrayList()
        postsList = findViewById(R.id.user_posts)
        var postsArr: ArrayList<NewsItem> = ArrayList()

        if (userId != null) {
            getAllInBackground(userId, friendsArray, notificationsArr, postsArr, postsList!!, friendsList)
        }

        val lm = LinearLayoutManager(this)
        lm.orientation = LinearLayoutManager.HORIZONTAL
        friendsList?.layoutManager = lm
        val adapter = FriendsAdapter(friendsArray)
        friendsList?.adapter = adapter


        notificationsList?.adapter = NotificationsAdapter(notificationsArr, userData!!)
        notificationsList?.layoutManager = LinearLayoutManager(this)
        postsList?.adapter = HomeNewsAdapter(postsArr, userData!!, isOwner)
        postsList?.layoutManager = LinearLayoutManager(this)
    }
    private fun getAllInBackground(id: String, friends: ArrayList<Friend>, notifications: ArrayList<Notification>, posts: ArrayList<NewsItem>, postsList: RecyclerView, friendsList: RecyclerView) {
        Thread {
            try {
                getUser(id)
                getFriends(id, friends)
                checkNotifications(id)
                checkFriends(id)
                getNotifications(id, notifications)
                getPosts(posts, id)
                runOnUiThread {
                    pb?.visibility = View.GONE
                    postsList.adapter?.notifyDataSetChanged()
                    friendsList.adapter?.notifyDataSetChanged()
                }

                for(i in 0 until posts.size) {
                    Log.e("TAG", posts[i].title)
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", e.message!!)
            }
        }.start()
    }
    private fun getUser(id: String) {
        val userString = Utils.request(this,  "user/$id", "GET", true, null)
        Log.e("TAG", userString)
        val user = JSONObject(userString).getJSONObject("user")
        Log.e("TAG", user.getString("bannerUrl"))
        Log.e("TAG", user.getString("avatarUrl"))
        runOnUiThread {
            userName?.text = user.getString("name")
            if(user.getString("age").isNotEmpty()) {
                userDate?.visibility = View.VISIBLE
                userDate?.text = user.getString("age")
            }
            if(user.getString("city").isNotEmpty()) {
                userPlace?.visibility = View.VISIBLE
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
            if(user.getString("bannerUrl") != "") {
               Thread {
                   val bitmap = BitmapFactory.decodeStream(URL(Constants().SITE_NAME_FILES + "/userbanners/${user.getString("bannerUrl")}").content as InputStream)
                   val banner: Drawable = BitmapDrawable(resources, bitmap)
                   runOnUiThread { userHead?.background = banner }
               }.start()
            }
        }
    }
    private fun getFriends(id: String, friends: ArrayList<Friend>) {
        val friendsArray = Utils.request(this, "userfriends/$id", "GET", true, null)
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
    private fun checkNotifications(id: String) {
        val result = Utils.request(this, "checknotifications/$id", "GET", true, null)
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
        val result = Utils.request(this, "checkfriends/$id", "GET", true, null)
        Log.e("TAG", JSONObject(result).getBoolean("message").toString())
        isFriends = JSONObject(result).getBoolean("message")
        if(isFriends) {
            runOnUiThread {
                makeFriendsButton?.visibility = View.GONE
                removeFromFriends?.visibility = View.VISIBLE
                removeFromFriends?.setOnClickListener {
                    deleteFriendInBackground(id)
                }
            }
        }
    }

    private fun deleteFriendInBackground(id: String) {
        Thread {
            try {
                deleteFriend(id)
            } catch(e: InterruptedException) {
                Log.e("TAG", "Не удалось удалить из друзей")
            }
        }.start()
    }

    private fun deleteFriend(id: String) {
        val result = Utils.request(this, "deletefriend/$id", "DELETE", true, null)
        runOnUiThread {
            Toast.makeText(this, R.string.removed_from_friends, Toast.LENGTH_LONG).show()
            removeFromFriends?.visibility = View.GONE
            makeFriendsButton?.visibility = View.VISIBLE
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
        val result = Utils.request(this, "makefriends/$id", "GET", true, null)
        runOnUiThread {
            Toast.makeText(this, R.string.friends_request, Toast.LENGTH_SHORT).show()
            makeFriendsButton?.visibility = View.GONE
            alreadyInFriends?.visibility = View.VISIBLE
            alreadyInFriends?.setText(R.string.waiting_for_friends)
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
        val result = Utils.request(this, "checkrooms/$id", "GET", true, null)
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
        val result = Utils.request(this, "createroom/$id", "GET", true, null)
        getRoom(id, result)
    }
    private fun getRoom(id: String, resultData: String) {
        val result = Utils.request(this, "getroom/$id", "GET", true, null)
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
    private fun getNotifications(id: String, notifications: ArrayList<Notification>) {
        val notificationsData = Utils.request(this, "getnotifications/$id", "GET", true, null)
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
                notificationsArray.getJSONObject(i).getString("postID"),
                notificationsArray.getJSONObject(i).getString("_id")))
        }
        notifications.reverse()
    }
    private fun getPosts(posts: ArrayList<NewsItem>, id: String) {
        val newsData = Utils.request(this, "getuserpostsmobile/$id", "GET", true, null)
        Log.e("TAG", newsData)
        val newsArray = JSONObject(newsData).getJSONArray("posts")
        for(i in 0 until newsArray.length()) {
            posts.add(NewsItem(newsArray.getJSONObject(i).getString("title"),
                newsArray.getJSONObject(i).getString("date"),
                newsArray.getJSONObject(i).getString("userName"),
                newsArray.getJSONObject(i).getString("avatar"),
                if(newsArray.getJSONObject(i).getJSONArray("images").length() > 0) newsArray.getJSONObject(i).getJSONArray("images").getString(0) else "",
                newsArray.getJSONObject(i).getInt("likes"),
                newsArray.getJSONObject(i).getInt("comments"),
                newsArray.getJSONObject(i).getBoolean("liked"),
                newsArray.getJSONObject(i).getJSONArray("images"),
                newsArray.getJSONObject(i).getString("_id"), "", ""
            ))
            Log.e("TAG", newsArray.getJSONObject(i).getString("title"))
        }
    }
}