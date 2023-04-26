package com.chatlog.chatlog

import android.R.attr.data
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.math.roundToInt


class MessagesAdapter(private val messages: ArrayList<Message>,
                      private val context: Context,
                      private val userData: JSONObject,
                      private var messageField: EditText,
                      private var editing: Boolean,
                      private var currentMessageId: String,
                      private var currentMessageText: String,
                      private var sendButton: ImageView,
                      private var editButton: com.sanojpunchihewa.glowbutton.GlowButton) : RecyclerView.Adapter<MessagesAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.message, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        val color = Constants().neonColors[(Math.random()*6).roundToInt()]
        holder.name?.text = message.name
        holder.text?.text = message.message
        holder.date?.text = message.date
        holder.text?.setTextColor(Color.parseColor(color))
        if(holder.avatar != null && message.avatarUrl != "") {
            Picasso.get().load(Constants().SITE_NAME_FILES + "/useravatars/${message.avatarUrl}").into(holder.avatar)
            holder.avatar?.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        if(message.user == userData.getJSONObject("user").getString("_id")) {
            holder.root?.background = context.getDrawable(R.drawable.my_message)
        }
        if(message.imageUrl != "") {
            Picasso.get().load(Constants().SITE_NAME_FILES + "/messagefotos/${message.imageUrl}").into(holder.image)
            holder.image?.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            if(message.uri != null) {
                holder.image?.setImageURI(message.uri)
                holder.image?.scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }
        if(message.message == "") {
            holder.text?.visibility = View.GONE
        }
        holder.prefs?.setOnClickListener {
            holder.editMessage?.visibility = View.VISIBLE
            holder.deleteMessage?.visibility = View.VISIBLE
            currentMessageId = message.id
            currentMessageText = message.message
        }
        holder.editMessage?.setOnClickListener {
            sendButton.visibility = View.GONE
            editButton.visibility = View.VISIBLE
            messageField.setText(message.message)
            currentMessageId = message.id
            currentMessageText = message.message
            editing = true
        }
        holder.deleteMessage?.setOnClickListener {
            deleteMessageInBackground(message.id, position)
            holder.editMessage?.visibility = View.GONE
            holder.deleteMessage?.visibility = View.GONE
        }
        editButton.setOnClickListener {
            editMessageInBackground(message.id)
            holder.text?.text = messageField.text.toString()
            val imm = context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(messageField?.windowToken, 0)
            sendButton.visibility = View.VISIBLE
            editButton.visibility = View.GONE
            holder.editMessage?.visibility = View.GONE
            holder.deleteMessage?.visibility = View.GONE
        }
    }


    override fun getItemCount(): Int {
        return messages.size
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var text: TextView? = null
        var avatar: ImageView? = null
        var name: TextView? = null
        var date: TextView? = null
        var editMessage: TextView? = null
        var deleteMessage: TextView? = null
        var root: View? = null
        var prefs: ImageView? = null
        var image: ImageView? = null

        init {
            name = itemView.findViewById(R.id.message_name)
            avatar = itemView.findViewById(R.id.message_avatar)
            text = itemView.findViewById(R.id.message_text)
            date = itemView.findViewById(R.id.message_date)
            editMessage = itemView.findViewById(R.id.edit_message)
            deleteMessage = itemView.findViewById(R.id.delete_message)
            prefs = itemView.findViewById(R.id.prefs)
            image = itemView.findViewById(R.id.message_img)
            root = itemView.findViewById(R.id.message)
        }
    }
    fun deleteMessageInBackground(id: String, position: Int) {
        Thread {
            try {
                deleteMessage(id)
            } catch(e: InterruptedException) {
                Log.e("TAG", e.message.toString())
            }
        }.start()
        messages.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, messages.size)
    }
    fun deleteMessage(id: String) {
        URL(Constants().SITE_NAME + "deletemessage/${id}").readText(Charsets.UTF_8)
        currentMessageId = ""
        currentMessageText = ""

    }
    fun editMessageInBackground(id: String) {
        Thread {
            try {
                editMessage(id)
            } catch(e: InterruptedException) {
                Log.e("TAG", e.message.toString())
            }
        }.start()
    }
    fun editMessage(id: String) {
        val url = URL(Constants().SITE_NAME + "editmessage/${id}")
        val connection = url.openConnection() as HttpsURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept-Charset", "utf-8")
        val json = "{\"message\": \"${messageField?.text}\"}"
        connection.outputStream.write(json.toByteArray())
        var data: Int = connection.inputStream.read()
        var result = ""
        var byteArr = byteArrayOf()
        while(data != -1) {
            result += data.toChar().toString()
            byteArr.plus(data.toByte())
            data = connection.inputStream.read()
        }
        messageField?.setText("")
        editing = false
        currentMessageId = ""
        currentMessageText = ""
    }
    fun clear() {
        val size: Int = messages.count()
        if (size > 0) {
            for (i in 0 until size) {
                messages.removeAt(0)
            }
            notifyItemRangeRemoved(0, size)
        }
    }
}