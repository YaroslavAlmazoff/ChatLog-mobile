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
import android.view.View
import android.view.ViewGroup
import android.widget.*
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_discussion)

        val util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        createButton = findViewById(R.id.create_button)
        uploadButton = findViewById(R.id.upload_button)
        cancelButton = findViewById(R.id.cancel_button)
        titleField = findViewById(R.id.title_field)

        greed = findViewById(R.id.greed)
        pickImages = findViewById(R.id.pick_images)
        pickImagesCancel = findViewById(R.id.pick_images_cancel)
        image = findViewById(R.id.image)

        pickImagesCancel?.setOnClickListener {
            pickImages?.visibility = View.GONE
        }

        uploadButton?.setOnClickListener {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), 101)
            } else {
                listFiles()
            }
        }
        cancelButton?.setOnClickListener {
            onBackPressed()
        }
        createButton?.setOnClickListener {
            createDiscussion()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.e("TAG", requestCode.toString())
        when (requestCode) {
            101 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    listFiles()
                } else {
                    Toast.makeText(this, "Разрешение отклонено", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }
    private fun listFiles() {
        var cols = listOf(MediaStore.Images.Thumbnails.DATA).toTypedArray()
        rs = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cols, null, null, null)!!
        pickImages?.visibility = View.VISIBLE
        greed?.adapter = ImagesAdapter(applicationContext)
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

                if(imageFile != null) {
                    requestFile = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), imageFile!!)
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
                    requestFile = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), imageFile!!)
                    body = MultipartBody.Part.createFormData("file", imageFile?.name, requestFile)
                }

                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        chatLogApi.createDiscussion(
                            titleField?.text.toString(), body, "Bearer $token"
                        )
                    }
                } catch(e: IllegalStateException) {
                    Log.e("TAG", "Ошибка но ничего страшного")
                } finally {
                    val intent = Intent(this, MessengerListActivity::class.java)
                    startActivity(intent)
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", e.message!!)
            }
        }.start()
    }


    inner class ImagesAdapter : BaseAdapter {
        var context: Context
        constructor(context: Context) {
            this.context = context
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
                    val f = File(filesDir, "file")
                    f.createNewFile()

                    val bitmap = bitmap
                    val bos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos)
                    val bitmapData = bos.toByteArray()

                    val fos = FileOutputStream(f)
                    fos.write(bitmapData)
                    fos.flush()
                    fos.close()
                    imageFile = f
                    pickImages?.visibility = View.GONE
                    image?.setImageURI(Uri.fromFile(imageFile))
                    pickImagesCancel?.text = "Отмена"
                }

                return iv
        }
    }
}