package com.chatlog.chatlog

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class LoadingActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var percentage: TextView
    private var progress: Int = 0

    private var count = 0

    val dbHelper: DatabaseHelper = DatabaseHelper(this)
    var userData: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        val util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        progressBar = findViewById(R.id.progress_bar)
        percentage = findViewById(R.id.percentage)

        updatePercentage(0)

        saveFileFromUrl()
    }

    private fun updatePercentage(number: Int) {
        progress += number
        progressBar.progress = progress
        percentage.text = "$progress%"
    }

    fun saveFileFromUrl() {
        val id = userData?.getJSONObject("user")?.getString("_id")
        Thread {
            val urlsArray = ArrayList<SavingImage>()

            val friendsData = Utils.request(this, "userfriends/$id", "GET", true, null)
            val postsData = Utils.request(this, "getuserpostsmobile/$id", "GET", true, null)
            val newsData = Utils.request(this, "ffn/$id", "GET", false, null)
            val gamesData = Utils.request(this, "games/games", "GET", false, null)
            val usersData = Utils.request(this, "load-users", "GET", false, null)

            Log.e("TAG", friendsData)
            Log.e("TAG", postsData)
            Log.e("TAG", gamesData)

            val friendsArray = JSONObject(friendsData).getJSONArray("friends")
            val postsArray = JSONObject(postsData).getJSONArray("posts")
            val usersArray = JSONObject(usersData).getJSONArray("users")
            val newsArray = JSONObject(newsData).getJSONArray("news")
            val gamesArray = JSONObject(gamesData).getJSONArray("games")

            for(i in 0 until friendsArray.length()) {
                val item = friendsArray.getJSONObject(i)
                urlsArray.add(SavingImage("useravatars", item.getString("avatarUrl")))
                dbHelper.addFriend(item.getString("name"), item.getString("avatarUrl"))
            }
            for(i in 0 until postsArray.length()) {
                val item = postsArray.getJSONObject(i)
                val images = item.getJSONArray("images")
                val hasImage = images.length() > 0
                dbHelper.addPost(
                    item.getString("title"),
                    item.getString("date"),
                    if(hasImage) images.getString(0) else ""
                )
                if(hasImage) urlsArray.add(SavingImage("articles", images.getString(0)))
            }
            for(i in 0 until newsArray.length()) {
                val item = newsArray.getJSONObject(i)
                val images = item.getJSONArray("images")
                val hasImage = images.length() > 0
                dbHelper.addNews(
                    item.getString("title"),
                    item.getString("date"),
                    item.getString("userName"),
                    if(hasImage) images.getString(0) else "",
                    item.getString("avatar")
                )
                if(hasImage) urlsArray.add(SavingImage("articles", images.getString(0)))
                urlsArray.add(SavingImage("useravatars", item.getString("avatar")))
            }

            for(i in 0 until usersArray.length()) {
                val item = usersArray.getJSONObject(i)
                dbHelper.addUser(
                    item.getString("name"),
                    item.getString("surname"),
                    item.getString("avatarUrl")
                )
                urlsArray.add(SavingImage("useravatars", item.getString("avatarUrl")))
            }

            for(i in 0 until gamesArray.length()) {
                val item = gamesArray.getJSONObject(i)
                dbHelper.addGame(
                    item.getString("name"),
                    item.getString("previewUrl")
                )
                urlsArray.add(SavingImage("gamepreviews", item.getString("previewUrl")))
            }

            urlsArray.add(SavingImage("useravatars", userData?.getJSONObject("user")?.getString("avatarUrl")!!))
            urlsArray.add(SavingImage("userbanners", userData?.getJSONObject("user")?.getString("bannerUrl")!!))


            for(i in 0 until urlsArray.size) {
                val item = urlsArray[i]
                val connection = URL(Constants().SITE_NAME_FILES + "/${item.type}/" + item.name).openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val inputStream = connection.inputStream

                val outputStream = openFileOutput(item.name, Context.MODE_PRIVATE)

                val buffer = ByteArray(1024)
                var bytesRead = inputStream.read(buffer)
                while (bytesRead != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    bytesRead = inputStream.read(buffer)
                }

                outputStream.close()
                inputStream.close()
                Log.e("TAG", "file saved")
                runOnUiThread { updatePercentage(i) }
            }
            startActivity(Intent(this, HomeActivity::class.java))
        }.start()
    }
}