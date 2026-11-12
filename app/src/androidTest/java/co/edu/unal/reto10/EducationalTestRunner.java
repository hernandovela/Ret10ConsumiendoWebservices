package co.edu.unal.reto10;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import android.widget.ListView;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Pruebas en Android real/emulado, sin dependencias de pruebas externas. */
public class EducationalTestRunner extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }

    @Override public void onStart() {
        Bundle result = new Bundle();
        Activity activity = null;
        try {
            String query = BloodBankService.query("O'Brien", "Antioquia", "Medellín");
            check(query.contains("O''Brien"), "Apóstrofes escapados en SoQL");
            check(query.contains("upper(departamento) = upper('Antioquia')"), "Filtro de departamento enviado");
            check(query.contains("upper(ciudad) = upper('Medellín')"), "Filtro de ciudad enviado");
            check(!BloodBankService.query("", "", "").contains("WHERE"), "Consulta sin filtros válida");
            activity = startActivitySync(new Intent(getTargetContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            Activity current = activity;
            waitUntil(() -> ((Button) current.findViewById(R.id.search)).isEnabled()
                    && ((AutoCompleteTextView) current.findViewById(R.id.name)).getAdapter() != null);
            onMain(() -> {
                AutoCompleteTextView bank = current.findViewById(R.id.name);
                bank.setText("hospital", false); bank.dismissDropDown(); bank.clearFocus();
                ((android.view.inputmethod.InputMethodManager) current.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(bank.getWindowToken(), 0);
            });
            waitForIdleSync();
            verifyLocationPicker(current, R.id.department, R.string.department_hint);
            onMain(() -> ((AutoCompleteTextView) current.findViewById(R.id.name)).setText("hospital", false));
            verifyLocationPicker(current, R.id.city, R.string.city_hint);
            onMain(() -> current.findViewById(R.id.clear).performClick());
            waitForIdleSync();
            verifyLocationPicker(current, R.id.department, R.string.department_hint);
            verifyLocationPicker(current, R.id.city, R.string.city_hint);
            onMain(() -> current.findViewById(R.id.clear).performClick());
            waitForIdleSync();
            onMain(() -> {
                check(((AutoCompleteTextView) current.findViewById(R.id.name)).getAdapter().getCount() > 1, "Sugerencias de bancos cargadas");
                check(((LocationPicker) current.findViewById(R.id.department)).getCount() > 1, "Lista de departamentos cargada");
                check(((LocationPicker) current.findViewById(R.id.city)).getCount() > 1, "Lista de ciudades cargada");
                check(current.findViewById(R.id.list) != null, "Listado presente");
                check(((Button) current.findViewById(R.id.search)).isEnabled(), "Consulta disponible");
            });
            BloodBankService service = new BloodBankService();
            JSONArray all = service.fetch(BloodBankService.query("", "", ""));
            check(all.length() > 0, "Servicio real devuelve registros");
            JSONObject first = all.getJSONObject(0);
            String bankName = first.getString("bancodesangre");
            onMain(() -> {
                AutoCompleteTextView bankSelector = current.findViewById(R.id.name);
                int index = -1;
                for (int i = 0; i < bankSelector.getAdapter().getCount(); i++)
                    if (bankName.equals(bankSelector.getAdapter().getItem(i))) index = i;
                check(index >= 0, "El banco real está disponible para seleccionar");
                bankSelector.setText(bankName, false);
            });
            waitForIdleSync();
            onMain(() -> current.findViewById(R.id.search).performClick());
            waitUntil(() -> ((Button) current.findViewById(R.id.search)).isEnabled());
            onMain(() -> {
                ListView rows = current.findViewById(R.id.list);
                check(rows.getCount() > 0, "La selección genera resultados en la interfaz");
                for (int i = 0; i < rows.getCount(); i++)
                    check(bankName.equals(((Map<?, ?>) rows.getItemAtPosition(i)).get("title")), "Solo aparece el banco seleccionado");
            });
            String department = first.getString("departamento"), city = first.getString("ciudad");
            onMain(() -> current.findViewById(R.id.clear).performClick());
            waitForIdleSync();
            onMain(() -> {
                check(((AutoCompleteTextView) current.findViewById(R.id.name)).getText().length() == 0, "Limpiar borra el nombre");
                check(((LocationPicker) current.findViewById(R.id.department)).getSelectedItemPosition() == 0, "Limpiar restablece departamento");
                check(((LocationPicker) current.findViewById(R.id.city)).getSelectedItemPosition() == 0, "Limpiar restablece ciudad");
                check(((ListView) current.findViewById(R.id.list)).getCount() == 0, "Limpiar retira resultados anteriores");
                LocationPicker departments = current.findViewById(R.id.department);
                for (int i = 1; i < departments.getCount(); i++)
                    if (department.equals(departments.getItemAtPosition(i))) departments.setSelection(i);
            });
            waitForIdleSync();
            onMain(() -> {
                LocationPicker cities = current.findViewById(R.id.city);
                for (int i = 1; i < cities.getCount(); i++) {
                    boolean belongs = false;
                    for (int j = 0; j < all.length(); j++) {
                        JSONObject bank = all.optJSONObject(j);
                        if (department.equals(bank.optString("departamento")) && cities.getItemAtPosition(i).equals(bank.optString("ciudad"))) belongs = true;
                    }
                    check(belongs, "Ciudades restringidas al departamento");
                }
                check(((TextView) current.findViewById(R.id.service_mode)).getText().toString().matches(".*\\d{2}/\\d{2}/\\d{4}.*"), "Fecha real de actualización visible");
                current.findViewById(R.id.clear).performClick();
                ((AutoCompleteTextView) current.findViewById(R.id.name)).setText("BANCO", false);
            });
            waitForIdleSync();
            onMain(() -> current.findViewById(R.id.search).performClick());
            waitUntil(() -> ((Button) current.findViewById(R.id.search)).isEnabled());
            onMain(() -> {
                ListView rows = current.findViewById(R.id.list);
                check(rows.getCount() > 0, "Búsqueda por texto parcial disponible");
                for (int i = 0; i < rows.getCount(); i++) check(((Map<?, ?>) rows.getItemAtPosition(i)).get("title").toString().contains("BANCO"), "Coincidencia parcial enviada al servicio");
            });
            JSONArray filtered = service.fetch(BloodBankService.query("", department, city));
            check(filtered.length() > 0, "Filtros reales con resultados");
            for (int i = 0; i < filtered.length(); i++) {
                check(filtered.getJSONObject(i).getString("departamento").equalsIgnoreCase(department), "Departamento filtrado correctamente");
                check(filtered.getJSONObject(i).getString("ciudad").equalsIgnoreCase(city), "Ciudad filtrada correctamente");
            }
            JSONArray empty = service.fetch(BloodBankService.query("ZZZ_RETO10_INEXISTENTE_O'Brien", "", ""));
            check(empty.length() == 0, "Sin resultados y apóstrofe aceptado por el servidor");
            result.putString("stream", "\nPASS: Departamento y Ciudad, centro y flecha, nombre escrito, después de Limpiar, seleccionar y reabrir (8 aperturas por DOWN/UP)."
                    + "\nPASS: sugerencias, nombre completo/parcial, Limpiar, ciudades dependientes y fecha de fuente."
                    + "\nPASS: API real. Total=" + all.length() + "; filtrados=" + filtered.length() + "; inexistentes=" + empty.length() + "\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "\nFAIL: " + error + "\n");
            finish(Activity.RESULT_CANCELED, result);
        } finally {
            if (activity != null) { Activity current = activity; onMain(current::finish); }
        }
    }

    private void onMain(Runnable action) {
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        super.runOnMainSync(() -> { try { action.run(); } catch (Throwable error) { failure.set(error); } });
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private void verifyLocationPicker(Activity activity, int fieldId, int titleId) throws Exception {
        for (boolean arrow : new boolean[]{false, true}) {
            onMain(() -> {
                android.view.View field = activity.findViewById(fieldId);
                float x = arrow ? field.getWidth() - 12 : field.getWidth() / 2f;
                float y = field.getHeight() / 2f;
                long time = android.os.SystemClock.uptimeMillis();
                android.view.MotionEvent down = android.view.MotionEvent.obtain(time, time, android.view.MotionEvent.ACTION_DOWN, x, y, 0);
                android.view.MotionEvent up = android.view.MotionEvent.obtain(time, time + 80, android.view.MotionEvent.ACTION_UP, x, y, 0);
                try { field.dispatchTouchEvent(down); field.dispatchTouchEvent(up); }
                finally { down.recycle(); up.recycle(); }
            });
            waitForIdleSync();
            onMain(() -> {
                android.app.AlertDialog dialog = ((LocationPicker) activity.findViewById(fieldId)).getSelectionDialog();
                check(dialog != null && dialog.isShowing() && dialog.getListView().isShown(), "Pulsación en " + (arrow ? "flecha" : "centro") + " abre " + activity.getString(titleId));
                check(dialog.getListView().getCount() > 1, "Opciones reales visibles");
                if (!arrow) {
                    dialog.getListView().performItemClick(dialog.getListView().getChildAt(1), 1, dialog.getListView().getAdapter().getItemId(1));
                    check(((LocationPicker) activity.findViewById(fieldId)).getSelectedItemPosition() == 1, "La opción elegida se refleja en el campo");
                } else {
                    check(dialog.getListView().getCheckedItemPosition() == 1, "Reabrir conserva la opción seleccionada");
                    dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick();
                }
            });
            waitForIdleSync();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void waitUntil(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = android.os.SystemClock.uptimeMillis() + 45000;
        java.util.concurrent.atomic.AtomicBoolean ready = new java.util.concurrent.atomic.AtomicBoolean();
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            onMain(() -> ready.set(condition.getAsBoolean()));
            if (ready.get()) return;
            Thread.sleep(100);
        }
        throw new AssertionError("La operación de interfaz no terminó correctamente en 45 segundos");
    }
}
