package com.chatlog.chatlog

import android.util.Log
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.nio.charset.Charset

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
    @Throws(IOException::class)
    fun getUserFromFile(file: File): String {
        if(!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        val stream = FileInputStream(file)
        return getStringFromStream(stream)
    }
    private fun getStringFromStream(stream: InputStream): String {
        stream.use { s ->
            val builder = StringBuilder()
            var b = s.read()
            while(b != -1) {
                builder.append(b.toChar())
                b = s.read()
            }
            return builder.toString()
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
        val user = URL(Constants().SITE_NAME + "user/" + id).readText(Charsets.UTF_8)
        Log.e("TAG", id)
        Log.e("TAG", user)
        val util = Utils()
        val userFile = File(filesDir, util.userFileName)
        util.writeToUserFile(userFile, user.toByteArray())
    }
}