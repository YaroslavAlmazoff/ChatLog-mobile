package com.chatlog.chatlog

import android.R.attr.data
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.ContactsContract.Data
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.timer


class HomeActivity : AppCompatActivity() {
    var weatherCity: TextView? = null
    var weatherText: TextView? = null
    var weatherImage: ImageView? = null

    var newsList: RecyclerView? = null
    var publicNewsList: RecyclerView? = null

    var savedNewsList: RecyclerView? = null
    var savedNewsArray: ArrayList<NewsItem> = ArrayList()

    var newsArr: ArrayList<NewsItem> = ArrayList()
    var publicNewsArr: ArrayList<NewsItem> = ArrayList()

    val dbHelper = DatabaseHelper(this)

    var user: JSONObject? = null
    var userData: JSONObject? = null

    var pb: ProgressBar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
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

        val homeGreeting = findViewById<TextView>(R.id.home_greeting)
        val homeTime = findViewById<TextView>(R.id.home_time)
        val weather = findViewById<View>(R.id.weather)

        pb = findViewById(R.id.pb)

        weatherCity = findViewById(R.id.weather_city)
        weatherText = findViewById(R.id.weather_text)
        weatherImage = findViewById(R.id.weather_image)

        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))


        user = userData?.getJSONObject("user")

        val unicode = 0x1F3E0
        weatherCity?.text = util.getEmojiByUnicode(unicode) + user?.getString("city").toString()

        if(user?.getString("city")?.length!! > 0) {
            getInBackground()
            weather.visibility = View.VISIBLE
        } else {
            weather.visibility = View.GONE
        }

        homeGreeting.text = util.getDayTime(filesDir)
        val timer = object: CountDownTimer(1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                homeTime.text = util.getCurrentDate()
            }
            override fun onFinish() {
                this.start()
            }
        }
        timer.start()
        newsList = findViewById(R.id.home_news)
        savedNewsList = findViewById(R.id.saved_home_news)

        publicNewsList = findViewById(R.id.home_news_p)


        val friendsNewsSwitch = findViewById<TextView>(R.id.home_friends_news)
        val publicsNewsSwitch = findViewById<TextView>(R.id.home_publics_news)
        friendsNewsSwitch.setOnClickListener {
            publicNewsList?.visibility = View.GONE
            newsList?.visibility = View.VISIBLE
        }
        publicsNewsSwitch.setOnClickListener {
            newsList?.visibility = View.GONE
            publicNewsList?.visibility = View.VISIBLE
        }
        savedNewsList?.layoutManager = LinearLayoutManager(this)
        savedNewsList?.adapter = SavedPostsAdapter(savedNewsArray, userData!!, false, this)
        newsList?.layoutManager = LinearLayoutManager(this)
        newsList?.adapter = HomeNewsAdapter(newsArr, userData!!, false, this)
        publicNewsList?.layoutManager = LinearLayoutManager(this)
        publicNewsList?.adapter = PublicNewsAdapter(publicNewsArr, userData!!, this)


        getCacheInBackground()

        getNewsInBackground()
        getNewsInBackground()
    }

    private fun getCacheInBackground() {
        val newsArr = dbHelper.getNews(this)
        newsArr.reverse()
        for(i in 0 until newsArr.size) {
            val item = newsArr[i]
            savedNewsArray.add(
                NewsItem(
                    item.title,
                    item.date,
                    item.user,
                    item.userAvatar,
                    item.image,
                    item.likes,
                    item.comments, false, JSONArray(), "", "", ""
                )
            )
        }

        savedNewsList?.adapter?.notifyDataSetChanged()
        pb?.visibility = View.GONE
    }
    private fun getNewsInBackground() {
        Thread {
            try {
                getPublicNews()
                getFriendsNews()
            } catch(e: InterruptedException) {
                Log.e("TAG", "все плохо")
            }
        }.start()
    }
    private fun getFriendsNews() {
        val newsData = Utils.request(this, "ffn/${user?.getString("_id")}", "GET", false, null)
        val newsArray = JSONObject(newsData).getJSONArray("news")
        for(i in 0 until newsArray.length()) {
            newsArr.add(NewsItem(newsArray.getJSONObject(i).getString("title"),
                newsArray.getJSONObject(i).getString("date"),
                newsArray.getJSONObject(i).getString("userName"),
                newsArray.getJSONObject(i).getString("avatar"),
                if(newsArray.getJSONObject(i).getJSONArray("images").length() > 0) newsArray.getJSONObject(i).getJSONArray("images").getString(0) else "",
                newsArray.getJSONObject(i).getInt("likes"),
                newsArray.getJSONObject(i).getInt("comments"),
                newsArray.getJSONObject(i).getBoolean("liked"),
                newsArray.getJSONObject(i).getJSONArray("images"),
                newsArray.getJSONObject(i).getString("_id"),
                "",
                ""
            ))
        }

        runOnUiThread {
            newsList?.adapter?.notifyDataSetChanged()
            savedNewsList?.visibility = View.GONE
            newsList?.visibility = View.VISIBLE
        }
    }
    private fun getPublicNews() {
        val newsData = Utils.request(this, "fpn/${user?.getString("_id")}", "GET", false, null)
        val newsArray = JSONObject(newsData).getJSONArray("news")
        for(i in 0 until newsArray.length()) {
            publicNewsArr.add(NewsItem(newsArray.getJSONObject(i).getString("title"),
                newsArray.getJSONObject(i).getString("date"),
                newsArray.getJSONObject(i).getString("publicName"),
                newsArray.getJSONObject(i).getString("avatar"),
                if(newsArray.getJSONObject(i).getJSONArray("images").length() > 0) newsArray.getJSONObject(i).getJSONArray("images").getString(0) else "",
                newsArray.getJSONObject(i).getInt("likes"),
                newsArray.getJSONObject(i).getInt("comments"),
                newsArray.getJSONObject(i).getBoolean("liked"),
                newsArray.getJSONObject(i).getJSONArray("images"),
                newsArray.getJSONObject(i).getString("_id"),
                newsArray.getJSONObject(i).getString("public"),
                newsArray.getJSONObject(i).getString("admin")
                ))
        }
        runOnUiThread {
            publicNewsList?.adapter?.notifyDataSetChanged()
        }
    }
    private fun getInBackground() {
        Thread {
            try {
                getData()
            } catch (e: InterruptedException) {
                Log.e("TAG", "AAA")
            }
        }.start()
    }
    private fun getData() {
        val url = "https://api.openweathermap.org/data/2.5/weather"
        val city = Utils.request(this, "mobile/translit/${user?.getString("city")}", "GET", false, null)
        val weatherData = URL(url + "?q=${JSONObject(city).getString("message")}&units=metric&APPID=${Constants().WEATHER_API_KEY}").readText(Charsets.UTF_8)
        Log.e("TAG", weatherData)
        val resultWeather = "${JSONObject(weatherData).getJSONObject("main").getString("temp").toFloat().toInt()}, " +
                "${JSONObject(weatherData).getJSONArray("weather").getJSONObject(0).getString("main")}"
        runOnUiThread {
            weatherText?.text = resultWeather
            Picasso.get().load("https://openweathermap.org/img/wn/${JSONObject(weatherData).getJSONArray("weather").getJSONObject(0).getString("icon")}@2x.png").into(weatherImage)
        }
    }
}