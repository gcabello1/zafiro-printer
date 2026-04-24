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
 * Genera bytes ESC/POS para impresoras térmicas EPSON.
 *
 * Soporta los siguientes elementos de plantilla:
 *   text       — texto con alineación, bold, italic, underline, size
 *   columns    — fila de columnas de ancho fijo
 *   rows       — itera un array del PrintData
 *   separator  — línea de caracteres repetidos
 *   newline    — líneas en blanco
 *   logo       — imagen NV de la impresora
 *   barcode    — código de barras (CODE128, EAN13)
 *   qr         — código QR
 *   cut        — corte de papel
 */
public class EscPosBuilder {

    private static final Logger log = LoggerFactory.getLogger(EscPosBuilder.class);

    // ── Comandos ESC/POS ────────────────────────────────────────────────────

    // Inicialización
    static final byte[] INIT             = {0x1B, 0x40};

    // Alineación
    static final byte[] ALIGN_LEFT       = {0x1B, 0x61, 0x00};
    static final byte[] ALIGN_CENTER     = {0x1B, 0x61, 0x01};
    static final byte[] ALIGN_RIGHT      = {0x1B, 0x61, 0x02};

    // Estilos
    static final byte[] BOLD_ON          = {0x1B, 0x45, 0x01};
    static final byte[] BOLD_OFF         = {0x1B, 0x45, 0x00};
    static final byte[] ITALIC_ON        = {0x1B, 0x34, 0x01};
    static final byte[] ITALIC_OFF       = {0x1B, 0x34, 0x00};
    static final byte[] UNDERLINE_ON     = {0x1B, 0x2D, 0x01};
    static final byte[] UNDERLINE_OFF    = {0x1B, 0x2D, 0x00};

    // Tamaño de texto
    static final byte[] SIZE_NORMAL      = {0x1D, 0x21, 0x00};
    static final byte[] SIZE_DOUBLE      = {0x1D, 0x21, 0x11}; // doble ancho + alto
    static final byte[] SIZE_TALL        = {0x1D, 0x21, 0x01}; // solo doble alto

    // Salto de línea y corte
    static final byte[] NEWLINE          = {0x0A};
    static final byte[] CUT_FULL         = {0x1D, 0x56, 0x00};
    static final byte[] CUT_PARTIAL      = {0x1D, 0x56, 0x01};

    // Logo NV (imagen bitmap almacenada en memoria NV)
    static final byte[] LOGO_PRINT       = {0x1C, 0x70, 0x01, 0x00};

    // Beep
    static final byte[] BEEP             = {0x1B, 0x42, 0x03, 0x02};

    // ── Constructor ─────────────────────────────────────────────────────────

    private final PrintConfig config;
    private final PrintData   data;
    private final Charset     charset;
    private final ByteArrayOutputStream buffer;

    public EscPosBuilder(PrintConfig config, PrintData data) {
        this.config  = config;
        this.data    = data;
        this.charset = Charset.forName(config.getCharset());
        this.buffer  = new ByteArrayOutputStream();
    }

    // ── API pública ─────────────────────────────────────────────────────────

    /**
     * Construye el documento completo y retorna los bytes ESC/POS.
     */
    public byte[] build() {
        write(INIT);
        renderSection(config.getHeader());
        renderSection(config.getDetail());
        renderSection(config.getFooter());
        if (config.isBeep()) write(BEEP);
        if (config.isCut())  write(CUT_FULL);
        return buffer.toByteArray();
    }

    // ── Renderizado de secciones ─────────────────────────────────────────────

    private void renderSection(JsonArray section) {
        if (section == null) return;
        for (int i = 0; i < section.size(); i++) {
            JsonObject element = section.getJsonObject(i);
            renderElement(element);
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
            case "logo":      renderLogo();        break;
            case "barcode":   renderBarcode(el);   break;
            case "qr":        renderQr(el);        break;
            case "cut":       renderCut(el);       break;
            default:
                log.warn("Elemento ESC/POS desconocido: {}", type);
        }
    }

    // ── Elemento: text ───────────────────────────────────────────────────────

    private void renderText(JsonObject el) {
        String value  = el.getString("value", "");
        String align  = el.getString("align", "left");
        String size   = el.getString("size", "normal");
        String format = el.containsKey("format") ? el.getString("format") : null;
        boolean bold      = el.getBoolean("bold",      false);
        boolean italic    = el.getBoolean("italic",    false);
        boolean underline = el.getBoolean("underline", false);

        String resolved = TemplateEngine.resolve(value, data, format);

        applyAlign(align);
        applySize(size);
        if (bold)      write(BOLD_ON);
        if (italic)    write(ITALIC_ON);
        if (underline) write(UNDERLINE_ON);

        writeText(resolved);
        write(NEWLINE);

        // Restaurar estilos
        if (bold)      write(BOLD_OFF);
        if (italic)    write(ITALIC_OFF);
        if (underline) write(UNDERLINE_OFF);
        if (!size.equals("normal")) write(SIZE_NORMAL);
        applyAlign("left");
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
            JsonObject col  = items.getJsonObject(i);
            String value    = col.getString("value", "");
            int    width    = col.getInt("width", 10);
            String align    = col.getString("align", "left");
            String colFmt   = col.containsKey("format") ? col.getString("format") : format;
            boolean colBold = col.getBoolean("bold", false);

            String resolved = TemplateEngine.resolve(value, data, colFmt);
            String padded   = TemplateEngine.pad(resolved, width, align);
            line.append(padded);
        }

