package com.chatlog.chatlog

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import javax.net.ssl.HttpsURLConnection


class FilesAdapter(private val files: ArrayList<CloudFile>,
                   val context: Context,
                   val activity: Activity,
                   val currentFolder: CurrentFolder,
                   val updatePath: () -> Unit,
                   val showImage: (String) -> Unit,
                   val noFiles: TextView?,
                   val userData: JSONObject) : RecyclerView.Adapter<FilesAdapter.ViewHolder>(), IFilter {
    private var filteredList = ArrayList(files)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.file_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        holder.name?.text = Utils.shortName(file.name, 20)

        if(file.type != "folder") {
            holder.download?.setOnClickListener {
                holder.download?.text = "Скачивание...."
                Log.e("TAG", "DOWNLOAD")
                val pathArray = file.path.split("/")
                Utils.downloadFile(file.name,
                    Constants().SITE_NAME_FILES + "/" + pathArray.slice(4 until pathArray.size).joinToString("/"),
                    it.context, "") {
                    holder?.download?.text = "Скачано в папку DOWNLOADS"
                    holder?.download?.setOnClickListener {  }
                }
            }
        } else {
            holder.download?.setOnClickListener {
                holder.download?.text = "Скачивание...."
                downloadFolder(file.id, file.name) {
                    holder?.download?.text = "Скачано в папку DOWNLOADS"
                    holder?.download?.setOnClickListener {  }
                }
            }
        }


        Thread {
            try {
                var path = ""
                if(file.ext == "jpg" ||
                    file.ext == "jpeg" ||
                    file.ext == "png" ||
                    file.ext == "gif" ||
                    file.ext == "bmp") {
                    Log.e("TAG", file.path)
                    val pathArray = file.path.split("/")
                    path = Constants().SITE_NAME_FILES + "/" + pathArray.slice(4 until pathArray.size)
                        .joinToString("/")
                    Log.e("TAG", path)
                    activity.runOnUiThread {
                        Picasso.get().load(path).into(holder.image)
                        holder.image?.scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    activity.runOnUiThread {
                        holder.image?.setOnClickListener {
                            showImage(path)
                        }
                    }
                } else if(file.ext == "mp3") {
                    val pathArray = file.path.split("/")
                    path = Constants().SITE_NAME_FILES + "/" + pathArray.slice(4 until pathArray.size)
                        .joinToString("/")

                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(path)
                    val pictureByteArray = retriever.embeddedPicture

                    if (pictureByteArray != null) {
                        val bitmap = BitmapFactory.decodeByteArray(pictureByteArray, 0, pictureByteArray.size)
                        activity.runOnUiThread {holder.image?.setImageBitmap(bitmap)}
                    } else {
                        activity.runOnUiThread {holder.image?.setImageResource(R.drawable.default_music)}
                    }

                } else if(file.ext == "mp4" || file.ext == "avi") {
                    val path = Constants().SITE_NAME_FILES + "/filepreviews/${file.previewUrl}"
                    Log.e("TAG", path)
                    if(file.previewUrl != null) {
                        Log.e("TAG", file.previewUrl!!)
                    }
                    activity.runOnUiThread {
                        Picasso.get().load(path).into(holder.image)
                        holder.image?.scaleType = ImageView.ScaleType.CENTER_CROP
                    }

                } else if(file.type == "folder") {
                    path = "/filesicons/${file.type.lowercase()}.png"
                    Log.e("TAG", Constants().SITE_NAME_FILES + path)
                    activity.runOnUiThread {Picasso.get().load(Constants().SITE_NAME_FILES + path).into(holder.image)}
                } else {
                    path = "/filesicons/${file.ext.lowercase()}.png"
                    Log.e("TAG", Constants().SITE_NAME_FILES + path)
                    activity.runOnUiThread {Picasso.get().load(Constants().SITE_NAME_FILES + path).into(holder.image)}
                }
                if(file.type == "folder") {
                    activity.runOnUiThread {
                        holder.root?.setOnClickListener {
                            currentFolder.name = file.name
                            updatePath()
                        }
                    }
                } else {
                    activity.runOnUiThread { holder.download?.visibility = View.VISIBLE }
                }
            } catch (e: InterruptedException) {
                Log.e("TAG", "Error")
            }
        }.start()

        if(file.type != "folder") {
            holder?.root?.setOnClickListener {
                val intent = Intent(it.context, FileActivity::class.java)
                intent.putExtra("name", file.name)
                intent.putExtra("type", file.type)
                intent.putExtra("size", file.size)
                intent.putExtra("path", file.path)
                intent.putExtra("id", file.id)
                intent.putExtra("ext", file.ext)
                intent.putExtra("preview", file.previewUrl)
                intent.putExtra("owner", file.owner)
                intent.putExtra("public", file.public)
                it.context.startActivity(intent)
            }
        }
    }

    fun downloadFolder(id: String, name: String, onComplete: () -> Unit) {
        Thread {
            try {
                val token = userData?.getString("token")
                val url = URL(Constants().SITE_NAME + "cloud/download-folder/$id")
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
                val archiveUrl = JSONObject(result).getString("archiveUrl")
                if(archiveUrl != "") {
                    val archivePath = Constants().SITE_NAME_FILES + "/temp/$archiveUrl"
                    Log.e("TAG", archivePath)
                    Utils.downloadFile(name, archivePath, context, archiveUrl) {
                        onComplete()
                    }
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", e.message!!)
            }
        }.start()
    }


    override fun getItemCount(): Int {
        return filteredList.size
    }

    override fun filter(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(files)
        } else {
            for (item in files) {
                if (item.name.contains(query, ignoreCase = true)) {
                    filteredList.add(item)
                }
            }
            if(filteredList.isEmpty()) {
                noFiles?.visibility = View.VISIBLE
                noFiles?.text = "Ваш запрос не дал результатов"
            } else {
                noFiles?.visibility = View.GONE
                noFiles?.text = ""
            }
        }
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var name: TextView? = null
        var image: ImageView? = null
        var download: TextView? = null
        var card: View? = null
        var root: View? = null

        init {
            name = itemView.findViewById(R.id.file_name)
            image = itemView.findViewById(R.id.file_icon)
            download = itemView.findViewById(R.id.file_download)
            card = itemView.findViewById(R.id.file_image_wrapper)
            root = itemView
        }
    }
}