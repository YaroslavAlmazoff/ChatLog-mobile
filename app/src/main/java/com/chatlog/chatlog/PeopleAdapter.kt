package com.chatlog.chatlog

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class PeopleAdapter(private val people: ArrayList<User>) : RecyclerView.Adapter<PeopleAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.user_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = people[position]
        holder.nameAndSurname?.text = user.name + " " + user.surname
        if(user.country.isNotEmpty() && user.city.isNotEmpty()) {
            val unicode = 0x1F3E0
            holder.address?.text = Utils().getEmojiByUnicode(unicode) + user.city + ", " + user.country
        }
        if(holder.avatar != null && user.avatarUrl != "") {
            Picasso.get().load(Constants().SITE_NAME_FILES + "/useravatars/${user.avatarUrl}").into(holder.avatar)
            holder.avatar?.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        holder.userItem?.setOnClickListener {
            val intent = Intent(it.context, UserActivity::class.java)
            intent.putExtra("id", user.id)
            it.context.startActivity(intent)
        }
    }


    override fun getItemCount(): Int {
        return people.size
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var nameAndSurname: TextView? = null
        var birthDate: TextView? = null
        var address: TextView? = null
        var avatar: ImageView? = null
        var userItem: View? = null

        init {
            nameAndSurname = itemView.findViewById(R.id.user_item_name_and_surname)
            birthDate = itemView.findViewById(R.id.user_item_birth_date)
            address = itemView.findViewById(R.id.user_item_address)
            avatar = itemView.findViewById(R.id.user_item_avatar)
            userItem = itemView.findViewById(R.id.user_item)
        }
    }
}