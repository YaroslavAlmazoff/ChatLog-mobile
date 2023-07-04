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
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.net.URL

class AddMembersAdapter(
    private val people: ArrayList<User>,
    private val id: String,
    private val members: ArrayList<String>,
    private val userData: JSONObject?,
    private val activity: Activity
    ) : RecyclerView.Adapter<AddMembersAdapter.ViewHolder>(), IFilter {
    private var filteredList = ArrayList<User>(people)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.add_member_item, parent, false)
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
        holder.add?.setOnClickListener {
            invite(holder, user)
        }
        for(i in 0 until members.size) {
            Log.e("TAG", members[i])
        }
        if(user.id in members) {
            holder.add?.visibility = View.GONE
            holder?.added?.visibility = View.VISIBLE
        } else {
            holder.add?.visibility = View.VISIBLE
            holder?.added?.visibility = View.GONE
        }
    }


    override fun getItemCount(): Int {
        return people.size
    }

    override fun filter(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(people)
        } else {
            for (item in people) {
                if (item.name.contains(query, ignoreCase = true)) {
                    filteredList.add(item)
                }
            }
        }
        notifyDataSetChanged()
    }

    private fun invite(holder: ViewHolder, user: User) {
        holder.add?.visibility = View.VISIBLE
        holder?.added?.visibility = View.GONE
        Thread {
            try {
                val membersData = URL(Constants().SITE_NAME + "invite-mobile/$id/${user.id}").readText(Charsets.UTF_8)
                Log.e("TAG", membersData)
            } catch(e: InterruptedException) {
                Log.e("TAG", "Interrupted Exception")
            }
        }.start()
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var nameAndSurname: TextView? = null
        var birthDate: TextView? = null
        var address: TextView? = null
        var avatar: ImageView? = null
        var userItem: View? = null
        var add: com.sanojpunchihewa.glowbutton.GlowButton? = null
        var added: TextView? = null

        init {
            nameAndSurname = itemView.findViewById(R.id.user_item_name_and_surname)
            birthDate = itemView.findViewById(R.id.user_item_birth_date)
            address = itemView.findViewById(R.id.user_item_address)
            avatar = itemView.findViewById(R.id.user_item_avatar)
            userItem = itemView.findViewById(R.id.user_item)
            add = itemView.findViewById(R.id.user_button)
            added = itemView.findViewById(R.id.added)
        }
    }
}