        applyAlign("left");
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
            JsonObject row  = rows.getJsonObject(r);
            StringBuilder line = new StringBuilder();
            for (int c = 0; c < columns.size(); c++) {
                JsonObject col  = columns.getJsonObject(c);
                String field    = col.getString("field", "");
                int    width    = col.getInt("width", 10);
                String align    = col.getString("align", "left");
                String fmt      = col.containsKey("format") ? col.getString("format") : null;

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

        applyAlign("left");
        writeText(TemplateEngine.separator(ch, width));
        write(NEWLINE);
    }

    // ── Elemento: newline ────────────────────────────────────────────────────

    private void renderNewline(JsonObject el) {
        int count = el.getInt("count", 1);
        for (int i = 0; i < count; i++) write(NEWLINE);
    }

    // ── Elemento: logo ───────────────────────────────────────────────────────

    private void renderLogo() {
        applyAlign("center");
        write(LOGO_PRINT);
        write(NEWLINE);
        applyAlign("left");
    }

    // ── Elemento: barcode ────────────────────────────────────────────────────

    private void renderBarcode(JsonObject el) {
        String field    = el.getString("field", "");
        String format   = el.getString("format", "CODE128");
        int    height   = el.getInt("height", 60);
        String align    = el.getString("align", "center");
        String value    = data.getValue(field);

        if (value.isEmpty()) {
            log.warn("Campo '{}' vacío para barcode.", field);
            return;
        }

        applyAlign(align);

        // Altura del barcode (GS h n)
        write(new byte[]{0x1D, 0x68, (byte) height});

        // HRI (texto legible) debajo (GS H 2)
        write(new byte[]{0x1D, 0x48, 0x02});

        // Seleccionar tipo de barcode (GS k m d1..dk NUL)
        if ("EAN13".equalsIgnoreCase(format)) {
            // EAN-13: GS k 2 (12 dígitos + check)
            write(new byte[]{0x1D, 0x6B, 0x02});
            writeText(value);
            write(new byte[]{0x00});
        } else {
            // CODE128 por defecto: GS k 73 n d1..dn
            byte[] data128 = buildCode128Data(value);
            write(new byte[]{0x1D, 0x6B, 0x49, (byte) data128.length});
            write(data128);
        }

        write(NEWLINE);
        applyAlign("left");
    }

    /** Construye el payload CODE128 con subconjunto B (caracteres ASCII). */
    private byte[] buildCode128Data(String value) {
        // {B → subconjunto B → bytes: 123, 66, datos
        byte[] prefix = {123, 66};
        byte[] text   = value.getBytes(charset);
        byte[] result = new byte[prefix.length + text.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(text, 0, result, prefix.length, text.length);
        return result;
    }

    // ── Elemento: qr ────────────────────────────────────────────────────────

    private void renderQr(JsonObject el) {
        String field = el.getString("field", "");
        int    size  = el.getInt("size", 4);    // módulo QR (1-8)
        String align = el.getString("align", "center");
        String value = data.getValue(field);

        if (value.isEmpty()) {
            log.warn("Campo '{}' vacío para QR.", field);
            return;
        }

        applyAlign(align);
        byte[] qrData = value.getBytes(charset);
        int    len    = qrData.length + 3;
        byte   pL     = (byte)(len & 0xFF);
        byte   pH     = (byte)((len >> 8) & 0xFF);

        // Seleccionar modelo QR (GS ( k 4 0 49 65 50 0)
        write(new byte[]{0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00});
        // Tamaño de módulo (GS ( k 3 0 49 67 n)
        write(new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, (byte) size});
        // Nivel de corrección L (GS ( k 3 0 49 69 48)
        write(new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x30});
        // Almacenar datos (GS ( k pL pH 49 80 48 data)
        write(new byte[]{0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30});
        write(qrData);
        // Imprimir (GS ( k 3 0 49 81 48)
        write(new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30});

        write(NEWLINE);
        applyAlign("left");
    }

    // ── Elemento: cut ────────────────────────────────────────────────────────

    private void renderCut(JsonObject el) {
        boolean partial = el.getBoolean("partial", false);
        write(partial ? CUT_PARTIAL : CUT_FULL);
    }

    // ── Helpers de escritura ─────────────────────────────────────────────────

    private void applyAlign(String align) {
        if (align == null) align = "left";
        switch (align.toLowerCase()) {
            case "center": write(ALIGN_CENTER); break;
            case "right":  write(ALIGN_RIGHT);  break;
            default:       write(ALIGN_LEFT);   break;
        }
    }

    private void applySize(String size) {
        if (size == null) size = "normal";
        switch (size.toLowerCase()) {
            case "double": write(SIZE_DOUBLE); break;
            case "tall":   write(SIZE_TALL);   break;
            default:       write(SIZE_NORMAL); break;
        }
    }

    private void write(byte[] bytes) {
        try {
            buffer.write(bytes);
        } catch (IOException e) {
            log.error("Error escribiendo al buffer ESC/POS: {}", e.getMessage());
        }
    }

    private void writeText(String text) {
        try {
            buffer.write(text.getBytes(charset));
        } catch (IOException e) {
            log.error("Error escribiendo texto al buffer ESC/POS: {}", e.getMessage());
        }
    }
}
