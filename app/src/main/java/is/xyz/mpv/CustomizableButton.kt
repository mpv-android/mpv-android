package `is`.xyz.mpv

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button

import java.util.IllegalFormatException

class CustomizableButton {
    private var enabled = false
    private var instance: Button? = null

    private var command = ""

    private var appearance = Appearance.STATIC
    // STATIC
    private var textStatic = ""
    // PROPERTY
    private var property = ""
    private var propertyNumeric = false
    private var textFormat = ""

    fun readFromPreferences(prefs: SharedPreferences, key: String) {
        enabled = prefs.getBoolean(key, false)
        if (!enabled)
            return

        command = prefs.getString("${key}_command", "")!!
        when (prefs.getString("${key}_appearance", "")) {
            "static" -> appearance = Appearance.STATIC
            "property" -> appearance = Appearance.PROPERTY
        }
        textStatic = prefs.getString("${key}_static", "")!!
        property = prefs.getString("${key}_property", "")!!
        propertyNumeric = prefs.getBoolean("${key}_property_numeric", false)
        textFormat = prefs.getString("${key}_format", "")!!

        // Do some validation
        val tmp = arrayOf(
                command == "",
                appearance == Appearance.STATIC && textStatic == "",
                appearance == Appearance.PROPERTY && property == "",
                appearance == Appearance.PROPERTY && textFormat == ""
        )
        if (tmp.any { b -> b }) {
            enabled = false
            Log.e(TAG, "CustomizableButton: values provided but validation failed")
        }
    }

    fun observeProperties() {
        if (!enabled)
            return
        if (appearance == Appearance.PROPERTY)
            MPVLib.observeProperty(property, MPVLib.mpvFormat.MPV_FORMAT_STRING)
    }

    fun instantiate(context: Context): View {
        val b = Button(context, null, R.attr.buttonBarButtonStyle)

        // The style forces all caps but we don't want it for the custom buttons
        b.isAllCaps = false

        b.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        when (appearance) {
            Appearance.STATIC -> b.text = textStatic
            Appearance.PROPERTY -> b.text = "..."
        }
        b.setTextColor(context.resources.getColor(android.R.color.white))
        b.setOnClickListener {
            Log.v(TAG, "CustomizableButton: executing ${command}")
            MPVLib.commandString(command)
        }
        b.visibility = if (enabled) View.VISIBLE else View.GONE

        instance = b
        return b
    }

    fun destroy() {
        instance = null
    }

    fun eventProperty(property: String, value: String) {
        if (instance == null)
            return
        if (appearance != Appearance.PROPERTY || property != this.property)
            return

        var text = "ERROR"
        try {
            text = if (propertyNumeric)
                String.format(textFormat, value.toDouble())
            else
                String.format(textFormat, value)
        } catch (e: IllegalFormatException) {
        } catch (e: NumberFormatException) {
        }
        instance!!.text = text
    }

    private enum class Appearance {
        STATIC, PROPERTY
    }

    companion object {
        private const val TAG = "mpv"
    }
}
