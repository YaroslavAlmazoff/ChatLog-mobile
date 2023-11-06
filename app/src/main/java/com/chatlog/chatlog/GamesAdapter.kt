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
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class GamesAdapter(private val games: ArrayList<Game>, private var context: Context) : RecyclerView.Adapter<GamesAdapter.ViewHolder>(), IFilter {
    private var filteredList = ArrayList<Game>(games)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.game_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = filteredList[position]
        holder.title?.text = Utils.shortName(game.title, 18)
        if(holder.avatar != null && game.previewUrl != "") {
            if(File(context.filesDir.path + "/${game.previewUrl}").exists()) {
                holder.avatar?.setImageBitmap(Utils.getBitmapFromFile(context, game.previewUrl))
            } else {
                Picasso.get().load(Constants().SITE_NAME_FILES + "/gamepreviews/${game.previewUrl}").into(holder.avatar)
            }
            holder.avatar?.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        holder.itemView?.setOnClickListener {
            val intent = Intent(it.context, GameActivity::class.java)
            intent.putExtra("id", game.id)
            intent.putExtra("title", game.title)
            intent.putExtra("description", game.description)
            intent.putExtra("version", game.version)
            intent.putExtra("previewUrl", game.previewUrl)
            intent.putExtra("downloadUrl", game.downloadUrl)
            it.context.startActivity(intent)
        }
        holder.download?.setOnClickListener {
            Utils.downloadFile(game.title + ".apk", Constants().SITE_NAME_FILES + "/gamedownloads/${game.downloadUrl}", it.context, "") {
                Toast.makeText(it.context, R.string.downloaded, Toast.LENGTH_LONG).show()
                it.visibility = View.GONE
                holder.downloaded?.visibility = View.VISIBLE
            }
        }
    }


    override fun getItemCount(): Int {
        return filteredList.size
    }

    override fun filter(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(games)
        } else {
            for (item in games) {
                if (item.title.contains(query, ignoreCase = true)) {
                    filteredList.add(item)
                }
            }
        }
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var title: TextView? = null
        var avatar: ImageView? = null
        var download: com.sanojpunchihewa.glowbutton.GlowButton? = null
        var downloaded: TextView? = null

        init {
            title = itemView.findViewById(R.id.game_title)
            avatar = itemView.findViewById(R.id.game_avatar)
            download = itemView.findViewById(R.id.download_button)
            downloaded = itemView.findViewById(R.id.game_downloaded)
        }
    }
}