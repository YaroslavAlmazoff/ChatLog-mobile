package com.chatlog.chatlog

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Im
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

class GameActivity : AppCompatActivity() {
    var userData: JSONObject? = null
    var reviewsList: RecyclerView? = null
    var reviewsArray: ArrayList<Comment> = ArrayList()
    var adapter: CommentsAdapter? = null

    var reviewsWrapper: View? = null
    var openReviews: TextView? = null
    var closeReviews: TextView? = null

    var openWriteReview: TextView? = null
    var closeWriteReview: TextView? = null
    var writeReview: View? = null
    var reviewField: TextView? = null
    var sendButton: com.sanojpunchihewa.glowbutton.GlowButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        var util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        reviewsWrapper = findViewById(R.id.reviews_wrapper)
        openReviews = findViewById(R.id.game_open_reviews)
        closeReviews = findViewById(R.id.reviews_cancel)

        openReviews?.setOnClickListener {
            reviewsWrapper?.visibility = View.VISIBLE
        }
        closeReviews?.setOnClickListener {
            reviewsWrapper?.visibility = View.GONE
        }

        openWriteReview = findViewById(R.id.open_write_review)
        closeWriteReview = findViewById(R.id.close_write_review)
        writeReview = findViewById(R.id.write_review)
        reviewField = findViewById(R.id.review_field)
        sendButton = findViewById(R.id.send_button)

        val id = intent.getStringExtra("id")!!
        val title = intent.getStringExtra("title")!!
        val description = intent.getStringExtra("description")!!
        val version = intent.getStringExtra("version")!!
        val previewUrl = intent.getStringExtra("previewUrl")!!
        val downloadUrl = intent.getStringExtra("downloadUrl")!!

        findViewById<TextView>(R.id.game_title).text = title
        findViewById<TextView>(R.id.game_description).text = description
        findViewById<TextView>(R.id.game_version).text = "Версия $version"

        val preview = findViewById<ImageView>(R.id.game_avatar)

        if(previewUrl != "") {
            Picasso.get().load(Constants().SITE_NAME_FILES + "/gamepreviews/${previewUrl}").into(preview)
            preview?.scaleType = ImageView.ScaleType.CENTER_CROP
        }

        findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.download_button).setOnClickListener {
            Utils.downloadFile("$title.apk", Constants().SITE_NAME_FILES + "/gamedownloads/$downloadUrl", this, "") {
                Toast.makeText(this, R.string.downloaded, Toast.LENGTH_LONG).show()
                it.visibility = View.GONE
                findViewById<TextView>(R.id.game_downloaded).visibility = View.VISIBLE
            }
        }

        openWriteReview?.setOnClickListener {
            writeReview?.visibility = View.VISIBLE
            closeWriteReview?.visibility = View.VISIBLE
            openWriteReview?.visibility = View.GONE
        }

        closeWriteReview?.setOnClickListener {
            writeReview?.visibility = View.GONE
            closeWriteReview?.visibility = View.GONE
            openWriteReview?.visibility = View.VISIBLE
        }

        reviewsList = findViewById(R.id.reviews)

        getReviewsInBackground(id)
        adapter = CommentsAdapter(reviewsArray)
        reviewsList?.adapter = adapter
        reviewsList?.layoutManager = LinearLayoutManager(this)

        val dividerItemDecoration = DividerItemDecoration(this, R.drawable.divider)
        reviewsList?.addItemDecoration(dividerItemDecoration)

        sendButton?.setOnClickListener {
            sendReview(id)
        }
    }

    fun getReviewsInBackground(id: String) {
        Thread {
            try {
                val usersData = Utils.request(this, "games/comments/$id", "GET", false, null)
                val usersArray = JSONObject(usersData).getJSONArray("comments")
                Log.e("TAG", usersData)
                for(i in 0 until usersArray.length()) {
                    reviewsArray.add(
                        Comment(
                            usersArray.getJSONObject(i).getString("userName"),
                            usersArray.getJSONObject(i).getString("comment"),
                            usersArray.getJSONObject(i).getString("date"),
                            usersArray.getJSONObject(i).getString("avatarUrl"),
                        )
                    )
                }

                reviewsArray.reverse()

                runOnUiThread {
                    adapter?.notifyDataSetChanged()
                    if(usersArray.length() > 0) {
                        openReviews?.visibility = View.VISIBLE
                        openReviews?.text = "Показать отзывы об игре (${usersArray.length()})"
                    }
                }
            } catch (e: InterruptedException) {
                Log.e("TAG", "Error")
            }
        }.start()
    }

    private fun sendReview(id: String) {
        if(reviewField?.text == "") {
            return
        }
        val name = userData?.getJSONObject("user")?.getString("name")
        val surname = userData?.getJSONObject("user")?.getString("surname")
        val avatarUrl = userData?.getJSONObject("user")?.getString("avatarUrl")
        reviewsArray?.add(0, Comment(name + surname, reviewField?.text.toString(), Utils().getCurrentDate(), avatarUrl!!))
        adapter?.notifyDataSetChanged()
        Thread {
            try {
                val token = userData?.getString("token")
                val url = URL(Constants().SITE_NAME + "games/comment/$id")
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept-Charset", "utf-8")
                connection.setRequestProperty("Authorization", "Bearer $token")

                val json = "{\"comment\": \"${reviewField?.text}\", \"date\": \"${Utils().getCurrentDate()}\"}"
                Log.e("TAG", json)
                connection.outputStream.write(json.toByteArray())

                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
                var line: String? = reader.readLine()
                var result = ""

                while (line != null) {
                    result += line
                    line = reader.readLine()
                }

                reader.close()
                connection.disconnect()
                Log.e("TAG", result)
                runOnUiThread {
                    reviewField?.text = ""
                }
            } catch (e: InterruptedException) {
                Log.e("TAG", "Error")
            }
        }.start()
    }
}