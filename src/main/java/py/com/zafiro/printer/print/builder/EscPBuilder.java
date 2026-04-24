package py.com.zafiro.printer.print.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.com.zafiro.printer.print.PrintConfig;
import py.com.zafiro.printer.print.PrintData;
import py.com.zafiro.printer.print.TemplateEngine;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Genera bytes ESC/P para impresoras matriciales (Epson LX-350, FX, etc.).
 *
 * Soporta los siguientes elementos de plantilla:
 *   text       — texto con alineación, bold, italic, underline
 *   columns    — fila de columnas de ancho fijo
 *   rows       — itera un array del PrintData
 *   separator  — línea de caracteres repetidos
 *   newline    — líneas en blanco
 *   formfeed   — avance de página
 *   hline      — línea horizontal
 */
public class EscPBuilder {

    private static final Logger log = LoggerFactory.getLogger(EscPBuilder.class);

    // ── Comandos ESC/P ───────────────────────────────────────────────────────

    // Inicialización
    static final byte[] INIT               = {0x1B, 0x40};

    // Calidad de impresión
    static final byte[] QUALITY_DRAFT      = {0x1B, 0x78, 0x00};
    static final byte[] QUALITY_NLQ        = {0x1B, 0x78, 0x01};

    // CPI (caracteres por pulgada)
    static final byte[] CPI_10             = {0x1B, 0x50};  // 10 CPI (pica)
    static final byte[] CPI_12             = {0x1B, 0x4D};  // 12 CPI (elite)
    static final byte[] CPI_17            = {0x1B, 0x67};  // 17 CPI (condensado)

    // Estilos
    static final byte[] BOLD_ON            = {0x1B, 0x45};
    static final byte[] BOLD_OFF           = {0x1B, 0x46};
    static final byte[] ITALIC_ON          = {0x1B, 0x34};
    static final byte[] ITALIC_OFF         = {0x1B, 0x35};
    static final byte[] UNDERLINE_ON       = {0x1B, 0x2D, 0x01};
    static final byte[] UNDERLINE_OFF      = {0x1B, 0x2D, 0x00};
    static final byte[] DOUBLE_WIDTH_ON    = {0x1B, 0x57, 0x01};
    static final byte[] DOUBLE_WIDTH_OFF   = {0x1B, 0x57, 0x00};

    // Salto de línea y página
    static final byte[] NEWLINE            = {0x0D, 0x0A};
    static final byte[] FORMFEED           = {0x0C};

    // ── Constructor ─────────────────────────────────────────────────────────

    private final PrintConfig config;
    private final PrintData   data;
    private final Charset     charset;
    private final ByteArrayOutputStream buffer;

    public EscPBuilder(PrintConfig config, PrintData data) {
        this.config  = config;
        this.data    = data;
        this.charset = Charset.forName(config.getCharset());
        this.buffer  = new ByteArrayOutputStream();
    }

    // ── API pública ─────────────────────────────────────────────────────────

    /**
     * Construye el documento completo y retorna los bytes ESC/P.
     */
    public byte[] build() {
        write(INIT);
        applyQuality(config.getQuality());
        applyCpi(config.getCpi());

        renderSection(config.getHeader());
        renderSection(config.getDetail());
        renderSection(config.getFooter());

        return buffer.toByteArray();
    }

    // ── Renderizado de secciones ─────────────────────────────────────────────

    private void renderSection(JsonArray section) {
        if (section == null) return;
        for (int i = 0; i < section.size(); i++) {
            renderElement(section.getJsonObject(i));
        }
    }

    private void renderElement(JsonObject el) {
        String type = el.getString("type", "");
        switch (type.toLowerCase()) {
            case "text":      renderText(el);      break;
            case "columns":   renderColumns(el);   break;
            case "rows":      renderRows(el);       break;
            case "separator": renderSeparator(el); break;
            case "newline":   renderNewline(el);   break;
            case "formfeed":  renderFormfeed();    break;
            case "hline":     renderHline(el);     break;
            default:
                log.warn("Elemento ESC/P desconocido o no soportado en matricial: {}", type);
        }
    }

    // ── Elemento: text ───────────────────────────────────────────────────────

    private void renderText(JsonObject el) {
        String value      = el.getString("value", "");
        String align      = el.getString("align", "left");
        String format     = el.containsKey("format") ? el.getString("format") : null;
        boolean bold      = el.getBoolean("bold",      false);
        boolean italic    = el.getBoolean("italic",    false);
        boolean underline = el.getBoolean("underline", false);
        boolean dblWidth  = "double".equalsIgnoreCase(el.getString("size", ""));

        String resolved = TemplateEngine.resolve(value, data, format);
        String aligned  = applyTextAlign(resolved, align, config.getWidth());

        if (bold)      write(BOLD_ON);
        if (italic)    write(ITALIC_ON);
        if (underline) write(UNDERLINE_ON);
        if (dblWidth)  write(DOUBLE_WIDTH_ON);

        writeText(aligned);
        write(NEWLINE);

        if (bold)      write(BOLD_OFF);
        if (italic)    write(ITALIC_OFF);
        if (underline) write(UNDERLINE_OFF);
        if (dblWidth)  write(DOUBLE_WIDTH_OFF);
    }

