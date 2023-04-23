package com.chatlog.chatlog

import android.net.Uri
import org.json.JSONArray

data class Message(var message: String,
                   var name: String,
                   var avatarUrl: String,
                   var date: String,
                   var user: String,
                   var room: String,
                   var isNotReading: Boolean,
                   var isFile: Boolean,
                   var id: String,
                   var imageUrl: String,
                   var videoUrl: String,
                   var audioUrl: String,
                   var readThisMessage: JSONArray,
                   var uri: Uri?
)
