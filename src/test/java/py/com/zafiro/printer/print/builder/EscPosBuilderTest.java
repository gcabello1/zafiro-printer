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
 * Tests unitarios para EscPosBuilder.
 * Verifica que los bytes ESC/POS generados sean correctos para cada elemento.
 *
 * @author gcabello on 21/04/2026.
 */
@DisplayName("EscPosBuilder")
class EscPosBuilderTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static final byte ESC = 0x1B;
    private static final byte GS  = 0x1D;
    private static final byte LF  = 0x0A;

    private PrintConfig buildConfig(JsonArrayBuilder header,
                                    JsonArrayBuilder detail,
                                    JsonArrayBuilder footer,
                                    boolean cut) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("type",    "thermal")
                .add("width",   40)
                .add("charset", "ISO-8859-1")
                .add("cut",     cut)
                .add("beep",    false);
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
        @DisplayName("los primeros 2 bytes son ESC @ (comando INIT)")
        void firstBytesAreInit() {
            PrintConfig config = buildConfig(null, null, null, false);
            byte[] result = new EscPosBuilder(config, emptyData()).build();

            assertNotNull(result);
            assertTrue(result.length >= 2);
            assertEquals(ESC,  result[0]);
            assertEquals(0x40, result[1]);
        }

        @Test
        @DisplayName("build() sin secciones retorna solo INIT (sin corte)")
        void emptyConfigReturnsOnlyInit() {
            PrintConfig config = buildConfig(null, null, null, false);
            byte[] result = new EscPosBuilder(config, emptyData()).build();
            assertEquals(2, result.length, "Solo 2 bytes de INIT");
        }
    }

    // ── Elemento: text ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Elemento text")
    class TextElementTests {

        @Test
        @DisplayName("genera el texto literal en los bytes de salida")
        void generatesLiteralText() {
            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type",  "text")
                            .add("value", "Hola Mundo")
                            .build());

            PrintConfig config = buildConfig(header, null, null, false);
            byte[] result = new EscPosBuilder(config, emptyData()).build();

            assertTrue(containsSequence(result, "Hola Mundo".getBytes(StandardCharsets.ISO_8859_1)),
                    "El texto 'Hola Mundo' debe aparecer en los bytes");
        }

        @Test
        @DisplayName("resuelve variable {campo} con el valor del data")
        void resolvesVariableFromData() {
            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type",  "text")
                            .add("value", "{empresa}")
                            .build());
            PrintConfig config = buildConfig(header, null, null, false);
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .add("empresa", "Zafiro S.A.")
                    .build());

            byte[] result = new EscPosBuilder(config, data).build();
            assertTrue(containsSequence(result, "Zafiro S.A.".getBytes(StandardCharsets.ISO_8859_1)));
        }

        @Test
        @DisplayName("bold:true incluye los bytes ESC E 1 antes del texto")
        void boldOnIncludesEscE1() {
            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type",  "text")
                            .add("value", "negrita")
                            .add("bold",  true)
                            .build());
            PrintConfig config = buildConfig(header, null, null, false);
            byte[] result = new EscPosBuilder(config, emptyData()).build();

            // ESC E 1 = BOLD ON
            assertTrue(containsSequence(result, new byte[]{ESC, 0x45, 0x01}),
                    "Debe contener ESC E 1 (BOLD ON)");
            // ESC E 0 = BOLD OFF
            assertTrue(containsSequence(result, new byte[]{ESC, 0x45, 0x00}),
                    "Debe contener ESC E 0 (BOLD OFF)");
        }

        @Test
        @DisplayName("underline:true incluye los bytes ESC - 1 antes del texto")
        void underlineOnIncludesEscMinus1() {
            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type",      "text")
                            .add("value",     "subrayado")
                            .add("underline", true)
                            .build());
            PrintConfig config = buildConfig(header, null, null, false);
            byte[] result = new EscPosBuilder(config, emptyData()).build();

            // ESC - 1 = UNDERLINE ON
            assertTrue(containsSequence(result, new byte[]{ESC, 0x2D, 0x01}),
                    "Debe contener ESC - 1 (UNDERLINE ON)");
        }

        @Test
        @DisplayName("align:center incluye ESC a 1")
        void alignCenterIncludesEscA1() {
            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type",  "text")
                            .add("value", "centrado")
                            .add("align", "center")
                            .build());
            PrintConfig config = buildConfig(header, null, null, false);
            byte[] result = new EscPosBuilder(config, emptyData()).build();

            // ESC a 1 = ALIGN CENTER
            assertTrue(containsSequence(result, new byte[]{ESC, 0x61, 0x01}),
                    "Debe contener ESC a 1 (ALIGN CENTER)");
        }

        @Test
        @DisplayName("cada elemento text termina con LF (0x0A)")
        void textEndsWithLineFeed() {
            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type",  "text")
                            .add("value", "test")
                            .build());
            PrintConfig config = buildConfig(header, null, null, false);
            byte[] result = new EscPosBuilder(config, emptyData()).build();

            assertTrue(containsSequence(result, new byte[]{LF}),
                    "Debe haber al menos un LF en los bytes");
        }
    }

    // ── Elemento: separator ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Elemento separator")
    class SeparatorTests {

        @Test
        @DisplayName("genera línea de guiones con el ancho del config")
        void generatesDashLine() {
            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type", "separator")
                            .build());
            PrintConfig config = buildConfig(header, null, null, false);  // width=40
            byte[] result = new EscPosBuilder(config, emptyData()).build();

            String text = bytesToText(result);
            assertTrue(text.contains("----------------------------------------"),
                    "Debe contener 40 guiones");
        }

        @Test
        @DisplayName("usa el carácter '=' cuando se configura char:=")
        void usesEqualsChar() {
            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type", "separator")
                            .add("char", "=")
                            .build());
            PrintConfig config = buildConfig(header, null, null, false);
            byte[] result = new EscPosBuilder(config, emptyData()).build();

            assertTrue(containsSequence(result, "========".getBytes(StandardCharsets.ISO_8859_1)),
                    "Debe contener caracteres '='");
        }
    }

    // ── Elemento: newline ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Elemento newline")
    class NewlineTests {

        @Test
        @DisplayName("count:3 agrega exactamente 3 bytes LF adicionales")
        void count3AddsThreeLFs() {
            // Config solo con INIT (sin secciones) para contar LF de base
            PrintConfig baseConfig  = buildConfig(null, null, null, false);
            byte[] baseResult = new EscPosBuilder(baseConfig, emptyData()).build();
            long baseLFs = countByte(baseResult, LF);

            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type",  "newline")
                            .add("count", 3)
                            .build());
            PrintConfig config = buildConfig(header, null, null, false);
            byte[] result = new EscPosBuilder(config, emptyData()).build();

            long totalLFs = countByte(result, LF);
            assertEquals(baseLFs + 3, totalLFs, "Debe haber exactamente 3 LF más que la base");
        }

        private long countByte(byte[] bytes, byte b) {
            long count = 0;
            for (byte x : bytes) if (x == b) count++;
            return count;
        }
    }

    // ── Elemento: columns ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Elemento columns")
    class ColumnsTests {

        @Test
        @DisplayName("genera los textos de cada columna en los bytes")
        void generatesEachColumnText() {
            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type", "columns")
                            .add("items", Json.createArrayBuilder()
                                    .add(Json.createObjectBuilder()
                                            .add("value", "Izquierda")
                                            .add("width", 20)
                                            .build())
                                    .add(Json.createObjectBuilder()
                                            .add("value", "Derecha")
                                            .add("width", 20)
                                            .add("align", "right")
                                            .build())
                                    .build())
                            .build());
            PrintConfig config = buildConfig(header, null, null, false);
            byte[] result = new EscPosBuilder(config, emptyData()).build();

            assertTrue(containsSequence(result, "Izquierda".getBytes(StandardCharsets.ISO_8859_1)));
            assertTrue(containsSequence(result, "Derecha".getBytes(StandardCharsets.ISO_8859_1)));
        }

        @Test
        @DisplayName("la fila de columnas tiene exactamente width total de caracteres + LF")
        void rowHasCorrectTotalWidth() {
            // width=40, dos columnas de 20 cada una
            JsonArrayBuilder header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type", "columns")
                            .add("items", Json.createArrayBuilder()
                                    .add(Json.createObjectBuilder().add("value", "A").add("width", 20).build())
                                    .add(Json.createObjectBuilder().add("value", "B").add("width", 20).build())
                                    .build())
                            .build());
            PrintConfig config = buildConfig(header, null, null, false);
            byte[] result = new EscPosBuilder(config, emptyData()).build();

            // Buscar la línea: 40 chars + LF
            // Los bytes de INIT son los 2 primeros, la línea de columns empieza después
            // Buscamos una secuencia de 40 chars seguida de LF
            boolean found = false;
            for (int i = 0; i <= result.length - 41; i++) {
                if (result[i + 40] == LF) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Debe existir una fila de 40 caracteres seguida de LF");
        }
    }

    // ── Elemento: rows ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Elemento rows")
    class RowsTests {

        @Test
        @DisplayName("itera cada objeto del array y genera una fila por item")
        void generatesOneRowPerItem() {
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .add("items", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add("descripcion", "Producto A")
                                    .add("precio", 15000)
                                    .build())
                            .add(Json.createObjectBuilder()
                                    .add("descripcion", "Producto B")
                                    .add("precio", 25000)
                                    .build())
                            .build())
                    .build());

            JsonArrayBuilder detail = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type",   "rows")
                            .add("source", "items")
                            .add("columns", Json.createArrayBuilder()
                                    .add(Json.createObjectBuilder()
                                            .add("field", "descripcion")
                                            .add("width", 20)
                                            .build())
                                    .add(Json.createObjectBuilder()
                                            .add("field",  "precio")
                                            .add("width",  20)
                                            .add("align",  "right")
                                            .add("format", "currency")
                                            .build())
                                    .build())
                            .build());

            PrintConfig config = buildConfig(null, detail, null, false);
            byte[] result = new EscPosBuilder(config, data).build();

            assertTrue(containsSequence(result, "Producto A".getBytes(StandardCharsets.ISO_8859_1)),
                    "Debe contener 'Producto A'");
            assertTrue(containsSequence(result, "Producto B".getBytes(StandardCharsets.ISO_8859_1)),
                    "Debe contener 'Producto B'");
            assertTrue(containsSequence(result, "15.000".getBytes(StandardCharsets.ISO_8859_1)),
                    "Debe contener precio formateado '15.000'");
        }

        @Test
        @DisplayName("source inexistente en data no lanza excepción y no genera filas")
        void missingSourceDoesNotThrow() {
            JsonArrayBuilder detail = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type",   "rows")
                            .add("source", "productos_inexistentes")
                            .add("columns", Json.createArrayBuilder()
                                    .add(Json.createObjectBuilder()
                                            .add("field", "nombre")
                                            .add("width", 40)
                                            .build())
                                    .build())
                            .build());
            PrintConfig config = buildConfig(null, detail, null, false);

            assertDoesNotThrow(() -> new EscPosBuilder(config, emptyData()).build());
        }
    }

    // ── Elemento: cut ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Elemento cut / config.cut")
    class CutTests {

        @Test
        @DisplayName("config cut:true agrega GS V 0x00 al final")
        void cutTrueAddsGsV() {
            PrintConfig config = buildConfig(null, null, null, true);
            byte[] result = new EscPosBuilder(config, emptyData()).build();

            // GS V 0x00 = corte completo
            assertTrue(containsSequence(result, new byte[]{GS, 0x56, 0x00}),
                    "Debe contener GS V 0x00 (corte completo)");
        }

        @Test
        @DisplayName("config cut:false no incluye GS V")
        void cutFalseNoGsV() {
            PrintConfig config = buildConfig(null, null, null, false);
            byte[] result = new EscPosBuilder(config, emptyData()).build();

            assertFalse(containsSequence(result, new byte[]{GS, 0x56}),
                    "No debe contener bytes de corte (GS V)");
        }

        @Test
        @DisplayName("elemento cut en footer con partial:false usa GS V 0x00")
        void footerCutFullPartialFalse() {
            JsonArrayBuilder footer = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type",    "cut")
                            .add("partial", false)
                            .build());
            PrintConfig config = buildConfig(null, null, footer, false);
            byte[] result = new EscPosBuilder(config, emptyData()).build();

            assertTrue(containsSequence(result, new byte[]{GS, 0x56, 0x00}),
                    "Debe contener GS V 0x00 (corte completo)");
        }

        @Test
        @DisplayName("elemento cut en footer con partial:true usa GS V 0x01")
        void footerCutPartialTrue() {
            JsonArrayBuilder footer = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("type",    "cut")
                            .add("partial", true)
                            .build());
            PrintConfig config = buildConfig(null, null, footer, false);
            byte[] result = new EscPosBuilder(config, emptyData()).build();

            assertTrue(containsSequence(result, new byte[]{GS, 0x56, 0x01}),
                    "Debe contener GS V 0x01 (corte parcial)");
        }
    }

    // ── Secciones completas ──────────────────────────────────────────────────

    @Test
    @DisplayName("header + detail + footer se concatenan en el orden correcto")
    void sectionsAreRenderedInOrder() {
        JsonArrayBuilder header = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("type", "text").add("value", "CABECERA").build());
        JsonArrayBuilder detail = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("type", "text").add("value", "DETALLE").build());
        JsonArrayBuilder footer = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("type", "text").add("value", "PIE").build());

        PrintConfig config = buildConfig(header, detail, footer, false);
        byte[] result = new EscPosBuilder(config, emptyData()).build();
        String text = bytesToText(result);

        int posHeader = text.indexOf("CABECERA");
        int posDetail = text.indexOf("DETALLE");
        int posFooter = text.indexOf("PIE");

        assertTrue(posHeader >= 0, "Debe contener 'CABECERA'");
        assertTrue(posDetail >= 0, "Debe contener 'DETALLE'");
        assertTrue(posFooter >= 0, "Debe contener 'PIE'");
        assertTrue(posHeader < posDetail, "CABECERA debe aparecer antes que DETALLE");
        assertTrue(posDetail < posFooter, "DETALLE debe aparecer antes que PIE");
    }
}
