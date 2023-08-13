package com.chatlog.chatlog

import org.json.JSONArray

data class PublicPostItem(var title: String,
                          var text: String,
                          var date: String,
                          var name: String,
                          var avatarUrl: String,
                          var image: String = "",
                          var likes: Int = 0,
                          var comments: Int = 0,
                          var liked: Boolean = false,
                          var images: JSONArray,
                          var id: String = "",
                          var public: String)
