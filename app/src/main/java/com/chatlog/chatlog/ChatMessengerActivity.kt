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
import android.provider.MediaStore
import android.util.Log
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
    var pickImagesCancel: TextView? = null

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

        pickImagesCancel = findViewById(R.id.pick_images_cancel)
        images = findViewById(R.id.images)
        greed = findViewById(R.id.greed)
        pickImages = findViewById(R.id.pick_images)
        pickImagesCancel?.setOnClickListener {
            pickImages?.visibility = View.GONE
            uploadScreen?.visibility = View.GONE
        }

        bgImage = findViewById(R.id.bg_image)


        image = findViewById(R.id.image)
        video = findViewById(R.id.video)

        uploadImage?.setOnClickListener {
            currentMode = "image"
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), 101)
            } else {
                listFiles(currentMode)
            }

        }
        uploadVideo?.setOnClickListener {
            currentMode = "video"
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_VIDEO), 101)
            } else {
                listFiles(currentMode)
            }
        }
        uploadBg?.setOnClickListener {
            currentMode = "bg"
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), 101)
            } else {
                listFiles(currentMode)
            }
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
        adapter = MessagesAdapter(messagesArray!!, applicationContext, userData!!, messageField!!, editing, currentMessageId!!, currentMessageText!!, sendImg!!, editButton!!)
        sendImg?.setOnClickListener {
            sendMessageInBackground()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(messageField?.windowToken, 0)
        }

        messagesList?.adapter = adapter
        messagesList?.layoutManager = LinearLayoutManager(this)

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
            101 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    listFiles(currentMode)
                    currentMode = ""
                } else {
                    Toast.makeText(this, "Разрешение отклонено", Toast.LENGTH_LONG).show()
                }
                return
            }
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

    private fun listFiles(mode: String) {
        if(mode == "image") {
            var cols = listOf(MediaStore.Images.Thumbnails.DATA).toTypedArray()
            rs = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cols, null, null, null)!!
        } else if(mode == "video") {
            var cols = listOf(MediaStore.Video.Thumbnails.DATA).toTypedArray()
            rs = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cols, null, null, null)!!
        } else if(mode == "bg") {
            var cols = listOf(MediaStore.Images.Thumbnails.DATA).toTypedArray()
            rs = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cols, null, null, null)!!
        }
        pickImages?.visibility = View.VISIBLE
        greed?.adapter = ImagesAdapter(applicationContext, mode)
    }

    fun getMessagesInBackground() {
        Thread {
            try {
                Log.e("TAG", "before get messages")
                getMessages()
                Log.e("TAG", "before get room")
                getRoom()
                runOnUiThread {
                    messagesList?.adapter?.notifyDataSetChanged()
                    pb?.visibility = View.GONE
                    if(messagesArray?.isNotEmpty()!!) {
                        messagesArray?.count()?.minus(1)?.let { messagesList?.smoothScrollToPosition(it) }
                    }
                }
//                if(messagesArray!![messagesArray?.count()?.minus(1)!!].user != null) {
//                    if(userData?.getJSONObject("user")?.getString("_id") != messagesArray!![messagesArray?.count()?.minus(1)!!].user) {
//                        read()
//                    }
//                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "все плохо")
            }
        }.start()
    }
    fun getRoom() {
        val roomData = URL(Constants().SITE_NAME + "chatroombyid/${intent.getStringExtra("id")!!}").readText(Charsets.UTF_8)
        Log.e("TAG", roomData)
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
        val messagesData = URL(Constants().SITE_NAME + "getmessagesstart/${intent.getStringExtra("id")!!}").readText(Charsets.UTF_8)
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
                null, null, null
            ))
        }
        runOnUiThread {
            adapter?.filter("")
            messagesList?.adapter?.notifyDataSetChanged()
        }
    }
