package py.com.zafiro.printer.print;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Wrapper sobre el JsonObject de datos recibido del servidor.
 *
 * Las claves del JSON deben coincidir con los nombres de variable usados
 * en el PrintConfig (escritos entre llaves: {variable}).
 *
 * Formatos disponibles para getValue(key, format):
 *   "currency"  → 5.375.000
 *   "currency2" → 5.375.000,00
 *   "number"    → 2.000
 *   "uppercase" → TEXTO EN MAYÚSCULAS
 *   "date"      → valor directo (ya debe venir formateado)
 *   "datetime"  → valor directo (ya debe venir formateado)
 *   null / ""   → valor directo como String
 */
public class PrintData {

    private static final DecimalFormatSymbols SYMBOLS;

    static {
        SYMBOLS = new DecimalFormatSymbols(Locale.getDefault());
        SYMBOLS.setGroupingSeparator('.');
        SYMBOLS.setDecimalSeparator(',');
    }

    private final JsonObject json;

    public PrintData(JsonObject json) {
        this.json = json;
    }

    /**
     * Obtiene el valor de una clave raíz como String, sin formato.
     */
    public String getValue(String key) {
        return getValue(key, null);
    }

    /**
     * Obtiene el valor de una clave raíz como String aplicando el formato indicado.
     *
     * @param key    nombre del campo en el JSON de datos
     * @param format "currency" | "currency2" | "number" | "uppercase" | null
     */
    public String getValue(String key, String format) {
        if (!json.containsKey(key)) {
            return "";
        }

        JsonValue val = json.get(key);
        String raw = toRawString(val);
        return applyFormat(raw, format);
    }

    /**
     * Obtiene un array de objetos del JSON (para el elemento "rows").
     *
     * @param key nombre del campo array en el JSON de datos
     */
    public JsonArray getArray(String key) {
        if (!json.containsKey(key)) {
            return null;
        }
        try {
            return json.getJsonArray(key);
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Obtiene un valor desde un JsonObject hijo (para iterar rows).
     */
    public static String getValueFromRow(JsonObject row, String key, String format) {
        if (row == null || !row.containsKey(key)) {
            return "";
        }
        String raw = toRawString(row.get(key));
        return applyFormat(raw, format);
    }

    // ── Helpers internos ─────────────────────────────────────────────────────

    private static String toRawString(JsonValue val) {
        if (val == null) return "";
        switch (val.getValueType()) {
            case STRING:
                return ((JsonString) val).getString();
            case NUMBER:
                return val.toString();
            case TRUE:
                return "true";
            case FALSE:
                return "false";
            case NULL:
                return "";
            default:
                return val.toString();
        }
    }

    public static String applyFormat(String raw, String format) {
        if (format == null || format.isEmpty() || raw.isEmpty()) {
            return raw;
        }
        switch (format.toLowerCase()) {
            case "currency":
                return formatNumber(raw, "#,##0");
            case "currency2":
                return formatNumber(raw, "#,##0.00");
            case "number":
                return formatNumber(raw, "#,##0");
            case "uppercase":
                return raw.toUpperCase();
            default:
                return raw;
        }
    }

    private static String formatNumber(String raw, String pattern) {
        try {
            double number = Double.parseDouble(raw);
            DecimalFormat df = new DecimalFormat(pattern, SYMBOLS);
            return df.format(number);
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    public JsonObject getJson() {
        return json;
    }
}
