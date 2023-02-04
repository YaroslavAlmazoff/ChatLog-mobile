package com.chatlog.chatlog

data class NewsItem(var title: String, var date: String, var user: String, var userAvatar: String, var image: String = "", var likes: Int = 0, var liked: Boolean = false, var id: String = "")
