package com.chatlog.chatlog

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class SelectedImagesAdapter(private val items: ArrayList<SelectedImage>, private val photos: ArrayList<File>) : RecyclerView.Adapter<SelectedImagesAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.selected_image_item, parent, false)
        return ViewHolder(itemView)
    }



    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var image: ImageView? = null
        var removeImage: TextView? = null

        init {
            image = itemView.findViewById(R.id.selected_image)
            removeImage = itemView.findViewById(R.id.selected_image_delete)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.image?.setImageURI(item.uri)
        holder.removeImage?.setOnClickListener {
            items.removeAt(position)
            photos.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, items.size)
        }
    }
}