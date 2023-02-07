package com.chatlog.chatlog

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.json.JSONObject
import java.io.File

class Navigation: Fragment() {
    var user: JSONObject? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_navigation, container, false)

        val navHome = rootView.findViewById<View>(R.id.nav_home_view)
        val navProfile = rootView.findViewById<View>(R.id.nav_profile_view)
        val navPeople = rootView.findViewById<View>(R.id.nav_people_view)
        val navMessenger = rootView.findViewById<View>(R.id.nav_messenger_view)
        val navOther = rootView.findViewById<View>(R.id.nav_other_view)

        val util = Utils()
        user = JSONObject(util.readUserFile(File(context?.filesDir, util.userFileName))).getJSONObject("user")

        navHome.setOnClickListener {
            val intent = Intent(activity, HomeActivity::class.java)
            activity?.startActivity(intent)
        }
        navProfile.setOnClickListener {
            val intent = Intent(activity, UserActivity::class.java)
            intent.putExtra("id", user?.getString("_id"))
            activity?.startActivity(intent)
        }
        navPeople.setOnClickListener {
            val intent = Intent(activity, PeopleActivity::class.java)
            activity?.startActivity(intent)
        }
        navMessenger.setOnClickListener {
            val intent = Intent(activity, MessengerActivity::class.java)
            activity?.startActivity(intent)
        }
        navOther.setOnClickListener {
            val intent = Intent(activity, NavigationActivity::class.java)
            activity?.startActivity(intent)
        }
        return rootView
    }

}