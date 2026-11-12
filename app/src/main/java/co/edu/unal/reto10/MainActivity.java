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
    private Button search, clear;
    private View progress;
    private ListView list;
    private AutoCompleteTextView name;
    private LocationPicker department, city;
    private View chooseBank;
    private JSONArray catalog = new JSONArray();
    private String lastDepartment = "", lastCity = "";
    private boolean filtersReady;

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
        clear = findViewById(R.id.clear); chooseBank = findViewById(R.id.choose_bank);
        progress = findViewById(R.id.progress); list = findViewById(R.id.list);
        ((TextView) findViewById(R.id.service_mode)).setText(getString(R.string.version_loading, BuildConfig.VERSION_NAME));
        status.setText(R.string.instructions);
        search.setOnClickListener(v -> { if (filtersReady) load(); else loadOptions(); });
        list.setOnItemClickListener((parent, view, position, id) -> showDetail(banks.get(position)));
        name.setOnClickListener(v -> name.showDropDown());
        chooseBank.setOnClickListener(v -> { name.requestFocus(); name.showDropDown(); });
        name.setOnEditorActionListener((view, action, event) -> {
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_DONE && filtersReady && search.isEnabled()) {
                load(); return true;
            }
            return false;
        });
        clear.setOnClickListener(v -> clearFilters());
        department.setOnSelectionChangedListener(() -> {
                String current = selected(department);
                if (!filtersReady || current.equals(lastDepartment)) return;
                lastDepartment = current; lastCity = "";
                populate(city, matchingCatalog(false), "ciudad", R.string.all_cities);
                name.setText("", false); updateBankOptions(); clearResults();
        });
        city.setOnSelectionChangedListener(() -> {
                String current = selected(city);
                if (!filtersReady || current.equals(lastCity)) return;
                lastCity = current; name.setText("", false); updateBankOptions(); clearResults();
        });
        loadOptions();
    }

    /** Las opciones se obtienen del servicio, no de listas inventadas o fijas. */
    private void loadOptions() {
        search.setEnabled(false); setFiltersEnabled(false);
        progress.setVisibility(View.VISIBLE); status.setText(R.string.loading_options);
        executor.execute(() -> {
            try {
                BloodBankService service = new BloodBankService();
                JSONArray data = service.fetch(BloodBankService.query("", "", ""));
                long updatedAt;
                try { updatedAt = service.fetchUpdatedAt(); } catch (Exception unavailable) { updatedAt = 0; }
                final long sourceDate = updatedAt;
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    catalog = data;
                    populate(department, catalog, "departamento", R.string.all_departments);
                    populate(city, catalog, "ciudad", R.string.all_cities);
                    updateBankOptions();
                    String date = sourceDate > 0 ? java.time.Instant.ofEpochSecond(sourceDate)
                            .atZone(java.time.ZoneId.of("America/Bogota"))
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("es", "CO"))) : getString(R.string.date_unavailable);
                    ((TextView) findViewById(R.id.service_mode)).setText(getString(R.string.version_data, BuildConfig.VERSION_NAME, date));
                    filtersReady = true;
                    search.setText(R.string.search);
                    finishLoad(getString(R.string.instructions));
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    search.setText(R.string.retry_options);
                    ((TextView) findViewById(R.id.service_mode)).setText(getString(R.string.version_data, BuildConfig.VERSION_NAME, getString(R.string.date_unavailable)));
                    finishLoad(getString(R.string.options_error));
                });
            }
        });
    }

    private JSONArray matchingCatalog(boolean includeCity) {
        JSONArray matches = new JSONArray();
        String dep = selected(department), town = includeCity ? selected(city) : "";
        for (int i = 0; i < catalog.length(); i++) {
            JSONObject bank = catalog.optJSONObject(i);
            if (bank != null && (dep.isEmpty() || dep.equals(bank.optString("departamento")))
                    && (town.isEmpty() || town.equals(bank.optString("ciudad")))) matches.put(bank);
        }
        return matches;
    }

    private void updateBankOptions() {
        SortedSet<String> options = new TreeSet<>();
        JSONArray matches = matchingCatalog(true);
        for (int i = 0; i < matches.length(); i++) {
            String bank = matches.optJSONObject(i).optString("bancodesangre").trim();
            if (!bank.isEmpty()) options.add(bank);
        }
        name.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_option, new ArrayList<>(options)));
    }

    private void clearResults() {
        banks.clear(); list.setAdapter(null); status.setText(R.string.instructions);
    }

    private void clearFilters() {
        lastDepartment = ""; lastCity = "";
        department.setSelection(0);
        populate(city, catalog, "ciudad", R.string.all_cities);
        name.setText("", false); name.dismissDropDown(); updateBankOptions(); clearResults();
        name.clearFocus();
        android.view.inputmethod.InputMethodManager keyboard = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        keyboard.hideSoftInputFromWindow(name.getWindowToken(), 0);
        status.setText(R.string.filters_cleared);
    }

    private void populate(LocationPicker spinner, JSONArray catalog, String field, int allLabel) {
        SortedSet<String> unique = new TreeSet<>();
        for (int i = 0; i < catalog.length(); i++) {
            JSONObject bank = catalog.optJSONObject(i);
            if (bank != null && !bank.isNull(field)) {
                String option = bank.optString(field).trim();
                if (!option.isEmpty()) unique.add(option);
            }
        }
        List<String> options = new ArrayList<>();
        options.add(getString(allLabel)); options.addAll(unique);
        spinner.setOptions(options);
    }

    private String selected(LocationPicker spinner) {
        return spinner.getSelectedItemPosition() <= 0 ? "" : spinner.getSelectedItem().toString();
    }

    private void setFiltersEnabled(boolean enabled) {
        name.setEnabled(enabled); department.setEnabled(enabled); city.setEnabled(enabled);
        clear.setEnabled(enabled); chooseBank.setEnabled(enabled);
    }

    private void load() {
        final String query = BloodBankService.query(name.getText().toString(), selected(department), selected(city));
        name.dismissDropDown(); name.clearFocus();
        ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(name.getWindowToken(), 0);
        search.setEnabled(false); setFiltersEnabled(false); progress.setVisibility(View.VISIBLE); status.setText(R.string.loading);
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
        search.setEnabled(true); setFiltersEnabled(filtersReady); progress.setVisibility(View.GONE); status.setText(message);
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

