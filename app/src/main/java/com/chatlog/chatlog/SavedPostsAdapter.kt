package com.chatlog.chatlog

import android.content.Context
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
import java.lang.RuntimeException
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class SavedPostsAdapter(private val items: ArrayList<NewsItem>, private var userData: JSONObject, private var isOwner: Boolean = true, private val context: Context) : RecyclerView.Adapter<SavedPostsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.short_post, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title?.text = item.title
        holder.date?.text = item.date
        holder.user?.text = item.user
        if(holder.image != null && item.image != "") {
            if(isOwner) {
                holder.image?.setImageBitmap(Utils.getBitmapFromFile(context, item.image))
            } else {
                Picasso.get().load(Constants().SITE_NAME_FILES + "/articles/${item.image}").into(holder.image)
            }
        }

        if(isOwner) {
            holder.userAvatar?.setImageBitmap(Utils.getBitmapFromFile(context, userData.getJSONObject("user").getString("avatarUrl")!!))
            holder.userAvatar?.scaleType = ImageView.ScaleType.CENTER_CROP
        }

        if(item.image == "") {
            holder.image?.visibility = View.GONE
        }
        if(holder.userAvatar != null && !isOwner) {
            Log.e("TAG", item.userAvatar)
            try {
                Picasso.get().load(Constants().SITE_NAME_FILES + "/useravatars/${item.userAvatar}").into(holder.userAvatar)
            } catch(e: RuntimeException) {
                holder.userAvatar?.setImageDrawable(context.resources.getDrawable(R.drawable.gradient3))
            }
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
            title = itemView.findViewById(R.id.title)
            date = itemView.findViewById(R.id.date)
            user = itemView.findViewById(R.id.user)
            image = itemView.findViewById(R.id.image)
            userAvatar = itemView.findViewById(R.id.avatar)
            root = itemView
        }
    }
}