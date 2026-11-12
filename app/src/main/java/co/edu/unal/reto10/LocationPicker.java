package co.edu.unal.reto10;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import java.util.ArrayList;
import java.util.List;

/** Botón de selección: usa el recorrido táctil estándar de Button, sin Spinner ni hijos que capturen el toque. */
public class LocationPicker extends Button {
    private final List<String> options = new ArrayList<>();
    private int selection;
    private Runnable onSelectionChanged;
    private AlertDialog dialog;

    public LocationPicker(Context context, AttributeSet attributes) {
        super(context, attributes);
        setAllCaps(false);
        setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
        setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);
        setOnClickListener(view -> openOptions());
    }

    public void setOptions(List<String> values) {
        options.clear(); options.addAll(values); selection = 0;
        setText(options.isEmpty() ? "" : getContext().getString(R.string.all_locations));
        if (onSelectionChanged != null) onSelectionChanged.run();
    }

    public int getCount() { return options.size(); }
    public String getItemAtPosition(int position) { return options.get(position); }
    public int getSelectedItemPosition() { return selection; }
    public String getSelectedItem() { return options.isEmpty() ? "" : options.get(selection); }
    public void setOnSelectionChangedListener(Runnable listener) { onSelectionChanged = listener; }

    public void setSelection(int position) {
        if (position < 0 || position >= options.size() || selection == position) return;
        selection = position; setText(position == 0 ? getContext().getString(R.string.all_locations) : options.get(position));
        if (onSelectionChanged != null) onSelectionChanged.run();
    }

    private void openOptions() {
        if (!isEnabled() || options.isEmpty() || (dialog != null && dialog.isShowing())) return;
        if (getContext() instanceof Activity) {
            Activity activity = (Activity) getContext();
            AutoCompleteTextView bank = activity.findViewById(R.id.name);
            if (bank != null) bank.dismissDropDown();
            View focused = activity.getCurrentFocus();
            if (focused != null) focused.clearFocus();
        }
        ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(getWindowToken(), 0);
        dialog = new AlertDialog.Builder(getContext()).setTitle(getContentDescription())
                .setSingleChoiceItems(options.toArray(new String[0]), selection, (window, position) -> {
                    setSelection(position); window.dismiss();
                }).setNegativeButton(R.string.cancel, null).create();
        dialog.show();
    }

    AlertDialog getSelectionDialog() { return dialog; }

    @Override protected void onDetachedFromWindow() {
        if (dialog != null) dialog.dismiss();
        super.onDetachedFromWindow();
    }
}
