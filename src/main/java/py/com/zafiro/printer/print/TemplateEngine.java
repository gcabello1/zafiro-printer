package py.com.zafiro.printer.print;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor de plantillas simple.
 *
 * Resuelve expresiones {variable} dentro de strings del config
 * reemplazándolas con los valores correspondientes del PrintData.
 *
 * Sintaxis de variable con formato: {campo}
 * El formato se define en el elemento del config con el campo "format".
 *
 * Ejemplo:
 *   template:  "Total: {total}"
 *   data:      { "total": 5375000 }
 *   resultado: "Total: 5.375.000"  (si format = "currency")
 */
public class TemplateEngine {

    /** Patrón que detecta {variable} en un string */
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    private TemplateEngine() {}

    /**
     * Resuelve todas las variables {campo} en el template usando los datos provistos.
     *
     * @param template string con posibles variables {campo}
     * @param data     datos de impresión
     * @param format   formato a aplicar a los valores (puede ser null)
     * @return string con variables reemplazadas
     */
    public static String resolve(String template, PrintData data, String format) {
        if (template == null || template.isEmpty()) return "";
        Matcher matcher = VAR_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = data.getValue(key, format);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Resuelve variables sin aplicar formato.
     */
    public static String resolve(String template, PrintData data) {
        return resolve(template, data, null);
    }

    /**
     * Ajusta un texto al ancho indicado:
     * - Si es más corto, lo rellena con espacios (alineado a izquierda por defecto).
     * - Si es más largo, lo trunca.
     *
     * @param text   texto a ajustar
     * @param width  ancho deseado en caracteres
     * @param align  "left", "right" o "center"
     * @return string de exactamente {@code width} caracteres
     */
    public static String pad(String text, int width, String align) {
        if (text == null) text = "";
        if (text.length() > width) {
            return text.substring(0, width);
        }
        int padding = width - text.length();
        switch (align == null ? "left" : align.toLowerCase()) {
            case "right":
                return spaces(padding) + text;
            case "center":
                int left  = padding / 2;
                int right = padding - left;
                return spaces(left) + text + spaces(right);
            case "left":
            default:
                return text + spaces(padding);
        }
    }

    /** Sobrecarga con alineación izquierda por defecto. */
    public static String pad(String text, int width) {
        return pad(text, width, "left");
    }

    /** Genera una línea de {@code width} repeticiones del carácter {@code ch}. */
    public static String separator(char ch, int width) {
        StringBuilder sb = new StringBuilder(width);
        for (int i = 0; i < width; i++) sb.append(ch);
        return sb.toString();
    }

    // ── Helper ─────────────────────────────────────────────────────────────

    private static String spaces(int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(' ');
        return sb.toString();
    }
}
