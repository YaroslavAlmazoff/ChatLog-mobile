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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
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

class CreateDiscussion : AppCompatActivity() {
    var titleField: TextView? = null
    var createButton: com.sanojpunchihewa.glowbutton.GlowButton? = null
    var cancelButton: com.sanojpunchihewa.glowbutton.GlowButton? = null
    var uploadButton: com.sanojpunchihewa.glowbutton.GlowButton? = null

    lateinit var rs: Cursor
    var greed: GridView? = null
    var pickImages: View? = null
    var pickImagesCancel: TextView? = null
    var image: ImageView? = null
    var imageFile: File? = null
    var userData: JSONObject? = null

    var pb: ProgressBar? = null
    var pb2: ProgressBar? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_discussion)

        val util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        createButton = findViewById(R.id.create_button)
        uploadButton = findViewById(R.id.upload_button)
        cancelButton = findViewById(R.id.cancel_button)
        titleField = findViewById(R.id.title_field)

        pickImages = findViewById(R.id.pick_images)
        pickImagesCancel = findViewById(R.id.pick_images_cancel)
        image = findViewById(R.id.image)

        pb = findViewById(R.id.pb)

        uploadButton?.setOnClickListener {
            pb?.visibility = View.VISIBLE
            createButton?.visibility = View.GONE
            selectImageLauncher.launch("image/*")
        }
        cancelButton?.setOnClickListener {
            onBackPressed()
        }
        createButton?.setOnClickListener {
            pb?.visibility = View.VISIBLE
            createDiscussion()
        }
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
                    createButton?.visibility = View.VISIBLE
                }
            }.start()
        }
    }


    private fun createDiscussion() {
        Thread {
            try {
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

                val token = Utils.updateToken(this)

                if (imageFile != null) {
                    requestFile =
                        RequestBody.create("multipart/form-data".toMediaTypeOrNull(), imageFile!!)
                    body = MultipartBody.Part.createFormData("file", imageFile?.name, requestFile)
                } else {
                    val f = File(filesDir, "file")
                    f.createNewFile()

                    val bitmap = BitmapFactory.decodeStream(resources.openRawResource(R.raw.group))
                    val bos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos)
                    val bitmapData = bos.toByteArray()

                    val fos = FileOutputStream(f)
                    fos.write(bitmapData)
                    fos.flush()
                    fos.close()
                    imageFile = f
                    requestFile =
                        RequestBody.create("multipart/form-data".toMediaTypeOrNull(), imageFile!!)
                    body = MultipartBody.Part.createFormData("file", imageFile?.name, requestFile)
                }

                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        chatLogApi.createDiscussion(
                            titleField?.text.toString(), body, "Bearer $token"
                        )
                    }
                } catch (e: IllegalStateException) {
                    Log.e("TAG", "Ошибка но ничего страшного")
                } finally {
                    val intent = Intent(this, MessengerListActivity::class.java)
                    startActivity(intent)
                }
            } catch (e: InterruptedException) {
                Log.e("TAG", e.message!!)
            }
        }.start()
    }


}