package com.chatlog.chatlog

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import androidx.core.app.ActivityCompat

class ImagesTestActivity : AppCompatActivity() {
    lateinit var rs: Cursor
    var greed: GridView? = null
    var pickImages: View? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_images_test)

        listImages()
        checkPermission()
    }


    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 101)
        } else {
            listImages()
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.e("TAG", grantResults.isNotEmpty().toString())
        Log.e("TAG", grantResults[0].toString())
        Log.e("TAG", (grantResults[0] == PackageManager.PERMISSION_GRANTED).toString())
        Log.e("TAG", (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED).toString())
        when (requestCode) {
            101 -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    try {
                        listImages()
                    } catch(ex: SecurityException) {
                        Log.e("TAG", "Security Exception, no location available")
                    }
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }



    private fun listImages() {
        var cols = listOf(MediaStore.Images.Thumbnails.DATA).toTypedArray()
        rs = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cols, null, null, null)!!
        pickImages?.visibility = View.VISIBLE
        greed?.adapter = ImagesAdapter(applicationContext)
    }
    inner class ImagesAdapter : BaseAdapter {
        var context: Context
        constructor(context: Context) {
            this.context = context
        }
        override fun getCount(): Int {
            return rs.count
        }

        override fun getItem(position: Int): Any {
            return position
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            var iv = ImageView(context)
            rs.moveToPosition(position)
            var path = rs.getString(0)
            var bitmap = BitmapFactory.decodeFile(path)
            iv.setImageBitmap(bitmap)
            iv.layoutParams = AbsListView.LayoutParams(300, 300)
            return iv
        }

    }
}