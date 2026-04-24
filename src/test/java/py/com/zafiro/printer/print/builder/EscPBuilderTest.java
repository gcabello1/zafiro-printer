package py.com.zafiro.printer.print.builder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import py.com.zafiro.printer.print.PrintConfig;
import py.com.zafiro.printer.print.PrintData;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para EscPBuilder (impresoras matriciales ESC/P).
 *
 * @author gcabello on 21/04/2026.
 */
@DisplayName("EscPBuilder")
class EscPBuilderTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static final byte ESC  = 0x1B;
    private static final byte CR   = 0x0D;
    private static final byte LF   = 0x0A;
    private static final byte FF   = 0x0C;  // Form Feed

    private PrintConfig buildConfig(JsonArrayBuilder header,
                                    JsonArrayBuilder detail,
                                    JsonArrayBuilder footer,
                                    String cpi,
                                    String quality) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("type",         "matricial")
                .add("width",        80)
                .add("charset",      "ISO-8859-1")
                .add("linesPerPage", 60)
                .add("cpi",     cpi     != null ? cpi     : "10")
                .add("quality", quality != null ? quality : "draft");
        if (header != null) b.add("header", header);
        if (detail != null) b.add("detail", detail);
        if (footer != null) b.add("footer", footer);
        return new PrintConfig(b.build());
    }

    private PrintData emptyData() {
        return new PrintData(Json.createObjectBuilder().build());
    }

    private boolean containsSequence(byte[] bytes, byte[] seq) {
        outer:
        for (int i = 0; i <= bytes.length - seq.length; i++) {
            for (int j = 0; j < seq.length; j++) {
                if (bytes[i + j] != seq[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    private String bytesToText(byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    // ── Inicialización ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Inicialización")
    class InitTests {

        @Test
        @DisplayName("los primeros 2 bytes son ESC @ (INIT)")
        void firstBytesAreInit() {
            PrintConfig config = buildConfig(null, null, null, "10", "draft");
            byte[] result = new EscPBuilder(config, emptyData()).build();

            assertNotNull(result);
            assertTrue(result.length >= 2);
            assertEquals(ESC,  result[0]);
            assertEquals(0x40, result[1]);
        }

        @Test
        @DisplayName("calidad 'draft' incluye ESC x 0")
        void draftQualityIncludesEscX0() {
            PrintConfig config = buildConfig(null, null, null, "10", "draft");
            byte[] result = new EscPBuilder(config, emptyData()).build();

            assertTrue(containsSequence(result, new byte[]{ESC, 0x78, 0x00}),
                    "Debe contener ESC x 0 (calidad draft)");
        }

        @Test
        @DisplayName("calidad 'nlq' incluye ESC x 1")
        void nlqQualityIncludesEscX1() {
            PrintConfig config = buildConfig(null, null, null, "10", "nlq");
            byte[] result = new EscPBuilder(config, emptyData()).build();

            assertTrue(containsSequence(result, new byte[]{ESC, 0x78, 0x01}),
                    "Debe contener ESC x 1 (calidad NLQ)");
        }

        @Test
        @DisplayName("CPI 10 incluye ESC P")
        void cpi10IncludesEscP() {
            PrintConfig config = buildConfig(null, null, null, "10", "draft");
            byte[] result = new EscPBuilder(config, emptyData()).build();

            assertTrue(containsSequence(result, new byte[]{ESC, 0x50}),
                    "Debe contener ESC P (10 CPI)");
        }

        @Test
        @DisplayName("CPI 12 incluye ESC M")
        void cpi12IncludesEscM() {
            PrintConfig config = buildConfig(null, null, null, "12", "draft");
            byte[] result = new EscPBuilder(config, emptyData()).build();

            assertTrue(containsSequence(result, new byte[]{ESC, 0x4D}),
                    "Debe contener ESC M (12 CPI)");
        }

        @Test
        @DisplayName("CPI 17 incluye ESC g")
        void cpi17IncludesEscG() {
            PrintConfig config = buildConfig(null, null, null, "17", "draft");
            byte[] result = new EscPBuilder(config, emptyData()).build();

            assertTrue(containsSequence(result, new byte[]{ESC, 0x67}),
                    "Debe contener ESC g (17 CPI)");
        }
    }

    // ── Elemento: text ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Elemento text")
    class TextTests {

        @Test
        @DisplayName("genera el texto literal en los bytes")
        void generatesLiteralText() {
            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type",  "text")
                            .add("value", "Texto de prueba")
                            .build());
            PrintConfig config = buildConfig(header, null, null, "10", "draft");
            byte[] result = new EscPBuilder(config, emptyData()).build();

            assertTrue(containsSequence(result,
                    "Texto de prueba".getBytes(StandardCharsets.ISO_8859_1)));
        }

        @Test
        @DisplayName("resuelve variable {campo} desde PrintData")
        void resolvesVariable() {
            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type",  "text")
                            .add("value", "{cliente}")
                            .build());
            PrintConfig config = buildConfig(header, null, null, "10", "draft");
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .add("cliente", "Juan Pérez")
                    .build());

            byte[] result = new EscPBuilder(config, data).build();
            assertTrue(containsSequence(result,
                    "Juan P".getBytes(StandardCharsets.ISO_8859_1)));
        }

        @Test
        @DisplayName("bold:true incluye ESC E antes del texto")
        void boldOnIncludesEscE() {
            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type",  "text")
                            .add("value", "negrita")
                            .add("bold",  true)
                            .build());
            PrintConfig config = buildConfig(header, null, null, "10", "draft");
            byte[] result = new EscPBuilder(config, emptyData()).build();

            assertTrue(containsSequence(result, new byte[]{ESC, 0x45}),
                    "Debe contener ESC E (BOLD ON)");
            assertTrue(containsSequence(result, new byte[]{ESC, 0x46}),
                    "Debe contener ESC F (BOLD OFF)");
        }

        @Test
        @DisplayName("underline:true incluye ESC - 1 antes del texto")
        void underlineOnIncludesEscMinus1() {
            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type",      "text")
                            .add("value",     "subrayado")
                            .add("underline", true)
                            .build());
            PrintConfig config = buildConfig(header, null, null, "10", "draft");
            byte[] result = new EscPBuilder(config, emptyData()).build();

            assertTrue(containsSequence(result, new byte[]{ESC, 0x2D, 0x01}),
                    "Debe contener ESC - 1 (UNDERLINE ON)");
        }

        @Test
        @DisplayName("cada elemento text termina con CR LF")
        void textEndsWithCrLf() {
            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type",  "text")
                            .add("value", "test")
                            .build());
            PrintConfig config = buildConfig(header, null, null, "10", "draft");
            byte[] result = new EscPBuilder(config, emptyData()).build();

            assertTrue(containsSequence(result, new byte[]{CR, LF}),
                    "Debe contener CR LF como fin de línea");
        }
    }

    // ── Elemento: separator ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Elemento separator")
    class SeparatorTests {

        @Test
        @DisplayName("genera línea con el carácter '-' por defecto del ancho del config (80)")
        void generatesDefaultSeparator() {
            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type", "separator")
                            .build());
            PrintConfig config = buildConfig(header, null, null, "10", "draft");
            byte[] result = new EscPBuilder(config, emptyData()).build();

            // Buscar 10 guiones consecutivos como mínimo
            assertTrue(containsSequence(result,
                    "----------".getBytes(StandardCharsets.ISO_8859_1)),
                    "Debe contener al menos 10 guiones consecutivos");
        }

        @Test
        @DisplayName("usa el carácter '=' cuando se configura")
        void usesEqualsChar() {
            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type", "separator")
                            .add("char", "=")
                            .build());
            PrintConfig config = buildConfig(header, null, null, "10", "draft");
            byte[] result = new EscPBuilder(config, emptyData()).build();

            assertTrue(containsSequence(result,
                    "========".getBytes(StandardCharsets.ISO_8859_1)),
                    "Debe contener caracteres '='");
        }
    }

    // ── Elemento: newline ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Elemento newline")
    class NewlineTests {

        @Test
        @DisplayName("count:2 agrega 2 pares CR LF adicionales")
        void count2AddsTwoCrLf() {
            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type",  "newline")
                            .add("count", 2)
                            .build());
            PrintConfig config  = buildConfig(header, null, null, "10", "draft");
            PrintConfig noLines = buildConfig(null,   null, null, "10", "draft");

            byte[] withNewlines = new EscPBuilder(config,  emptyData()).build();
            byte[] without      = new EscPBuilder(noLines, emptyData()).build();

            long crLfWith    = countCrLf(withNewlines);
            long crLfWithout = countCrLf(without);
            assertEquals(crLfWithout + 2, crLfWith,
                    "Debe haber exactamente 2 CR LF adicionales");
        }

        private long countCrLf(byte[] bytes) {
            long count = 0;
            for (int i = 0; i < bytes.length - 1; i++) {
                if (bytes[i] == CR && bytes[i + 1] == LF) count++;
            }
            return count;
        }
    }

    // ── Elemento: formfeed ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Elemento formfeed")
    class FormfeedTests {

        @Test
        @DisplayName("genera el byte 0x0C (Form Feed)")
        void generatesFormFeedByte() {
            JsonArrayBuilder footer = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type", "formfeed")
                            .build());
            PrintConfig config = buildConfig(null, null, footer, "10", "draft");
            byte[] result = new EscPBuilder(config, emptyData()).build();

            assertTrue(containsSequence(result, new byte[]{FF}),
                    "Debe contener 0x0C (Form Feed)");
        }
    }

    // ── Elemento: rows ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Elemento rows")
    class RowsTests {

        @Test
        @DisplayName("genera una fila por cada item del array")
        void generatesOneRowPerItem() {
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .add("items", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add("codigo",      "A001")
                                    .add("descripcion", "Artículo Uno")
                                    .add("cantidad",    5)
                                    .build())
                            .add(Json.createObjectBuilder()
                                    .add("codigo",      "B002")
                                    .add("descripcion", "Artículo Dos")
                                    .add("cantidad",    3)
                                    .build())
                            .build())
                    .build());

            JsonArrayBuilder detail = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type",   "rows")
                            .add("source", "items")
                            .add("columns", Json.createArrayBuilder()
                                    .add(Json.createObjectBuilder()
                                            .add("field", "codigo")
                                            .add("width", 8)
                                            .build())
                                    .add(Json.createObjectBuilder()
                                            .add("field", "descripcion")
                                            .add("width", 40)
                                            .build())
                                    .add(Json.createObjectBuilder()
                                            .add("field", "cantidad")
                                            .add("width", 8)
                                            .add("align", "right")
                                            .build())
                                    .build())
                            .build());

            PrintConfig config = buildConfig(null, detail, null, "10", "draft");
            byte[] result = new EscPBuilder(config, data).build();

            assertTrue(containsSequence(result, "A001".getBytes(StandardCharsets.ISO_8859_1)),
                    "Debe contener código 'A001'");
            assertTrue(containsSequence(result, "B002".getBytes(StandardCharsets.ISO_8859_1)),
                    "Debe contener código 'B002'");
        }
    }

    // ── Flujo completo ────────────────────────────────────────────────────────

    @Test
    @DisplayName("header + detail + footer aparecen en el orden correcto")
    void sectionsInOrder() {
        JsonArrayBuilder header = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("type", "text").add("value", "ENCABEZADO").build());
        JsonArrayBuilder detail = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("type", "text").add("value", "CUERPO").build());
        JsonArrayBuilder footer = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("type", "text").add("value", "FIRMA").build());

        PrintConfig config = buildConfig(header, detail, footer, "10", "draft");
        byte[] result = new EscPBuilder(config, emptyData()).build();
        String text = bytesToText(result);

        int posH = text.indexOf("ENCABEZADO");
        int posD = text.indexOf("CUERPO");
        int posF = text.indexOf("FIRMA");

        assertTrue(posH >= 0, "Debe contener 'ENCABEZADO'");
        assertTrue(posD >= 0, "Debe contener 'CUERPO'");
        assertTrue(posF >= 0, "Debe contener 'FIRMA'");
        assertTrue(posH < posD, "ENCABEZADO antes que CUERPO");
        assertTrue(posD < posF, "CUERPO antes que FIRMA");
    }
}
