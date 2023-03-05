package com.chatlog.chatlog

import android.R.attr.data
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
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
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.timer


class HomeActivity : AppCompatActivity() {
    var weatherCity: TextView? = null
    var weatherText: TextView? = null
    var weatherImage: ImageView? = null

    var newsList: RecyclerView? = null
    var publicNewsList: RecyclerView? = null

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
        user = JSONObject(util.readUserFile(File(filesDir, util.userFileName))).getJSONObject("user")
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        checkToken()

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
        var newsArr: ArrayList<NewsItem> = ArrayList()

        publicNewsList = findViewById(R.id.home_news_p)
        var publicNewsArr: ArrayList<NewsItem> = ArrayList()

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


        getNewsInBackground(newsArr, true)
        getNewsInBackground(publicNewsArr, false)

        newsList?.adapter = HomeNewsAdapter(newsArr, userData!!)
        newsList?.layoutManager = LinearLayoutManager(this)
        publicNewsList?.adapter = PublicNewsAdapter(publicNewsArr, userData!!)
        publicNewsList?.layoutManager = LinearLayoutManager(this)
    }
    private fun getNewsInBackground(news: ArrayList<NewsItem>, isFriends: Boolean) {
        Thread {
            try {
                if(isFriends) {
                    getFriendsNews(news)
                    runOnUiThread {
                        newsList?.adapter?.notifyDataSetChanged()
                        pb?.visibility = View.GONE
                    }
                } else {
                    getPublicNews(news)
                    runOnUiThread {
                        publicNewsList?.adapter?.notifyDataSetChanged()
                    }
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
                if(newsArray.getJSONObject(i).getJSONArray("images").length() > 0) newsArray.getJSONObject(i).getJSONArray("images").getString(0) else "",
                newsArray.getJSONObject(i).getInt("likes"),
                newsArray.getJSONObject(i).getInt("comments"),
                newsArray.getJSONObject(i).getBoolean("liked"),
                newsArray.getJSONObject(i).getJSONArray("images"),
                newsArray.getJSONObject(i).getString("_id")
            ))
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
                if(newsArray.getJSONObject(i).getJSONArray("images").length() > 0) newsArray.getJSONObject(i).getJSONArray("images").getString(0) else "",
                newsArray.getJSONObject(i).getInt("likes"),
                newsArray.getJSONObject(i).getInt("comments"),
                newsArray.getJSONObject(i).getBoolean("liked"),
                newsArray.getJSONObject(i).getJSONArray("images"),
                newsArray.getJSONObject(i).getString("_id")
                ))
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
    private fun checkToken() {
        Thread {
            try {
                Log.e("TAG", userData.toString())
                val token = userData?.getString("token")
                val url = URL(Constants().SITE_NAME + "verify")
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept-Charset", "utf-8")
                connection.setRequestProperty("Authorization", "Bearer $token")
                var data: Int = connection.inputStream.read()
                var result = ""
                var byteArr = byteArrayOf()
                while(data != -1) {
                    result += data.toChar().toString()
                    byteArr.plus(data.toByte())
                    data = connection.inputStream.read()
                }
                Log.e("TAG", result)
                if(!JSONObject(result).getBoolean("verified")) {
                    Utils().clearUserData(filesDir)
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                }
            } catch (e: InterruptedException) {
                Log.e("TAG", "Не удалось выполнить проверку")
            }
        }.start()
    }
}