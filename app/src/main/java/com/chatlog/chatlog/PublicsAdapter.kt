package com.chatlog.chatlog

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class PublicsAdapter(private val publics: ArrayList<Public>, private val userData: JSONObject, private val activity: Activity, var context: Context) : RecyclerView.Adapter<PublicsAdapter.ViewHolder>(), IFilter {
    private var filteredList = ArrayList<Public>(publics)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.public_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val public = filteredList[position]
        holder.name?.text = Utils.shortName(public.name, 18)
        if(holder.avatar != null && public.avatarUrl != "") {
            Picasso.get().load(Constants().SITE_NAME_FILES + "/publicavatars/${public.avatarUrl}").into(holder.avatar)
            holder.avatar?.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        holder.itemView?.setOnClickListener {
            val intent = Intent(it.context, PublicActivity::class.java)
            val subscribers = public.subscribers.toString()
            val posts = public.posts.toString()
            intent.putExtra("id", public.id)
            intent.putExtra("name", public.name)
            intent.putExtra("description", public.description)
            intent.putExtra("avatarUrl", public.avatarUrl)
            intent.putExtra("bannerUrl", public.bannerUrl)
            intent.putExtra("admin", public.admin)
            intent.putExtra("subscribers", subscribers)
            intent.putExtra("posts", posts)
            intent.putExtra("isSubscriber", public.isSubscriber)
            it.context.startActivity(intent)
        }
        if(public.isSubscriber) {
            holder.subscribe?.text = context.resources.getString(R.string.unscribe)
        } else {
            holder.subscribe?.text = context.resources.getString(R.string.subscribe)
        }
        holder.subscribe?.setOnClickListener {
            Thread {
                try {
                    val result = Utils.request(it.context, "public/subscribe-list-mobile/${public.id}/${if(public.isSubscriber) 1 else 0}", "GET", true, null)
                    activity.runOnUiThread {
                        if(JSONObject(result).getBoolean("isSubscriber")) {
                            Toast.makeText(it.context, R.string.subscribed, Toast.LENGTH_SHORT).show()
                            it.visibility = View.GONE
                            holder.subscribed?.visibility = View.VISIBLE
                            public.isSubscriber = true
                        } else {
                            Toast.makeText(it.context, R.string.unsubscribed, Toast.LENGTH_SHORT).show()
                            holder.subscribe?.text = context.resources.getString(R.string.subscribe)
                            public.isSubscriber = false
                        }
                    }
                } catch(e: InterruptedException) {
                    Log.e("TAG", "ERROR")
                }
            }.start()
        }
    }




    override fun getItemCount(): Int {
        return filteredList.size
    }

    override fun filter(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(publics)
        } else {
            for (item in publics) {
                if (item.name.contains(query, ignoreCase = true) || item.description.contains(query, ignoreCase = true)) {
                    filteredList.add(item)
                }
            }
        }
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var name: TextView? = null
        var avatar: ImageView? = null
        var subscribe: com.sanojpunchihewa.glowbutton.GlowButton? = null
        var subscribed: TextView? = null

        init {
            name = itemView.findViewById(R.id.public_name)
            avatar = itemView.findViewById(R.id.public_avatar)
            subscribe = itemView.findViewById(R.id.subscribe_button)
            subscribed = itemView.findViewById(R.id.public_subscribed)
        }
    }
}