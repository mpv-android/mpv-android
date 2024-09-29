package `is`.xyz.mpv.controls

import android.view.LayoutInflater
import android.view.View

internal interface PickerDialog {
    fun buildView(layoutInflater: LayoutInflater): View

    fun isInteger(): Boolean // eh....
    fun reset()

    var number: Double?
}
