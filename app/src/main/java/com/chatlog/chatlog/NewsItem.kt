package com.chatlog.chatlog

import org.json.JSONArray

data class NewsItem(var title: String,
                    var date: String,
                    var user: String,
                    var userAvatar: String,
                    var image: String = "",
                    var likes: Int = 0,
                    var comments: Int = 0,
                    var liked: Boolean = false,
                    var images: JSONArray,
                    var id: String = "",
                    var public: String = "",
                    var admin: String)

