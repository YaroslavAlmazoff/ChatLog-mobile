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

class PeopleAdapter(private val people: ArrayList<User>, private val userData: JSONObject, private val activity: Activity) : RecyclerView.Adapter<PeopleAdapter.ViewHolder>() {
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
        holder.messaging?.setOnClickListener {
            createRoom(user.id, it.context)
        }
    }


    override fun getItemCount(): Int {
        return people.size
    }

    private fun createRoom(id: String, context: Context) {
        Thread {
            try {
                val result = Utils.request(context, "createroom-mobile/${userData.getJSONObject("user").getString("_id")}/$id", "GET", true, null)
                Log.e("TAG", result)
                activity.runOnUiThread {
                    if(JSONObject(result).getInt("err") == 0 || JSONObject(result).getInt("err") == 1) {
                        val intent = Intent(context, MessengerActivity::class.java)
                        intent.putExtra("id", JSONObject(result).getJSONObject("room").getString("_id"))
                        activity.startActivity(intent)
                    } else {
                        Toast.makeText(context, activity.resources.getString(R.string.server_error), Toast.LENGTH_LONG).show()
                    }
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "Error")
            }
        }.start()
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var nameAndSurname: TextView? = null
        var birthDate: TextView? = null
        var address: TextView? = null
        var avatar: ImageView? = null
        var userItem: View? = null
        var messaging: com.sanojpunchihewa.glowbutton.GlowButton? = null

        init {
            nameAndSurname = itemView.findViewById(R.id.user_item_name_and_surname)
            birthDate = itemView.findViewById(R.id.user_item_birth_date)
            address = itemView.findViewById(R.id.user_item_address)
            avatar = itemView.findViewById(R.id.user_item_avatar)
            userItem = itemView.findViewById(R.id.user_item)
            messaging = itemView.findViewById(R.id.user_button)
        }
    }
}