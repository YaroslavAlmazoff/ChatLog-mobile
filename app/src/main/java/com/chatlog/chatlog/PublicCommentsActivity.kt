package com.chatlog.chatlog

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class PublicCommentsActivity : AppCompatActivity() {
    var commentsList: RecyclerView? = null
    var noComments: TextView? = null
    var commentField: EditText? = null

    var publicId: String = ""

    var userData: JSONObject? = null
    var pb: ProgressBar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)
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
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))
        pb = findViewById(R.id.pb)

        val postId = intent.getStringExtra("id")
        publicId = intent.getStringExtra("public")!!

        commentsList = findViewById(R.id.comments_list)
        val goBackButton = findViewById<Button>(R.id.go_back)
        val sendButton = findViewById<Button>(R.id.send)
        noComments = findViewById(R.id.no_comments)
        commentField = findViewById(R.id.comment_field)
        var commentsArray: ArrayList<Comment> = ArrayList()

        goBackButton.setOnClickListener {
            val intent = Intent(it.context, HomeActivity::class.java)
            it.context.startActivity(intent)
        }

        sendButton.setOnClickListener {
            Thread {
                try {
                    if (postId != null) {
                        sendComment(postId, commentsArray)
                    }
                } catch(e: InterruptedException) {
                    Log.e("TAG", "Комментарий не отправился")
                }
            }.start()
        }

        commentsList?.adapter = CommentsAdapter(commentsArray)
        commentsList?.layoutManager = LinearLayoutManager(this)

        //commentsList?.smoothScrollBy(-commentsList!!.computeVerticalScrollOffset(), 0)
        if (postId != null) {
            getCommentsInBackground(commentsArray, postId)
        }
    }

    private fun getCommentsInBackground(comments: ArrayList<Comment>, id: String) {
        Thread {
            try {
                getComments(comments, id)
                runOnUiThread {
                    pb?.visibility = View.GONE
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "Все плохо $e")
            }
        }.start()
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun getComments(comments: ArrayList<Comment>, id: String) {
        val result = Utils.request(this, "public/comments/$id", "GET", true, null)
        Log.e("TAG", result)
        val commentsArray = JSONObject(result).getJSONArray("comments")
        for(i in 0 until commentsArray.length()) {
            comments.add(
                Comment(
                    commentsArray.getJSONObject(i).getString("userName"),
                    commentsArray.getJSONObject(i).getString("text"),
                    commentsArray.getJSONObject(i).getString("date"),
                    commentsArray.getJSONObject(i).getString("avatarUrl"),
                ))
        }

        comments.reverse()

        runOnUiThread {
            commentsList?.adapter?.notifyDataSetChanged()
            if(commentsArray.length() == 0) {
                commentsList?.visibility = View.GONE
                noComments?.visibility = View.VISIBLE
            }
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun sendComment(id: String, comments: ArrayList<Comment>) {
        val json = "{\"text\": \"${commentField?.text.toString()}\", \"date\": \"${Utils().getCurrentDate()}\", \"pub\": \"$publicId\"}"
        val result = Utils.request(this, "public/comment/$id", "POST", true, json)
        Log.e("TAG", result)
        val responseComment = JSONObject(result).getJSONObject("comment")
        comments.add(0, Comment(
            responseComment.getString("userName"),
            responseComment.getString("text"),
            responseComment.getString("date"),
            responseComment.getString("avatarUrl")
        ))
        commentField?.setText("")

        runOnUiThread {
            commentsList?.adapter?.notifyDataSetChanged()
        }
    }
}