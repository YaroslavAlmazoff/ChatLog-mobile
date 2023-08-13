package com.chatlog.chatlog

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.URL

class PublicActivity : AppCompatActivity() {
    var isAdmin = false
    var userData: JSONObject? = null

    var pb: ProgressBar? = null

    var postsList: RecyclerView? = null
    var posts: ArrayList<PublicPostItem> = ArrayList()

    var subscribersList: RecyclerView? = null
    var subscribers: ArrayList<Friend> = ArrayList()

    var notificationsList: RecyclerView? = null
    var notifications: ArrayList<PublicNotification> = ArrayList()

    var id: String? = null
    var name: String? = null
    var description: String? = null
    var avatarUrl: String? = null
    var bannerUrl: String? = null
    var admin: String? = null
    var isSubscriber: Boolean? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_public)

        val util = Utils()

        id = intent.getStringExtra("id")!!
        name = intent.getStringExtra("name")!!
        description = intent.getStringExtra("description")!!
        avatarUrl = intent.getStringExtra("avatarUrl")!!
        bannerUrl = intent.getStringExtra("bannerUrl")!!
        admin = intent.getStringExtra("admin")!!
        isSubscriber = intent.getBooleanExtra("isSubscriber", false)!!

        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        isAdmin = admin == userData?.getJSONObject("user")?.getString("_id")

        pb = findViewById(R.id.pb)

        findViewById<TextView>(R.id.public_name).text = intent.getStringExtra("name")
        if(intent.getStringExtra("description") != "") {
            val desc = findViewById<TextView>(R.id.public_description)
            desc.text = Utils.shortName(description!!, 70)
            desc.setOnClickListener {
                val intent = Intent(this, TextActivity::class.java)
                intent.putExtra("text", description)
                startActivity(intent)
            }
            desc.visibility = View.VISIBLE
        }
        //findViewById<TextView>(R.id.already_subscribed).visibility = if(isSubscriber!!) View.VISIBLE else View.GONE

        findViewById<View>(R.id.admin_panel).visibility = if(isAdmin) View.VISIBLE else View.GONE
        val notificationIcon = findViewById<ImageView>(R.id.notification_icon)
        val createPostIcon = findViewById<ImageView>(R.id.create_post_icon)
        val editPublicIcon = findViewById<ImageView>(R.id.edit_icon)


        val subscribeButton = findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.subscribe_button)
        subscribeButton.text = if(isSubscriber!!) resources.getString(R.string.unscribe) else resources.getString(R.string.subscribe)
        val avatar = findViewById<ImageView>(R.id.public_avatar)
        if(avatar != null && avatarUrl != "") {
            Picasso.get().load(Constants().SITE_NAME_FILES + "/publicavatars/${avatarUrl}").into(avatar)
            avatar?.scaleType = ImageView.ScaleType.CENTER_CROP
        }

        Thread {
            try {
                val bitmap = BitmapFactory.decodeStream(URL(Constants().SITE_NAME_FILES + "/publicbanners/${bannerUrl}").content as InputStream)
                val banner: Drawable = BitmapDrawable(resources, bitmap)
                runOnUiThread {
                    findViewById<View>(R.id.public_head)?.background = banner
                }
            } catch (e: InterruptedException) {
                Log.e("TAG", "Error")
            }
        }.start()

        notificationsList = findViewById(R.id.public_notifications)
        subscribersList = findViewById(R.id.subscribers_list)
        postsList = findViewById(R.id.public_posts)

        subscribersList?.adapter = FriendsAdapter(subscribers)
        val lm = LinearLayoutManager(this)
        lm.orientation = LinearLayoutManager.HORIZONTAL
        subscribersList?.layoutManager = lm



        notificationsList?.adapter = PublicNotificationsAdapter(notifications)
        notificationsList?.layoutManager = LinearLayoutManager(this)

        postsList?.adapter = PublicPostsAdapter(posts, userData!!, isAdmin, id!!, this)
        postsList?.layoutManager = LinearLayoutManager(this)

        notificationIcon.setOnClickListener {
            if(notificationsList?.visibility == View.GONE) {
                notificationsList?.visibility = View.VISIBLE
            } else {
                notificationsList?.visibility = View.GONE
            }
        }
        createPostIcon.setOnClickListener {
            val intent = Intent(this, CreatePublicPostActivity::class.java)
            intent.putExtra("id", id)
            startActivity(intent)
        }
        editPublicIcon.setOnClickListener {
            val intent = Intent(this, UpdatePublicActivity::class.java)
            intent.putExtra("id", id)
            startActivity(intent)
        }


        if (id != null) {
            getDataInBackground()
        }
    }

    fun getDataInBackground() {
        Thread {
            try {
                getSubscribers()
                getNotifications()
                getPosts()
            } catch(e: InterruptedException) {
                Log.e("TAG", "Error")
            }
        }.start()
    }


    fun getSubscribers() {
        val subscribersData = Utils.request(this,  "public/subscribers/$id", "GET", true, null)
        val subscribersArray = JSONObject(subscribersData).getJSONArray("subscribers")
        Log.e("TAG", subscribersData)
        for(i in 0 until subscribersArray.length()) {
            subscribers.add(
                Friend(
                subscribersArray.getJSONObject(i).getString("name"),
                subscribersArray.getJSONObject(i).getString("avatarUrl"),
                subscribersArray.getJSONObject(i).getString("_id"),
            )
            )
        }

        runOnUiThread {
            subscribersList?.adapter?.notifyDataSetChanged()
            findViewById<TextView>(R.id.public_subscribers_count).text = resources.getString(R.string.subscribers) + " ${subscribers.size}"
        }
    }

    fun getNotifications() {
        val notificationsData = Utils.request(this, "public/notifications/$id", "GET", true, null)
        val notificationsArray = JSONObject(notificationsData).getJSONArray("notifications")
        Log.e("TAG", notificationsData)
        for(i in 0 until notificationsArray.length()) {
            notifications.add(
                PublicNotification(
                    notificationsArray.getJSONObject(i).getString("text"),
                    notificationsArray.getJSONObject(i).getString("public"),
                )
            )
        }

        notifications.reverse()

        runOnUiThread {
            notificationsList?.adapter?.notifyDataSetChanged()
        }
    }

    fun getPosts() {
        val postsData = Utils.request(this, "public/posts-mobile/$id/${userData?.getJSONObject("user")?.getString("_id")}", "GET", true, null)
        val postsArray = JSONObject(postsData).getJSONArray("posts")
        Log.e("TAG", postsData)
        for(i in 0 until postsArray.length()) {
            posts.add(
                PublicPostItem(
                    postsArray.getJSONObject(i).getString("title"),
                    postsArray.getJSONObject(i).getString("text"),
                    postsArray.getJSONObject(i).getString("date"),
                    name!!,
                    avatarUrl!!,
                    if(postsArray.getJSONObject(i).getJSONArray("images").length() > 0) postsArray.getJSONObject(i).getJSONArray("images").getString(0) else "",
                    postsArray.getJSONObject(i).getInt("likes"),
                    postsArray.getJSONObject(i).getInt("comments"),
                    postsArray.getJSONObject(i).getBoolean("liked"),
                    postsArray.getJSONObject(i).getJSONArray("images"),
                    postsArray.getJSONObject(i).getString("_id"),
                    postsArray.getJSONObject(i).getString("public"),

                )
            )
        }

        posts.reverse()

        runOnUiThread {
            postsList?.adapter?.notifyDataSetChanged()
            pb?.visibility = View.GONE
        }
    }
}