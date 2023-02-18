package com.chatlog.chatlog

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class FriendsAdapter(private val items: ArrayList<Friend>) : RecyclerView.Adapter<FriendsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.friend_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val c = Constants()
        if(item.name == c.HIDDEN_ITEM && item.avatarUrl == c.DEFAULT_AVATAR) {
            holder.friend?.visibility = View.GONE
            holder.friend?.layoutParams?.width = 0
        }
        if(item.name.length >= 8) {
            holder.name?.text = item.name.substring(0, 8) + "..."
        } else {
            holder.name?.text = item.name
        }
        val color = Utils().generateRandomNeonColor()
        Log.e("TAG", color.toString())
        holder.name?.setTextColor(color)
        if(holder.avatar != null && item.avatarUrl != "") {
            Picasso.get().load(Constants().SITE_NAME_FILES + "/useravatars/${item.avatarUrl}").into(holder.avatar)
            holder.avatar?.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        holder.friend?.setOnClickListener {
            val intent = Intent(it.context, UserActivity::class.java)
            intent.putExtra("id", item.id)
            it.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var name: TextView? = null
        var avatar: ImageView? = null
        var friend: View? = null

        init {
            name = itemView.findViewById(R.id.friend_name)
            avatar = itemView.findViewById(R.id.friend_avatar)
            friend = itemView.findViewById(R.id.friend)
        }
    }
}