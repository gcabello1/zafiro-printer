package py.com.zafiro.printer.print;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para PrintRenderer.
 *
 * @author gcabello on 21/04/2026.
 */
@DisplayName("PrintRenderer")
class PrintRendererTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static final byte ESC = 0x1B;
    private static final byte AT  = 0x40;   // ESC @ = INIT

    private PrintConfig configOf(String type) {
        return new PrintConfig(Json.createObjectBuilder()
                .add("type",  type)
                .add("width", 48)
                .build());
    }

    private PrintData emptyData() {
        return new PrintData(Json.createObjectBuilder().build());
    }

    // ── Routing por tipo ─────────────────────────────────────────────────────

    @Test
    @DisplayName("thermal → genera bytes ESC/POS (comienza con ESC @)")
    void thermalConfigGeneratesEscPosBytes() {
        byte[] result = PrintRenderer.render(configOf("thermal"), emptyData());

        assertNotNull(result);
        assertTrue(result.length >= 2, "Debe tener al menos los bytes de INIT");
        assertEquals(ESC, result[0], "Primer byte debe ser ESC (0x1B)");
        assertEquals(AT,  result[1], "Segundo byte debe ser @ (0x40)");
    }

    @Test
    @DisplayName("matricial → genera bytes ESC/P (comienza con ESC @)")
    void matricialConfigGeneratesEscPBytes() {
        byte[] result = PrintRenderer.render(configOf("matricial"), emptyData());

        assertNotNull(result);
        assertTrue(result.length >= 2, "Debe tener al menos los bytes de INIT");
        assertEquals(ESC, result[0], "Primer byte debe ser ESC (0x1B)");
        assertEquals(AT,  result[1], "Segundo byte debe ser @ (0x40)");
    }

    @Test
    @DisplayName("thermal y matricial producen bytes distintos para el mismo contenido")
    void thermalAndMatricialProduceDifferentBytes() {
        JsonObject section = Json.createObjectBuilder()
                .add("header", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("type",  "text")
                                .add("value", "Hola")
                                .build())
                        .build())
                .build();

        PrintConfig thermal   = new PrintConfig(Json.createObjectBuilder()
                .add("type", "thermal").add("width", 48)
                .add("header", section.getJsonArray("header"))
                .build());
        PrintConfig matricial = new PrintConfig(Json.createObjectBuilder()
                .add("type", "matricial").add("width", 48)
                .add("header", section.getJsonArray("header"))
                .build());

        byte[] thermalBytes   = PrintRenderer.render(thermal,   emptyData());
        byte[] matricialBytes = PrintRenderer.render(matricial, emptyData());

        assertNotNull(thermalBytes);
        assertNotNull(matricialBytes);
        // Los bytes pueden diferir porque ESC/POS agrega comandos de alineación,
        // tamaño, etc. que ESC/P no tiene
        assertFalse(java.util.Arrays.equals(thermalBytes, matricialBytes),
                "Los bytes de térmica y matricial no deben ser idénticos");
    }

    // ── Null guards ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("config null → retorna null")
    void nullConfigReturnsNull() {
        assertNull(PrintRenderer.render(null, emptyData()));
    }

    @Test
    @DisplayName("data null → retorna null")
    void nullDataReturnsNull() {
        assertNull(PrintRenderer.render(configOf("thermal"), null));
    }

    @Test
    @DisplayName("tipo desconocido → retorna null")
    void unknownTypeReturnsNull() {
        assertNull(PrintRenderer.render(configOf("laser"), emptyData()));
    }

    // ── Corte al final (thermal) ──────────────────────────────────────────────

    @Test
    @DisplayName("config thermal con cut:true → último byte es GS V (corte)")
    void thermalWithCutHasCutAtEnd() {
        PrintConfig config = new PrintConfig(Json.createObjectBuilder()
                .add("type",  "thermal")
                .add("width", 48)
                .add("cut",   true)
                .build());

        byte[] result = PrintRenderer.render(config, emptyData());

        assertNotNull(result);
        // GS V 0x00 = corte completo → 0x1D 0x56 0x00
        // El corte es los últimos 3 bytes
        int len = result.length;
        assertTrue(len >= 3);
        assertEquals((byte) 0x1D, result[len - 3], "Penúltimo grupo debe iniciar con GS (0x1D)");
        assertEquals((byte) 0x56, result[len - 2], "Segundo byte del corte debe ser V (0x56)");
    }

    @Test
    @DisplayName("config thermal con cut:false → no termina con bytes de corte")
    void thermalWithoutCutHasNoCutBytes() {
        PrintConfig config = new PrintConfig(Json.createObjectBuilder()
                .add("type",  "thermal")
                .add("width", 48)
                .add("cut",   false)
                .build());

        byte[] result = PrintRenderer.render(config, emptyData());
        assertNotNull(result);

        int len = result.length;
        // Si no hay corte, el último byte no debería ser 0x56 precedido de 0x1D
        if (len >= 3) {
            assertFalse(
                result[len - 3] == (byte) 0x1D && result[len - 2] == (byte) 0x56,
                "No debe haber bytes de corte al final"
            );
        }
    }
}
