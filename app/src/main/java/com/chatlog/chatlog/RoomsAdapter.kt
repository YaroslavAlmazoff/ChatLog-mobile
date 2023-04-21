package com.chatlog.chatlog

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.DeterminateDrawable
import com.squareup.picasso.Picasso
import java.util.*
import kotlin.collections.ArrayList

class RoomsAdapter(private val rooms: ArrayList<Room>, val context: Context) : RecyclerView.Adapter<RoomsAdapter.ViewHolder>(), Filterable {
    var filteredRooms = ArrayList<Room>()
    init {
        filteredRooms = rooms
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.room_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val room = rooms[position]
        holder.name?.text = room.name
        if(holder.avatar != null && room.avatar != "") {
            Picasso.get().load(Constants().SITE_NAME_FILES + "/useravatars/${room.avatar}").into(holder.avatar)
            holder.avatar?.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        Log.e("TAG", room.read.toString())
        if(!room.read) {
            holder.newMessagesExists?.visibility = View.VISIBLE
        }
        if(room.lastMessage.length >= 20) {
            holder.lastMessage?.text = room.lastMessage.substring(0, 15) + "..."
        } else {
            holder.lastMessage?.text = room.lastMessage
        }
        holder.root?.setOnClickListener {
            val intent = Intent(it.context, MessengerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("id", room.id)
            context.startActivity(intent)
        }
    }


    override fun getItemCount(): Int {
        return rooms.size
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var name: TextView? = null
        var avatar: ImageView? = null
        var lastMessage: TextView? = null
        var newMessagesExists: TextView? = null
        var root: View? = null

        init {
            name = itemView.findViewById(R.id.room_item_name)
            avatar = itemView.findViewById(R.id.room_item_avatar)
            lastMessage = itemView.findViewById(R.id.room_item_message)
            newMessagesExists = itemView.findViewById(R.id.room_item_new)
            root = itemView.findViewById(R.id.room_item)
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charSearch = constraint.toString()
                filteredRooms = if (charSearch.isEmpty()) {
                    rooms
                } else {
                    val resultList = ArrayList<Room>()
                    for (row in rooms) {
                        if (row.name.contains(charSearch)) {
                            resultList.add(row)
                        }
                    }
                    resultList
                }
                val filterResults = FilterResults()
                filterResults.values = filteredRooms
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredRooms = results?.values as ArrayList<Room>
                notifyDataSetChanged()
            }

        }
    }
}