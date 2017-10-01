package is.xyz.mpv;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.Arrays;

public class ScalerDialogPreference extends DialogPreference {
    public ScalerDialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }
    public ScalerDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
    public ScalerDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private View myView;
    private String[] entries;

    private void init(Context context, AttributeSet attrs_) {
        setPersistent(false);
        setDialogLayoutResource(R.layout.scaler_pref);
        TypedArray attrs = context.obtainStyledAttributes(attrs_, R.styleable.ScalerPreferenceDialog);

        // read list of scalers from specified resource
        int res = attrs.getResourceId(R.styleable.ScalerPreferenceDialog_entries, -1);
        entries = context.getResources().getStringArray(res);

        attrs.recycle();
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        SharedPreferences prefs = getSharedPreferences();
        myView = view;

        Spinner s = (Spinner) myView.findViewById(R.id.scaler);
        EditText e1 = (EditText) myView.findViewById(R.id.param1);
        EditText e2 = (EditText) myView.findViewById(R.id.param2);

        // populate Spinner and set selected item
        s.setAdapter(new ArrayAdapter<>(getContext(), R.layout.scaler_pref_textview, entries));
        int idx = Arrays.asList(entries).indexOf(prefs.getString(getKey(), ""));
        if(idx != -1)
            s.setSelection(idx, false);

        // populate EditText's
        e1.setText(prefs.getString(getKey() + "_param1", ""));
        e2.setText(prefs.getString(getKey() + "_param2", ""));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        Editor e = getEditor();

        // save setting values if user presses OK
        if(positiveResult) {
            Spinner s = (Spinner) myView.findViewById(R.id.scaler);
            EditText e1 = (EditText) myView.findViewById(R.id.param1);
            EditText e2 = (EditText) myView.findViewById(R.id.param2);

            e.putString(getKey(), (String) s.getSelectedItem());
            e.putString(getKey() + "_param1", e1.getText().toString());
            e.putString(getKey() + "_param2", e2.getText().toString());
            e.commit();
        }
    }
}
