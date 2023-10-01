package com.chatlog.chatlog

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject
import java.io.File


class MyFirebaseMessagingService: FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.e("TAG", token)
        val util = Utils()
        val userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))
        Utils.request(this, "new-token/$token/${userData.getJSONObject("user").getString("_id")}", "GET", false, null)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.e("TAG", message.notification?.title.toString())
        Log.e("TAG", message.notification?.body.toString())
        val intent = Intent(INTENT_FILTER)
        message.data.forEach {
            intent.putExtra(it.key, it.value)
        }

        sendBroadcast(intent)
    }

    companion object {
        const val INTENT_FILTER = "PUSH_ACTION"
        const val KEY_TYPE = "type"
        const val KEY_ID = "id"
        const val KEY_MESSAGE = "message"
        const val KEY_NAME = "name"
        const val ACTION_MESSAGE = "message"
        const val ACTION_CHAT_MESSAGE = "chatmessage"
    }
}