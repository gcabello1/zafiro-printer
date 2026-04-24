package py.com.zafiro.printer.print;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para PrintConfig.
 *
 * @author gcabello on 21/04/2026.
 */
@DisplayName("PrintConfig")
class PrintConfigTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private PrintConfig thermalConfig() {
        return new PrintConfig(Json.createObjectBuilder()
                .add("type",    "thermal")
                .add("width",   48)
                .add("charset", "ISO-8859-1")
                .add("cut",     true)
                .add("beep",    false)
                .build());
    }

    private PrintConfig matricialConfig() {
        return new PrintConfig(Json.createObjectBuilder()
                .add("type",         "matricial")
                .add("width",        80)
                .add("charset",      "ISO-8859-1")
                .add("linesPerPage", 60)
                .add("cpi",          "12")
                .add("quality",      "nlq")
                .build());
    }

    private PrintConfig defaultConfig() {
        // Solo el tipo, todo lo demás usa defaults
        return new PrintConfig(Json.createObjectBuilder()
                .add("type", "thermal")
                .build());
    }

    // ── Tipo ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getType() / isThermal() / isMatricial()")
    class TypeTests {

        @Test
        @DisplayName("isThermal() es true para type 'thermal'")
        void isThermalForThermalType() {
            assertTrue(thermalConfig().isThermal());
            assertFalse(thermalConfig().isMatricial());
        }

        @Test
        @DisplayName("isMatricial() es true para type 'matricial'")
        void isMatricialForMatricialType() {
            assertTrue(matricialConfig().isMatricial());
            assertFalse(matricialConfig().isThermal());
        }

        @Test
        @DisplayName("isThermal() es case-insensitive")
        void isThermalCaseInsensitive() {
            PrintConfig config = new PrintConfig(
                    Json.createObjectBuilder().add("type", "THERMAL").build());
            assertTrue(config.isThermal());
        }

        @Test
        @DisplayName("isMatricial() es case-insensitive")
        void isMatricialCaseInsensitive() {
            PrintConfig config = new PrintConfig(
                    Json.createObjectBuilder().add("type", "MATRICIAL").build());
            assertTrue(config.isMatricial());
        }

        @Test
        @DisplayName("type desconocido no es ni thermal ni matricial")
        void unknownTypeIsNeither() {
            PrintConfig config = new PrintConfig(
                    Json.createObjectBuilder().add("type", "laser").build());
            assertFalse(config.isThermal());
            assertFalse(config.isMatricial());
        }
    }

    // ── Opciones generales ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getWidth() / getCharset()")
    class GeneralOptionsTests {

        @Test
        @DisplayName("getWidth() retorna el valor configurado")
        void returnsConfiguredWidth() {
            assertEquals(48, thermalConfig().getWidth());
            assertEquals(80, matricialConfig().getWidth());
        }

        @Test
        @DisplayName("getWidth() retorna 48 por defecto")
        void returnsDefaultWidth() {
            assertEquals(48, defaultConfig().getWidth());
        }

        @Test
        @DisplayName("getCharset() retorna el charset configurado")
        void returnsConfiguredCharset() {
            assertEquals("ISO-8859-1", thermalConfig().getCharset());
        }

        @Test
        @DisplayName("getCharset() retorna 'ISO-8859-1' por defecto")
        void returnsDefaultCharset() {
            assertEquals("ISO-8859-1", defaultConfig().getCharset());
        }
    }

    // ── Opciones térmica ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Opciones térmica: isCut() / isBeep()")
    class ThermalOptionsTests {

        @Test
        @DisplayName("isCut() retorna true cuando se configura cut:true")
        void cutTrueWhenConfigured() {
            assertTrue(thermalConfig().isCut());
        }

        @Test
        @DisplayName("isCut() retorna true por defecto")
        void cutTrueByDefault() {
            assertTrue(defaultConfig().isCut());
        }

        @Test
        @DisplayName("isBeep() retorna false cuando se configura beep:false")
        void beepFalseWhenConfigured() {
            assertFalse(thermalConfig().isBeep());
        }

        @Test
        @DisplayName("isBeep() retorna false por defecto")
        void beepFalseByDefault() {
            assertFalse(defaultConfig().isBeep());
        }

        @Test
        @DisplayName("isBeep() retorna true cuando se configura beep:true")
        void beepTrueWhenConfigured() {
            PrintConfig config = new PrintConfig(Json.createObjectBuilder()
                    .add("type", "thermal")
                    .add("beep", true)
                    .build());
            assertTrue(config.isBeep());
        }
    }

    // ── Opciones matricial ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Opciones matricial: getLinesPerPage() / getCpi() / getQuality()")
    class MatricialOptionsTests {

        @Test
        @DisplayName("getLinesPerPage() retorna el valor configurado")
        void returnsConfiguredLinesPerPage() {
            assertEquals(60, matricialConfig().getLinesPerPage());
        }

        @Test
        @DisplayName("getLinesPerPage() retorna 60 por defecto")
        void returnsDefaultLinesPerPage() {
            assertEquals(60, defaultConfig().getLinesPerPage());
        }

        @Test
        @DisplayName("getCpi() retorna '12' cuando se configura")
        void returnsConfiguredCpi() {
            assertEquals("12", matricialConfig().getCpi());
        }

        @Test
        @DisplayName("getCpi() retorna '10' por defecto")
        void returnsDefaultCpi() {
            assertEquals("10", defaultConfig().getCpi());
        }

        @Test
        @DisplayName("getQuality() retorna 'nlq' cuando se configura")
        void returnsConfiguredQuality() {
            assertEquals("nlq", matricialConfig().getQuality());
        }

        @Test
        @DisplayName("getQuality() retorna 'draft' por defecto")
        void returnsDefaultQuality() {
            assertEquals("draft", defaultConfig().getQuality());
        }
    }

    // ── Secciones ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getHeader() / getDetail() / getFooter()")
    class SectionTests {

        private PrintConfig configWithSections() {
            JsonArray header = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder().add("type", "text").add("value", "{empresa}").build())
                    .build();
            JsonArray detail = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder().add("type", "rows").add("source", "items").build())
                    .build();
            JsonArray footer = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder().add("type", "cut").build())
                    .build();

            return new PrintConfig(Json.createObjectBuilder()
                    .add("type",   "thermal")
                    .add("header", header)
                    .add("detail", detail)
                    .add("footer", footer)
                    .build());
        }

        @Test
        @DisplayName("getHeader() retorna el array configurado")
        void returnsHeader() {
            JsonArray header = configWithSections().getHeader();
            assertNotNull(header);
            assertEquals(1, header.size());
            assertEquals("text", header.getJsonObject(0).getString("type"));
        }

        @Test
        @DisplayName("getDetail() retorna el array configurado")
        void returnsDetail() {
            JsonArray detail = configWithSections().getDetail();
            assertNotNull(detail);
            assertEquals(1, detail.size());
            assertEquals("rows", detail.getJsonObject(0).getString("type"));
        }

        @Test
        @DisplayName("getFooter() retorna el array configurado")
        void returnsFooter() {
            JsonArray footer = configWithSections().getFooter();
            assertNotNull(footer);
            assertEquals(1, footer.size());
            assertEquals("cut", footer.getJsonObject(0).getString("type"));
        }

        @Test
        @DisplayName("getHeader() retorna null si no hay header en el config")
        void headerNullWhenAbsent() {
            assertNull(defaultConfig().getHeader());
        }

        @Test
        @DisplayName("getDetail() retorna null si no hay detail en el config")
        void detailNullWhenAbsent() {
            assertNull(defaultConfig().getDetail());
        }

        @Test
        @DisplayName("getFooter() retorna null si no hay footer en el config")
        void footerNullWhenAbsent() {
            assertNull(defaultConfig().getFooter());
        }
    }
}
