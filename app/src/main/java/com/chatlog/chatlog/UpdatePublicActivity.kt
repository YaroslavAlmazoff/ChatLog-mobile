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
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.squareup.picasso.Picasso
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
import java.io.OutputStream
import java.lang.IllegalStateException
import java.net.URI
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class UpdatePublicActivity : AppCompatActivity() {
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

    var updateButton: Button? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_public)

        pb2 = findViewById(R.id.pb2)

        nameField = findViewById(R.id.name_field)
        descriptionField = findViewById(R.id.description_field)

        val goBack = findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.go_back)

        goBack.setOnClickListener {
            val intent = Intent(this, UserActivity::class.java)
            intent.putExtra("id", userData?.getJSONObject("user")?.getString("_id"))
            startActivity(intent)
        }

        val addAvatar = findViewById<Button>(R.id.update_profile_add_avatar)
        val addBanner = findViewById<Button>(R.id.update_profile_add_banner)
        updateButton = findViewById<Button>(R.id.update_profile_button)

        avatar = findViewById(R.id.update_profile_avatar)
        banner = findViewById(R.id.update_profile_banner)

        updateButton?.setOnClickListener {
            pb?.visibility = View.VISIBLE
            update()
        }

        addAvatar.setOnClickListener {
            currentMode = "avatar"
            pb?.visibility = View.VISIBLE
            updateButton?.visibility = View.GONE
            selectImageLauncher.launch("image/*")
        }
        addBanner.setOnClickListener {
            currentMode = "banner"
            pb?.visibility = View.VISIBLE
            updateButton?.visibility = View.GONE
            selectImageLauncher.launch("image/*")
        }

        val util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        getData()
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
                    updateButton?.visibility = View.VISIBLE
                }
            }.start()
        }
    }

    private fun getData() {
        Thread {
            try {
                val data = Utils.request(this, "public/public/${intent.getStringExtra("id")}", "GET", true, null)
                val obj = JSONObject(data).getJSONObject("pub")
                runOnUiThread {
                    nameField?.setText(obj.getString("name"))
                    descriptionField?.setText(obj.getString("description"))
                    if(avatar != null && obj.getString("avatarUrl") != "") {
                        Picasso.get().load(Constants().SITE_NAME_FILES + "/publicavatars/${obj.getString("avatarUrl")}").into(avatar)
                        avatar?.scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    if(banner != null && obj.getString("bannerUrl") != "") {
                        Picasso.get().load(Constants().SITE_NAME_FILES + "/publicbanners/${obj.getString("bannerUrl")}").into(banner)
                        banner?.scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "ERROR")
            }
        }.start()
    }
    private fun update() {
        if (nameField?.text.toString() == "") {
            Toast.makeText(this, R.string.update_public_title_error, Toast.LENGTH_SHORT).show()
            return
        }
        Log.e("TAG", "Проверка прошла")
        updateInBackground()
    }

    private fun updateInBackground() {
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

        Log.e("TAG", avatarUri.toString())
        Log.e("TAG", bannerUri.toString())

        var requestFile1: RequestBody? = null
        var requestFile2: RequestBody? = null
        var body1: MultipartBody.Part? = null
        var body2: MultipartBody.Part? = null

        val isAvatar = RequestBody.create("text/plain".toMediaTypeOrNull(), if(avatarFile != null) "1" else "0" )
        val isBanner = RequestBody.create("text/plain".toMediaTypeOrNull(), if(bannerFile != null) "1" else "0")

        if(avatarFile != null) {
            requestFile1 = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), avatarFile!!)
            body1 = MultipartBody.Part.createFormData("avatar", avatarFile?.name, requestFile1)
        }
        if(bannerFile != null) {
            requestFile2 = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), bannerFile!!)
            body2 = MultipartBody.Part.createFormData("banner", bannerFile?.name, requestFile2)
        }
        val name = RequestBody.create("text/plain".toMediaTypeOrNull(), nameField?.text?.toString()!!)
        val description = RequestBody.create("text/plain".toMediaTypeOrNull(), descriptionField?.text?.toString()!!)

        try {
            CoroutineScope(Dispatchers.IO).launch {
                val response = chatLogApi.updatePublic(
                    intent.getStringExtra("id")!!,
                    name, description,
                    body1, body2, isAvatar, isBanner, "Bearer $token"
                )
                val public = JSONObject(response)
                val intent = Intent(applicationContext, PublicActivity::class.java)
                intent.putExtra("id", public.getString("_id"))
                intent.putExtra("name", public.getString("name"))
                intent.putExtra("description", public.getString("description"))
                intent.putExtra("avatarUrl", public.getString("avatarUrl"))
                intent.putExtra("bannerUrl", public.getString("bannerUrl"))
                intent.putExtra("admin", public.getString("admin"))
                intent.putExtra("isSubscriber", true)
                startActivity(intent)
            }
        } catch(e: IllegalStateException) {
            Log.e("TAG", "Ошибка но ничего страшного")
        }
    }
}
