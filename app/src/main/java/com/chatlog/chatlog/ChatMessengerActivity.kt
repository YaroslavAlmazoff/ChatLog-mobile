package com.chatlog.chatlog

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.squareup.picasso.Picasso
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
import java.io.*
import java.lang.IllegalStateException
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection


class ChatMessengerActivity : AppCompatActivity() {
    var messagesList: RecyclerView? = null
    var layoutManager: LinearLayoutManager? = null
    var userData: JSONObject? = null
    var headText: TextView? = null
    var headMembersText: TextView? = null
    var pb: ProgressBar? = null
    var sendImg: ImageView? = null
    var editButton: com.sanojpunchihewa.glowbutton.GlowButton? = null
    var images: View? = null

    var avatarUrl: String? = null
    var creator: String? = null

    var settings: ImageView? = null

    var currentMode: String = ""

    var searchField: SearchView? = null

    var upload: ImageView? = null
    var cancel: TextView? = null
    var uploadScreen: View? = null

    var uploadImage: ImageView? = null
    var uploadVideo: ImageView? = null
    var uploadAudio: ImageView? = null
    var uploadBg: ImageView? = null

    var writeMessage: View? = null
    var messageField: EditText? = null
    var messagesArray: ArrayList<Message>? = null
    var editing: Boolean = false
    var currentMessageId: String = ""
    var currentMessageText: String = ""

    lateinit var rs: Cursor
    var greed: GridView? = null
    var pickImages: View? = null

    var audioMessage = -1


    var image: ImageView? = null
    var video: VideoView? = null
    var imageFile: File? = null
    var videoFile: File? = null

    var adapter: MessagesAdapter? = null

    private var state: Boolean = false

    var recordVoiceMessage: View? = null
    var startRecording: ImageView? = null
    var stopRecording: ImageView? = null
    var recordText: TextView? = null
    var recordCancel: TextView? = null
    var recording: ImageView? = null

    var voiceText: TextView? = null
    var mediaRecorder: MediaRecorder? = null

    var bgImage: ImageView? = null

    override fun onStop() {
        super.onStop()
        stopEventSource()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_messenger)

        searchField = findViewById(R.id.search_field)
        settings = findViewById(R.id.discussion_settings)


        val initWidth = searchField?.width

        searchField?.setOnSearchClickListener {
            val layoutParams = searchField?.layoutParams
            layoutParams?.width = initWidth!! + 500 // change this value to adjust the width
            searchField?.layoutParams = layoutParams
        }
        searchField?.setOnCloseListener {
            val layoutParams = searchField?.layoutParams
            layoutParams?.width = 140 // change this value to adjust the width
            searchField?.layoutParams = layoutParams
            return@setOnCloseListener false
        }

