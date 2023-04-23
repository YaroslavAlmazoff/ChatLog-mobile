package com.chatlog.chatlog

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tylerjroach.eventsource.EventSource
import com.tylerjroach.eventsource.EventSourceHandler
import com.tylerjroach.eventsource.MessageEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.IllegalStateException
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection


class MessengerActivity : AppCompatActivity() {
    var messagesList: RecyclerView? = null
    var userData: JSONObject? = null
    var headText: TextView? = null
    var headDate: TextView? = null
    var pb: ProgressBar? = null
    var sendImg: ImageView? = null
    var editButton: com.sanojpunchihewa.glowbutton.GlowButton? = null
    var images: View? = null

    var upload: ImageView? = null
    var cancel: TextView? = null
    var uploadScreen: View? = null

    var uploadImage: ImageView? = null
    var uploadVideo: ImageView? = null
    var uploadAudio: ImageView? = null

    var messageField: EditText? = null
    var messagesArray: ArrayList<Message>? = null
    var editing: Boolean = false
    var currentMessageId: String = ""
    var currentMessageText: String = ""

    val GALERY_ADD_IMAGE = 1
    val GALERY_ADD_VIDEO = 2
    var image: ImageView? = null
    var video: ImageView? = null
    var imageFile: File? = null
    var videoFile: File? = null

    var imageUri: Uri? = null
    var videoUri: Uri? = null

    var uploading: Boolean = false

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
        sendImg = findViewById(R.id.send_message)
        editButton = findViewById(R.id.edit_button)
        messageField = findViewById(R.id.message_field)
        upload = findViewById(R.id.upload)
        cancel = findViewById(R.id.cancel)
        uploadScreen = findViewById(R.id.upload_screen)
        uploadImage = findViewById(R.id.upload_image)
        uploadVideo = findViewById(R.id.upload_video)
        uploadAudio = findViewById(R.id.upload_audio)

        images = findViewById(R.id.images)

        image = findViewById(R.id.image)
        video = findViewById(R.id.video)

        uploadImage?.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, GALERY_ADD_IMAGE)
        }
        uploadVideo?.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "video/*"
            startActivityForResult(intent, GALERY_ADD_VIDEO)
        }

        upload?.setOnClickListener {
            uploadScreen?.visibility = View.VISIBLE
        }
        cancel?.setOnClickListener {
            uploadScreen?.visibility = View.GONE
        }

        pb = findViewById(R.id.pb)
        messagesArray = ArrayList()
        val adapter = MessagesAdapter(messagesArray!!, applicationContext, userData!!, messageField!!, editing, currentMessageId!!, currentMessageText!!, sendImg!!, editButton!!)
        sendImg?.setOnClickListener {
            sendMessageInBackground(adapter)
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(messageField?.windowToken, 0)
        }

        getMessagesInBackground(messagesArray!!)

        messagesList?.adapter = adapter
        messagesList?.layoutManager = LinearLayoutManager(this)

        startEventSource()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALERY_ADD_IMAGE && resultCode == RESULT_OK) {
            if(data?.data != null) {
                imageUri = data?.data
                images?.visibility = View.VISIBLE
                image?.setImageURI(data?.data)
                val inputStream = contentResolver.openInputStream(data?.data!!)
                var outputStream: OutputStream? = null
                try {
                    outputStream = FileOutputStream(File(filesDir, "file"))
                    var byteRead = inputStream?.read()
                    while(byteRead  != -1) {
                        outputStream.write(byteRead!!)
                        byteRead = inputStream?.read()
                    }
                } finally {
                    inputStream?.close()
                    outputStream?.close()
                }
                imageFile = File(filesDir, "file")
                uploadScreen?.visibility = View.GONE
                uploading = true
            }
        } else if (requestCode == GALERY_ADD_VIDEO && resultCode == RESULT_OK) {
            //image?.setImageURI(data?.data)
            videoUri = data?.data
            val inputStream = contentResolver.openInputStream(data?.data!!)
            var outputStream: OutputStream? = null
            try {
                outputStream = FileOutputStream(File(filesDir, "video"))
                var byteRead = inputStream?.read()
                while(byteRead  != -1) {
                    outputStream.write(byteRead!!)
                    byteRead = inputStream?.read()
                }
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
            imageFile = File(filesDir, "video")
        } else {
            Log.e("TAG", "Error")
        }
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
                usersArray.getJSONObject(i).getJSONArray("readedThisMessage"), null
            ))
        }
        Log.e("TAG", messagesArray?.isEmpty()!!.toString())
        Log.e("TAG", uploading.toString())
        if(imageUri != null) {
            messagesArray?.add(Message(
                messageField?.text?.toString()!!,
                userData?.getJSONObject("user")?.getString("name")!!,
                userData?.getJSONObject("user")?.getString("avatarUrl")!!,
                "Сейчас",
                userData?.getJSONObject("user")?.getString("_id")!!,
                intent.getStringExtra("id")!!,
                true, false, "", "", "", "",
                JSONArray(), imageUri
            ))
            imageUri = null
            videoUri = null
            runOnUiThread { messagesList?.adapter?.notifyDataSetChanged() }
        }
        runOnUiThread { messageField?.setText("") }
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
                sendMessage2(adapter)
                runOnUiThread {
                    images?.visibility = View.GONE
                    if(imageUri != null) {
                        getMessagesInBackground(messagesArray!!)
                    }
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "все плохо")
            }
        }.start()
    }
    private fun sendMessage2(adapter: MessagesAdapter) {
        runOnUiThread {
            adapter.clear()
        }
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.MINUTES)
            .writeTimeout(60, TimeUnit.MINUTES)
            .readTimeout(60, TimeUnit.MINUTES)
            .addInterceptor(interceptor)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants().SITE_NAME).client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
        val chatLogApi = retrofit.create(ChatLogApi::class.java)

        var requestFile1: RequestBody? = null
        var requestFile2: RequestBody? = null
        var body1: MultipartBody.Part? = null
        var body2: MultipartBody.Part? = null

        val token = userData?.getString("token")

        val isFile = RequestBody.create("text/plain".toMediaTypeOrNull(), "false")
        val message = RequestBody.create("text/plain".toMediaTypeOrNull(), messageField?.text?.toString()!!)
        val audio = RequestBody.create("text/plain".toMediaTypeOrNull(), "")

        if(imageFile != null) {
            requestFile1 = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), imageFile!!)
            body1 = MultipartBody.Part.createFormData("file", imageFile?.name, requestFile1)
        }
        if(videoFile != null) {
            requestFile2 = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), videoFile!!)
            body2 = MultipartBody.Part.createFormData("videoFile", videoFile?.name, requestFile2)
        }

        try {
            CoroutineScope(Dispatchers.IO).launch {
                chatLogApi.sendMessage(
                    intent.getStringExtra("id")!!,
                    message,
                    isFile,
                    body1, body2, audio,"Bearer $token"
                )
            }
        } catch(e: IllegalStateException) {
            Log.e("TAG", "Ошибка но ничего страшного")
        }
        imageFile = null
        videoFile = null
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
                    messages.getJSONObject(i).getJSONArray("readedThisMessage"), null
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