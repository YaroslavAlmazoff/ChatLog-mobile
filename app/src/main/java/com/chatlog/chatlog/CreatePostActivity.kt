package com.chatlog.chatlog

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


class CreatePostActivity : AppCompatActivity() {
    var userData: JSONObject? = null
    private var photos: ArrayList<LoadingFile>? = null
    var adapter: SelectedImagesAdapter? = null

    var imagesList: RecyclerView? = null
    var selectedImagesArray: ArrayList<SelectedImage>? = null

    var currentPhotoNumber = 0

    var postText: TextView? = null
    var imageFile: File? = null

    var pb: ProgressBar? = null
    var sendButton: com.sanojpunchihewa.glowbutton.GlowButton? = null

    @RequiresApi(33)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)
        pb = findViewById(R.id.pb)

        photos = ArrayList()

        imagesList = findViewById(R.id.selected_images_list)
        selectedImagesArray = ArrayList()
        adapter = SelectedImagesAdapter(selectedImagesArray!!, photos!!, ArrayList())
        imagesList?.adapter = adapter
        imagesList?.layoutManager = LinearLayoutManager(this)

        postText = findViewById(R.id.post_text)


        val util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        val goBack = findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.go_back)
        sendButton = findViewById(R.id.send_button)

        sendButton?.setOnClickListener {
            pb?.visibility = View.VISIBLE
            sendInBackground()
        }

        goBack.setOnClickListener {
            val intent = Intent(this, UserActivity::class.java)
            intent.putExtra("id", userData?.getJSONObject("user")?.getString("_id"))
            startActivity(intent)
        }

        findViewById<Button>(R.id.upload_image_button).setOnClickListener {
            pb?.visibility = View.VISIBLE
            sendButton?.visibility = View.GONE
            selectImageLauncher.launch("image/*")
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
        val token = Utils.updateToken(this)
        updateProfile(token)
    }



    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Обработка выбранного изображения
        if (uri != null) {
            Thread {
                val inputStream = contentResolver.openInputStream(uri)
                val file = File(cacheDir, "file$currentPhotoNumber") // Создаем временный файл
                file.createNewFile()
                val fos = FileOutputStream(file)

                inputStream?.copyTo(fos)

                imageFile = file

                runOnUiThread {
                    pb?.visibility = View.GONE
                    sendButton?.visibility = View.VISIBLE

                    photos?.add(LoadingFile(file, false))
                    selectedImagesArray?.add(SelectedImage(uri, false))
                    adapter?.notifyDataSetChanged()
                    currentPhotoNumber++
                }
            }.start()
        }
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
            if(!photos?.get(i)?.deleted!!) {
                parts.add(prepareFilePart(i)!!)
            }
        }
        val dt = Utils().getCurrentDate()
        val txt = postText?.text?.toString()!!

        val date = RequestBody.create("text/plain".toMediaTypeOrNull(), dt)
        val title = RequestBody.create("text/plain".toMediaTypeOrNull(), txt)

        try {
            CoroutineScope(Dispatchers.IO).launch {
                val response = chatLogApi.sendPost(
                    title,
                    date,
                    parts,
                    "Bearer $token"
                )
                Log.e("TAG", "response -> $response")
                DatabaseHelper(applicationContext).addPost(txt, dt, response)
                Utils.saveFileFromUrl(applicationContext, Constants().SITE_NAME_FILES + "/articles/" + response, response) {}
                val intent = Intent(applicationContext, UserActivity::class.java)
                intent.putExtra("id", userData?.getJSONObject("user")?.getString("_id"))
                startActivity(intent)
            }
        } catch(e: IllegalStateException) {
            Log.e("TAG", "Ошибка но ничего страшного")
        }
    }
    private fun prepareFilePart(i: Int): MultipartBody.Part? {
        val file = photos?.get(i)
        val requestBody: RequestBody = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file?.file!!)
        return MultipartBody.Part.createFormData("file$i", photos?.get(i)?.file?.name, requestBody)
    }
    private fun runUserActivity() {
        val intent = Intent(this, UserActivity::class.java)
        intent.putExtra("id", userData?.getJSONObject("user")?.getString("_id"))
        startActivity(intent)
    }
}