    // ── Elemento: columns ────────────────────────────────────────────────────

    private void renderColumns(JsonObject el) {
        JsonArray items = el.getJsonArray("items");
        if (items == null) return;

        boolean bold      = el.getBoolean("bold",      false);
        boolean underline = el.getBoolean("underline", false);
        String  format    = el.containsKey("format") ? el.getString("format") : null;

        if (bold)      write(BOLD_ON);
        if (underline) write(UNDERLINE_ON);

        StringBuilder line = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            JsonObject col = items.getJsonObject(i);
            String value   = col.getString("value", "");
            int    width   = col.getInt("width", 10);
            String align   = col.getString("align", "left");
            String colFmt  = col.containsKey("format") ? col.getString("format") : format;
            boolean cBold  = col.getBoolean("bold", false);

            String resolved = TemplateEngine.resolve(value, data, colFmt);
            String padded   = TemplateEngine.pad(resolved, width, align);
            line.append(padded);
        }

        writeText(line.toString());
        write(NEWLINE);

        if (bold)      write(BOLD_OFF);
        if (underline) write(UNDERLINE_OFF);
    }

    // ── Elemento: rows ───────────────────────────────────────────────────────

    private void renderRows(JsonObject el) {
        String    source  = el.getString("source", "items");
        JsonArray columns = el.getJsonArray("columns");
        if (columns == null) return;

        JsonArray rows = data.getArray(source);
        if (rows == null) {
            log.warn("Array '{}' no encontrado en PrintData.", source);
            return;
        }

        for (int r = 0; r < rows.size(); r++) {
            JsonObject row = rows.getJsonObject(r);
            StringBuilder line = new StringBuilder();
            for (int c = 0; c < columns.size(); c++) {
                JsonObject col = columns.getJsonObject(c);
                String field   = col.getString("field", "");
                int    width   = col.getInt("width", 10);
                String align   = col.getString("align", "left");
                String fmt     = col.containsKey("format") ? col.getString("format") : null;

                String value  = PrintData.getValueFromRow(row, field, fmt);
                String padded = TemplateEngine.pad(value, width, align);
                line.append(padded);
            }
            writeText(line.toString());
            write(NEWLINE);
        }
    }

    // ── Elemento: separator ──────────────────────────────────────────────────

    private void renderSeparator(JsonObject el) {
        String charStr = el.getString("char", "-");
        int    width   = el.getInt("width", config.getWidth());
        char   ch      = charStr.isEmpty() ? '-' : charStr.charAt(0);

        writeText(TemplateEngine.separator(ch, width));
        write(NEWLINE);
    }

    // ── Elemento: newline ────────────────────────────────────────────────────

    private void renderNewline(JsonObject el) {
        int count = el.getInt("count", 1);
        for (int i = 0; i < count; i++) write(NEWLINE);
    }

    // ── Elemento: formfeed ───────────────────────────────────────────────────

    private void renderFormfeed() {
        write(FORMFEED);
    }

    // ── Elemento: hline ──────────────────────────────────────────────────────

    private void renderHline(JsonObject el) {
        int width = el.getInt("width", config.getWidth());
        writeText(TemplateEngine.separator('-', width));
        write(NEWLINE);
    }

    // ── Helpers de configuración ─────────────────────────────────────────────

    private void applyQuality(String quality) {
        if ("nlq".equalsIgnoreCase(quality)) {
            write(QUALITY_NLQ);
        } else {
            write(QUALITY_DRAFT);
        }
    }

    private void applyCpi(String cpi) {
        switch (cpi) {
            case "12": write(CPI_12); break;
            case "17": write(CPI_17); break;
            default:   write(CPI_10); break;
        }
    }

    /**
     * Aplica alineación de texto para impresoras matriciales (que no tienen
     * comandos de alineación nativos): centra o alinea a derecha con espacios.
     */
    private String applyTextAlign(String text, String align, int width) {
        if (text == null) text = "";
        return TemplateEngine.pad(text, width, align);
    }

    // ── Helpers de escritura ─────────────────────────────────────────────────

    private void write(byte[] bytes) {
        try {
            buffer.write(bytes);
        } catch (IOException e) {
            log.error("Error escribiendo al buffer ESC/P: {}", e.getMessage());
        }
    }

    private void writeText(String text) {
        try {
            buffer.write(text.getBytes(charset));
        } catch (IOException e) {
            log.error("Error escribiendo texto al buffer ESC/P: {}", e.getMessage());
        }
    }
}
