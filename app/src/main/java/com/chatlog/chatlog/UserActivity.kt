package com.chatlog.chatlog

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.IllegalStateException
import java.lang.RuntimeException
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class UserActivity : AppCompatActivity() {
    var userName: TextView? = null
    var userDate: TextView? = null
    var userPlace: TextView? = null
    var userAvatar: ImageView? = null
    var userAboutMe: TextView? = null
    var userHead: View? = null

    var dbHelper: DatabaseHelper = DatabaseHelper(this)

    var isOwner = false
    var userData: JSONObject? = null

    var pb: ProgressBar? = null
    var avatarPb: ProgressBar? = null
    var bannerPb: ProgressBar? = null

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
    var friendsList: RecyclerView? = null
    var notificationsList: RecyclerView? = null

    var friendsArray: ArrayList<Friend> = ArrayList()
    var notificationsArray: ArrayList<Notification> = ArrayList()
    var postsArray: ArrayList<NewsItem> = ArrayList()

    var savedPostsList: RecyclerView? = null
    var savedPostsArray: ArrayList<NewsItem> = ArrayList()

    var currentMode = ""

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

    @SuppressLint("UseCompatLoadingForDrawables")
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
        val user = userData?.getJSONObject("user")
        pb = findViewById(R.id.pb)
        avatarPb = findViewById(R.id.avatar_pb)
        bannerPb = findViewById(R.id.banner_pb)

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
        isOwner = user?.getString("_id") == userId
        if(isOwner) {
            notOwnerOptions.visibility = View.GONE
            ownerOptions.visibility = View.VISIBLE
        } else {
            notOwnerOptions.visibility = View.VISIBLE
            ownerOptions.visibility = View.GONE
        }

        if (isOwner) {
            try {
                userAvatar?.setImageBitmap(Utils.getBitmapFromFile(this, user?.getString("avatarUrl")!!))
            } catch (e: RuntimeException) {
                userAvatar?.setImageDrawable(resources.getDrawable(R.drawable.gradient2))
            }

            userAvatar?.setOnClickListener {
                currentMode = "avatar"
                avatarPb?.visibility = View.VISIBLE
                selectImageLauncher.launch("image/*")
            }

            userHead?.setOnClickListener {
                currentMode = "banner"
                bannerPb?.visibility = View.VISIBLE
                selectImageLauncher.launch("image/*")
            }
            userName?.text = user?.getString("name") + " " + user?.getString("surname")
            userDate?.text = user?.getString("age")

            if(user?.getString("city")?.isNotEmpty()!!) {
                userPlace?.visibility = View.VISIBLE
                val unicode = 0x1F3E0
                userPlace?.text = Utils().getEmojiByUnicode(unicode) + user?.getString("city").toString()
            }

            try {
                val banner: Drawable = BitmapDrawable(resources, Utils.getBitmapFromFile(this, userData?.getJSONObject("user")?.getString("bannerUrl")!!))
                userHead?.background = banner
            } catch (e: RuntimeException) {
                userHead?.background = resources.getDrawable(R.drawable.gradient1)
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
        }
        val c = Constants()

        friendsList = findViewById(R.id.friends_list)
        postsList = findViewById(R.id.user_posts)
        notificationsList = findViewById(R.id.user_notifications)

        friendsArray.add(Friend(c.HIDDEN_ITEM, "user.png", c.HIDDEN_ITEM))

        val lm = LinearLayoutManager(this)
        lm.orientation = LinearLayoutManager.HORIZONTAL
        friendsList?.layoutManager = lm

        val adapter = FriendsAdapter(friendsArray, this, false)
        friendsList?.adapter = adapter

        notificationsList?.layoutManager = LinearLayoutManager(this)
        notificationsList?.adapter = NotificationsAdapter(notificationsArray, userData!!)

        postsList?.layoutManager = LinearLayoutManager(this)
        postsList?.adapter = HomeNewsAdapter(postsArray, userData!!, isOwner, this)

        savedPostsList = findViewById(R.id.saved_posts)

        savedPostsList?.layoutManager = LinearLayoutManager(this)
        savedPostsList?.adapter = SavedPostsAdapter(savedPostsArray, userData!!, isOwner, this)

        if(isOwner) {
            getCacheInBackground()
        }

        if (userId != null) {
            getAllInBackground(userId)
        }
    }
    private fun getCacheInBackground() {
        val postsArr = dbHelper.getPosts(this)
        postsArr.reverse()
        for(i in 0 until postsArr.size) {
            val item = postsArr[i]
            savedPostsArray.add(
                NewsItem(
                    item.title,
                    item.date,
                    item.user,
                    item.userAvatar,
                    item.image,
                    item.likes,
                    item.comments,
                    item.liked,
                    item.images,
                    item.id,
                    item.public,
                    item.admin
                )
            )
        }
        val friendsArr = dbHelper.getFriends()
        for(i in 0 until friendsArr.size) {
            val item = friendsArr[i]
            friendsArray.add(
                Friend(
                    item.name,
                    item.avatarUrl
                )
            )
        }

        savedPostsList?.adapter?.notifyDataSetChanged()
        friendsList?.adapter?.notifyDataSetChanged()
        pb?.visibility = View.GONE

        for(i in 0 until savedPostsArray.size) {
            Log.e("TAG", savedPostsArray[i].title)
        }
    }
    private fun getAllInBackground(id: String) {
        Thread {
            try {
                getUser(id)
                getFriends(id)
                checkNotifications(id)
                checkFriends(id)
                getNotifications(id)
                getPosts(id)
                runOnUiThread {
                    postsList?.adapter?.notifyDataSetChanged()
                    friendsList?.adapter?.notifyDataSetChanged()
                    pb?.visibility = View.GONE
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
            userName?.text = user.getString("name") + " " + user.getString("surname")
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
//            if(user.getString("avatarUrl") != null && user.getString("avatarUrl") != "") {
//                Picasso.get().load(Constants().SITE_NAME_FILES + "/useravatars/${user.getString("avatarUrl")}").into(userAvatar)
//                userAvatar?.scaleType = ImageView.ScaleType.CENTER_CROP
//            }
//            if(user.getString("bannerUrl") != "") {
//               Thread {
//                   val bitmap = BitmapFactory.decodeStream(URL(Constants().SITE_NAME_FILES + "/userbanners/${user.getString("bannerUrl")}").content as InputStream)
//                   val banner: Drawable = BitmapDrawable(resources, bitmap)
//                   runOnUiThread { userHead?.background = banner }
//               }.start()
//            }
        }
    }
    private fun getFriends(id: String) {
        friendsArray.clear()
        val friendsData = Utils.request(this, "userfriends/$id", "GET", true, null)
        val jsonFriends = JSONObject(friendsData).getJSONArray("friends")
        for(i in 0 until jsonFriends.length()) {
            friendsArray.add(Friend(jsonFriends.getJSONObject(i).getString("name"),
                jsonFriends.getJSONObject(i).getString("avatarUrl"),
                jsonFriends.getJSONObject(i).getString("_id")))
        }
        runOnUiThread {
            val friendsCount = findViewById<TextView>(R.id.user_friends_count)
            friendsCount.text = "Друзья " + jsonFriends.length().toString()
            friendsList?.adapter = FriendsAdapter(friendsArray, this, true)
            friendsList?.adapter?.notifyDataSetChanged()
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
    private fun getNotifications(id: String) {
        val notificationsData = Utils.request(this, "getnotifications/$id", "GET", true, null)
        Log.e("TAG", notificationsData)
        val notificationsArr = JSONObject(notificationsData).getJSONArray("notifications")
        Log.e("TAG", notificationsArr.toString())
        for(i in 0 until notificationsArr.length()) {
            notificationsArray.add(Notification(
                notificationsArr.getJSONObject(i).getString("title"),
                notificationsArr.getJSONObject(i).getString("type"),
                notificationsArr.getJSONObject(i).getString("from"),
                notificationsArr.getJSONObject(i).getString("to"),
                notificationsArr.getJSONObject(i).getString("postType"),
                notificationsArr.getJSONObject(i).getString("postID"),
                notificationsArr.getJSONObject(i).getString("_id")))
        }
        notificationsArray.reverse()
        runOnUiThread {
            notificationsList?.adapter?.notifyDataSetChanged()
        }
    }
    private fun getPosts(id: String) {
        val newsData = Utils.request(this, "getuserpostsmobile/$id", "GET", true, null)
        val newsArray = JSONObject(newsData).getJSONArray("posts")
        for(i in 0 until newsArray.length()) {
            postsArray.add(NewsItem(newsArray.getJSONObject(i).getString("title"),
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
        }
        runOnUiThread {
            postsList?.adapter?.notifyDataSetChanged()
            savedPostsList?.visibility = View.GONE
            postsList?.visibility = View.VISIBLE
        }
    }

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Обработка выбранного изображения
        if (uri != null) {
            Thread {
                val inputStream = contentResolver.openInputStream(uri)
                val file = File(cacheDir, "$currentMode.jpg") // Создаем временный файл
                file.createNewFile()
                val fos = FileOutputStream(file)

                inputStream?.copyTo(fos)
                val token = Utils.updateToken(this)
                updateProfile(currentMode, file, token)
            }.start()
        }
    }


    private fun updateProfile(mode: String, file: File, token: String) {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(Constants().SITE_NAME).client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
        val chatLogApi = retrofit.create(ChatLogApi::class.java)

        var requestFile1: RequestBody? = null
        var requestFile2: RequestBody? = null
        var body1: MultipartBody.Part? = null
        var body2: MultipartBody.Part? = null

        if(mode == "avatar") {
            requestFile1 = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file)
            body1 = MultipartBody.Part.createFormData("avatar", file.name, requestFile1)
        }
        if(mode == "banner") {
            requestFile2 = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file)
            body2 = MultipartBody.Part.createFormData("banner", file.name, requestFile2)
        }

        try {
            CoroutineScope(Dispatchers.IO).launch {
                val response = chatLogApi.updateImages(
                    mode == "avatar", mode == "banner",
                    body1, body2, "Bearer $token"
                )
                Log.e("TAG", response)
                val avatar = JSONObject(response).getString("avatarUrl")
                val banner = JSONObject(response).getString("bannerUrl")
                Log.e("TAG", avatar)
                Log.e("TAG", banner)
                Log.e("TAG", mode)
                Log.e("TAG", currentMode)
                if(mode == "avatar") {
                    Utils.saveFileFromUrl(applicationContext, Constants().SITE_NAME_FILES + "/useravatars/" + avatar, avatar) {
                        runOnUiThread {
                            userAvatar?.setImageBitmap(Utils.getBitmapFromFile(applicationContext, avatar))
                            avatarPb?.visibility = View.GONE
                        }
                    }
                }
                if(mode == "banner") {
                    Utils.saveFileFromUrl(applicationContext, Constants().SITE_NAME_FILES + "/userbanners/" + banner, banner) {
                        runOnUiThread {
                            userHead?.background = BitmapDrawable(resources, Utils.getBitmapFromFile(applicationContext, banner))
                            bannerPb?.visibility = View.GONE
                        }
                    }
                }
                currentMode = ""
            }
        } catch(e: IllegalStateException) {
            Log.e("TAG", "Ошибка но ничего страшного")
        }
    }
}