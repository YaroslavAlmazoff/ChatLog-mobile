package com.chatlog.chatlog

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
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

    var uploadScreen: View? = null
    var pickImagesCancel: TextView? = null
    lateinit var rs: Cursor
    var greed: GridView? = null
    var pickImages: View? = null

    var currentMode: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_public)

        pickImagesCancel = findViewById(R.id.pick_images_cancel)
        greed = findViewById(R.id.greed)
        pickImages = findViewById(R.id.pick_images)
        pickImagesCancel?.setOnClickListener {
            greed?.adapter = null
            pickImages?.visibility = View.GONE
            uploadScreen?.visibility = View.GONE
        }

        nameField = findViewById(R.id.name_field)
        descriptionField = findViewById(R.id.description_field)

//        val goBack = findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.go_back)
//
//        goBack.setOnClickListener {
//            val intent = Intent(this, UserActivity::class.java)
//            intent.putExtra("id", userData?.getJSONObject("user")?.getString("_id"))
//            startActivity(intent)
//        }

        val addAvatar = findViewById<Button>(R.id.create_public_add_avatar)
        val addBanner = findViewById<Button>(R.id.create_public_add_banner)
        val createButton = findViewById<Button>(R.id.create_button)

        avatar = findViewById(R.id.create_public_avatar)
        banner = findViewById(R.id.create_public_banner)

        createButton.setOnClickListener {
            update()
        }

        val imagesLayout = findViewById<View>(R.id.update_profile_images)

        addAvatar.setOnClickListener {
            currentMode = "avatar"
            uploadImage("avatar")
        }
        addBanner.setOnClickListener {
            currentMode = "banner"
            uploadImage("banner")
        }

        val util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))
    }

    private fun uploadImage(mode: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 101)
        } else {
            listFiles(mode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.e("TAG", requestCode.toString())
        when (requestCode) {
            101 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    listFiles(currentMode)
                } else {
                    Toast.makeText(this, "Разрешение отклонено", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun listFiles(mode: String) {
        var cols = listOf(MediaStore.Images.Thumbnails.DATA).toTypedArray()
        rs = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cols, null, null, null)!!
        pickImages?.visibility = View.VISIBLE
        greed?.adapter = ImagesAdapter(applicationContext, mode)
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

                if(avatarFile != null) {
                    requestFile1 = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), avatarFile!!)
                    body1 = MultipartBody.Part.createFormData("avatar", avatarFile?.name, requestFile1)
                }
                if(bannerFile != null) {
                    requestFile2 = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), bannerFile!!)
                    body2 = MultipartBody.Part.createFormData("banner", bannerFile?.name, requestFile2)
                }

                var name = RequestBody.create("text/plain".toMediaTypeOrNull(), nameField?.text.toString())
                var description = RequestBody.create("text/plain".toMediaTypeOrNull(), descriptionField?.text.toString())

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
                    }
                } catch(e: IllegalStateException) {
                    Log.e("TAG", "Ошибка но ничего страшного")
                }
            } catch (e: InterruptedException) {
                Log.e("TAG", "AAA")
            }
        }.start()
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
            var iv = ImageView(context)
            rs.moveToPosition(position)
            var path = rs.getString(0)
            var bitmap = BitmapFactory.decodeFile(path)
            iv.setImageBitmap(bitmap)
            iv.layoutParams = AbsListView.LayoutParams(300, 300)

            iv.setOnClickListener {
                pickImagesCancel?.text = "Загрузка изображения..."
                Log.e("TAG", "Вы нажали на картинку $path")
                val f = if(mode == "avatar") File(filesDir, "file")
                else if(mode == "banner") File(filesDir, "file2")
                else File(filesDir, "file")


                f.createNewFile();

                val bitmap = bitmap;
                val bos = ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
                val bitmapData = bos.toByteArray();

                val fos = FileOutputStream(f);
                fos.write(bitmapData);
                fos.flush();
                fos.close();
                pickImages?.visibility = View.GONE
                uploadScreen?.visibility = View.GONE
                if(mode == "avatar") {
                    avatarFile = f
                    avatar?.setImageBitmap(bitmap)
                    avatar?.visibility = View.VISIBLE
                } else if(mode == "banner") {
                    bannerFile = f
                    banner?.setImageBitmap(bitmap)
                    banner?.visibility = View.VISIBLE
                }
                pickImagesCancel?.text = "Отмена"
                currentMode = ""
            }
            return iv
        }
    }
}