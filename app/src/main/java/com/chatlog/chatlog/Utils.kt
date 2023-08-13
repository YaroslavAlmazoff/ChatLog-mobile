package com.chatlog.chatlog

import android.app.DownloadManager
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.math.floor

class Utils {
    val userFileName = "user/user.txt"
    @Throws(IOException::class)
    fun writeToUserFile(file: File, data: ByteArray) {
        var outputStream: OutputStream? = null
        try {
            if(!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }
            outputStream = FileOutputStream(file)
            outputStream.write(data)
        } finally {
            outputStream?.close()
        }
    }
    fun readUserFile(file: File): String {
        if(!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        return file.inputStream().readBytes().toString(Charsets.UTF_8)
    }
    fun saveUserData(data: String, filesDir: File) {
        val id = JSONObject(data).getString("userId")
        val token = JSONObject(data).getString("token")
        val refreshToken = JSONObject(data).getString("refreshToken")
        var user = URL(Constants().SITE_NAME + "user/" + id).readText(Charsets.UTF_8)
        var user2 = JSONObject(data).getString("user")
        Log.e("TAG", user)
        Log.e("TAG", user2)
        var userWithToken = user.substring(0, user.length - 1)
        userWithToken += ", \"token\": \"$token\", \"refreshToken\": \"$refreshToken\" }"
        Log.e("TAG", id)
        Log.e("TAG", userWithToken)
        val userFile = File(filesDir, userFileName)
        writeToUserFile(userFile, userWithToken.toByteArray())
    }
    fun clearUserData(filesDir: File) {
        writeToUserFile(File(filesDir, userFileName), "".toByteArray())
    }
    fun getCurrentDate():String{
        val sdf = SimpleDateFormat("HH:mm:ss")
        return sdf.format(Date())
    }
    fun getCurrentDateAndTime():String{
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm")
        return sdf.format(Date())
    }
    fun getDayTime(filesDir: File): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val name = JSONObject(readUserFile(File(filesDir, userFileName))).getJSONObject("user").getString("name")
        return when (hour) {
            in 5..10 -> "Доброе утро, $name"
            in 11..15 -> "Добрый день, $name"
            in 16..23 -> "Добрый вечер, $name"
            in 0..4 -> "Доброй ночи, $name"
            else -> {
                "Доброго времени суток, $name"
            }
        }
    }
    fun getEmojiByUnicode(unicode: Int): String? {
        return String(Character.toChars(unicode))
    }
    fun generateRandomNeonColor(): Int {
        return Color.parseColor(Constants().neonColors[Math.random().toInt()])
    }

    companion object {
        fun downloadFile(name: String, url: String, context: Context, archiveUrl: String, onComplete: () -> Unit) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(url))
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            request.setTitle("Загрузка файла")
            request.setDescription("Скачивание файла")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)

            val downloadId = downloadManager.enqueue(request)

            val onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        onComplete()
                        context?.unregisterReceiver(this)
                        if(archiveUrl != "") {
                            Thread {
                                try {
                                    val data = URL(Constants().SITE_NAME + "delete-temp-file/$archiveUrl").readText(Charsets.UTF_8)
                                    Log.e("TAG", data)
                                } catch (e: InterruptedException) {
                                    Log.e("TAG", "Error")
                                }
                            }.start()
                        }
                    }
                }
            }

            context.registerReceiver(onCompleteReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
        fun shortName(text: String, limit: Int): String {
            return if(text.length > limit) text.slice(0..limit) + "..."
            else text
        }
        fun copyFile(inputStream: InputStream?, destinationFile: File) {
            inputStream?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        fun getFileNameFromUri(context: Context, uri: Uri): String? {
            val contentResolver: ContentResolver = context.contentResolver
            val cursor = contentResolver.query(uri, null, null, null, null)
            var fileName: String? = null

            cursor?.let {
                if (it.moveToFirst()) {
                    val displayNameColumnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameColumnIndex != -1) {
                        fileName = it.getString(displayNameColumnIndex)
                    }
                }
                cursor.close()
            }

            return fileName
        }
        fun fileSize(size: Int?): String {
            return if (size != null) {
                return when(size) {
                    in 1000..999999 -> ( floor((size / 1000).toDouble()) ).toString() + "Kb"
                    in 1000000..999999999 -> ( floor((size / 1000000).toDouble()) ).toString() + "Mb"
                    in 1000000000..30000000000 -> ( floor((size / 1000000000).toDouble()) ).toString() + "Gb"
                    else -> "${size}b"
                }
            } else "0b"
        }
        fun convertJsonToList(jsonArray: JSONArray): List<List<String>> {
            val resultList: MutableList<MutableList<String>> = mutableListOf()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val innerList: MutableList<String> = mutableListOf()

                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = jsonObject.getString(key)
                    innerList.add(value)
                }

                resultList.add(innerList)
            }

            return resultList
        }

        fun request(context: Context, url: String, method: String, tokenRequired: Boolean, json: String?): String {
            return try {
                val token = updateToken(context)
                val url = URL(Constants().SITE_NAME + url)
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = method
                connection.doOutput = method == "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept-Charset", "utf-8")
                Log.e("TAG", token.toString())
                Log.e("TAG", tokenRequired.toString())
                if (tokenRequired) {
                    connection.setRequestProperty("Authorization", "Bearer $token")
                }

                if(json != null) {
                    connection.outputStream.write(json.toByteArray())
                }

                val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                var line: String? = reader.readLine()
                var result = ""
                while (line != null) {
                    result += line
                    line = reader.readLine()
                }
                reader.close()
                return result
            }
            catch(e: InterruptedException) {
                Log.e("TAG", "Error")
                return ""
            }
        }

        fun updateToken(context: Context): String {
            val util = Utils()
            val userData = JSONObject(util.readUserFile(File(context.filesDir, util.userFileName)))
            val refreshToken = userData?.getString("refreshToken")
            val url = URL(Constants().SITE_NAME + "refresh-mobile")
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept-Charset", "utf-8")
            connection.setRequestProperty("Authorization", "Bearer $refreshToken")
            var data: Int = connection.inputStream.read()
            var result = ""
            var byteArr = byteArrayOf()
            while(data != -1) {
                result += data.toChar().toString()
                byteArr.plus(data.toByte())
                data = connection.inputStream.read()
            }

            val token = JSONObject(result).getString("token")
            var user = userData.toString()
            var userWithToken = user.substring(0, user.length - 1)
            userWithToken += ", \"token\": \"$token\", \"refreshToken\": \"${JSONObject(result).getString("refreshToken")}\" }"
            Log.e("TAG", userWithToken)
            val userFile = File(context.filesDir, util.userFileName)
            util.writeToUserFile(userFile, userWithToken.toByteArray())
            return token
        }
    }
}