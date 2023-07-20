package com.chatlog.chatlog

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

class FileActivity : AppCompatActivity() {
    var userData: JSONObject? = null

    var download: TextView? = null
    var delete: TextView? = null
    var send: TextView? = null
    var sendLink: TextView? = null
    var edit: TextView? = null

    var text: TextView? = null
    var image: ImageView? = null
    var imageWrapper: View? = null
    var audio: View? = null
    var startAudio: ImageView? = null
    var pauseAudio: ImageView? = null
    var playing: ImageView? = null
    var audioText: TextView? = null
    var playVideo: ImageView? = null

    var videoWrapper: View? = null

    private lateinit var video: VideoView
    private var isFullScreen = false
    private var originalSystemUiVisibility = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file)
        val util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        val path = intent.getStringExtra("path")
        val name = intent.getStringExtra("name")
        val id = intent.getStringExtra("id")
        val ext = intent.getStringExtra("ext")!!
        val preview = intent.getStringExtra("preview")!!

        video = findViewById(R.id.video)
        videoWrapper = findViewById(R.id.fullscreen)
        playVideo = findViewById(R.id.play_video)

        download = findViewById(R.id.download_file)
        delete = findViewById(R.id.delete_file)
        send = findViewById(R.id.send_file)
        sendLink = findViewById(R.id.send_file_link)
        edit = findViewById(R.id.edit_file)

        text = findViewById(R.id.text)
        image = findViewById(R.id.image)
        imageWrapper = findViewById(R.id.image_wrapper)
        audio = findViewById(R.id.audio)
        startAudio = findViewById(R.id.audio_start)
        pauseAudio = findViewById(R.id.audio_pause)
        playing = findViewById(R.id.playing)
        audioText = findViewById(R.id.audio_text)

        findViewById<TextView>(R.id.file_name_value).text = name
        findViewById<TextView>(R.id.file_type_value).text = intent.getStringExtra("type")
        findViewById<TextView>(R.id.file_size_value).text = Utils.fileSize(intent.getStringExtra("size")?.toInt())

        findViewById<TextView>(R.id.go_back).setOnClickListener {
            onBackPressed()
        }

        val pathArray = path?.split("/")
        val fullPath = Constants().SITE_NAME_FILES + "/" + pathArray?.slice(4 until pathArray.size)
            ?.joinToString("/")

        download?.setOnClickListener {
            download?.text = "Скачивание...."
            Log.e("TAG", "DOWNLOAD")
            Utils.downloadFile(name!!,
                fullPath, it.context) {
                download?.text = "Скачано в папку DOWNLOADS"
                download?.setOnClickListener {  }
            }
        }
        delete?.setOnClickListener {
            delete?.text = "Удаление..."
            deleteInBackground(id!!)
        }
        if(ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "gif" || ext == "bmp") {
            imageWrapper?.visibility = View.VISIBLE
            Picasso.get().load(fullPath).into(image)
        } else if(ext == "mp4" || ext == "avi") {
            imageWrapper?.visibility = View.VISIBLE
            val previewPath = Constants().SITE_NAME_FILES + "/filepreviews/${preview}"
            Log.e("TAG", previewPath)
            if(preview != null) {
                Log.e("TAG", preview!!)
            }
            Picasso.get().load(previewPath).into(image)
            image?.scaleType = ImageView.ScaleType.CENTER_CROP

            playVideo?.setOnClickListener {
                val intent = Intent(this, CloudVideoActivity::class.java)
                intent.putExtra("path", fullPath)
                startActivity(intent)
            }
        } else if(ext == "mp3") {
            audio?.visibility = View.VISIBLE
            playAudio(path!!)
            audioText?.text = name
        } else if(ext == "txt") {
            text?.visibility = View.VISIBLE
            getTextInBackground(id!!, "text")
        } else if(ext == "doc" || ext == "docx" || ext == "pdf") {
            text?.visibility = View.VISIBLE
            getTextInBackground(id!!, "hardtext")
        } else {
            imageWrapper?.visibility = View.VISIBLE
            image?.setImageResource(resources.getIdentifier(ext, "drawable", packageName))
        }
    }


    private fun deleteInBackground(id: String) {
        Thread {
            try {
                val token = userData?.getString("token")
                val url = URL(Constants().SITE_NAME + "cloud/delete-mobile/${id}")
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "DELETE"
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
                val response = JSONObject(result).getBoolean("deleted")
                runOnUiThread {
                    if(!response) {
                        Toast.makeText(this, "Не удалось удалить файл. Попробуйте еще раз", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Файл удален", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, CloudActivity::class.java))
                    }
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "Error")
            }
        }.start()
    }
    private fun getTextInBackground(id: String, mode: String) {
        Thread {
            try {
                val token = userData?.getString("token")
                var url: URL? = if(mode == "text") {
                    URL(Constants().SITE_NAME + "cloud/filetext/${id}")
                } else {
                    URL(Constants().SITE_NAME + "cloud/hardfiletext/${id}")
                }

                val connection = url?.openConnection() as HttpsURLConnection
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
                val fileText = JSONObject(result).getString("text")
                val err = JSONObject(result).getBoolean("err")
                if(err) {
                    Toast.makeText(this, "Не удалось прочитать файл. Попробуйте еще раз", Toast.LENGTH_SHORT).show()
                } else {
                    runOnUiThread {
                        text?.visibility = View.VISIBLE
                        text?.text = fileText
                    }
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "Error")
            }
        }.start()
    }

    private fun playAudio(path: String) {
        val pathArray = path?.split("/")
        val fullPath = Constants().SITE_NAME_FILES + "/" + pathArray?.slice(4 until pathArray.size)
            ?.joinToString("/")

        var mediaPlayer: MediaPlayer? = null

        mediaPlayer = MediaPlayer()
        mediaPlayer?.setDataSource(fullPath)
        mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mediaPlayer?.prepare()

        audio?.visibility = View.VISIBLE
        startAudio?.setOnClickListener {
            mediaPlayer?.start()
            startAudio?.visibility = View.GONE
            pauseAudio?.visibility = View.VISIBLE
            playing?.visibility = View.VISIBLE
            mediaPlayer?.setOnCompletionListener {
                mediaPlayer?.start()
            }
        }
//        stopAudio?.setOnClickListener {
//            pauseAudio?.visibility = View.GONE
//            startAudio?.visibility = View.VISIBLE
//            playing?.visibility = View.GONE
//            Log.e("TAG", "audio stopped")
//            if (mediaPlayer?.isPlaying == true) {
//                mediaPlayer?.stop()
//                mediaPlayer?.reset()
//                mediaPlayer?.release()
//            }
//        }
        pauseAudio?.setOnClickListener {
            pauseAudio?.visibility = View.GONE
            startAudio?.visibility = View.VISIBLE
            playing?.visibility = View.GONE
            Log.e("TAG", "audio stopped")
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        }
    }

    fun playVideo(path: String) {
        videoWrapper?.visibility = View.VISIBLE
        val mediaController = MediaController(this)
        mediaController.setAnchorView(video)
        video?.setMediaController(mediaController)
        video?.requestFocus()
        video?.setVideoPath(path)
        video?.start()
        //toggleFullscreen(videoWrapper!!)
    }

    fun toggleFullscreen(view: View) {
        if (!isFullScreen) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            supportActionBar?.hide()
            originalSystemUiVisibility = window.decorView.systemUiVisibility
            setSystemUiVisibility()
            isFullScreen = true
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            supportActionBar?.show()
            window.decorView.systemUiVisibility = originalSystemUiVisibility
            isFullScreen = false
        }
    }

    private fun setSystemUiVisibility() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }
}