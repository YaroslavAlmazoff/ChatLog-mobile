package com.chatlog.chatlog

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView

class CapabilitiesAdapter(var context: Context, var capability:ArrayList<Capability>) : BaseAdapter() {
    private class ViewHolder(row: View?) {
        var name: TextView
        var image: ImageView
        init {
            this.name = row?.findViewById(R.id.capabilityName) as TextView
            this.image = row?.findViewById(R.id.capabilityImage) as ImageView
        }
    }
    override fun getCount(): Int {
        return capability.count()
    }

    override fun getItem(p0: Int): Any {
        return capability[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        var view: View?
        var viewHolder: ViewHolder
        if(p1 == null) {
            var layout = LayoutInflater.from(context)
            view = layout.inflate(R.layout.capabilities_item, p2, false)
            viewHolder = ViewHolder(view)
            view?.tag = viewHolder
        } else {
            view = p1
            viewHolder = view.tag as ViewHolder
        }
        var capabilityItem: Capability = getItem(p0) as Capability
        viewHolder.name.text = capabilityItem.name
        viewHolder.name.setTextColor(Color.parseColor(capabilityItem.color))
        viewHolder.image.setImageResource(capabilityItem.image)
        return view as View
    }

}