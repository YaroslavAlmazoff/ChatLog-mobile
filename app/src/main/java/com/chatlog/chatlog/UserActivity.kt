package com.chatlog.chatlog

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.io.InputStream
import java.net.URL


class UserActivity : AppCompatActivity() {
    var userName: TextView? = null
    var userDate: TextView? = null
    var userPlace: TextView? = null
    var userAvatar: ImageView? = null
    var userAboutMe: TextView? = null
    var userHead: View? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        userName = findViewById(R.id.user_name)
        userDate = findViewById(R.id.user_date)
        userPlace = findViewById(R.id.user_place)
        userAboutMe = findViewById(R.id.user_about_me)
        userAvatar = findViewById(R.id.user_avatar)
        userHead = findViewById(R.id.user_head)

        val userId = intent.getStringExtra("id")
        if (userId != null) {
            getUserInBackground(userId)
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
            userAboutMe?.text = user.getString("aboutMe")
            if(user.getString("avatarUrl") != null && user.getString("avatarUrl") != "") {
                Picasso.get().load(Constants().SITE_NAME_FILES + "/useravatars/${user.getString("avatarUrl")}").into(userAvatar)
                userAvatar?.scaleType = ImageView.ScaleType.CENTER_CROP
            }
            userHead?.background = banner
        }
    }
}