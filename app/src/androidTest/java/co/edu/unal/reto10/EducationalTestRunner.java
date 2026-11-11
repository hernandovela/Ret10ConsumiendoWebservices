package co.edu.unal.reto10;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
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
            runOnMainSync(() -> {
                check(current.findViewById(R.id.name) != null, "Campo de nombre presente");
                check(current.findViewById(R.id.list) != null, "Listado presente");
                check(((Button) current.findViewById(R.id.search)).isEnabled(), "Consulta disponible");
            });
            BloodBankService service = new BloodBankService();
            JSONArray all = service.fetch(BloodBankService.query("", "", ""));
            check(all.length() > 0, "Servicio real devuelve registros");
            JSONObject first = all.getJSONObject(0);
            String department = first.getString("departamento"), city = first.getString("ciudad");
            JSONArray filtered = service.fetch(BloodBankService.query("", department, city));
            check(filtered.length() > 0, "Filtros reales con resultados");
            for (int i = 0; i < filtered.length(); i++) {
                check(filtered.getJSONObject(i).getString("departamento").equalsIgnoreCase(department), "Departamento filtrado correctamente");
                check(filtered.getJSONObject(i).getString("ciudad").equalsIgnoreCase(city), "Ciudad filtrada correctamente");
            }
            JSONArray empty = service.fetch(BloodBankService.query("ZZZ_RETO10_INEXISTENTE_O'Brien", "", ""));
            check(empty.length() == 0, "Sin resultados y apóstrofe aceptado por el servidor");
            result.putString("stream", "\nOK: filtros, escape de literales, actividad y API real. Total=" + all.length() + "; filtrados=" + filtered.length() + "; inexistentes=" + empty.length() + "\n");
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
}
