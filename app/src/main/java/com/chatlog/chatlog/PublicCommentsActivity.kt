package com.chatlog.chatlog

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
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

    var userData: JSONObject? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)
        val util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        val postId = intent.getStringExtra("id")

        commentsList = findViewById(R.id.comments_list)
        val goBackButton = findViewById<Button>(R.id.go_back)
        val sendButton = findViewById<Button>(R.id.send)
        noComments = findViewById(R.id.no_comments)
        commentField = findViewById(R.id.comment_field)
        var commentsArray: ArrayList<Comment> = ArrayList()
        commentsArray.add(Comment("useless", "useless", "useless", "user.png"))
        if (postId != null) {
            getCommentsInBackground(commentsArray, postId)
            if(commentsArray.size == 0) {
                commentsList?.visibility = View.GONE
                noComments?.visibility = View.VISIBLE
            }
        }
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
            } catch(e: InterruptedException) {
                Log.e("TAG", "Все плохо $e")
            }
        }.start()
    }
    private fun getComments(comments: ArrayList<Comment>, id: String) {
        val json = URL(Constants().SITE_NAME + "public/comments/${id}").readText(Charsets.UTF_8)
        val commentsArray = JSONObject(json).getJSONArray("comments")
        for(i in 0 until commentsArray.length()) {
            comments.add(
                Comment(
                    commentsArray.getJSONObject(i).getString("userName"),
                    commentsArray.getJSONObject(i).getString("text"),
                    commentsArray.getJSONObject(i).getString("date"),
                    commentsArray.getJSONObject(i).getString("avatarUrl"),
                ))
        }
    }
    private fun sendComment(id: String, comments: ArrayList<Comment>) {
        val token = userData?.getString("token")
        val url = URL(Constants().SITE_NAME + "public/comment/$id")
        val connection = url.openConnection() as HttpsURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept-Charset", "utf-8")
        connection.setRequestProperty("Authorization", "Bearer $token")
        val json = "{\"text\": \"${commentField?.text.toString()}\"}"
        connection.outputStream.write(json.toByteArray())
        var data: Int = connection.inputStream.read()
        var result = ""
        var byteArr = byteArrayOf()
        while(data != -1) {
            result += data.toChar().toString()
            byteArr.plus(data.toByte())
            data = connection.inputStream.read()
        }
        Log.e("TAG", result)
        val responseComment = JSONObject(result).getJSONObject("comment")
        comments.add(Comment(
            responseComment.getString("userName"),
            responseComment.getString("text"),
            responseComment.getString("date"),
            responseComment.getString("avatarUrl")
        ))
        commentField?.setText("")
    }
}