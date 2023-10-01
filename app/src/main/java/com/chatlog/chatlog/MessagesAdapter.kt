package com.chatlog.chatlog

import android.R.attr.data
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
                      private var editButton: com.sanojpunchihewa.glowbutton.GlowButton,
                      private var activity: Activity, private var type: String) : RecyclerView.Adapter<MessagesAdapter.ViewHolder>(), IFilter {
    private var filteredList = ArrayList<Message>(messages)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.message, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = filteredList[position]

        Log.e("TAG", holder.itemView.layoutParams.height.toString())

        if(message.id != "") {
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
            if(message.videoUrl != "") {
                val uri = Uri.parse(Constants().SITE_NAME_FILES + "/messagevideos/${message.videoUrl}")
                Glide.with(context)
                    .load(uri)
                    .into(holder.thumbnail!!)
                holder.video?.visibility = View.VISIBLE
                holder.video?.setOnClickListener {
                    val intent = Intent(it.context, MessageVideoActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra("videoUrl", message.videoUrl)
                    intent.putExtra("isUri", false)
                    intent.putExtra("id", activity.intent.getStringExtra("id"))
                    intent.putExtra("type", type)
                    context.startActivity(intent)
                }
            } else {
                if(message.videoUri != null) {
                    Log.e("TAG", message.videoUri?.toString()!!)
                    holder.video?.visibility = View.VISIBLE
                    holder.video?.setOnClickListener {
                        val intent = Intent(it.context, MessageVideoActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.putExtra("videoUri", message.videoUri.toString())
                        intent.putExtra("isUri", true)
                        intent.putExtra("id", activity.intent.getStringExtra("id"))
                        intent.putExtra("type", type)
                        context.startActivity(intent)
                    }
                }
            }
            if(message.audioUrl != "") {
                var mediaPlayer: MediaPlayer? = null
                holder.audio?.visibility = View.VISIBLE
                holder.playAudio?.setOnClickListener {
                    holder.playAudio?.visibility = View.GONE
                    holder.stopAudio?.visibility = View.VISIBLE
                    holder.audioPlaying?.visibility = View.VISIBLE
                    mediaPlayer = MediaPlayer()
                    mediaPlayer?.setDataSource(Constants().SITE_NAME_FILES + "/messageaudios/${message.audioUrl}")
                    mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    mediaPlayer?.prepare()
                    mediaPlayer?.start()
                    mediaPlayer?.setOnCompletionListener {
                        mediaPlayer?.start()
                    }
                }
                holder.stopAudio?.setOnClickListener {
                    holder.stopAudio?.visibility = View.GONE
                    holder.playAudio?.visibility = View.VISIBLE
                    holder.audioPlaying?.visibility = View.GONE
                    Log.e("TAG", "audio stopped")
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.stop()
                        mediaPlayer?.reset()
                        mediaPlayer?.release()
                    }
                }
            } else {
                if(message.audioUri != null) {
                    var mediaPlayer: MediaPlayer? = null
                    holder.audio?.visibility = View.VISIBLE
                    holder.playAudio?.setOnClickListener {
                        holder.playAudio?.visibility = View.GONE
                        holder.stopAudio?.visibility = View.VISIBLE
                        holder.audioPlaying?.visibility = View.VISIBLE
                        mediaPlayer = MediaPlayer()
                        mediaPlayer?.setDataSource(message.audioUri?.toString())
                        mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
                        mediaPlayer?.prepare()
                        mediaPlayer?.start()
                        mediaPlayer?.setOnCompletionListener {
                            mediaPlayer?.start()
                        }
                    }
                    holder.stopAudio?.setOnClickListener {
                        holder.stopAudio?.visibility = View.GONE
                        holder.playAudio?.visibility = View.VISIBLE
                        holder.audioPlaying?.visibility = View.GONE
                        Log.e("TAG", "audio stopped")
                        if (mediaPlayer?.isPlaying == true) {
                            mediaPlayer?.stop()
                            mediaPlayer?.reset()
                            mediaPlayer?.release()
                        }
                    }
                }
            }
            if(message.message == "") {
                holder.text?.visibility = View.GONE
            }
            if(message.user == userData.getJSONObject("user").getString("_id")) {
                holder?.prefs?.visibility = View.VISIBLE
                holder.prefs?.setOnClickListener {
                    holder.editMessage?.visibility = View.VISIBLE
                    holder.deleteMessage?.visibility = View.VISIBLE
                    currentMessageId = message.id
                    currentMessageText = message.message
                }
            } else {
                holder?.prefs?.visibility = View.GONE
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
                holder.text?.text = messageField.text.toString()
                val imm = context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(messageField?.windowToken, 0)
                sendButton.visibility = View.VISIBLE
                editButton.visibility = View.GONE
                holder.editMessage?.visibility = View.GONE
                holder.deleteMessage?.visibility = View.GONE
                messages[position].message = messageField.text.toString()
                filter("")
                editMessageInBackground(message.id)
            }
            if(message.videoUrl == "") {
                holder.video?.visibility = View.GONE
            } else {
                holder.video?.visibility = View.VISIBLE
            }
            if(message.audioUrl == "") {
                holder.audio?.visibility = View.GONE
            } else {
                holder.audio?.visibility = View.VISIBLE
            }
            if(message.imageUrl == "") {
                holder.image?.visibility = View.GONE
            } else {
                holder.image?.visibility = View.VISIBLE
            }

            if(message.fileLink != null && message.fileLink != "") {
                holder.openButton?.visibility = View.VISIBLE
                holder.openButton?.setOnClickListener {
                    Thread {
                        try {
                            val result = Utils.request(it.context, "cloud/file/${message.fileLink}", "GET", false, null)
                            val file = JSONObject(result).getJSONObject("file")
                            val intent = Intent(it.context, FileActivity::class.java)
                            intent.putExtra("name", file.getString("name"))
                            intent.putExtra("type", file.getString("type"))
                            intent.putExtra("size", file.getString("size"))
                            intent.putExtra("path", file.getString("path"))
                            intent.putExtra("id", file.getString("_id"))
                            intent.putExtra("ext", file.getString("ext"))
                            intent.putExtra("preview", file.getString("previewUrl"))
                            intent.putExtra("owner", file.getString("owner"))
                            intent.putExtra("public", file.getBoolean("public"))
                            it.context.startActivity(intent)
                        } catch (e: InterruptedException) {
                            Log.e("TAG", "error")
                        }
                    }.start()
                }
            } else {
                holder.openButton?.visibility = View.GONE
            }
        } else {
            holder.itemView.visibility = View.VISIBLE
        }
    }


    override fun getItemCount(): Int {
        return filteredList.size
    }

    override fun filter(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(messages)
        } else {
            for (item in messages) {
                if (item.message.contains(query, ignoreCase = true)) {
                    filteredList.add(item)
                }
            }
        }
        notifyDataSetChanged()
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
        var video: View? = null
        var audio: View? = null
        var playAudio: ImageView? = null
        var stopAudio: ImageView? = null
        var audioPlaying: ImageView? = null
        var thumbnail: ImageView? = null
        var openButton: com.sanojpunchihewa.glowbutton.GlowButton

        init {
            name = itemView.findViewById(R.id.message_name)
            avatar = itemView.findViewById(R.id.message_avatar)
            text = itemView.findViewById(R.id.message_text)
            date = itemView.findViewById(R.id.message_date)
            editMessage = itemView.findViewById(R.id.edit_message)
            deleteMessage = itemView.findViewById(R.id.delete_message)
            prefs = itemView.findViewById(R.id.prefs)
            image = itemView.findViewById(R.id.message_img)
            video = itemView.findViewById(R.id.video)
            root = itemView.findViewById(R.id.message)
            audio = itemView.findViewById(R.id.audio)
            playAudio = itemView.findViewById(R.id.play_audio)
            stopAudio = itemView.findViewById(R.id.stop_audio)
            audioPlaying = itemView.findViewById(R.id.audio_playing)
            thumbnail = itemView.findViewById(R.id.thumbnail)
            openButton = itemView.findViewById(R.id.open_button)
        }
    }
    fun deleteMessageInBackground(id: String, position: Int) {
        messages.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, messages.size)
        filter("")
        Thread {
            try {
                deleteMessage(id)
            } catch(e: InterruptedException) {
                Log.e("TAG", e.message.toString())
            }
        }.start()
    }
    fun deleteMessage(id: String) {
        val result = Utils.request(context, "deletemessage/${id}", "GET", true, null)
        Log.e("TAG", result)
        currentMessageId = ""
        currentMessageText = ""
    }
    fun editMessageInBackground(id: String) {
        filter("")
        Thread {
            try {
                editMessage(id)
            } catch(e: InterruptedException) {
                Log.e("TAG", e.message.toString())
            }
        }.start()
    }
    fun editMessage(id: String) {
        val json = "{\"message\": \"${messageField?.text}\"}"
        val result = Utils.request(context, "editmessage/${id}", "POST", true, json)
        Log.e("TAG", result)
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