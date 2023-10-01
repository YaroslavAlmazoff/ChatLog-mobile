package com.chatlog.chatlog

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.json.JSONObject
import java.io.File

class Notifications: Fragment() {
    private lateinit var pushBroadcastReceiver: BroadcastReceiver
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_notification, container, false)

        val name = rootView.findViewById<TextView>(R.id.notification_name)
        val text = rootView.findViewById<TextView>(R.id.notification_text)
        val notification = rootView.findViewById<View>(R.id.notification)

        val actions = rootView.findViewById<View>(R.id.notification_actions)

        val reply = rootView.findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.notification_reply)
        val reject = rootView.findViewById<com.sanojpunchihewa.glowbutton.GlowButton>(R.id.notification_reject)
        val check = rootView.findViewById<View>(R.id.notification_check)

        pushBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val extras = intent?.extras
                Log.e("TAG", "message received")

                notification?.visibility = View.VISIBLE
                if(extras?.getString(MyFirebaseMessagingService.KEY_TYPE) == "mf" || extras?.getString(MyFirebaseMessagingService.KEY_TYPE) == "cloud") {
                    actions.visibility = View.VISIBLE
                    check.setOnClickListener {
                        val intent = Intent(activity, UserActivity::class.java)
                        intent.putExtra("id", extras.getString("id"))
                        startActivity(intent)
                    }
                    reply.setOnClickListener {
                        if(extras?.getString(MyFirebaseMessagingService.KEY_TYPE) == "mf") {
                            Thread {Utils.request(context!!, "/reply/${extras.getString(MyFirebaseMessagingService.KEY_ID)}", "GET", true, null)}.start()
                        } else {
                            Thread {Utils.request(context!!, "/getsentfile/${extras.getString(MyFirebaseMessagingService.KEY_ID)}", "GET", true, null)}.start()
                        }
                    }
                    reject.setOnClickListener {
                        if(extras?.getString(MyFirebaseMessagingService.KEY_TYPE) == "mf") {
                            Thread {Utils.request(context!!, "/reject/${extras.getString(MyFirebaseMessagingService.KEY_ID)}", "GET", true, null)}.start()
                        }
                    }
                }
                val animation = AlphaAnimation(0.0f, 1.0f)
                animation.duration = 500
                animation.startOffset = 0
                notification.startAnimation(animation)
                val mainHandler = Handler(Looper.getMainLooper())
                val runnable = Runnable {
                    val animation = AlphaAnimation(1.0f, 0.0f)
                    animation.duration = 500
                    animation.startOffset = 0
                    notification.startAnimation(animation)
                    val secondHandler = Handler(Looper.getMainLooper())
                    val secondRunnable = Runnable {
                        notification.visibility = View.GONE
                    }
                    secondHandler.postDelayed(secondRunnable, 500)
                }
                mainHandler.postDelayed(runnable, 6000)
                extras?.keySet()?.firstOrNull { it == MyFirebaseMessagingService.KEY_TYPE }?.let {
                    when (extras.getString(it)) {
                        MyFirebaseMessagingService.ACTION_MESSAGE -> {
                            extras.getString(MyFirebaseMessagingService.KEY_MESSAGE)?.let { message ->
                                name.visibility = View.VISIBLE
                                name.text = extras.getString(MyFirebaseMessagingService.KEY_NAME)
                                text.text = message
                                notification.setOnClickListener {
                                    val messengerIntent = Intent(activity, MessengerActivity::class.java)
                                    messengerIntent.putExtra("id", extras.getString(MyFirebaseMessagingService.KEY_ID))
                                    startActivity(messengerIntent)
                                }
                            }
                        }
                        MyFirebaseMessagingService.ACTION_CHAT_MESSAGE -> {
                            extras.getString(MyFirebaseMessagingService.KEY_MESSAGE)?.let {
                                    message ->
                                name.visibility = View.VISIBLE
                                name.text = extras.getString(MyFirebaseMessagingService.KEY_NAME)
                                text.text = message
                                notification.setOnClickListener {
                                    val messengerIntent = Intent(activity, ChatMessengerActivity::class.java)
                                    messengerIntent.putExtra("id", extras.getString(MyFirebaseMessagingService.KEY_ID))
                                    startActivity(messengerIntent)
                                }
                            }
                        }
                        "mf", "df", "like", "comment", "public", "reply", "reject" -> {
                            extras.getString(MyFirebaseMessagingService.KEY_MESSAGE)?.let {
                                    message ->
                                name.text = extras.getString(MyFirebaseMessagingService.KEY_NAME)
                                text.text = message
                                notification.setOnClickListener {
                                    val messengerIntent = Intent(activity, UserActivity::class.java)
                                    messengerIntent.putExtra("id", extras.getString(MyFirebaseMessagingService.KEY_ID))
                                    startActivity(messengerIntent)
                                }
                            }
                        }
                        "cloud" -> {
                            extras.getString(MyFirebaseMessagingService.KEY_MESSAGE)?.let {
                                    message ->
                                name.text = extras.getString(MyFirebaseMessagingService.KEY_NAME)
                                text.text = message
                                notification.setOnClickListener {
                                    startActivity(Intent(activity, CloudActivity::class.java))
                                }
                            }
                        } else -> Log.e("TAG", "No type")
                    }
                }
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(MyFirebaseMessagingService.INTENT_FILTER)

        activity?.registerReceiver(pushBroadcastReceiver, intentFilter)


        return rootView
    }

}