        searchField?.setOnQueryTextListener(object : SearchView.OnQueryTextListener, SearchView.OnSuggestionListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    adapter?.filter(it)
                }
                return true
            }

            override fun onSuggestionClick(position: Int): Boolean {
                Log.e("TAG", "chatlog")
                return true
            }

            override fun onSuggestionSelect(position: Int): Boolean {
                Log.e("TAG", "chatlog")
                return true
            }
        })

        val util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))
        messagesList = findViewById(R.id.messages_list)
        headText = findViewById(R.id.head_text)
        headMembersText = findViewById(R.id.head_members_text)
        sendImg = findViewById(R.id.send_message)
        editButton = findViewById(R.id.edit_button)
        messageField = findViewById(R.id.message_field)
        upload = findViewById(R.id.upload)
        cancel = findViewById(R.id.cancel)
        uploadScreen = findViewById(R.id.upload_screen)
        uploadImage = findViewById(R.id.upload_image)
        uploadVideo = findViewById(R.id.upload_video)
        uploadAudio = findViewById(R.id.upload_audio)
        uploadBg = findViewById(R.id.upload_bg)
        recordVoiceMessage = findViewById(R.id.record_voice_message)
        startRecording = findViewById(R.id.start_recording)
        stopRecording = findViewById(R.id.stop_recording)
        recording = findViewById(R.id.recording)
        recordText = findViewById(R.id.record_text)
        recordCancel = findViewById(R.id.record_cancel)
        writeMessage = findViewById(R.id.write_message)
        voiceText = findViewById(R.id.voice_text)

        images = findViewById(R.id.images)
        pickImages = findViewById(R.id.pick_images)
        bgImage = findViewById(R.id.bg_image)


        image = findViewById(R.id.image)
        video = findViewById(R.id.video)

        uploadImage?.setOnClickListener {
            val myIntent = Intent(this, SendImageActivity::class.java)
            myIntent.putExtra("id", intent.getStringExtra("id"))
            myIntent.putExtra("messengerType", "chat")
            myIntent.putExtra("fileType", "image")
            startActivity(myIntent)
        }
        uploadVideo?.setOnClickListener {
            val myIntent = Intent(this, SendImageActivity::class.java)
            myIntent.putExtra("id", intent.getStringExtra("id"))
            myIntent.putExtra("messengerType", "chat")
            myIntent.putExtra("fileType", "video")
            startActivity(myIntent)
        }
        uploadBg?.setOnClickListener {
//            currentMode = "bg"
//            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
//                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 101)
//            } else {
//                //loadImageFiles()
//            }
        }
        uploadAudio?.setOnClickListener {
            uploadScreen?.visibility = View.GONE
            writeMessage?.visibility = View.GONE
            recordVoiceMessage?.visibility = View.VISIBLE
        }

        startRecording?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO),102)
            } else {
                startingRecording()
            }
        }
        stopRecording?.setOnClickListener {
            stopRecording?.visibility = View.GONE
            startRecording?.visibility = View.VISIBLE
            recordText?.setText(R.string.voice)
            recordCancel?.visibility = View.VISIBLE
            recording?.visibility = View.GONE
            mediaRecorder?.stop()
            mediaRecorder?.release()
            state = false
            recordVoiceMessage?.visibility = View.GONE
            writeMessage?.visibility = View.VISIBLE
            messageField?.visibility = View.GONE
            voiceText?.visibility = View.VISIBLE
            mediaRecorder = null
        }
        recordCancel?.setOnClickListener {
            recordVoiceMessage?.visibility = View.GONE
            writeMessage?.visibility = View.VISIBLE
        }


        upload?.setOnClickListener {
            uploadScreen?.visibility = View.VISIBLE
        }
        cancel?.setOnClickListener {
            uploadScreen?.visibility = View.GONE
        }

        pb = findViewById(R.id.pb)
        messagesArray = ArrayList()
        adapter = MessagesAdapter(messagesArray!!, applicationContext, userData!!, messageField!!, editing, currentMessageId!!, currentMessageText!!, sendImg!!, editButton!!, this, "chat")
        sendImg?.setOnClickListener {
            sendMessageInBackground()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(messageField?.windowToken, 0)
        }

        messagesList?.adapter = adapter
        layoutManager = LinearLayoutManager(this)
        messagesList?.layoutManager = layoutManager

        getMessagesInBackground()

        settings?.setOnClickListener {
            val myIntent = Intent(this, DiscussionSettingsActivity::class.java)
            myIntent.putExtra("id", intent.getStringExtra("id")!!)
            myIntent.putExtra("avatarUrl", avatarUrl)
            myIntent.putExtra("title", headText?.text)
            myIntent.putExtra("creator", creator)
            startActivity(myIntent)
        }

        adapter?.filter("")
        startEventSource()

        if(intent.getStringExtra("file") != null) {
            messageField?.setText("${intent.getStringExtra("name")}")
            sendMessage2()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            findViewById<ImageView>(R.id.image).setImageURI(data?.data)
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
        } else {
            Log.e("TAG", "Error")
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun startingRecording() {
        if(File(filesDir, "voice.mp3").exists()) {
            File(filesDir, "voice.mp3").delete()
        }
        mediaRecorder = MediaRecorder()
        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder?.setOutputFile(File(filesDir, "voice.mp3"))

        startRecording?.visibility = View.GONE
        stopRecording?.visibility = View.VISIBLE
        recordText?.setText(R.string.recording_text)
        recordCancel?.visibility = View.GONE
        recording?.visibility = View.VISIBLE

        mediaRecorder?.prepare()
        mediaRecorder?.start()
        state = true
        Toast.makeText(this, "Recording started!", Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.e("TAG", requestCode.toString())
        when (requestCode) {
            102 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startingRecording()
                } else {
                    Toast.makeText(this, "Разрешение отклонено", Toast.LENGTH_LONG).show()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun getMessagesInBackground() {
        Thread {
            try {
                getMessages()
                getRoom()
                runOnUiThread {
                    messagesList?.adapter?.notifyDataSetChanged()
                    pb?.visibility = View.GONE
                    if(messagesArray?.isNotEmpty()!!) {
                        layoutManager?.stackFromEnd = true
                    }
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "все плохо")
            }
        }.start()
    }
    fun getRoom() {
        val roomData = Utils.request(this, "chatroombyid/${intent.getStringExtra("id")!!}", "GET", false, null)
        val image = JSONObject(roomData).getJSONObject("room").getString("bg")
        avatarUrl = JSONObject(roomData).getJSONObject("room").getString("avatarUrl")
        creator = JSONObject(roomData).getJSONObject("room").getString("creator")
        runOnUiThread {
            settings?.visibility = View.VISIBLE
        }
        runOnUiThread {
            Picasso.get().load(Constants().SITE_NAME_FILES + "/roombackgrounds/${image}").into(bgImage)
            bgImage?.scaleType = ImageView.ScaleType.CENTER_CROP
            headText?.text = JSONObject(roomData).getJSONObject("room").getString("title")
            Log.e("TAG", JSONObject(roomData).getJSONObject("room").getJSONArray("members").length().toString())
            Log.e("TAG", declination(JSONObject(roomData).getJSONObject("room").getJSONArray("members").length().toString()))
            findViewById<TextView>(R.id.head_members_text).text = declination(JSONObject(roomData).getJSONObject("room").getJSONArray("members").length().toString())
        }
    }
    fun getMessages() {
        Log.e("TAG", intent.getStringExtra("id")!!)
        val messagesData = Utils.request(this, "getmessagesstart/${intent.getStringExtra("id")!!}", "GET", false, null)
        Log.e("TAG", messagesData)
        val usersArray = JSONObject(messagesData).getJSONArray("messages")
        for(i in 0 until usersArray.length()) {
            messagesArray?.add(Message(
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
                usersArray.getJSONObject(i).getJSONArray("readedThisMessage"),
                null, null, null, usersArray.getJSONObject(i).getString("fileLink")
            ))
        }
        runOnUiThread {
            adapter?.filter("")
            messagesList?.adapter?.notifyDataSetChanged()
        }
    }

    private fun declination(count: String = "0"): String {
        Log.e("TAG", count)
        if (count == null) return ""
        var last = count.takeLast(1)
        Log.e("TAG", last)
        return if (last == "0") {
            "$count участников"
        } else if (last == "1") {
            "$count участник"
        } else if (last > "1" && last < "5") {
            "$count участника"
        } else if (last > "5") {
            "$count участников"
        } else { "$count участников" }
    }
    fun sendMessageInBackground() {
        Thread {
            try {
                sendMessage2()
            } catch(e: InterruptedException) {
                Log.e("TAG", "все плохо")
            }
        }.start()
    }

    private fun sendMessage2() {
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

        val audioFile = File(filesDir, "voice.mp3")

        var requestFile1: RequestBody? = null
        var requestFile2: RequestBody? = null
        var requestFile3: RequestBody? = null
        var body1: MultipartBody.Part? = null
        var body2: MultipartBody.Part? = null
        var body3: MultipartBody.Part? = null

        val token = Utils.updateToken(this)

        val isFile = RequestBody.create("text/plain".toMediaTypeOrNull(), "false")
        val message = RequestBody.create("text/plain".toMediaTypeOrNull(), messageField?.text?.toString()!!)
        val date = RequestBody.create("text/plain".toMediaTypeOrNull(), Utils().getCurrentDateAndTime())

        var fileLink: RequestBody? = null
        if(intent.getStringExtra("file") != null) {
            fileLink = RequestBody.create("text/plain".toMediaTypeOrNull(), intent.getStringExtra("file")!!)
        }

        if(imageFile != null) {
            requestFile1 = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), imageFile!!)
            body1 = MultipartBody.Part.createFormData("file", imageFile?.name, requestFile1)
            imageFile = null
        }
        if(videoFile != null) {
            requestFile2 = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), videoFile!!)
            body2 = MultipartBody.Part.createFormData("videoFile", videoFile?.name, requestFile2)
            videoFile = null
        }
        if(audioFile.exists()) {
            requestFile3 = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), audioFile)
            body3 = MultipartBody.Part.createFormData("audioFile", audioFile?.name, requestFile3)
            audioMessage++
        }

        try {
            CoroutineScope(Dispatchers.IO).launch {
                chatLogApi.sendChatMessage(
                    intent.getStringExtra("id")!!,
                    message,
                    date,
                    isFile,
                    body1, body2, body3, fileLink,"Bearer $token"
                )
            }
        } catch(e: IllegalStateException) {
            Log.e("TAG", "Ошибка но ничего страшного")
        }
        runOnUiThread {
            messageField?.visibility = View.VISIBLE
            messageField?.setText("")
            images?.visibility = View.GONE
            voiceText?.visibility = View.GONE
        }
    }
    private var sseHandler: SSEHandler? = SSEHandler()
    private var eventSource: EventSource? = null
    private fun startEventSource() {
        eventSource = EventSource.Builder("https://chatlog.ru/api/connect-mobile/${intent.getStringExtra("id")}")
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
            Log.e("TAG", message.data)
            val message = JSONObject(message.data)
            messagesArray?.add(Message(
                message.getString("message"),
                message.getString("name"),
                message.getString("avatarUrl"),
                message.getString("date"),
                message.getString("user"),
                message.getString("room"),
                message.getBoolean("isNotReaded"),
                message.getBoolean("isFile"),
                message.getString("_id"),
                message.getString("imageUrl"),
                message.getString("videoUrl"),
                message.getString("audioUrl"),
                message.getJSONArray("readedThisMessage"),
                    null, null, null, message.getString("fileLink")
                ))
            runOnUiThread { adapter?.filter("") }
            runOnUiThread {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(messageField?.windowToken, 0)
                messageField?.setText("")
                if(messagesArray?.isNotEmpty()!!) {
                    messagesArray?.count()?.minus(1)?.let { messagesList?.smoothScrollToPosition(it) }
                }
                messagesList?.adapter?.notifyDataSetChanged()
            }
//            if(messagesArray!![messagesArray?.count()?.minus(1)!!].user != null) {
//                if(userData?.getJSONObject("user")?.getString("_id") != messagesArray!![messagesArray?.count()?.minus(1)!!].user) {
//                    read()
//                }
//            }
        }

        override fun onComment(comment: String) {
            Log.e("TAG", comment)
        }

        override fun onError(t: Throwable) {
            Log.e("TAG", t.toString())
        }

        override fun onClosed(willReconnect: Boolean) {
            Log.e("TAG", "reconnect? $willReconnect")
            stopEventSource()
            startEventSource()
        }
    }
}