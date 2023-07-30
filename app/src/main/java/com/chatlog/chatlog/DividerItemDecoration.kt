package com.chatlog.chatlog

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class DividerItemDecoration(context: Context, private val dividerResId: Int) :
    RecyclerView.ItemDecoration() {
    private val divider: Drawable? = ContextCompat.getDrawable(context, dividerResId)

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight

        for (i in 0 until parent.childCount - 1) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + divider?.intrinsicHeight!!
            divider.setBounds(left, top, right, bottom)
            divider.draw(c)
        }
    }
}