package co.edu.unal.reto10;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;

/** Abre una lista explícita y evita que el texto hijo o el autocompletado absorban el toque. */
public class LocationSpinner extends Spinner {
    private AlertDialog selectionDialog;

    public LocationSpinner(Context context, AttributeSet attributes) { super(context, attributes); }

    AlertDialog getSelectionDialog() { return selectionDialog; }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        return isEnabled();
    }

    // super.performClick() abriría además el diálogo nativo que aquí se reemplaza.
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    @Override public boolean performClick() {
        if (!isEnabled() || getAdapter() == null) return false;
        if (selectionDialog != null && selectionDialog.isShowing()) return true;
        if (getContext() instanceof Activity) {
            Activity activity = (Activity) getContext();
            AutoCompleteTextView bank = activity.findViewById(R.id.name);
            if (bank != null) bank.dismissDropDown();
            View focused = activity.getCurrentFocus();
            if (focused != null) focused.clearFocus();
        }
        ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(getWindowToken(), 0);
        CharSequence[] options = new CharSequence[getAdapter().getCount()];
        for (int i = 0; i < options.length; i++) options[i] = String.valueOf(getAdapter().getItem(i));
        selectionDialog = new AlertDialog.Builder(getContext())
                .setTitle(getPrompt())
                .setSingleChoiceItems(options, getSelectedItemPosition(), (dialog, position) -> {
                    setSelection(position); dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null).create();
        selectionDialog.show();
        sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED);
        return true;
    }

    @Override protected void onDetachedFromWindow() {
        if (selectionDialog != null) selectionDialog.dismiss();
        super.onDetachedFromWindow();
    }
}
