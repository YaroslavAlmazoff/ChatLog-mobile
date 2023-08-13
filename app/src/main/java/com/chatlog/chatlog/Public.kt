package com.chatlog.chatlog

import org.json.JSONArray

data class Public(var name: String,
                  var description: String,
                  var avatarUrl: String,
                  var bannerUrl: String,
                  var subscribers: JSONArray,
                  var posts: JSONArray,
                  var admin: String,
                  var isSubscriber: Boolean,
                  var id: String)
