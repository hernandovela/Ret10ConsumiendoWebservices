package co.edu.unal.reto10;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Consulta SODA3 en segundo plano. No usa bibliotecas externas para procesar JSON. */
public final class BloodBankService {
    private static final String ENDPOINT = "https://www.datos.gov.co/api/v3/views/kwki-6kd6/query.json";
    private static final String PUBLIC_ENDPOINT = "https://www.datos.gov.co/resource/kwki-6kd6.json";

    public static boolean usesSoda3() { return !BuildConfig.SOCRATA_APP_TOKEN.trim().isEmpty(); }

    /** Fecha de actualización de filas de la fuente, no fecha de instalación de la app. */
    public long fetchUpdatedAt() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://www.datos.gov.co/api/views/kwki-6kd6.json").openConnection();
        try {
            connection.setConnectTimeout(10000); connection.setReadTimeout(10000);
            if (connection.getResponseCode() != 200) throw new IOException("Metadatos no disponibles");
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line; while ((line = reader.readLine()) != null) body.append(line);
            }
            long timestamp = new JSONObject(body.toString()).optLong("rowsUpdatedAt", 0);
            if (timestamp <= 0) throw new IOException("Fecha no publicada");
            return timestamp;
        } finally { connection.disconnect(); }
    }

    static String literal(String value) {
        return "'" + value.trim().replace("'", "''") + "'";
    }

    public static String query(String name, String department, String city) {
        List<String> filters = new ArrayList<>();
        if (!name.trim().isEmpty()) filters.add("contains(upper(bancodesangre), upper(" + literal(name) + "))");
        if (!department.trim().isEmpty()) filters.add("upper(departamento) = upper(" + literal(department) + ")");
        if (!city.trim().isEmpty()) filters.add("upper(ciudad) = upper(" + literal(city) + ")");
        return "SELECT *" + (filters.isEmpty() ? "" : " WHERE " + String.join(" AND ", filters))
                + " ORDER BY bancodesangre, numero_de_consecutivo";
    }

    public JSONArray fetch(String query) throws Exception {
        boolean soda3 = usesSoda3();
        String endpoint = soda3 ? ENDPOINT : PUBLIC_ENDPOINT + "?$query="
                + URLEncoder.encode(query + " LIMIT 1000", "UTF-8");
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        try {
            connection.setRequestMethod(soda3 ? "POST" : "GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Accept", "application/json");
            if (soda3) {
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("X-App-Token", BuildConfig.SOCRATA_APP_TOKEN);
                connection.setDoOutput(true);
                JSONObject request = new JSONObject();
                request.put("query", query);
                request.put("page", new JSONObject().put("pageNumber", 1).put("pageSize", 1000));
                request.put("includeSynthetic", false);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(request.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            int code = connection.getResponseCode();
            if (code == 401 || code == 403) throw new IOException("Acceso rechazado (HTTP " + code + "). Revisa el token local de Socrata.");
            if (code == 429) throw new IOException("Límite de consultas alcanzado. Intenta más tarde.");
            if (code < 200 || code >= 300) throw new IOException("El servicio respondió HTTP " + code + ". Intenta nuevamente.");
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }
            return new JSONArray(body.toString());
        } finally { connection.disconnect(); }
    }
}

