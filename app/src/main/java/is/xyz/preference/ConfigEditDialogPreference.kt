package `is`.xyz.preference

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import `is`.xyz.mpv.R
import `is`.xyz.mpv.databinding.ConfEditorBinding
import java.io.File

class ConfigEditDialogPreference(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {
    private var configFile: File
    private lateinit var binding: ConfEditorBinding
    private lateinit var editText: EditText

    init {
        isPersistent = false

        // determine where the file to be edited is located
        val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.ConfigEditDialog)
        val filename = styledAttrs.getString(R.styleable.ConfigEditDialog_filename)
        configFile = File("${context.filesDir.path}/${filename}")

        styledAttrs.recycle()
    }

    override fun onClick() {
        super.onClick()
        val dialog = AlertDialog.Builder(context)
        binding = ConfEditorBinding.inflate(LayoutInflater.from(context))
        dialog.setView(binding.root)
        setupViews()
        dialog.setNegativeButton(R.string.dialog_cancel) { _, _ -> }
        dialog.setPositiveButton(R.string.dialog_ok) { _, _ -> save() }
        dialog.create().show()
    }

    private fun setupViews() {
        editText = binding.editText
        if (configFile.exists())
            editText.setText(configFile.readText())
    }

    private fun save() {
        val content = editText.text.toString()
        if (content == "")
            configFile.delete()
        else
            configFile.writeText(content)
    }
}