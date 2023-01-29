package com.chatlog.chatlog

import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class HomeNewsAdapter(private val items: ArrayList<NewsItem>) : RecyclerView.Adapter<HomeNewsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.news_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title?.text = item.title
        holder.date?.text = item.date
        holder.user?.text = item.user
        if(holder.image != null && item.image != "") {
            Picasso.get().load(Constants().SITE_NAME_FILES + "/articles/${item.image}").into(holder.image)
        }
        if(item.image == "") {
            holder.image?.visibility = View.GONE
        }
        if(holder.userAvatar != null) {
            Log.e("TAG", item.userAvatar)
            Picasso.get().load(Constants().SITE_NAME_FILES + "/useravatars/${item.userAvatar}").into(holder.userAvatar)
            holder.userAvatar?.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }


    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var title: TextView? = null
        var date: TextView? = null
        var user: TextView? = null
        var image: ImageView? = null
        var userAvatar: ImageView? = null
        var root: View? = null

        init {
            title = itemView.findViewById(R.id.news_title)
            date = itemView.findViewById(R.id.news_date)
            user = itemView.findViewById(R.id.news_user)
            image = itemView.findViewById(R.id.news_image)
            userAvatar = itemView.findViewById(R.id.news_avatar)
            root = itemView.findViewById(R.id.home_news_item)
        }
    }
}