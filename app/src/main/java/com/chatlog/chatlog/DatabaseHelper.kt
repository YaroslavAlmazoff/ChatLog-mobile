package com.chatlog.chatlog

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    var userData: JSONObject? = null
    companion object {
        private const val DATABASE_NAME = "profile.db"
        private const val DATABASE_VERSION = 1
        private const val COLUMN_ID = "id"

        private const val TABLE_FRIENDS = "friends"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_AVATAR = "content"

        private const val TABLE_POSTS = "posts"
        private const val COLUMN_TEXT = "text"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_IMAGE = "image"

        private const val TABLE_USERS = "users"
        private const val COLUMN_SURNAME = "surname"

        private const val TABLE_NEWS = "news"
        private const val COLUMN_USER = "user"

        private const val TABLE_GAMES = "games"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createFriendsTable = "CREATE TABLE $TABLE_FRIENDS ($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_NAME TEXT, $COLUMN_AVATAR TEXT)"
        db.execSQL(createFriendsTable)

        val createPostsTable = "CREATE TABLE $TABLE_POSTS ($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_TEXT TEXT, $COLUMN_DATE TEXT, $COLUMN_IMAGE TEXT)"
        db.execSQL(createPostsTable)

        val createUsersTable = "CREATE TABLE $TABLE_USERS ($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_NAME TEXT, $COLUMN_SURNAME TEXT, $COLUMN_AVATAR TEXT)"
        db.execSQL(createUsersTable)

        val createNewsTable = "CREATE TABLE $TABLE_NEWS ($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_TEXT TEXT, $COLUMN_DATE TEXT, $COLUMN_USER TEXT, $COLUMN_IMAGE TEXT, $COLUMN_AVATAR TEXT)"
        db.execSQL(createNewsTable)

        val createGamesTable = "CREATE TABLE $TABLE_GAMES ($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_NAME TEXT, $COLUMN_AVATAR TEXT)"
        db.execSQL(createGamesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FRIENDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_POSTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NEWS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GAMES")
        onCreate(db)
    }

    fun addPost(text: String, date: String, image: String): Long {
        val values = ContentValues().apply {
            put(COLUMN_TEXT, text)
            put(COLUMN_DATE, date)
            put(COLUMN_IMAGE, image)
        }

        return writableDatabase.insert(TABLE_POSTS, null, values)
    }

    fun addNews(text: String, date: String, user: String, image: String, avatarUrl: String): Long {
        val values = ContentValues().apply {
            put(COLUMN_TEXT, text)
            put(COLUMN_DATE, date)
            put(COLUMN_USER, user)
            put(COLUMN_IMAGE, image)
            put(COLUMN_AVATAR, avatarUrl)
        }

        return writableDatabase.insert(TABLE_NEWS, null, values)
    }

    fun addFriend(name: String, avatarUrl: String): Long {
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_AVATAR, avatarUrl)
        }

        return writableDatabase.insert(TABLE_FRIENDS, null, values)
    }
    fun addUser(name: String, surname: String, avatarUrl: String): Long {
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_SURNAME, surname)
            put(COLUMN_AVATAR, avatarUrl)
        }

        return writableDatabase.insert(TABLE_USERS, null, values)
    }

    fun addGame(name: String, avatarUrl: String): Long {
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_AVATAR, avatarUrl)
        }

        return writableDatabase.insert(TABLE_GAMES, null, values)
    }

    fun getPosts(context: Context): ArrayList<NewsItem> {
        val util = Utils()
        userData = JSONObject(util.readUserFile(File(context.filesDir, util.userFileName)))
        var shorts: ArrayList<NewsItem> = ArrayList()
        val cursor = readableDatabase.query(TABLE_POSTS, null, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID))
                val text = cursor.getString(cursor.getColumnIndex(COLUMN_TEXT))
                val date = cursor.getString(cursor.getColumnIndex(COLUMN_DATE))
                val image = cursor.getString(cursor.getColumnIndex(COLUMN_IMAGE))

                val post = NewsItem(
                    text,
                    date,
                    "${userData?.getJSONObject("user")?.getString("name")!!} ${userData?.getJSONObject("user")?.getString("surname")!!}" ,
                    userData?.getJSONObject("user")?.getString("avatarUrl")!!,
                    image, 0, 0, false, JSONArray(), "", "", ""
                )
                shorts.add(0, post)
            } while (cursor.moveToNext())
        }

        cursor.close()

        // Обработка полученных записей
        for (post in shorts) {
            Log.e("TAG", "Name: ${post.title}, Date: ${post.date}, FileName: ${post.image}")
        }

        return shorts
    }

    fun getNews(context: Context): ArrayList<NewsItem> {
        val util = Utils()
        userData = JSONObject(util.readUserFile(File(context.filesDir, util.userFileName)))
        var shorts: ArrayList<NewsItem> = ArrayList()
        val cursor = readableDatabase.query(TABLE_NEWS, null, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID))
                val text = cursor.getString(cursor.getColumnIndex(COLUMN_TEXT))
                val date = cursor.getString(cursor.getColumnIndex(COLUMN_DATE))
                val user = cursor.getString(cursor.getColumnIndex(COLUMN_USER))
                val image = cursor.getString(cursor.getColumnIndex(COLUMN_IMAGE))
                val avatarUrl = cursor.getString(cursor.getColumnIndex(COLUMN_AVATAR))

                val post = NewsItem(
                    text,
                    date,
                    user,
                    avatarUrl,
                    image, 0, 0, false, JSONArray(), "", "", ""
                )
                shorts.add(0, post)
            } while (cursor.moveToNext())
        }

        cursor.close()

        // Обработка полученных записей
        for (post in shorts) {
            Log.e("TAG", "Name: ${post.title}, Date: ${post.date}, FileName: ${post.image}")
        }

        return shorts
    }

    fun getFriends(): ArrayList<Friend> {
        var shorts: ArrayList<Friend> = ArrayList()
        val cursor = readableDatabase.query(TABLE_FRIENDS, null, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID))
                val name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME))
                val avatarUrl = cursor.getString(cursor.getColumnIndex(COLUMN_AVATAR))

                val friend = Friend(name, avatarUrl)
                shorts.add(friend)
            } while (cursor.moveToNext())
        }

        cursor.close()

        // Обработка полученных записей
        for (friend in shorts) {
            Log.e("TAG", "Name: ${friend.name}, AVATAR: ${friend.avatarUrl}")
        }

        return shorts
    }

    fun getUsers(): ArrayList<User> {
        var shorts: ArrayList<User> = ArrayList()
        val cursor = readableDatabase.query(TABLE_USERS, null, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID))
                val name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME))
                val surname = cursor.getString(cursor.getColumnIndex(COLUMN_SURNAME))
                val avatarUrl = cursor.getString(cursor.getColumnIndex(COLUMN_AVATAR))

                val user = User(name, surname, "", "Солнечная система", "Планета Земля", avatarUrl, "")
                shorts.add(user)
            } while (cursor.moveToNext())
        }

        cursor.close()

        // Обработка полученных записей
        for (user in shorts) {
            Log.e("TAG", "Name: ${user.name}, AVATAR: ${user.avatarUrl}")
        }

        return shorts
    }

    fun getGames(): ArrayList<Game> {
        var shorts: ArrayList<Game> = ArrayList()
        val cursor = readableDatabase.query(TABLE_GAMES, null, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID))
                val name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME))
                val avatarUrl = cursor.getString(cursor.getColumnIndex(COLUMN_AVATAR))

                val game = Game(name, avatarUrl, "", "", "")
                shorts.add(game)
            } while (cursor.moveToNext())
        }

        cursor.close()

        // Обработка полученных записей
        for (game in shorts) {
            Log.e("TAG", "Name: ${game.title}, AVATAR: ${game.previewUrl}")
        }

        return shorts
    }
}