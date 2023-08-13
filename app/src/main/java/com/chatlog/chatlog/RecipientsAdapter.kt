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
import org.w3c.dom.Text
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class RecipientsAdapter(private val people: ArrayList<User>, private val userData: JSONObject, private val activity: Activity) : RecyclerView.Adapter<RecipientsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.recipient_item, parent, false)
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
        holder.itemView?.setOnClickListener {
            if(activity.intent.getBooleanExtra("link", false)) {
                Thread {
                    try {
                        val result = Utils.request(it.context, "createroom-mobile/${userData.getJSONObject("user").getString("_id")}/${user.id}", "GET", true, null)
                        Log.e("TAG", result)
                        activity.runOnUiThread {
                            if(JSONObject(result).getInt("err") == 0 || JSONObject(result).getInt("err") == 1) {
                                val intent = Intent(it.context, MessengerActivity::class.java)
                                intent.putExtra("id", JSONObject(result).getJSONObject("room").getString("_id"))
                                intent.putExtra("file", activity.intent.getStringExtra("file"))
                                intent.putExtra("name", activity.intent.getStringExtra("name"))
                                activity.startActivity(intent)
                            } else {
                                Toast.makeText(it.context, activity.resources.getString(R.string.server_error), Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch(e: InterruptedException) {
                        Log.e("TAG", "Error")
                    }
                }.start()
            } else {
                Thread {
                    try {
                        val result = Utils.request(it.context, "cloud/sendfile-mobile/${user.id}/${activity.intent.getStringExtra("file")}", "GET", true, null)
                        activity.runOnUiThread {
                            holder.fileSent?.visibility = View.VISIBLE
                        }
                    } catch(e: InterruptedException) {
                        Log.e("TAG", "Error")
                    }
                }.start()
            }
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
        var fileSent: TextView? = null


        init {
            nameAndSurname = itemView.findViewById(R.id.user_item_name_and_surname)
            birthDate = itemView.findViewById(R.id.user_item_birth_date)
            address = itemView.findViewById(R.id.user_item_address)
            avatar = itemView.findViewById(R.id.user_item_avatar)
            userItem = itemView.findViewById(R.id.user_item)
            fileSent = itemView.findViewById(R.id.user_file_sent)
        }
    }
}