//    fun read() {
//        URL(Constants().SITE_NAME + "read/${intent.getStringExtra("id")!!}").readText(Charsets.UTF_8)
//    }

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

        val token = userData?.getString("token")

        val isFile = RequestBody.create("text/plain".toMediaTypeOrNull(), "false")
        val message = RequestBody.create("text/plain".toMediaTypeOrNull(), messageField?.text?.toString()!!)
        val date = RequestBody.create("text/plain".toMediaTypeOrNull(), Utils().getCurrentDateAndTime())

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
                chatLogApi.sendMessage(
                    intent.getStringExtra("id")!!,
                    message,
                    date,
                    isFile,
                    body1, body2, body3, "Bearer $token"
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
                    null, null, null
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
        }
    }
    inner class ImagesAdapter : BaseAdapter {
        var context: Context
        var mode: String = "image"
        constructor(context: Context, mode: String) {
            this.context = context
            this.mode = mode
        }
        override fun getCount(): Int {
            return rs.count
        }

        override fun getItem(position: Int): Any {
            return position
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            if(mode == "image") {
                var iv = ImageView(context)
                rs.moveToPosition(position)
                var path = rs.getString(0)
                var bitmap = BitmapFactory.decodeFile(path)
                iv.setImageBitmap(bitmap)
                iv.layoutParams = AbsListView.LayoutParams(300, 300)

                iv.setOnClickListener {
                    pickImagesCancel?.text = "Загрузка изображения..."
                    Log.e("TAG", "Вы нажали на картинку $path")
                    val f = File(filesDir, "file");
                    f.createNewFile();

                    val bitmap = bitmap;
                    val bos = ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
                    val bitmapData = bos.toByteArray();

                    val fos = FileOutputStream(f);
                    fos.write(bitmapData);
                    fos.flush();
                    fos.close();
                    imageFile = f
                    pickImages?.visibility = View.GONE
                    uploadScreen?.visibility = View.GONE
                    images?.visibility = View.VISIBLE
                    image?.setImageURI(Uri.fromFile(imageFile))
                    image?.visibility = View.VISIBLE
                    pickImagesCancel?.text = "Отмена"
                }

                return iv
            } else if (mode == "video") {
                var iv = ImageView(context)
                rs.moveToPosition(position)
                var path = rs.getString(0)
                var uri = Uri.parse("file://$path")
                Glide.with(context)
                    .load(uri)
                    .into(iv)
                iv.layoutParams = AbsListView.LayoutParams(300, 300)

                iv.setOnClickListener {
                    pickImagesCancel?.text = "Загрузка видео..."
                    Thread {
                        try {
                            val file = File(filesDir, "file")
                            val inputStream = contentResolver.openInputStream(uri)
                            var outputStream: OutputStream? = null
                            try {
                                outputStream = FileOutputStream(file)
                                var byteRead = inputStream?.read()
                                while(byteRead  != -1) {
                                    outputStream.write(byteRead!!)
                                    byteRead = inputStream?.read()
                                }
                            } finally {
                                inputStream?.close()
                                outputStream?.close()
                            }
                            videoFile = file
                        } catch (e: InterruptedException) {
                            Log.e("TAG", "ERROR")
                        }
                    }.start()
                    pickImages?.visibility = View.GONE
                    uploadScreen?.visibility = View.GONE
                    images?.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(uri)
                        .into(image!!)
                    image?.visibility = View.VISIBLE
                    pickImagesCancel?.text = "Отмена"
                }

                return iv
            } else if (mode == "bg") {
                var iv = ImageView(context)
                rs.moveToPosition(position)
                var path = rs.getString(0)
                var bitmap = BitmapFactory.decodeFile(path)
                iv.setImageBitmap(bitmap)

                iv.layoutParams = AbsListView.LayoutParams(300, 300)

                iv.setOnClickListener {
                    pickImagesCancel?.text = "Загрузка изображения..."
                    Log.e("TAG", "Вы нажали на картинку $path")
                    val f = File(filesDir, "bgfile")
                    f.createNewFile()

                    val bitmap = bitmap
                    val bos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos)
                    val bitmapData = bos.toByteArray()

                    val fos = FileOutputStream(f)
                    fos.write(bitmapData)
                    fos.flush()
                    fos.close()


                    val interceptor = HttpLoggingInterceptor()
                    interceptor.level = HttpLoggingInterceptor.Level.BODY

                    val client = OkHttpClient.Builder()
                        .addInterceptor(interceptor)
                        .build()
                    val retrofit = Retrofit.Builder()
                        .baseUrl(Constants().SITE_NAME).client(client)
                        .addConverterFactory(GsonConverterFactory.create()).build()
                    val chatLogApi = retrofit.create(ChatLogApi::class.java)

                    var requestFile: RequestBody? = null
                    var body: MultipartBody.Part? = null

                    if(f != null) {
                        requestFile = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), f)
                        body = MultipartBody.Part.createFormData("file", f.name, requestFile)
                    }

                    try {
                        CoroutineScope(Dispatchers.IO).launch {
                            chatLogApi.sendRoomBg(
                                intent.getStringExtra("id")!!,
                                body
                            )
                        }
                    } catch(e: IllegalStateException) {
                        Log.e("TAG", "Ошибка но ничего страшного")
                    }
                    pickImages?.visibility = View.GONE
                    uploadScreen?.visibility = View.GONE
                    bgImage?.setImageBitmap(bitmap)
                    pickImagesCancel?.text = "Отмена"
                }

                return iv
            }
            return ImageView(context)
        }
    }
}