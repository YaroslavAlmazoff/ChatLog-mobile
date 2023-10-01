package com.chatlog.chatlog

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.IllegalStateException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class CreatePublicActivity : AppCompatActivity() {
    var nameField: EditText? = null
    var descriptionField: EditText? = null

    var avatarFile: File? = null
    var bannerFile: File? = null

    var avatar: ImageView? = null
    var banner: ImageView? = null

    var userData: JSONObject? = null

    var avatarUri: String? = null
    var bannerUri: String? = null

    var currentMode: String = ""

    var pb: ProgressBar? = null
    var pb2: ProgressBar? = null

    var createButton: com.sanojpunchihewa.glowbutton.GlowButton? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_public)

        nameField = findViewById(R.id.name_field)
        descriptionField = findViewById(R.id.description_field)

        pb = findViewById(R.id.pb)
        pb2 = findViewById(R.id.pb2)

//        val goBack = findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.go_back)
//
//        goBack.setOnClickListener {
//            val intent = Intent(this, UserActivity::class.java)
//            intent.putExtra("id", userData?.getJSONObject("user")?.getString("_id"))
//            startActivity(intent)
//        }

        val addAvatar = findViewById<Button>(R.id.create_public_add_avatar)
        val addBanner = findViewById<Button>(R.id.create_public_add_banner)
        createButton = findViewById(R.id.create_button)

        avatar = findViewById(R.id.create_public_avatar)
        banner = findViewById(R.id.create_public_banner)

        createButton?.setOnClickListener {
            pb?.visibility = View.VISIBLE
            update()
        }

        addAvatar.setOnClickListener {
            currentMode = "avatar"
            pb?.visibility = View.VISIBLE
            createButton?.visibility = View.GONE
            selectImageLauncher.launch("image/*")
        }
        addBanner.setOnClickListener {
            currentMode = "banner"
            pb?.visibility = View.VISIBLE
            createButton?.visibility = View.GONE
            selectImageLauncher.launch("image/*")
        }

        val util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))
    }


    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Обработка выбранного изображения
        if (uri != null) {
            if(currentMode == "avatar") {
                avatar?.setImageURI(uri)
            } else {
                banner?.setImageURI(uri)
            }

            Thread {
                val inputStream = contentResolver.openInputStream(uri)
                val file = File(cacheDir, "file") // Создаем временный файл
                file.createNewFile()
                val fos = FileOutputStream(file)

                inputStream?.copyTo(fos)

                if(currentMode == "avatar") {
                    avatarFile = file
                } else {
                    bannerFile = file
                }

                runOnUiThread {
                    pb?.visibility = View.GONE
                    createButton?.visibility = View.VISIBLE
                }
            }.start()
        }
    }

    private fun update() {
        if (nameField?.text.toString() == "") {
            Toast.makeText(this, R.string.create_public_name_error, Toast.LENGTH_SHORT).show()
            return
        }
        createInBackground()
    }

    private fun createInBackground() {
        Thread {
            try {
                val token = Utils.updateToken(this)
                val interceptor = HttpLoggingInterceptor()
                interceptor.level = HttpLoggingInterceptor.Level.BODY

                val client = OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(Constants().SITE_NAME).client(client)
                    .addConverterFactory(GsonConverterFactory.create()).build()
                val chatLogApi = retrofit.create(ChatLogApi::class.java)

                Log.e("TAG", avatarUri.toString())
                Log.e("TAG", bannerUri.toString())

                var requestFile1: RequestBody? = null
                var requestFile2: RequestBody? = null
                var body1: MultipartBody.Part? = null
                var body2: MultipartBody.Part? = null

                if (avatarFile != null) {
                    requestFile1 =
                        RequestBody.create("multipart/form-data".toMediaTypeOrNull(), avatarFile!!)
                    body1 =
                        MultipartBody.Part.createFormData("avatar", avatarFile?.name, requestFile1)
                }
                if (bannerFile != null) {
                    requestFile2 =
                        RequestBody.create("multipart/form-data".toMediaTypeOrNull(), bannerFile!!)
                    body2 =
                        MultipartBody.Part.createFormData("banner", bannerFile?.name, requestFile2)
                }

                var name =
                    RequestBody.create("text/plain".toMediaTypeOrNull(), nameField?.text.toString())
                var description = RequestBody.create(
                    "text/plain".toMediaTypeOrNull(),
                    descriptionField?.text.toString()
                )

                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        val response = chatLogApi.createPublic(
                            name,
                            description,
                            body1, body2, "Bearer $token"
                        )
                        Log.e("TAG", response)
                        val public = JSONObject(response).getJSONObject("public")
                        val intent = Intent(applicationContext, PublicActivity::class.java)
                        val subscribers = public.getJSONArray("subscribers").toString()
                        val posts = public.getJSONArray("posts").toString()
                        intent.putExtra("id", public.getString("_id"))
                        intent.putExtra("name", public.getString("name"))
                        intent.putExtra("description", public.getString("description"))
                        intent.putExtra("avatarUrl", public.getString("avatarUrl"))
                        intent.putExtra("bannerUrl", public.getString("bannerUrl"))
                        intent.putExtra("admin", public.getString("admin"))
                        intent.putExtra("subscribers", subscribers)
                        intent.putExtra("posts", posts)
                        intent.putExtra("isSubscriber", false)
                        startActivity(intent)
                    }
                } catch (e: IllegalStateException) {
                    Log.e("TAG", "Ошибка но ничего страшного")
                }
            } catch (e: InterruptedException) {
                Log.e("TAG", "AAA")
            }
        }.start()
    }

}