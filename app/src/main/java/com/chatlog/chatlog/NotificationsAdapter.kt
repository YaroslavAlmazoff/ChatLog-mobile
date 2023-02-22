package com.chatlog.chatlog

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class NotificationsAdapter(private val items: ArrayList<Notification>, private val userData: JSONObject) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.notification_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        if(getNotificationFormat(item.type) == Constants.NOTIFICATION_FORMAT_SIMPLE) {
            holder.simpleButtons?.visibility = View.VISIBLE
        } else if(getNotificationFormat(item.type) == Constants.NOTIFICATION_FORMAT_COMPLICATED) {
            holder.complicatedButtons?.visibility = View.VISIBLE
            if(item.type == Constants.NOTIFICATION_TYPE_FRIENDS) {
                holder.reply?.setOnClickListener {
                    replyFriendInBackground(true, item.id, item.from, item.title)
                    holder.notification?.visibility = View.GONE
                    Toast.makeText(it.context, R.string.already_friends, Toast.LENGTH_SHORT).show()
                }
                holder.reject?.setOnClickListener {
                    replyFriendInBackground(false, item.id, item.from, item.title)
                    holder.notification?.visibility = View.GONE
                    Toast.makeText(it.context, R.string.rejected, Toast.LENGTH_SHORT).show()
                }
            } else {
                holder.reply?.setOnClickListener {
                    getFileInBackground(true, item.id)
                    holder.notification?.visibility = View.GONE
                    Toast.makeText(it.context, R.string.file_ok, Toast.LENGTH_SHORT).show()
                }
                holder.reject?.setOnClickListener {
                    getFileInBackground(false, item.id)
                    holder.notification?.visibility = View.GONE
                    Toast.makeText(it.context, R.string.file_rejected, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.e("TAG", item.type)
        }

        holder.title?.text = item.title

        holder.checkProfile?.setOnClickListener {
            val intent = Intent(it.context, UserActivity::class.java)
            intent.putExtra("id", item.from)
            it.context.startActivity(intent)
        }
        holder.check?.setOnClickListener {
            val intent = Intent(it.context, UserActivity::class.java)
            intent.putExtra("id", item.from)
            it.context.startActivity(intent)
        }
        holder.delete?.setOnClickListener {
            holder.notification?.visibility = View.GONE
            Thread {
                try {
                    deleteNotification(item.title)
                } catch(e: InterruptedException) {
                    Log.e("TAG", e.message!!)
                }
            }.start()
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, items.size)
        }


    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var notification: View? = null
        var title: TextView? = null
        var simpleButtons: View? = null
        var complicatedButtons: View? = null

        var checkProfile: com.sanojpunchihewa.glowbutton.GlowButton? = null
        var reply: com.sanojpunchihewa.glowbutton.GlowButton? = null
        var reject: com.sanojpunchihewa.glowbutton.GlowButton? = null
        var check: com.sanojpunchihewa.glowbutton.GlowButton? = null
        var delete: com.sanojpunchihewa.glowbutton.GlowButton? = null

        init {
            title = itemView.findViewById(R.id.notification_title)
            simpleButtons = itemView.findViewById(R.id.notification_simple)
            complicatedButtons = itemView.findViewById(R.id.notification_complicated)
            checkProfile = itemView.findViewById(R.id.notification_check_profile)
            reply = itemView.findViewById(R.id.notification_reply)
            reject = itemView.findViewById(R.id.notification_reject)
            check = itemView.findViewById(R.id.notification_check)
            notification = itemView.findViewById(R.id.notification)
            delete = itemView.findViewById(R.id.notification_delete)
        }
    }

    private fun getNotificationFormat(type: String): String {
        Log.e("TAG", type)
        return if(type == Constants.NOTIFICATION_TYPE_REPLY ||
            type == Constants.NOTIFICATION_TYPE_REJECT ||
            type == Constants.NOTIFICATION_TYPE_DELETE ||
            type == Constants.NOTIFICATION_TYPE_LIKE ||
            type == Constants.NOTIFICATION_TYPE_COMMENT ||
            type == Constants.NOTIFICATION_TYPE_VISIT) Constants.NOTIFICATION_FORMAT_SIMPLE
        else if(type == Constants.NOTIFICATION_TYPE_FRIENDS || type == Constants.NOTIFICATION_TYPE_FILE)
            Constants.NOTIFICATION_FORMAT_COMPLICATED
        else ""
    }
    private fun replyFriendInBackground(value: Boolean, id: String, from: String, title: String) {
        Thread {
            try {
                fun reply() {
                    val token = userData?.getString("token")
                    val url = URL(Constants().SITE_NAME + "reply/$from")
                    val connection = url.openConnection() as HttpsURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Accept-Charset", "utf-8")
                    connection.setRequestProperty("Authorization", "Bearer $token")

                    var data: Int = connection.inputStream.read()
                    var result = ""
                    while(data != -1) {
                        result += data.toChar().toString()
                        data = connection.inputStream.read()
                    }
                    Log.e("TAG", result)
                }
                deleteNotification(title)
                if(value) {
                    reply()
                }
            } catch (e: InterruptedException) {
                Log.e("TAG", e?.message!!)
            }
        }.start()
    }
    private fun deleteNotification(title: String) {
        val token = userData?.getString("token")
        val url = URL(Constants().SITE_NAME + "deletenotification/$title")
        val connection = url.openConnection() as HttpsURLConnection
        connection.requestMethod = "DELETE"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept-Charset", "utf-8")
        connection.setRequestProperty("Authorization", "Bearer $token")

        var data: Int = connection.inputStream.read()
        var result = ""
        while(data != -1) {
            result += data.toChar().toString()
            data = connection.inputStream.read()
        }
        Log.e("TAG", result)
    }
    private fun getFileInBackground(value: Boolean, id: String) {
        Thread {
            try {
                if(value) {
                    val response = URL(Constants().SITE_NAME + "getsentfile/$id").readText(Charsets.UTF_8)
                    Log.e("TAG", response)
                }
            } catch (e: InterruptedException) {
                Log.e("TAG", e?.message!!)
            }
        }.start()
    }
}