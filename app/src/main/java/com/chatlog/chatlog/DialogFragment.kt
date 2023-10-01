package com.chatlog.chatlog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment

class ImagePickerDialogFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Используем наш макет для отображения окна выбора изображения
        return inflater.inflate(R.layout.image_picker_dialog, container, false)
    }

    override fun onResume() {
        super.onResume()

        // Устанавливаем размеры окна
        val width = (resources.displayMetrics.widthPixels * 0.5).toInt()
        val height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog?.window?.setLayout(width, height)
    }
}
