package com.chatlog.chatlog

data class Game(var title: String,
                var previewUrl: String,
                var downloadUrl: String,
                var id: String,
                var description: String,
                var version: String = "1.0",
                var rating: Int? = 0,
                var sumRating: Int? = 0,
                var marks: Int? = 0)
