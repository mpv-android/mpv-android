package `is`.xyz.mpv

import android.content.Context
import android.preference.EditTextPreference
import android.text.TextUtils
import android.util.AttributeSet

class SummaryEditTextPreference : EditTextPreference {

    var formatString: String? = null
        private set

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    constructor(context: Context) : super(context) {
    }

    // originally from somewhere on stack overflow, can't remember where.
    override fun getSummary(): CharSequence? {
        val text = text
        formatString = text
        if (TextUtils.isEmpty(text)) {
            return editText.hint
        } else {
            val summary = super.getSummary()
            if (summary != null) {
                return String.format(summary.toString(), text)
            } else {
                return null
            }
        }
    }
}
