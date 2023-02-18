package com.chatlog.chatlog

class Constants {
    val SITE_NAME = "https://chatlog.ru/api/"
    val WEATHER_API_KEY = "928ba56fdc47f5fe29a01f2ae34f87f3"
    val SITE_NAME_FILES = "https://chatlog.ru"
    val HIDDEN_ITEM = "useless"
    val DEFAULT_AVATAR = "user.png"



    val neonColors: ArrayList<String> = ArrayList()

    init {
        neonColors.add("#40A4FF")
        neonColors.add("#9A38D6")
        neonColors.add("#4D4DFF")
        neonColors.add("#FF073A")
        neonColors.add("#B71DDE")
        neonColors.add("#FF760D")
        neonColors.add("#FF073A")
    }

    companion object {
        const val NOTIFICATION_TYPE_REPLY = "reply"
        const val NOTIFICATION_TYPE_REJECT = "reject"
        const val NOTIFICATION_TYPE_DELETE = "delete"
        const val NOTIFICATION_TYPE_LIKE = "like"
        const val NOTIFICATION_TYPE_COMMENT = "comment"
        const val NOTIFICATION_TYPE_VISIT = "visit"

        const val NOTIFICATION_TYPE_FRIENDS = "friends"
        const val NOTIFICATION_TYPE_FILE = "file"

        const val NOTIFICATION_FORMAT_SIMPLE = "simple"
        const val NOTIFICATION_FORMAT_COMPLICATED = "complicated"
    }
}