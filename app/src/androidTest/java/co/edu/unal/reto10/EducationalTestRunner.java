package co.edu.unal.reto10;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Spinner;
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
                check(((AutoCompleteTextView) current.findViewById(R.id.name)).getAdapter().getCount() > 1, "Sugerencias de bancos cargadas");
                check(((Spinner) current.findViewById(R.id.department)).getCount() > 1, "Lista de departamentos cargada");
                check(((Spinner) current.findViewById(R.id.city)).getCount() > 1, "Lista de ciudades cargada");
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
                check(((Spinner) current.findViewById(R.id.department)).getSelectedItemPosition() == 0, "Limpiar restablece departamento");
                check(((Spinner) current.findViewById(R.id.city)).getSelectedItemPosition() == 0, "Limpiar restablece ciudad");
                check(((ListView) current.findViewById(R.id.list)).getCount() == 0, "Limpiar retira resultados anteriores");
                Spinner departments = current.findViewById(R.id.department);
                for (int i = 1; i < departments.getCount(); i++)
                    if (department.equals(departments.getItemAtPosition(i))) departments.setSelection(i);
            });
            waitForIdleSync();
            onMain(() -> {
                Spinner cities = current.findViewById(R.id.city);
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
            result.putString("stream", "\nOK: selección y texto, limpiar, ciudades dependientes, fecha de fuente y API real. Total=" + all.length() + "; filtrados=" + filtered.length() + "; inexistentes=" + empty.length() + "\n");
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
