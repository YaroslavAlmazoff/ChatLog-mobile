package com.chatlog.chatlog

import android.graphics.Color
import android.util.Log
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

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
        var user = URL(Constants().SITE_NAME + "user/" + id).readText(Charsets.UTF_8)
        var userWithToken = user.substring(0, user.length - 1)
        userWithToken += ", \"token\": \"$token\" }"
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
}