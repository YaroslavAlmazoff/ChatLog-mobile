package com.chatlog.chatlog

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class MainActivity : AppCompatActivity() {
    var user: String? = null
    var userData: JSONObject? = null

    private lateinit var pushBroadcastReceiver: BroadcastReceiver

    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    private fun askNotificationPermission() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e("TAG", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            Toast.makeText(baseContext, token, Toast.LENGTH_LONG).show()
            Log.e("TAG", token)
        })
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        askNotificationPermission()

        val util = Utils()
        user = util.readUserFile(File(filesDir, Utils().userFileName))
        if(user?.isNotEmpty() == true) {
            userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))
        }
        if(util.readUserFile(File(filesDir, util.userFileName)).isNotEmpty()) {
            try {
                if(JSONObject(user).getString("token") != null) {
                    checkToken()
                } else {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                }
            } catch(e: JSONException) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }

        var listView = findViewById<ListView>(R.id.capabilitiesList)
        var capabilitiesArr: ArrayList<Capability> = ArrayList()
        listView.adapter = CapabilitiesAdapter(applicationContext, capabilitiesArr)
        capabilitiesArr.add(Capability("Игры", R.drawable.games, "#9A38D6"))
        capabilitiesArr.add(Capability("Новости для геймеров", R.drawable.news, "#4D4DFF"))
        capabilitiesArr.add(Capability("Удобный мессенджер", R.drawable.messenger, "#B71DDE"))
        capabilitiesArr.add(Capability("Облачное хранилище", R.drawable.cloud, "#86ED26"))
        capabilitiesArr.add(Capability("Реклама игр", R.drawable.adverts, "#FF760D"))

        val appName = findViewById<TextView>(R.id.appName)
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.2f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.2f)
        val anim: ObjectAnimator = ObjectAnimator.ofPropertyValuesHolder(appName, scaleX, scaleY)
        anim.repeatCount = ValueAnimator.INFINITE
        anim.repeatMode = ValueAnimator.REVERSE
        anim.duration = 2000
        anim.start()
        Log.e("TAG", "what")
        Log.e("TAG", user!!)
    }
    private fun checkToken() {
        Thread {
            try {
                Log.e("TAG", userData.toString())
                val refreshToken = userData?.getString("refreshToken")
                val url = URL(Constants().SITE_NAME + "refresh-mobile")
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept-Charset", "utf-8")
                connection.setRequestProperty("Authorization", "Bearer $refreshToken")
                var data: Int = connection.inputStream.read()
                var result = ""
                var byteArr = byteArrayOf()
                while(data != -1) {
                    result += data.toChar().toString()
                    byteArr.plus(data.toByte())
                    data = connection.inputStream.read()
                }

                Log.e("TAG", result)
                if(!JSONObject(result).getBoolean("verified")) {
                    Utils().clearUserData(filesDir)
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                } else {
                    if(!JSONObject(result).getBoolean("isActivated")) {
                        val intent = Intent(this, NotActivated::class.java)
                        startActivity(intent)
                    } else {
                        runHomeActivity()
                    }
                }
                Utils().saveUserData(result, filesDir)
            } catch (e: InterruptedException) {
                Log.e("TAG", "Не удалось выполнить проверку")
            }
        }.start()
    }
    fun runLoginActivity(view: View) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
    fun runRegisterActivity(view: View) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
    fun runSupportActivity(view: View) {
        val intent = Intent(this, SupportActivity::class.java)
        startActivity(intent)
    }
    fun runHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
    }
}