package py.com.zafiro.printer.print;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

/**
 * Mapea el JSON de configuración de impresión recibido desde el servidor.
 *
 * Estructura esperada:
 * {
 *   "type":    "thermal" | "matricial",
 *   "width":   48,
 *   "charset": "ISO-8859-1",
 *   "cut":     true,           (solo thermal)
 *   "beep":    false,          (solo thermal)
 *   "linesPerPage": 60,        (solo matricial)
 *   "cpi":     "10",           (solo matricial: 10/12/17)
 *   "quality": "nlq",          (solo matricial: draft/nlq)
 *   "header":  [ ... ],
 *   "detail":  [ ... ],
 *   "footer":  [ ... ]
 * }
 */
public class PrintConfig {

    public static final String TYPE_THERMAL   = "thermal";
    public static final String TYPE_MATRICIAL = "matricial";

    private final JsonObject json;

    public PrintConfig(JsonObject json) {
        this.json = json;
    }

    /** "thermal" o "matricial" */
    public String getType() {
        return json.getString("type", TYPE_THERMAL);
    }

    /** Ancho en columnas de caracteres (ej: 32, 48, 80, 132) */
    public int getWidth() {
        return json.getInt("width", 48);
    }

    /** Charset a usar (default ISO-8859-1) */
    public String getCharset() {
        return json.getString("charset", "ISO-8859-1");
    }

    // ── Opciones Térmica ────────────────────────────────────────────────────

    /** true = corte automático al finalizar (solo thermal) */
    public boolean isCut() {
        return json.getBoolean("cut", true);
    }

    /** true = emitir beep al finalizar (solo thermal) */
    public boolean isBeep() {
        return json.getBoolean("beep", false);
    }

    // ── Opciones Matricial ──────────────────────────────────────────────────

    /** Líneas por página (solo matricial, default 60) */
    public int getLinesPerPage() {
        return json.getInt("linesPerPage", 60);
    }

    /** Caracteres por pulgada: "10", "12", "17" (solo matricial) */
    public String getCpi() {
        return json.getString("cpi", "10");
    }

    /** Calidad de impresión: "draft" o "nlq" (solo matricial) */
    public String getQuality() {
        return json.getString("quality", "draft");
    }

    // ── Secciones ───────────────────────────────────────────────────────────

    public JsonArray getHeader() {
        return json.containsKey("header") ? json.getJsonArray("header") : null;
    }

    public JsonArray getDetail() {
        return json.containsKey("detail") ? json.getJsonArray("detail") : null;
    }

    public JsonArray getFooter() {
        return json.containsKey("footer") ? json.getJsonArray("footer") : null;
    }

    public boolean isThermal() {
        return TYPE_THERMAL.equalsIgnoreCase(getType());
    }

    public boolean isMatricial() {
        return TYPE_MATRICIAL.equalsIgnoreCase(getType());
    }

    public JsonObject getJson() {
        return json;
    }
}
