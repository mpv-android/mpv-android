package `is`.xyz.mpv

import android.content.Context
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.EditText
import java.io.File

class ConfigEditDialog @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.dialogPreferenceStyle,
        defStyleRes: Int = 0
): DialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    private var configFile: File

    init {
        isPersistent = false
        dialogLayoutResource = R.layout.conf_editor

        // determine where the file to be edited is located
        val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.ConfigEditDialog)
        val filename = styledAttrs.getString(R.styleable.ConfigEditDialog_filename)
        configFile = File("${context.filesDir.path}/${filename}")

        styledAttrs.recycle()
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        view.findViewById<Button>(R.id.button_cancel).setOnClickListener {
            this.dialog.cancel()
        }
        view.findViewById<Button>(R.id.button_save).setOnClickListener {
            val content = view.findViewById<EditText>(R.id.editText).text.toString()
            if (content == "")
                configFile.delete()
            else
                configFile.writeText(content)
            this.dialog.dismiss()
        }

        val editText = view.findViewById<EditText>(R.id.editText)
        if (configFile.exists())
            editText.setText(configFile.readText())
    }
}