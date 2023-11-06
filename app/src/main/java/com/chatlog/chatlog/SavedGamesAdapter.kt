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

class SavedGamesAdapter(private var games: ArrayList<Game>, private var context: Context) : RecyclerView.Adapter<SavedGamesAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.short_game_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = games[position]
        holder.title?.text = Utils.shortName(game.title, 18)
        if(holder.avatar != null && game.previewUrl != "") {
            if(File(context.filesDir.path + "/${game.previewUrl}").exists()) {
                holder.avatar?.setImageBitmap(Utils.getBitmapFromFile(context, game.previewUrl))
            } else {
                Picasso.get().load(Constants().SITE_NAME_FILES + "/gamepreviews/${game.previewUrl}").into(holder.avatar)
            }
            holder.avatar?.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }


    override fun getItemCount(): Int {
        return games.size
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var title: TextView? = null
        var avatar: ImageView? = null

        init {
            title = itemView.findViewById(R.id.game_title)
            avatar = itemView.findViewById(R.id.game_avatar)
        }
    }
}