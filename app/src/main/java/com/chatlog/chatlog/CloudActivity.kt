package com.chatlog.chatlog

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.*
import java.lang.IllegalStateException
import java.net.URI
import java.net.URL
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

class CloudActivity : AppCompatActivity() {
    var filesList: RecyclerView? = null
    var pathList: RecyclerView? = null
    var filesArray: ArrayList<CloudFile> = ArrayList()
    var pathArray: ArrayList<PathItem> = ArrayList()

    var fullScreen: View? = null
    var fullScreenImage: ImageView? = null
    var cancel: TextView? = null

    var userData: JSONObject? = null
    var pb: ProgressBar? = null
    var currentFolder = CurrentFolder("root", "")
    var createFolderCancel: TextView? = null

    var files: ArrayList<File> = ArrayList()
    var file: File? = null

    var noFiles: TextView? = null
    var filesAdapter: FilesAdapter? = null
    var folderField: EditText? = null
    var searchField: SearchView? = null
    var sort: ImageView? = null
    var sortMenu: View? = null
    var createFolderWrapper: View? = null
    var searchCancel: TextView? = null

    var searchString: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud)

        searchField = findViewById(R.id.search_field)
        sort = findViewById(R.id.sort)
        sortMenu = findViewById(R.id.sort_menu)
        searchCancel = findViewById(R.id.search_cancel)
        noFiles = findViewById(R.id.no_files)

        val initWidth = searchField?.width

        searchField?.setOnSearchClickListener {
            val layoutParams = searchField?.layoutParams
            layoutParams?.width = initWidth!! + 500 // change this value to adjust the width
            searchField?.layoutParams = layoutParams
        }
        searchField?.setOnCloseListener {
            val layoutParams = searchField?.layoutParams
            layoutParams?.width = 140 // change this value to adjust the width
            searchField?.layoutParams = layoutParams
            return@setOnCloseListener false
        }

        sort?.setOnClickListener {
            if(sortMenu?.visibility == View.GONE) {
                sortMenu?.visibility = View.VISIBLE
            } else {
                sortMenu?.visibility = View.GONE
                getFiles("", null)
            }

        }


        findViewById<View>(R.id.sort_images).setOnClickListener {
            getFiles("sort", "images")
        }
        findViewById<View>(R.id.sort_videos).setOnClickListener {
            getFiles("sort", "videos")
        }
        findViewById<View>(R.id.sort_audios).setOnClickListener {
            getFiles("sort", "audios")
        }
        findViewById<View>(R.id.sort_documents).setOnClickListener {
            getFiles("sort", "documents")
        }
        findViewById<View>(R.id.sort_other).setOnClickListener {
            getFiles("sort", "other")
        }



        searchCancel?.setOnClickListener {
            searchString = ""
            getFiles("", null)
            searchCancel?.visibility = View.GONE
        }

        searchField?.setOnQueryTextListener(object : SearchView.OnQueryTextListener, SearchView.OnSuggestionListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if(query != null) {
                    searchString = query
                    getFiles("search", null)
                }
                searchCancel?.visibility = View.VISIBLE
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
//                newText?.let {
//                    filesAdapter?.filter(it)
//                }
                return true
            }

            override fun onSuggestionClick(position: Int): Boolean {
                Log.e("TAG", "chatlog")
                return true
            }

            override fun onSuggestionSelect(position: Int): Boolean {
                Log.e("TAG", "chatlog")
                return true
            }
        })



        val util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))
        filesList = findViewById(R.id.files_list)
        pathList = findViewById(R.id.path)

        fullScreen = findViewById(R.id.fullscreen)
        fullScreenImage = findViewById(R.id.fullscreen_image)
        cancel = findViewById(R.id.cancel)

        pb = findViewById(R.id.pb)
        folderField = findViewById(R.id.folder_name_field)
        createFolderWrapper = findViewById(R.id.create_folder_wrapper)
        createFolderCancel = findViewById(R.id.create_folder_cancel)

        createFolderCancel?.setOnClickListener {
            createFolderWrapper?.visibility = View.GONE
        }

        filesAdapter = FilesAdapter(filesArray, applicationContext, this, currentFolder, {
            updatePath()
            getFiles("", null)
        }, {
            showImage(it)
        }, noFiles, userData!!)

        filesList?.layoutManager = LinearLayoutManager(this)
        filesList?.adapter = filesAdapter

        pathList?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        pathList?.adapter = PathAdapter(pathArray, currentFolder) {
            updatePath()
            getFiles("", null)
        }

        pathArray?.add(PathItem("root", ""))

        getFiles("", null)

        filesAdapter?.filter("")

        findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.upload_button).setOnClickListener {
            openFileChooser()
        }
        findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.create_button).setOnClickListener {
            mkdir()
        }
        findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.create_folder_button).setOnClickListener {
            createFolderWrapper?.visibility = View.VISIBLE
        }

        cancel?.setOnClickListener {
            closeImage()
        }
    }

    private fun updatePath() {
        pathArray.clear()
        if(currentFolder.name != "root") {
            Thread {
                try {
                    val token = userData?.getString("token")
                    val url = URL(Constants().SITE_NAME + "cloud/getpath-mobile")
                    val connection = url.openConnection() as HttpsURLConnection
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Accept-Charset", "utf-8")
                    connection.setRequestProperty("Authorization", "Bearer $token")

                    val json = "{\"id\": \"${currentFolder.id}\"}"
                    Log.e("TAG", json)
                    connection.outputStream.write(json.toByteArray())

                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    var line: String? = reader.readLine()
                    var result = ""

                    while (line != null) {
                        result += line
                        line = reader.readLine()
                    }

                    reader.close()
                    connection.disconnect()
                    Log.e("TAG", result)
                    val pathArr = JSONObject(result).getJSONArray("path")
                    Log.e("TAG", "названия папок Json")
                    pathArray.add(PathItem("root", ""))
                    for(i in 0 until pathArr.length()) {
                        pathArray.add(PathItem(pathArr.getJSONObject(i).getString("name"), pathArr.getJSONObject(i).getString("id")))
                        Log.e("TAG", pathArr.getString(i))
                    }
                    Log.e("TAG", "названия папок array")
                    for(i in 0 until pathArray.size) {
                        Log.e("TAG", pathArray[i].name)
                    }
                    runOnUiThread {
                        pathList?.adapter?.notifyDataSetChanged()
                    }
                } catch (e: InterruptedException) {
                    Log.e("TAG", "Error")
                }
            }.start()
        } else {
            pathArray.add(PathItem(currentFolder.name, ""))
            pathList?.adapter?.notifyDataSetChanged()
        }

    }

    private fun getFiles(mode: String, type: String?) {
        Thread {
            try {
                filesArray?.clear()
                Log.e("TAG", currentFolder.name)
                val token = userData?.getString("token")
                val url =
                    if (mode == "search") URL(Constants().SITE_NAME + "cloud/filesbysearch")
                    else if(mode == "sort") URL(Constants().SITE_NAME + "cloud/sortedfiles")
                    else URL(Constants().SITE_NAME + "cloud/filesbyfolder")
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept-Charset", "utf-8")
                connection.setRequestProperty("Authorization", "Bearer $token")

                var fileType = ""

                if(type != null) {
                    fileType = type
                }

                val json = "{\"id\": \"${currentFolder.id}\", \"search\": \"${searchString}\", \"sort\": \"$fileType\"}"
                Log.e("TAG", json)
                connection.outputStream.write(json.toByteArray())

                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
                var line: String? = reader.readLine()
                var result = ""

                while (line != null) {
                    result += line
                    line = reader.readLine()
                }

                reader.close()
                connection.disconnect()
                Log.e("TAG", result)
                if(result.contains("verified")) {
                    return@Thread
                }
                val files = JSONObject(result).getJSONArray("files")
                if(files.length() == 0) {
                    runOnUiThread {
                        noFiles?.visibility = View.VISIBLE
                        if(mode == "search") noFiles?.text = "Ваш запрос не дал результатов"
                        if(mode == "sort") noFiles?.text = "Таких файлов здесь пока нет"
                        else noFiles?.text = "У Вас пока нет файлов"
                    }
                } else {
                    runOnUiThread {
                        noFiles?.visibility = View.GONE
                        noFiles?.text = ""
                    }
                }
                for(i in 0 until files.length()) {
                    filesArray.add(CloudFile(
                        files.getJSONObject(i).getString("name"),
                        files.getJSONObject(i).getString("path"),
                        files.getJSONObject(i).getString("ext"),
                        files.getJSONObject(i).getString("type"),
                        files.getJSONObject(i).getString("size"),
                        files.getJSONObject(i).getString("owner"),
                        files.getJSONObject(i).getString("folder"),
                        files.getJSONObject(i).getBoolean("public"),
                        files.getJSONObject(i).getString("_id"),
                        files.getJSONObject(i).getString("previewUrl"),
                    ))
                }

                filesArray.reverse()

                runOnUiThread {
                    filesList?.adapter?.notifyDataSetChanged()
                    filesAdapter?.filter("")
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", e.message!!)
            }
        }.start()
    }


    private fun mkdir() {
        createFolderWrapper?.visibility = View.GONE
        Thread {
            try {
                val token = userData?.getString("token")
                val url = URL(Constants().SITE_NAME + "cloud/mkdir-mobile")
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept-Charset", "utf-8")
                connection.setRequestProperty("Authorization", "Bearer $token")

                val json = "{\"folderId\": \"${currentFolder.id}\", \"name\": \"${folderField?.text}\"}"
                Log.e("TAG", json)
                connection.outputStream.write(json.toByteArray())

                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
                var line: String? = reader.readLine()
                var result = ""

                while (line != null) {
                    result += line
                    line = reader.readLine()
                }

                reader.close()
                connection.disconnect()

                Log.e("TAG", result)

                val file = JSONObject(result).getJSONObject("file")
                val folder = CloudFile(
                    file.getString("name"),
                    file.getString("path"),
                    file.getString("ext"),
                    file.getString("type"),
                    file.getString("size"),
                    file.getString("owner"),
                    file.getString("folder"),
                    file.getBoolean("public"),
                    file.getString("_id"),
                    file.getString("previewUrl")
                )

                filesArray.add(0, folder)
                runOnUiThread {
                    filesAdapter?.notifyDataSetChanged()
                    filesAdapter?.filter("")
                }
            } catch (e: InterruptedException) {
                Log.e("TAG", "Пон")
            }
        }.start()
    }


    private val REQUEST_CODE = 1

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"  // Допустимые типы файлов
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
            "text/plain",
            "image/jpeg",
            "image/bmp",
            "image/gif",
            "image/jpg",
            "image/png",
            "video/mp4",
            "audio/mp3",
            "application/msword",
            "application/vnd.ms-excel",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/pdf",
            "application/x-pdf",
            "application/vnd.android.package-archive",
            "application/zip", "application/octet-stream", "application/x-zip-compressed", "multipart/x-zip"
        ))
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // Разрешить выбор нескольких файлов
        startActivityForResult(Intent.createChooser(intent, "Выберите файлы"), REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val destinationFolder = File(this.filesDir, "copy_folder")
        destinationFolder.mkdirs()

        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data?.clipData != null) {
                val clipData = data.clipData!!
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    val inputStream = this.contentResolver.openInputStream(uri)
                    val fileName = Utils.getFileNameFromUri(this, uri)
                    val destinationFile = File(destinationFolder, fileName)
                    Utils.copyFile(inputStream, destinationFile)
                    files.add(destinationFile)
                }
            } else if (data?.data != null) {
                val uri = data.data!!
                val inputStream = this.contentResolver.openInputStream(uri)
                val fileName = Utils.getFileNameFromUri(this, uri)
                Log.e("TAG", fileName!!)
                val destinationFile = File(destinationFolder, fileName)
                Utils.copyFile(inputStream, destinationFile)
                files.add(destinationFile)
            }
            uploadFile()
        }
    }


    private fun uploadFile() {
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

                val token = userData?.getString("token")

                val parts: ArrayList<MultipartBody.Part> = ArrayList()

                val names = ArrayList<String>()

                for(i in 0 until files?.size!!) {
                    parts.add(prepareFilePart(i)!!)
                    Log.e("TAG", files[i]?.name!!)
                    names.add(files[i]?.name!!)
                }

                var stringArray = StringArray(names)


                Log.e("TAG", currentFolder.id)
                Log.e("TAG", currentFolder.name)

                var folderId = RequestBody.create("text/plain".toMediaTypeOrNull(), currentFolder.id)
                var folderName = RequestBody.create("text/plain".toMediaTypeOrNull(), currentFolder.name)

                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        val response = chatLogApi.uploadFile(
                            parts, true, folderId, folderName, stringArray,"Bearer $token"
                        )
                        Log.e("TAG", response)
                        val filesJSON = JSONObject(response).getJSONArray("files")

                        filesArray.clear()
                        runOnUiThread {
                            filesList?.scrollToPosition(0)
                            filesList?.itemAnimator = DefaultItemAnimator()
                        }

                        for(i in 0 until filesJSON.length()) {
                            filesArray.add(CloudFile(
                                filesJSON.getJSONObject(i).getString("name"),
                                filesJSON.getJSONObject(i).getString("path"),
                                filesJSON.getJSONObject(i).getString("ext"),
                                filesJSON.getJSONObject(i).getString("type"),
                                filesJSON.getJSONObject(i).getString("size"),
                                filesJSON.getJSONObject(i).getString("owner"),
                                filesJSON.getJSONObject(i).getString("folder"),
                                filesJSON.getJSONObject(i).getBoolean("public"),
                                filesJSON.getJSONObject(i).getString("_id"),
                                filesJSON.getJSONObject(i).getString("previewUrl"),
                            ))
                        }
                        filesArray.reverse()
                        runOnUiThread {
                            filesAdapter?.notifyDataSetChanged()
                            filesAdapter?.filter("")
                        }
                    }
                } catch(e: IllegalStateException) {
                    Log.e("TAG", "Ошибка но ничего страшного")
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", e.message!!)
            }
        }.start()
    }

    private fun prepareFilePart(i: Int): MultipartBody.Part? {
        val file = files[i]
        var type = URLConnection.guessContentTypeFromName(file.name)
        if(type == null) {
            type = "multipart/form-data"
        }
        val requestBody: RequestBody = RequestBody.create(type.toMediaTypeOrNull(), file!!)
        Log.e("TAG",  files[i]?.name!!)
        return MultipartBody.Part.createFormData("file$i", files[i]?.name, requestBody)
    }

    private fun showImage(src: String) {
        fullScreen?.visibility = View.VISIBLE
        Picasso.get().load(src).into(fullScreenImage)
    }

    private fun closeImage() {
        fullScreen?.visibility = View.GONE
        fullScreenImage?.setImageURI(Uri.parse(""))
    }

    private fun sort(type: String) {
        Thread {
            try {
                val token = userData?.getString("token")
                val url = URL(Constants().SITE_NAME + "cloud/sortedfiles/$type")
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept-Charset", "utf-8")
                connection.setRequestProperty("Authorization", "Bearer $token")

                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
                var line: String? = reader.readLine()
                var result = ""

                while (line != null) {
                    result += line
                    line = reader.readLine()
                }

                reader.close()
                connection.disconnect()
                Log.e("TAG", result)

            } catch (e: InterruptedException) {
                Log.e("TAG", "Error")
            }
        }.start()
    }
}