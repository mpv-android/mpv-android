package `is`.xyz.mpv.player

import android.view.LayoutInflater
import android.view.View

internal interface PickerDialog {
    fun buildView(layoutInflater: LayoutInflater): View

    fun isInteger(): Boolean // eh....
    fun reset(): Unit = TODO()

    val canReset: Boolean

    var number: Double?
}
