package com.chatlog.chatlog

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
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

class PublicNewsAdapter(private val items: ArrayList<NewsItem>, private var userData: JSONObject, val activity: Activity) : RecyclerView.Adapter<PublicNewsAdapter.ViewHolder>() {
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
        holder.likes?.text = item.likes.toString()
        holder.comments?.text = item.comments.toString()
        if(holder.image != null && item.image != "") {
            Picasso.get().load(Constants().SITE_NAME_FILES + "/publicposts/${item.image}").into(holder.image)
        }
        holder.likeImage?.setImageResource(R.drawable.blue_like)
        Log.e("TAG", item.liked.toString())

        Log.e("TAG",userData?.getJSONObject("user")?.getString("_id")!!)
        Log.e("TAG",item.admin)
        if(userData?.getJSONObject("user")?.getString("_id") == item.admin) {
            holder.delete?.visibility = View.VISIBLE
            holder?.delete?.setOnClickListener {
                deletePost(item.id, it.context)
                items.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, items.size)
            }
        }

        if(item.liked) {
            holder.likes?.text = item.likes.toString()
            holder.likeImage?.setImageResource(R.drawable.red_like)
            holder.likes?.setTextColor(Color.parseColor("#FF073A"))
        } else {
            holder.likes?.text = item.likes.toString()
            holder.likeImage?.setImageResource(R.drawable.blue_like)
            holder.likes?.setTextColor(Color.parseColor("#40A4FF"))
        }
        if(item.image == "") {
            holder.image?.visibility = View.GONE
        }
        if(holder.userAvatar != null) {
            Log.e("TAG", item.userAvatar)
            Picasso.get().load(Constants().SITE_NAME_FILES + "/publicavatars/${item.userAvatar}").into(holder.userAvatar)
            holder.userAvatar?.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        if(item.images.length() != 0 && item.images.length() != 1) {
            holder.viewAllImagesButton?.visibility = View.VISIBLE
        }
        holder.viewAllImagesButton?.setOnClickListener {
            Log.e("TAG", item.images.toString())
            if(item.images.length() > 1) {
                val imagesArray: ArrayList<String> = ArrayList()
                for(i in 0 until item.images.length()) {
                    imagesArray.add(item.images.getString(i))
                }

                val intent = Intent(it.context, PublicPhotosActivity::class.java)
                intent.putStringArrayListExtra("photos", imagesArray)
                it.context.startActivity(intent)
            }
        }
        holder.comment?.setOnClickListener {
            val intent = Intent(it.context, PublicCommentsActivity::class.java)
            intent.putExtra("id", item.id)
            intent.putExtra("public", item.public)
            it.context.startActivity(intent)
        }
        holder.like?.setOnClickListener {
            if(item.liked) {
                item.liked = false
                item.likes = item.likes - 1
                holder.likes?.text = (item.likes).toString()
                holder.likeImage?.setImageResource(R.drawable.blue_like)
                holder.likes?.setTextColor(Color.parseColor("#40A4FF"))
            } else {
                item.liked = true
                item.likes = item.likes + 1
                holder.likes?.text = (item.likes).toString()
                holder.likeImage?.setImageResource(R.drawable.red_like)
                holder.likes?.setTextColor(Color.parseColor("#FF073A"))
            }
            Thread {
                try {
                    val result = Utils.request(it.context, "public/likepost/${item.id}", "GET", true, null)
                    Log.e("TAG", result)
                } catch (e: InterruptedException) {
                    Log.e("TAG", "Не удалось поставить лайк")
                }
            }.start()
        }
        holder.user?.setOnClickListener {
            Thread {
                try {
                    val publicData = Utils.request(it.context, "public/public/${item.public}", "GET", true, null)
                    val public = JSONObject(publicData).getJSONObject("pub")
                    val intent = Intent(it.context, PublicActivity::class.java)
                    intent.putExtra("id", public.getString("_id"))
                    intent.putExtra("name", public.getString("name"))
                    intent.putExtra("description", public.getString("description"))
                    intent.putExtra("avatarUrl", public.getString("avatarUrl"))
                    intent.putExtra("bannerUrl", public.getString("bannerUrl"))
                    intent.putExtra("admin", public.getString("admin"))
                    intent.putExtra("isSubscriber", true)
                    activity.startActivity(intent)
                } catch(e: InterruptedException) {
                    Log.e("TAG", "Error")
                }
            }.start()
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
        var like: View? = null
        var likes: TextView? = null
        var likeImage: ImageView? = null
        var comment: View? = null
        var comments: TextView? = null
        var commentImage: ImageView? = null
        var viewAllImagesButton: Button? = null
        var delete: TextView? = null

        init {
            title = itemView.findViewById(R.id.news_title)
            date = itemView.findViewById(R.id.news_date)
            user = itemView.findViewById(R.id.news_user)
            image = itemView.findViewById(R.id.news_image)
            userAvatar = itemView.findViewById(R.id.news_avatar)
            root = itemView.findViewById(R.id.home_news_item)
            like = itemView.findViewById(R.id.news_like)
            likes = itemView.findViewById(R.id.news_likes)
            likeImage = itemView.findViewById(R.id.like_image)
            comment = itemView.findViewById(R.id.news_comment)
            comments = itemView.findViewById(R.id.news_comments)
            commentImage = itemView.findViewById(R.id.comment_image)
            viewAllImagesButton = itemView.findViewById(R.id.view_all_images)
            delete = itemView.findViewById(R.id.news_delete)
        }
    }

    private fun deletePost(id: String, context: Context) {
        Thread {
            val result = Utils.request(context, "public/deletepost/$id", "DELETE", true, null)
            Log.e("TAG", result)
        }.start()
    }
}