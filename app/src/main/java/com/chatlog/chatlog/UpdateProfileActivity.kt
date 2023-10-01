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
import android.os.CancellationSignal
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
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class UpdateProfileActivity : AppCompatActivity() {
    var nameField: EditText? = null
    var surnameField: EditText? = null
    var dateField: EditText? = null
    var emailField: EditText? = null
    var countryField: EditText? = null
    var cityField: EditText? = null
    var aboutMe: EditText? = null

    var avatarFile: File? = null
    var bannerFile: File? = null

    var avatar: ImageView? = null
    var banner: ImageView? = null

    var userData: JSONObject? = null

    val GALERY_ADD_AVATAR = 1
    val GALERY_ADD_BANNER = 2

    var updateButton: com.sanojpunchihewa.glowbutton.GlowButton? = null


    var avatarUri: String? = null
    var bannerUri: String? = null

    var currentMode: String = ""

    var pb: ProgressBar? = null
    var pb2: ProgressBar? = null

    var layoutManager: GridLayoutManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_profile)

        nameField = findViewById(R.id.name_field)
        surnameField = findViewById(R.id.surname_field)
        dateField = findViewById(R.id.date_field)
        emailField = findViewById(R.id.email_field)
        countryField = findViewById(R.id.country_field)
        cityField = findViewById(R.id.city_field)
        aboutMe = findViewById(R.id.about_me)

        pb = findViewById(R.id.pb)
        pb2 = findViewById(R.id.pb2)

        layoutManager = GridLayoutManager(this, 3)



        val goBack = findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.go_back)

        goBack.setOnClickListener {
            onBackPressed()
        }

        val addAvatar = findViewById<Button>(R.id.update_profile_add_avatar)
        val addBanner = findViewById<Button>(R.id.update_profile_add_banner)
        updateButton = findViewById(R.id.update_profile_button)

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
        val user = JSONObject(util.readUserFile(File(filesDir, util.userFileName))).getJSONObject("user")
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        nameField?.setText(user.getString("name"))
        surnameField?.setText(user.getString("surname"))
        dateField?.setText(user.getString("age"))
        emailField?.setText(user.getString("email"))
        countryField?.setText(user.getString("country"))
        cityField?.setText(user.getString("city"))

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
                val data = Utils.request(this, "user", "GET", true, null)
                val obj = JSONObject(data).getJSONObject("user")
                runOnUiThread {
                    if(avatar != null && obj.getString("avatarUrl") != "") {
                        Picasso.get().load(Constants().SITE_NAME_FILES + "/useravatars/${obj.getString("avatarUrl")}").into(avatar)
                        avatar?.scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    if(banner != null && obj.getString("bannerUrl") != "") {
                        Picasso.get().load(Constants().SITE_NAME_FILES + "/userbanners/${obj.getString("bannerUrl")}").into(banner)
                        banner?.scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "ERROR")
            }
        }.start()
    }

    private fun update() {
        if (nameField?.text?.toString()?.isEmpty()!!) {
            Toast.makeText(this, R.string.register_name_error, Toast.LENGTH_SHORT).show()
            return
        }
        if (dateField?.text?.toString()?.isNotEmpty()!!) {
            if (!isDateValid(dateField?.text.toString())) {
                Toast.makeText(this, R.string.register_date_error, Toast.LENGTH_SHORT).show()
                return
            }
        }
        if (!emailField?.text?.toString()?.isEmailValid()!!) {
            Toast.makeText(this, R.string.register_email_error, Toast.LENGTH_SHORT).show()
            return
        }
        Log.e("TAG", "Проверка прошла")
        updateInBackground()
    }

    private fun String.isEmailValid(): Boolean {
        return !TextUtils.isEmpty(this) && android.util.Patterns.EMAIL_ADDRESS.matcher(this)
            .matches()
    }

    private fun isDateValid(dateStr: String): Boolean {
        try {
            var formatter = SimpleDateFormat("dd.mm.yyyy", Locale.getDefault())
            val date = formatter.parse(dateStr)
            println(date)
        } catch (e: ParseException) {
            return false
        }
        return true
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
        updateProfile(token!!)
        Utils().clearUserData(filesDir)
        runLoginActivity()
    }

    private fun runLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
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

        if(avatarFile != null) {
            requestFile1 = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), avatarFile!!)
            body1 = MultipartBody.Part.createFormData("file", avatarFile?.name, requestFile1)
        }
        if(bannerFile != null) {
            requestFile2 = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), bannerFile!!)
            body2 = MultipartBody.Part.createFormData("file2", bannerFile?.name, requestFile2)
        }

        val nameBody = RequestBody.create("text/plain".toMediaTypeOrNull(), nameField?.text.toString())
        val surnameBody = RequestBody.create("text/plain".toMediaTypeOrNull(), surnameField?.text.toString())
        val dateBody = RequestBody.create("text/plain".toMediaTypeOrNull(), dateField?.text.toString())
        val emailBody = RequestBody.create("text/plain".toMediaTypeOrNull(), emailField?.text.toString())
        val aboutMeBody = RequestBody.create("text/plain".toMediaTypeOrNull(), aboutMe?.text.toString())
        val city = RequestBody.create("text/plain".toMediaTypeOrNull(), cityField?.text.toString())
        val country = RequestBody.create("text/plain".toMediaTypeOrNull(), countryField?.text.toString())

        try {
            CoroutineScope(Dispatchers.IO).launch {
                chatLogApi.uploadImage(
                    nameBody,
                    surnameBody,
                    dateBody,
                    emailBody,
                    aboutMeBody,
                    city, country,
                    body1, body2, "Bearer $token"
                )
            }
        } catch(e: IllegalStateException) {
            Log.e("TAG", "Ошибка но ничего страшного")
        }
    }

}
