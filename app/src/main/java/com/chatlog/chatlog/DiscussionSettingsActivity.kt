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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.util.Util
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
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class DiscussionSettingsActivity : AppCompatActivity() {
    var usersList: RecyclerView? = null
    var userData: JSONObject? = null
    var usersArray: ArrayList<User> = ArrayList()
    var pb: ProgressBar? = null
    var avatar: ImageView? = null
    var titleField: EditText? = null
    var saveButton: com.sanojpunchihewa.glowbutton.GlowButton? = null
    var addButton: com.sanojpunchihewa.glowbutton.GlowButton? = null
    var changeAvatar: TextView? = null

    var uploadScreen: View? = null
    var pickImagesCancel: TextView? = null
    lateinit var rs: Cursor
    var greed: GridView? = null
    var pickImages: View? = null
    var imageFile: File? = null

    var leave: TextView? = null
    var remove: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discussion_settings)

        var util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        avatar = findViewById(R.id.avatar)
        titleField = findViewById(R.id.title_field)
        saveButton = findViewById(R.id.save_button)
        addButton = findViewById(R.id.add_button)
        changeAvatar = findViewById(R.id.change_avatar)

        pickImagesCancel = findViewById(R.id.pick_images_cancel)
        greed = findViewById(R.id.greed)
        pickImages = findViewById(R.id.pick_images)
        pickImagesCancel?.setOnClickListener {
            greed?.adapter = null
            pickImages?.visibility = View.GONE
            uploadScreen?.visibility = View.GONE
        }

        leave = findViewById(R.id.leave_discussion)

        leave?.setOnClickListener {
            leaveDiscussion()
        }

        if(intent.getStringExtra("avatarUrl") != "") {
            Picasso.get().load(Constants().SITE_NAME_FILES + "/chatavatars/${intent.getStringExtra("avatarUrl")}").into(avatar)
        }
        Log.e("TAG", intent.getStringExtra("creator")!!)
        Log.e("TAG", userData?.getJSONObject("user")?.getString("_id")!!)
        Log.e("TAG", (intent.getStringExtra("creator")!! == userData?.getJSONObject("user")?.getString("_id")).toString())
        if(intent.getStringExtra("creator") == userData?.getJSONObject("user")?.getString("_id")) {
            remove = findViewById(R.id.remove_discussion)
            remove?.visibility = View.VISIBLE
            remove?.setOnClickListener {
                removeDiscussion()
            }
        }

        avatar?.scaleType = ImageView.ScaleType.CENTER_CROP
        titleField?.setText(intent.getStringExtra("title"))

        findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.back_button).setOnClickListener {
            val myIntent = Intent(this, ChatMessengerActivity::class.java)
            myIntent.putExtra("id", intent.getStringExtra("id"))
            startActivity(myIntent)
        }

        saveButton?.setOnClickListener {
            saveDiscussion()
        }

        changeAvatar?.setOnClickListener {
            uploadImage()
        }
        addButton?.setOnClickListener {
            val myIntent = Intent(this, AddMembersActivity::class.java)
            myIntent.putExtra("id", intent.getStringExtra("id"))
            startActivity(myIntent)
        }
        avatar?.setOnClickListener {
            uploadImage()
        }

        usersList = findViewById(R.id.members_list)

        pb = findViewById(R.id.pb)

        val adapter = MembersAdapter(usersArray, intent.getStringExtra("id")!!, userData, this)
        usersList?.adapter = adapter
        usersList?.layoutManager = LinearLayoutManager(this)

        Log.e("TAG", "before getUsersInBackground")

        getUsersInBackground(adapter)

        adapter?.filter("")
    }

    private fun uploadImage() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), 101)
        } else {
            listFiles()
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

    private fun getUsersInBackground(adapter: MembersAdapter) {
        Thread {
            try {
                Log.e("TAG", "in getUsersInBackground")
                getUsers(adapter)
                runOnUiThread {
                    adapter?.notifyDataSetChanged()
                    pb?.visibility = View.GONE
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "все плохо")
            }
        }.start()
    }
    private fun getUsers(adapter: MembersAdapter) {
        Log.e("TAG", "in getUsers")
        val usersData = Utils.request(this, "fullmembers/${intent.getStringExtra("id")}", "GET", false, null)
        val usersDataArray = JSONObject(usersData).getJSONArray("members")
        Log.e("TAG", usersData)
        for(i in 0 until usersDataArray.length()) {
            usersArray.add(User(
                usersDataArray.getJSONObject(i).getString("name"),
                usersDataArray.getJSONObject(i).getString("surname"),
                usersDataArray.getJSONObject(i).getString("age"),
                usersDataArray.getJSONObject(i).getString("country"),
                usersDataArray.getJSONObject(i).getString("city"),
                usersDataArray.getJSONObject(i).getString("avatarUrl"),
                usersDataArray.getJSONObject(i).getString("_id")
            ))
        }
        runOnUiThread {
            adapter?.filter("")
        }
    }

    private fun leaveDiscussion() {
        Thread {
            try {
                val result = Utils.request(this, "leave/${intent.getStringExtra("id")}", "DELETE", true, null)
                runOnUiThread {
                    if(JSONObject(result).getJSONArray("errors").length() != 0) {
                        Toast.makeText(applicationContext, JSONObject(result).getJSONArray("errors").getString(0), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(applicationContext, resources.getString(R.string.left), Toast.LENGTH_LONG).show()
                        val intent = Intent(this, MessengerListActivity::class.java)
                        startActivity(intent)
                    }
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "Error")
            }
        }.start()
    }

    private fun removeDiscussion() {
        Thread {
            try {
                Log.e("TAG", "fuck")
                val result = Utils.request(this, "remove/${intent.getStringExtra("id")}", "DELETE", true, null)
                Log.e("TAG", result)
                runOnUiThread {
                    if(JSONObject(result).getJSONArray("errors").length() != 0) {
                        Toast.makeText(applicationContext, JSONObject(result).getJSONArray("errors").getString(0), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(applicationContext, resources.getString(R.string.removed), Toast.LENGTH_LONG).show()
                        val intent = Intent(this, MessengerListActivity::class.java)
                        startActivity(intent)
                    }
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "Error")
            }
        }.start()
    }


    private fun saveDiscussion() {
        Thread {
            try {
                val interceptor = HttpLoggingInterceptor()
                interceptor.level = HttpLoggingInterceptor.Level.BODY

                val token = Utils.updateToken(this)

                val client = OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(Constants().SITE_NAME).client(client)
                    .addConverterFactory(GsonConverterFactory.create()).build()
                val chatLogApi = retrofit.create(ChatLogApi::class.java)

                var requestFile: RequestBody? = null
                var body: MultipartBody.Part? = null

                if(imageFile != null) {
                    requestFile = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), imageFile!!)
                    body = MultipartBody.Part.createFormData("file", imageFile?.name, requestFile)
                }

                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        chatLogApi.saveDiscussion(
                            intent?.getStringExtra("id")!!,
                            titleField?.text.toString(), body, token
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
                val f = File(filesDir, "file");
                f.createNewFile();
                val bitmap = bitmap;
                val bos = ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
                val bitmapData = bos.toByteArray();
                val fos = FileOutputStream(f);
                fos.write(bitmapData);
                fos.flush();
                fos.close();
                imageFile = f
                pickImages?.visibility = View.GONE
                uploadScreen?.visibility = View.GONE
                avatar?.setImageBitmap(bitmap)
                pickImagesCancel?.text = "Отмена"
                greed?.adapter = null
            }
            return iv
        }
    }
}