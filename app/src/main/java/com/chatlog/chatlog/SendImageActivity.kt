package com.chatlog.chatlog

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.IllegalStateException
import java.util.concurrent.TimeUnit

class SendImageActivity : AppCompatActivity() {
    var imageFile: File? = null

    var image: ImageView? = null

    var pb: ProgressBar? = null
    var sendButton: com.sanojpunchihewa.glowbutton.GlowButton? = null
    var selectButton: com.sanojpunchihewa.glowbutton.GlowButton? = null
    var messageField: EditText? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_image)

        pb = findViewById(R.id.pb)
        sendButton = findViewById(R.id.send_button)
        selectButton = findViewById(R.id.select_button)
        messageField = findViewById(R.id.message_field)

        selectButton?.setOnClickListener {
            pb?.visibility = View.VISIBLE
            sendButton?.visibility = View.GONE
            selectImageLauncher.launch("${intent.getStringExtra("fileType")}/*")
        }
        sendButton?.setOnClickListener {
            Thread {
                sendMessage()
                val myIntent =
                    if(
                        intent.getStringExtra("messengerType") != null &&
                        intent.getStringExtra("messengerType") == "messenger")
                        Intent(applicationContext, MessengerActivity::class.java)
                    else
                        Intent(applicationContext, ChatMessengerActivity::class.java)
                myIntent.putExtra("id", intent.getStringExtra("id"))
                startActivity(myIntent)
            }.start()
        }
        image = findViewById(R.id.image)
    }

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Обработка выбранного изображения
        if (uri != null) {
            image?.setImageURI(uri)
            Thread {
                val inputStream = contentResolver.openInputStream(uri)
                val file = File(cacheDir, "file") // Создаем временный файл
                file.createNewFile()
                val fos = FileOutputStream(file)

                inputStream?.copyTo(fos)

                imageFile = file
                runOnUiThread {
                    pb?.visibility = View.GONE
                    sendButton?.visibility = View.VISIBLE
                }
            }.start()
        }
    }

    private fun sendMessage() {
        if(imageFile == null) return
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
        var body3: MultipartBody.Part? = null

        val token = Utils.updateToken(this)

        val isFile = RequestBody.create("text/plain".toMediaTypeOrNull(), "false")
        val message = RequestBody.create("text/plain".toMediaTypeOrNull(), messageField?.text.toString())
        val date = RequestBody.create("text/plain".toMediaTypeOrNull(), Utils().getCurrentDateAndTime())

        var fileLink: RequestBody? = null
        if(intent.getStringExtra("file") != null) {
            fileLink = RequestBody.create("text/plain".toMediaTypeOrNull(), intent.getStringExtra("file")!!)
        }

        if(imageFile != null && intent.getStringExtra("fileType") == "image") {
            requestFile1 = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), imageFile!!)
            body1 = MultipartBody.Part.createFormData("file", imageFile?.name, requestFile1)
        }
        if(imageFile != null && intent.getStringExtra("fileType") == "video") {
            requestFile2 = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), imageFile!!)
            body2 = MultipartBody.Part.createFormData("videoFile", imageFile?.name, requestFile2)
        }
        Log.e("TAG", intent.getStringExtra("id")!!)

        try {
            CoroutineScope(Dispatchers.IO).launch {
                if(intent.getStringExtra("messengerType") != null &&
                    intent.getStringExtra("messengerType") == "messenger") {
                    chatLogApi.sendMessage(
                        intent.getStringExtra("id")!!,
                        message,
                        date,
                        isFile,
                        body1, body2, body3,  fileLink,"Bearer $token"
                    )
                } else {
                    chatLogApi.sendChatMessage(
                        intent.getStringExtra("id")!!,
                        message,
                        date,
                        isFile,
                        body1, body2, body3,  fileLink,"Bearer $token"
                    )
                }
            }
        } catch(e: IllegalStateException) {
            Log.e("TAG", "Ошибка но ничего страшного")
        }
    }

}