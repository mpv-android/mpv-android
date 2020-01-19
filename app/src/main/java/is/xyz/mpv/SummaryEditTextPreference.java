package is.xyz.mpv;

import android.content.Context;
import android.preference.EditTextPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

public class SummaryEditTextPreference extends EditTextPreference {

    public SummaryEditTextPreference(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); }
    public SummaryEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public SummaryEditTextPreference(Context context) {
        super(context);
    }

    // https://stackoverflow.com/questions/7017082/#answer-7018053
    @Override
    public CharSequence getSummary() {
        String text = getText();
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
}
