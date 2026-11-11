package co.edu.unal.reto10;

import android.app.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<JSONObject> banks = new ArrayList<>();
    private TextView status;
    private Button search;
    private View progress;
    private ListView list;
    private EditText name, department, city;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(co.edu.unal.reto10.R.layout.activity_main);
        View root = findViewById(R.id.root);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int padding = (int) (20 * getResources().getDisplayMetrics().density);
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars());
                v.setPadding(padding + bars.left, padding + bars.top, padding + bars.right, padding + bars.bottom);
            } else {
                v.setPadding(padding + insets.getSystemWindowInsetLeft(), padding + insets.getSystemWindowInsetTop(),
                        padding + insets.getSystemWindowInsetRight(), padding + insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        name = findViewById(R.id.name); department = findViewById(R.id.department); city = findViewById(R.id.city);
        status = findViewById(R.id.status); search = findViewById(R.id.search);
        progress = findViewById(R.id.progress); list = findViewById(R.id.list);
        ((TextView) findViewById(R.id.service_mode)).setText(BloodBankService.usesSoda3() ? R.string.soda3_mode : R.string.soda2_mode);
        status.setText(R.string.instructions);
        search.setOnClickListener(v -> load());
        list.setOnItemClickListener((parent, view, position, id) -> showDetail(banks.get(position)));
    }

    private void load() {
        final String query = BloodBankService.query(name.getText().toString(), department.getText().toString(), city.getText().toString());
        search.setEnabled(false); progress.setVisibility(View.VISIBLE); status.setText(R.string.loading);
        banks.clear(); list.setAdapter(null);
        executor.execute(() -> {
            try {
                JSONArray data = new BloodBankService().fetch(query);
                List<JSONObject> result = new ArrayList<>();
                for (int i = 0; i < data.length(); i++) result.add(data.getJSONObject(i));
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    banks.addAll(result);
                    List<Map<String, String>> rows = new ArrayList<>();
                    for (JSONObject bank : banks) {
                        Map<String, String> row = new HashMap<>();
                        row.put("title", value(bank, "bancodesangre"));
                        row.put("subtitle", value(bank, "ciudad") + " · " + value(bank, "departamento"));
                        rows.add(row);
                    }
                    list.setAdapter(new SimpleAdapter(this, rows, android.R.layout.simple_list_item_2,
                            new String[]{"title", "subtitle"}, new int[]{android.R.id.text1, android.R.id.text2}));
                    finishLoad(banks.isEmpty() ? getString(R.string.empty) : getResources().getQuantityString(R.plurals.results, banks.size(), banks.size())
                            + (banks.size() == 1000 ? " " + getString(R.string.limited) : ""));
                });
            } catch (Exception error) {
                String message = error instanceof java.net.UnknownHostException ? getString(R.string.network_error) :
                        error instanceof java.net.SocketTimeoutException ? getString(R.string.timeout_error) :
                        error instanceof JSONException ? getString(R.string.json_error) : error.getMessage();
                runOnUiThread(() -> { if (!isDestroyed()) finishLoad(message == null ? getString(R.string.generic_error) : message); });
            }
        });
    }

    private void finishLoad(String message) {
        search.setEnabled(true); progress.setVisibility(View.GONE); status.setText(message);
    }

    private String value(JSONObject bank, String field) {
        return bank.isNull(field) || bank.optString(field).trim().isEmpty() ? getString(R.string.missing) : bank.optString(field);
    }

    private void showDetail(JSONObject bank) {
        String[][] fields = {{"codigonacionaldesangre", "Código"}, {"categoria", "Categoría"},
                {"departamento", "Departamento"}, {"ciudad", "Ciudad"}, {"direccion", "Dirección"},
                {"estado", "Estado"}, {"concepto_tecnico", "Concepto técnico"}, {"aferesis", "Aféresis"},
                {"tecnica_aferesis", "Técnica de aféresis"}, {"numero_de_puestos_fijos_recolectores_sangre", "Puestos recolectores"},
                {"ultima_visita", "Última visita"}, {"a_o_de_cierre", "Año de cierre"}};
        StringBuilder detail = new StringBuilder();
        for (String[] field : fields) detail.append(field[1]).append(": ").append(value(bank, field[0])).append("\n\n");
        TextView text = new TextView(this); text.setText(detail); text.setTextIsSelectable(true); text.setPadding(24, 16, 24, 16);
        ScrollView scroll = new ScrollView(this); scroll.addView(text);
        new AlertDialog.Builder(this).setTitle(value(bank, "bancodesangre")).setView(scroll).setPositiveButton(R.string.close, null).show();
    }

    @Override protected void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}

