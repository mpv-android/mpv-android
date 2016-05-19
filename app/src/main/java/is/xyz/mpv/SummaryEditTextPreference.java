package is.xyz.mpv;

import android.app.Activity;
import android.content.Context;
import android.preference.EditTextPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

public class SummaryEditTextPreference extends EditTextPreference {

    private String formatString;

    public SummaryEditTextPreference(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); }
    public SummaryEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public SummaryEditTextPreference(Context context) {
        super(context);
    }

    // originally from somewhere on stack overflow, can't remember where.
    @Override
    public CharSequence getSummary() {
        String text = getText();
        formatString = text;
        if (TextUtils.isEmpty(text)) {
            return getEditText().getHint();
        } else {
            CharSequence summary = super.getSummary();
            if (summary != null) {
                return String.format(summary.toString(), text);
            } else {
                return null;
            }
        }
    }

    public String getFormatString() {
        return formatString;
    }
}
