package com.chatlog.chatlog

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.File
import java.net.URL

class GamesActivity : AppCompatActivity() {
    var userData: JSONObject? = null
    var gamesList: RecyclerView? = null
    var pb: ProgressBar? = null
    var searchField: SearchView? = null
    var adapter: GamesAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_games)

        searchField = findViewById(R.id.search_field)

        val initWidth = searchField?.width

        searchField?.setOnSearchClickListener {
            val layoutParams = searchField?.layoutParams
            layoutParams?.width = initWidth!! + 500 // change this value to adjust the width
            searchField?.layoutParams = layoutParams
        }
        searchField?.setOnCloseListener {
            val layoutParams = searchField?.layoutParams
            layoutParams?.width = 140 // change this value to adjust the width
            searchField?.layoutParams = layoutParams
            return@setOnCloseListener false
        }

        searchField?.setOnQueryTextListener(object : SearchView.OnQueryTextListener, SearchView.OnSuggestionListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    adapter?.filter(it)
                }
                return true
            }

            override fun onSuggestionClick(position: Int): Boolean {
                Log.e("TAG", "chatlog")
                return true
            }

            override fun onSuggestionSelect(position: Int): Boolean {
                Log.e("TAG", "chatlog")
                return true
            }
        })

        var util = Utils()
        userData = JSONObject(util.readUserFile(File(filesDir, util.userFileName)))

        gamesList = findViewById(R.id.games_list)
        var gamesArray: ArrayList<Game> = ArrayList()

        pb = findViewById(R.id.pb)

        getGamesInBackground(gamesArray)
        adapter = GamesAdapter(gamesArray, userData!!, this)
        gamesList?.adapter = adapter
        gamesList?.layoutManager = LinearLayoutManager(this)
    }
    private fun getGamesInBackground(games: ArrayList<Game>) {
        Thread {
            try {
                getGames(games)
                runOnUiThread {
                    gamesList?.adapter?.notifyDataSetChanged()
                    pb?.visibility = View.GONE
                }
            } catch(e: InterruptedException) {
                Log.e("TAG", "все плохо")
            }
        }.start()
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun getGames(games: ArrayList<Game>) {
        val gamesData = Utils.request(this, "games/games", "GET", false, null)
        val gamesArray = JSONObject(gamesData).getJSONArray("games")
        Log.e("TAG", gamesData)
        for(i in 0 until gamesArray.length()) {
            games.add(Game(
                gamesArray.getJSONObject(i).getString("name"),
                gamesArray.getJSONObject(i).getString("previewUrl"),
                gamesArray.getJSONObject(i).getString("downloadUrl"),
                gamesArray.getJSONObject(i).getString("_id"),
                gamesArray.getJSONObject(i).getString("description"),
                gamesArray.getJSONObject(i).getString("version"),
                gamesArray.getJSONObject(i).getInt("rating"),
                gamesArray.getJSONObject(i).getInt("summRating"),
                gamesArray.getJSONObject(i).getInt("marks")
            ))
        }

        runOnUiThread {
            gamesList?.adapter?.notifyDataSetChanged()
            adapter?.filter("")
        }
    }
}