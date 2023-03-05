package com.chatlog.chatlog

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Part
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


class CreatePostActivity : AppCompatActivity() {
    var userData: JSONObject? = null
    private var photos: ArrayList<File>? = null

    var imagesList: RecyclerView? = null
    var selectedImagesArray: ArrayList<SelectedImage>? = null

    var currentPhotoNumber = 1

    var postText: TextView? = null

    private val GALERY_ADD_PHOTO = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        photos = ArrayList()

        imagesList = findViewById(R.id.selected_images_list)
        selectedImagesArray = ArrayList()
        imagesList?.adapter = SelectedImagesAdapter(selectedImagesArray!!, photos!!)
        imagesList?.layoutManager = LinearLayoutManager(this)

        postText = findViewById(R.id.post_text)


        val util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        val goBack = findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.go_back)
        val sendButton = findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.send_button)

        sendButton.setOnClickListener {
            sendInBackground()
        }

        goBack.setOnClickListener {
            val intent = Intent(this, UserActivity::class.java)
            intent.putExtra("id", userData?.getJSONObject("user")?.getString("_id"))
            startActivity(intent)
        }
        findViewById<Button>(R.id.upload_image_button).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, GALERY_ADD_PHOTO)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALERY_ADD_PHOTO && resultCode == RESULT_OK) {
            selectedImagesArray?.add(SelectedImage(data?.data?.toString()!!))
            val inputStream = contentResolver.openInputStream(data?.data!!)
            var outputStream: OutputStream? = null
            try {
                outputStream = FileOutputStream(File(filesDir, "photo$currentPhotoNumber"))
                var byteRead = inputStream?.read()
                while(byteRead  != -1) {
                    outputStream.write(byteRead!!)
                    byteRead = inputStream?.read()
                }
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
            photos?.add(File(filesDir, "photo$currentPhotoNumber"))
            imagesList?.adapter?.notifyDataSetChanged()
            currentPhotoNumber++
            Log.e("TAG", currentPhotoNumber.toString())
            for(i in 0 until selectedImagesArray?.count()!!) {
                Log.e("TAG", selectedImagesArray!![i]?.uri)
            }
            for(i in 0 until photos?.count()!!) {
                Log.e("TAG", photos!![i]?.name)
            }
        } else {
            Log.e("TAG", "Error")
        }
    }
    private fun sendInBackground() {
        Thread {
            try {
                sendData()
            } catch (e: InterruptedException) {
                Log.e("TAG", "AAA")
            }
        }.start()
    }
    private fun sendData() {
        val token = userData?.getString("token")
        updateProfile(token!!)
        runUserActivity()
    }
    private fun updateProfile(token: String) {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(Constants().SITE_NAME).client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
        val chatLogApi = retrofit.create(ChatLogApi::class.java)


        val parts: ArrayList<MultipartBody.Part> = ArrayList()

        for(i in 0 until photos?.size!!) {
            parts.add(prepareFilePart(i)!!)
        }

        val date = RequestBody.create("text/plain".toMediaTypeOrNull(), Utils().getCurrentDate())
        val title = RequestBody.create("text/plain".toMediaTypeOrNull(), postText?.text?.toString()!!)

        try {
            CoroutineScope(Dispatchers.IO).launch {
                chatLogApi.sendPost(
                    title,
                    date,
                    parts,
                    "Bearer $token"
                )
            }
        } catch(e: IllegalStateException) {
            Log.e("TAG", "Ошибка но ничего страшного")
        }
    }
    private fun prepareFilePart(i: Int): MultipartBody.Part? {
        val file = photos?.get(i)
        val requestBody: RequestBody = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file!!)
        return MultipartBody.Part.createFormData("file$i", photos?.get(i)?.name, requestBody)
    }
    private fun runUserActivity() {
        val intent = Intent(this, UserActivity::class.java)
        intent.putExtra("id", userData?.getJSONObject("user")?.getString("_id"))
        startActivity(intent)
    }
}