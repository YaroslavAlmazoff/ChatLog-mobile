package com.chatlog.chatlog

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class CommentsActivity : AppCompatActivity() {
    var commentsList: RecyclerView? = null
    var noComments: TextView? = null
    var commentField: EditText? = null

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

        val postId = intent.getStringExtra("id")

        pb = findViewById(R.id.pb)

        commentsList = findViewById(R.id.comments_list)
        val goBackButton = findViewById<Button>(R.id.go_back)
        val sendButton = findViewById<Button>(R.id.send)
        noComments = findViewById(R.id.no_comments)
        commentField = findViewById(R.id.comment_field)
        var commentsArray: ArrayList<Comment> = ArrayList()
        commentsArray.add(Comment("useless", "useless", "useless", "user.png"))
        if (postId != null) {
            pb?.visibility = View.VISIBLE
            getCommentsInBackground(commentsArray, postId)
            if(commentsArray.size == 0) {
                commentsList?.visibility = View.GONE
                noComments?.visibility = View.VISIBLE
            }
        }
        goBackButton.setOnClickListener {
            onBackPressed()
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

        commentsList?.smoothScrollBy(-commentsList!!.computeVerticalScrollOffset(), 0)

        val lm = LinearLayoutManager(this)
        lm.orientation = LinearLayoutManager.VERTICAL
        commentsList?.layoutManager = lm
        val adapter = CommentsAdapter(commentsArray)
        commentsList?.adapter = adapter
    }

    private fun getCommentsInBackground(comments: ArrayList<Comment>, id: String) {
        Thread {
            try {
                getComments(comments, id)
                runOnUiThread {
                    commentsList?.adapter?.notifyDataSetChanged()
                    pb?.visibility = View.GONE
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "Все плохо $e")
            }
        }.start()
    }
    private fun getComments(comments: ArrayList<Comment>, id: String) {
        val json = Utils.request(this, "/userpost/comments/${id}", "GET", false, null)
        val commentsArray = JSONObject(json).getJSONArray("comments")
        for(i in 0 until commentsArray.length()) {
            comments.add(
                Comment(
                commentsArray.getJSONObject(i).getString("userName"),
                commentsArray.getJSONObject(i).getString("comment"),
                commentsArray.getJSONObject(i).getString("date"),
                commentsArray.getJSONObject(i).getString("avatarUrl"),
            ))
        }
    }
    private fun sendComment(id: String, comments: ArrayList<Comment>) {
        val json = "{\"text\": \"${commentField?.text.toString()}\", \"date\": \"${Utils().getCurrentDate()}\"}"
        val result = Utils.request(this, "userpost/comment/$id", "POST", true, json)
        Log.e("TAG", result)
        val responseComment = JSONObject(result).getJSONObject("comment")
        comments.add(Comment(
            responseComment.getString("userName"),
            responseComment.getString("comment"),
            responseComment.getString("date"),
            responseComment.getString("avatarUrl")
        ))
        commentField?.setText("")
        commentsList?.adapter?.notifyDataSetChanged()
    }
}