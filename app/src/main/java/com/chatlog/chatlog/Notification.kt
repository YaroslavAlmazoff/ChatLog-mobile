package com.chatlog.chatlog

data class Notification(var title: String,
                        var type: String = "",
                        var from: String = "",
                        var to: String = "",
                        var postType: String = "",
                        var postId: String = "")
