package co.edu.unal.reto10;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ListView;
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
                    && ((Spinner) current.findViewById(R.id.name)).getCount() > 1);
            runOnMainSync(() -> {
                check(((Spinner) current.findViewById(R.id.name)).getCount() > 1, "Lista de bancos cargada desde el servicio");
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
            runOnMainSync(() -> {
                Spinner bankSelector = current.findViewById(R.id.name);
                int index = -1;
                for (int i = 1; i < bankSelector.getCount(); i++)
                    if (bankName.equals(bankSelector.getItemAtPosition(i))) index = i;
                check(index > 0, "El banco real está disponible para seleccionar");
                bankSelector.setSelection(index);
            });
            waitForIdleSync();
            runOnMainSync(() -> current.findViewById(R.id.search).performClick());
            waitUntil(() -> ((Button) current.findViewById(R.id.search)).isEnabled());
            runOnMainSync(() -> {
                ListView rows = current.findViewById(R.id.list);
                check(rows.getCount() > 0, "La selección genera resultados en la interfaz");
                for (int i = 0; i < rows.getCount(); i++)
                    check(bankName.equals(((Map<?, ?>) rows.getItemAtPosition(i)).get("title")), "Solo aparece el banco seleccionado");
            });
            String department = first.getString("departamento"), city = first.getString("ciudad");
            JSONArray filtered = service.fetch(BloodBankService.query("", department, city));
            check(filtered.length() > 0, "Filtros reales con resultados");
            for (int i = 0; i < filtered.length(); i++) {
                check(filtered.getJSONObject(i).getString("departamento").equalsIgnoreCase(department), "Departamento filtrado correctamente");
                check(filtered.getJSONObject(i).getString("ciudad").equalsIgnoreCase(city), "Ciudad filtrada correctamente");
            }
            JSONArray empty = service.fetch(BloodBankService.query("ZZZ_RETO10_INEXISTENTE_O'Brien", "", ""));
            check(empty.length() == 0, "Sin resultados y apóstrofe aceptado por el servidor");
            result.putString("stream", "\nOK: tres listas reales, selección de banco en interfaz, filtros y API real. Total=" + all.length() + "; filtrados=" + filtered.length() + "; inexistentes=" + empty.length() + "\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "\nFAIL: " + error + "\n");
            finish(Activity.RESULT_CANCELED, result);
        } finally {
            if (activity != null) { Activity current = activity; runOnMainSync(current::finish); }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void waitUntil(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = android.os.SystemClock.uptimeMillis() + 45000;
        java.util.concurrent.atomic.AtomicBoolean ready = new java.util.concurrent.atomic.AtomicBoolean();
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            runOnMainSync(() -> ready.set(condition.getAsBoolean()));
            if (ready.get()) return;
            Thread.sleep(100);
        }
        throw new AssertionError("La operación de interfaz no terminó correctamente en 45 segundos");
    }
}
