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
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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

    val GALERY_ADD_AVATAR = 1
    val GALERY_ADD_BANNER = 2


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
        setContentView(R.layout.activity_update_public)

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

        val goBack = findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.go_back)

        goBack.setOnClickListener {
            val intent = Intent(this, UserActivity::class.java)
            intent.putExtra("id", userData?.getJSONObject("user")?.getString("_id"))
            startActivity(intent)
        }

        val addAvatar = findViewById<Button>(R.id.update_profile_add_avatar)
        val addBanner = findViewById<Button>(R.id.update_profile_add_banner)
        val updateButton = findViewById<Button>(R.id.update_profile_button)

        avatar = findViewById(R.id.update_profile_avatar)
        banner = findViewById(R.id.update_profile_banner)

        updateButton.setOnClickListener {
            update()
        }

        val imagesLayout = findViewById<View>(R.id.update_profile_images)

//        addAvatar.setOnClickListener {
//            imagesLayout.visibility = View.VISIBLE
//            val intent = Intent(Intent.ACTION_PICK)
//            intent.type = "image/*"
//            startActivityForResult(intent, GALERY_ADD_AVATAR)
//        }
        addAvatar.setOnClickListener {
            currentMode = "avatar"
            uploadImage("avatar")
        }
//        addBanner.setOnClickListener {
//            imagesLayout.visibility = View.VISIBLE
//            val intent = Intent(Intent.ACTION_PICK)
//            intent.type = "image/*"
//            startActivityForResult(intent, GALERY_ADD_BANNER)
//        }
        addBanner.setOnClickListener {
            currentMode = "banner"
            uploadImage("banner")
        }

        val util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        getData()
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



    private fun uploadImage(mode: String) {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 101)
        } else {
            listFiles(mode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.e("TAG", requestCode.toString())
        when (requestCode) {
            101 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    listFiles(currentMode)
                } else {
                    Toast.makeText(this, "Разрешение отклонено", Toast.LENGTH_LONG).show()
                }
                return
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
        updateProfile(token!!)
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
        val name = RequestBody.create("text/plain".toMediaTypeOrNull(), nameField?.text?.toString()!!)
        val description = RequestBody.create("text/plain".toMediaTypeOrNull(), descriptionField?.text?.toString()!!)

        try {
            CoroutineScope(Dispatchers.IO).launch {
                val response = chatLogApi.updatePublic(
                    intent.getStringExtra("id")!!,
                    name, description,
                    body1, body2, "Bearer $token"
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
