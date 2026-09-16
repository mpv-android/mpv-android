package `is`.xyz.mpv.preferences

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
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
    private lateinit var dialog: AlertDialog
    private var dialogMessage: String?

    init {
        isPersistent = false

        // determine where the file to be edited is located
        val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.ConfigEditDialog)
        val filename = styledAttrs.getString(R.styleable.ConfigEditDialog_filename)
        dialogMessage = styledAttrs.getString(R.styleable.ConfigEditDialog_dialogMessage)
        configFile = File("${context.filesDir.path}/${filename}")

        styledAttrs.recycle()
    }

    override fun onClick() {
        super.onClick()
        val builder = AlertDialog.Builder(context)
        binding = ConfEditorBinding.inflate(LayoutInflater.from(context))
        builder.setView(binding.root)
        builder.setTitle(title)
        builder.setMessage(dialogMessage)
        setupViews()
        builder.setNegativeButton(R.string.dialog_cancel) { _, _ -> }
        builder.setPositiveButton(R.string.dialog_save) { _, _ -> save() }
        dialog = builder.create().apply { show() }
    }

    private fun setupViews() {
        if (configFile.exists())
            binding.editText.setText(configFile.readText())
        binding.editText.doOnTextChanged { _, _, _, _ -> setUnsaved(true) }
    }

    private fun save() {
        val content = binding.editText.text.toString()
        if (content.isEmpty())
            configFile.delete()
        else
            configFile.writeText(content)
        setUnsaved(false)
    }

    private fun setUnsaved(state: Boolean) {
        binding.unsavedText.isVisible = state
        dialog.setCancelable(!state)
    }
}
