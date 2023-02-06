package com.chatlog.chatlog

import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class CommentsAdapter(private val items: ArrayList<Comment>) : RecyclerView.Adapter<CommentsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.comment_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.nameAndSurname?.text = item.user
        holder.date?.text = item.date
        holder.text?.text = item.text
        if(item.user == "useless" && item.avatarUrl == "user.png") {
            holder.root?.visibility = View.GONE
        }
        if(holder.avatar != null && item.avatarUrl != "") {
            Picasso.get().load(Constants().SITE_NAME_FILES + "/useravatars/${item.avatarUrl}").into(holder.avatar)
            holder.avatar?.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var nameAndSurname: TextView? = null
        var date: TextView? = null
        var text: TextView? = null
        var avatar: ImageView? = null
        var root: View? = null

        init {
            nameAndSurname = itemView.findViewById(R.id.comment_name_and_surname)
            date = itemView.findViewById(R.id.comment_date)
            text = itemView.findViewById(R.id.comment_text)
            avatar = itemView.findViewById(R.id.comment_item_avatar)
            root = itemView.findViewById(R.id.comment_item)
        }
    }
}