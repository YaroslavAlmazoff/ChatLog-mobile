package com.chatlog.chatlog

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tylerjroach.eventsource.EventSource
import com.tylerjroach.eventsource.EventSourceHandler
import com.tylerjroach.eventsource.MessageEvent
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class MessengerActivity : AppCompatActivity() {
    var messagesList: RecyclerView? = null
    var userData: JSONObject? = null
    var headText: TextView? = null
    var headDate: TextView? = null
    var pb: ProgressBar? = null
    var sendButton: com.sanojpunchihewa.glowbutton.GlowButton? = null
    var editButton: com.sanojpunchihewa.glowbutton.GlowButton? = null
    var messageField: EditText? = null
    var messagesArray: ArrayList<Message>? = null
    var editing: Boolean = false
    var currentMessageId: String = ""
    var currentMessageText: String = ""

    override fun onStop() {
        super.onStop()
        stopEventSource()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messenger)

        val util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))
        messagesList = findViewById(R.id.messages_list)
        headText = findViewById(R.id.head_text)
        headDate = findViewById(R.id.head_date)
        sendButton = findViewById(R.id.send_message)
        editButton = findViewById(R.id.edit_button)
        messageField = findViewById(R.id.message_field)

        pb = findViewById(R.id.pb)
        messagesArray = ArrayList()
        val adapter = MessagesAdapter(messagesArray!!, applicationContext, userData!!, messageField!!, editing, currentMessageId!!, currentMessageText!!, sendButton!!, editButton!!)
        sendButton?.setOnClickListener {
            sendMessageInBackground(adapter)
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(messageField?.windowToken, 0)
        }

        getMessagesInBackground(messagesArray!!)

        messagesList?.adapter = adapter
        messagesList?.layoutManager = LinearLayoutManager(this)

        startEventSource()
    }
    fun getMessagesInBackground(messages: ArrayList<Message>) {
        Thread {
            try {
                getUser()
                getMessages(messages)
                runOnUiThread {
                    messagesList?.adapter?.notifyDataSetChanged()
                    pb?.visibility = View.GONE
                    messagesArray?.count()?.minus(1)?.let { messagesList?.smoothScrollToPosition(it) }
                }
                if(messagesArray!![messagesArray?.count()?.minus(1)!!].user != null) {
                    if(userData?.getJSONObject("user")?.getString("_id") != messagesArray!![messagesArray?.count()?.minus(1)!!].user) {
                        read()
                    }
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "все плохо")
            }
        }.start()
    }
    fun getMessages(messages: ArrayList<Message>) {
        val usersData = URL(Constants().SITE_NAME + "getmessagesstart/${intent.getStringExtra("id")!!}").readText(Charsets.UTF_8)
        val usersArray = JSONObject(usersData).getJSONArray("messages")
        for(i in 0 until usersArray.length()) {
            messages.add(Message(
                usersArray.getJSONObject(i).getString("message"),
                usersArray.getJSONObject(i).getString("name"),
                usersArray.getJSONObject(i).getString("avatarUrl"),
                usersArray.getJSONObject(i).getString("date"),
                usersArray.getJSONObject(i).getString("user"),
                usersArray.getJSONObject(i).getString("room"),
                usersArray.getJSONObject(i).getBoolean("isNotReaded"),
                usersArray.getJSONObject(i).getBoolean("isFile"),
                usersArray.getJSONObject(i).getString("_id"),
                usersArray.getJSONObject(i).getString("imageUrl"),
                usersArray.getJSONObject(i).getString("videoUrl"),
                usersArray.getJSONObject(i).getString("audioUrl"),
                usersArray.getJSONObject(i).getJSONArray("readedThisMessage")
            ))
        }
    }
    fun getUser() {
        userData?.getJSONObject("user")?.getString("_id")?.let { Log.e("TAG", it) }
        intent.getStringExtra("id")?.let { Log.e("TAG", it) }
        val user = URL(Constants().SITE_NAME + "user2byroom/${userData?.getJSONObject("user")?.getString("_id")}/${intent.getStringExtra("id")!!}").readText(Charsets.UTF_8)
        runOnUiThread {
            headText?.text = JSONObject(user).getJSONObject("user").getString("name") + " " +
                    JSONObject(user).getJSONObject("user").getString("surname")
            headDate?.text = "В сети " + JSONObject(user).getJSONObject("user").getString("lastVisit")
        }
    }
    fun read() {
        URL(Constants().SITE_NAME + "read/${intent.getStringExtra("id")!!}").readText(Charsets.UTF_8)
    }
    fun sendMessageInBackground(adapter: MessagesAdapter) {
        Thread {
            try {
                sendMessage(adapter)
            } catch(e: InterruptedException) {
                Log.e("TAG", "все плохо")
            }
        }.start()
    }
    fun sendMessage(adapter: MessagesAdapter) {
        runOnUiThread {
            adapter.clear()
        }
        val token = userData?.getString("token")
        val url = URL(Constants().SITE_NAME + "new-messages/${intent.getStringExtra("id")!!}")
        val connection = url.openConnection() as HttpsURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept-Charset", "utf-8")
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Range", "bytes=100-")
        val json = "{\"message\": {" +
                "\"message\": \"${messageField?.text}\"," +
                "\"date\": \"${Utils().getCurrentDate()}\"," +
                "\"isFile\": false" +
                "}}"
        connection.outputStream.write(json.toByteArray())
        var data: Int = connection.inputStream.read()
        var result = ""
        var byteArr = byteArrayOf()
        while (data != -1) {
            result += data.toChar().toString()
            byteArr.plus(data.toByte())
            data = connection.inputStream.read()
        }
        messageField?.setText("")
    }
    private var sseHandler: SSEHandler? = SSEHandler()
    private var eventSource: EventSource? = null
    private fun startEventSource() {
        eventSource = EventSource.Builder("https://chatlog.ru/api/connect/${intent.getStringExtra("id")}")
            .eventHandler(sseHandler)
            .build()
        eventSource?.connect()
    }

    private fun stopEventSource() {
        if (eventSource != null) eventSource!!.close()
        sseHandler = null
    }

    inner class SSEHandler : EventSourceHandler {
        override fun onConnect() {
            Log.e("TAG", "True")
        }

        override fun onMessage(event: String, message: MessageEvent) {
            Log.e("TAG", "message")
            Log.e("TAG", JSONArray(message.data).getString(JSONArray(message.data).length() - 1))
            val messages = JSONArray(message.data)
            for(i in 0 until messages.length()) {
                messagesArray?.add(Message(
                    messages.getJSONObject(i).getString("message"),
                    messages.getJSONObject(i).getString("name"),
                    messages.getJSONObject(i).getString("avatarUrl"),
                    messages.getJSONObject(i).getString("date"),
                    messages.getJSONObject(i).getString("user"),
                    messages.getJSONObject(i).getString("room"),
                    messages.getJSONObject(i).getBoolean("isNotReaded"),
                    messages.getJSONObject(i).getBoolean("isFile"),
                    messages.getJSONObject(i).getString("_id"),
                    messages.getJSONObject(i).getString("imageUrl"),
                    messages.getJSONObject(i).getString("videoUrl"),
                    messages.getJSONObject(i).getString("audioUrl"),
                    messages.getJSONObject(i).getJSONArray("readedThisMessage")
                ))
            }
            runOnUiThread {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(messageField?.windowToken, 0)
                messageField?.setText("")
                messagesArray?.count()?.minus(1)?.let { messagesList?.smoothScrollToPosition(it) }
                messagesList?.adapter?.notifyDataSetChanged()
            }
            if(messagesArray!![messagesArray?.count()?.minus(1)!!].user != null) {
                if(userData?.getJSONObject("user")?.getString("_id") != messagesArray!![messagesArray?.count()?.minus(1)!!].user) {
                    read()
                }
            }
        }

        override fun onComment(comment: String) {
            Log.e("TAG", comment)
        }

        override fun onError(t: Throwable) {
            Log.e("TAG", t.toString())
        }

        override fun onClosed(willReconnect: Boolean) {
            Log.e("TAG", "reconnect? $willReconnect")
        }
    }
}