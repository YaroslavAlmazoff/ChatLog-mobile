package com.chatlog.chatlog

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.io.File
import java.net.URL

class HomeActivity : AppCompatActivity() {
    var weatherCity: TextView? = null
    var weatherText: TextView? = null
    var weatherImage: ImageView? = null

    var newsList: RecyclerView? = null
    var publicNewsList: RecyclerView? = null

    var user: JSONObject? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val util = Utils()

        val homeGreeting = findViewById<TextView>(R.id.home_greeting)
        val homeTime = findViewById<TextView>(R.id.home_time)
        val weather = findViewById<View>(R.id.weather)

        weatherCity = findViewById(R.id.weather_city)
        weatherText = findViewById(R.id.weather_text)
        weatherImage = findViewById(R.id.weather_image)
        user = JSONObject(util.readUserFile(File(filesDir, util.userFileName))).getJSONObject("user")


        val unicode = 0x1F3E0
        weatherCity?.text = util.getEmojiByUnicode(unicode) + user?.getString("city").toString()

        if(user?.getString("city")?.length!! > 0) {
            getInBackground()
            weather.visibility = View.VISIBLE
        } else {
            weather.visibility = View.GONE
        }

        homeGreeting.text = util.getDayTime(filesDir)
        val timer = object: CountDownTimer(1000000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                homeTime.text = util.getCurrentDate()
            }
            override fun onFinish() {
                homeTime.setText(R.string.home_default_time)
            }
        }
        timer.start()
        newsList = findViewById(R.id.home_news)
        var newsArr: ArrayList<NewsItem> = ArrayList()

        publicNewsList = findViewById(R.id.home_news_p)
        var publicNewsArr: ArrayList<NewsItem> = ArrayList()

        val friendsNewsSwitch = findViewById<TextView>(R.id.home_friends_news)
        val publicsNewsSwitch = findViewById<TextView>(R.id.home_publics_news)

        friendsNewsSwitch.setOnClickListener {
            getNewsInBackground(newsArr, true)
        }
        publicsNewsSwitch.setOnClickListener {
            getNewsInBackground(publicNewsArr, false)
        }

        getNewsInBackground(newsArr, true)
        getNewsInBackground(publicNewsArr, false)

        newsList?.adapter = HomeNewsAdapter(newsArr)
        newsList?.layoutManager = LinearLayoutManager(this)
        publicNewsList?.adapter = PublicNewsAdapter(publicNewsArr)
        publicNewsList?.layoutManager = LinearLayoutManager(this)
    }
    private fun getNewsInBackground(news: ArrayList<NewsItem>, isFriends: Boolean) {
        Thread {
            try {
                if(isFriends) {
                    getFriendsNews(news)
                } else {
                    getPublicNews(news)
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "все плохо")
            }
        }.start()
    }
    private fun getFriendsNews(news: ArrayList<NewsItem>) {
        Log.e("TAG", "bones")
        Log.e("TAG", user?.getString("_id").toString())
        runOnUiThread {
            publicNewsList?.visibility = View.GONE
            newsList?.visibility = View.VISIBLE
        }
        val newsData = URL(Constants().SITE_NAME + "ffn/${user?.getString("_id")}").readText(Charsets.UTF_8)
        val newsArray = JSONObject(newsData).getJSONArray("news")
        for(i in 0 until newsArray.length()) {
            news.add(NewsItem(newsArray.getJSONObject(i).getString("title"),
                newsArray.getJSONObject(i).getString("date"),
                newsArray.getJSONObject(i).getString("userName"),
                newsArray.getJSONObject(i).getString("avatar"),
                if(newsArray.getJSONObject(i).getJSONArray("images").length() > 0) newsArray.getJSONObject(i).getJSONArray("images").getString(0) else ""))
        }
    }
    private fun getPublicNews(news: ArrayList<NewsItem>) {
        Log.e("TAG", "sesh")
        Log.e("TAG", user?.getString("_id").toString())
        runOnUiThread {
            newsList?.visibility = View.GONE
            publicNewsList?.visibility = View.VISIBLE
        }
        val newsData = URL(Constants().SITE_NAME + "fpn/${user?.getString("_id")}").readText(Charsets.UTF_8)
        Log.e("TAG", newsData)
        val newsArray = JSONObject(newsData).getJSONArray("news")
        for(i in 0 until newsArray.length()) {
            news.add(NewsItem(newsArray.getJSONObject(i).getString("title"),
                newsArray.getJSONObject(i).getString("date"),
                newsArray.getJSONObject(i).getString("publicName"),
                newsArray.getJSONObject(i).getString("avatar"),
                if(newsArray.getJSONObject(i).getJSONArray("images").length() > 0) newsArray.getJSONObject(i).getJSONArray("images").getString(0) else ""))
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
        val city = URL(Constants().SITE_NAME + "mobile/translit/${user?.getString("city")}").readText(Charsets.UTF_8)
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