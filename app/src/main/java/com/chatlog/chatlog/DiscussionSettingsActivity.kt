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

    var imageFile: File? = null

    var leave: TextView? = null
    var remove: TextView? = null

    var pb2: ProgressBar? = null

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

        pb = findViewById(R.id.pb)
        pb2 = findViewById(R.id.pb2)

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
            pb?.visibility = View.VISIBLE
            saveDiscussion()
        }

        changeAvatar?.setOnClickListener {
            pb?.visibility = View.VISIBLE
            saveButton?.visibility = View.GONE
            selectImageLauncher.launch("image/*")
        }
        addButton?.setOnClickListener {
            val myIntent = Intent(this, AddMembersActivity::class.java)
            myIntent.putExtra("id", intent.getStringExtra("id"))
            startActivity(myIntent)
        }
        avatar?.setOnClickListener {
            pb?.visibility = View.VISIBLE
            saveButton?.visibility = View.GONE
            selectImageLauncher.launch("image/*")
        }

        usersList = findViewById(R.id.members_list)

        val adapter = MembersAdapter(usersArray, intent.getStringExtra("id")!!, userData, this)
        usersList?.adapter = adapter
        usersList?.layoutManager = LinearLayoutManager(this)

        Log.e("TAG", "before getUsersInBackground")

        getUsersInBackground(adapter)

        adapter?.filter("")
    }

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Обработка выбранного изображения
        if (uri != null) {
            avatar?.setImageURI(uri)
            Thread {
                val inputStream = contentResolver.openInputStream(uri)
                val file = File(cacheDir, "file") // Создаем временный файл
                file.createNewFile()
                val fos = FileOutputStream(file)

                inputStream?.copyTo(fos)

                imageFile = file

                runOnUiThread {
                    pb?.visibility = View.GONE
                    saveButton?.visibility = View.VISIBLE
                }
            }.start()
        }
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
}