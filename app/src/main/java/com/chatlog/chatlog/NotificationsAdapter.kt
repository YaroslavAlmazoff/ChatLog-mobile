package com.chatlog.chatlog

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class NotificationsAdapter(private val items: ArrayList<Notification>) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {
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
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var title: TextView? = null
        var simpleButtons: View? = null
        var complicatedButtons: View? = null

        var checkProfile: com.sanojpunchihewa.glowbutton.GlowButton? = null
        var reply: com.sanojpunchihewa.glowbutton.GlowButton? = null
        var reject: com.sanojpunchihewa.glowbutton.GlowButton? = null
        var check: com.sanojpunchihewa.glowbutton.GlowButton? = null

        init {
            title = itemView.findViewById(R.id.notification_title)
            simpleButtons = itemView.findViewById(R.id.notification_simple)
            complicatedButtons = itemView.findViewById(R.id.notification_complicated)
            checkProfile = itemView.findViewById(R.id.notification_check_profile)
            reply = itemView.findViewById(R.id.notification_reply)
            reject = itemView.findViewById(R.id.notification_reject)
            check = itemView.findViewById(R.id.notification_check)
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
}