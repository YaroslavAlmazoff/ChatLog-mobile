package com.chatlog.chatlog

import android.app.Activity
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

class MembersAdapter(private val people: ArrayList<User>, private val id: String, private val userData: JSONObject?, private val activity: Activity) : RecyclerView.Adapter<MembersAdapter.ViewHolder>(), IFilter {
    private var filteredList = ArrayList<User>(people)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.member_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = people[position]
        holder.nameAndSurname?.text = user.name + " " + user.surname
        if(holder.avatar != null && user.avatarUrl != "") {
            Picasso.get().load(Constants().SITE_NAME_FILES + "/useravatars/${user.avatarUrl}").into(holder.avatar)
            holder.avatar?.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        holder.userItem?.setOnClickListener {
            val intent = Intent(it.context, UserActivity::class.java)
            intent.putExtra("id", user.id)
            it.context.startActivity(intent)
        }
        holder.exclude?.setOnClickListener {
            holder.root?.visibility = View.GONE
            people.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, people.size)
            Thread {
                try {
                    val result = Utils.request(it.context, "exclude/$id/${user.id}", "DELETE", true, null)
                    activity.runOnUiThread {
                        if(JSONObject(result).getJSONArray("errors").length() != 0) {
                            Toast.makeText(it.context, JSONObject(result).getJSONArray("errors").getString(0), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(it.context, activity.resources.getString(R.string.excluded), Toast.LENGTH_LONG).show()
                        }
                    }
                } catch(e: InterruptedException) {
                    Log.e("TAG", "Error")
                }
            }.start()
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
        var exclude: com.sanojpunchihewa.glowbutton.GlowButton? = null
        var root: View? = null

        init {
            nameAndSurname = itemView.findViewById(R.id.user_item_name_and_surname)
            birthDate = itemView.findViewById(R.id.user_item_birth_date)
            address = itemView.findViewById(R.id.user_item_address)
            avatar = itemView.findViewById(R.id.user_item_avatar)
            userItem = itemView.findViewById(R.id.user_item)
            exclude = itemView.findViewById(R.id.exclude_button)
            root = itemView
        }
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
}