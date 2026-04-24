package py.com.zafiro.printer.print;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para TemplateEngine.
 *
 * @author gcabello on 21/04/2026.
 */
@DisplayName("TemplateEngine")
class TemplateEngineTest {

    // ── Helper ───────────────────────────────────────────────────────────────

    private PrintData dataOf(String key, String value) {
        JsonObject json = Json.createObjectBuilder().add(key, value).build();
        return new PrintData(json);
    }

    private PrintData dataOf(String key, long value) {
        JsonObject json = Json.createObjectBuilder().add(key, value).build();
        return new PrintData(json);
    }

    private PrintData emptyData() {
        return new PrintData(Json.createObjectBuilder().build());
    }

    // ── resolve() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resolve(template, data)")
    class ResolveTests {

        @Test
        @DisplayName("reemplaza una variable simple")
        void replacesSimpleVariable() {
            PrintData data = dataOf("empresa", "Zafiro S.A.");
            String result = TemplateEngine.resolve("Empresa: {empresa}", data);
            assertEquals("Empresa: Zafiro S.A.", result);
        }

        @Test
        @DisplayName("reemplaza múltiples variables en el mismo string")
        void replacesMultipleVariables() {
            JsonObject json = Json.createObjectBuilder()
                    .add("nombre", "Juan")
                    .add("apellido", "Pérez")
                    .build();
            PrintData data = new PrintData(json);
            String result = TemplateEngine.resolve("{nombre} {apellido}", data);
            assertEquals("Juan Pérez", result);
        }

        @Test
        @DisplayName("texto sin variables lo devuelve intacto")
        void noVariablesReturnsOriginal() {
            String result = TemplateEngine.resolve("Texto fijo", emptyData());
            assertEquals("Texto fijo", result);
        }

        @Test
        @DisplayName("variable inexistente se reemplaza con string vacío")
        void missingVariableReturnsEmpty() {
            String result = TemplateEngine.resolve("{campo_inexistente}", emptyData());
            assertEquals("", result);
        }

        @Test
        @DisplayName("template null devuelve string vacío")
        void nullTemplateReturnsEmpty() {
            assertEquals("", TemplateEngine.resolve(null, emptyData()));
        }

        @Test
        @DisplayName("template vacío devuelve string vacío")
        void emptyTemplateReturnsEmpty() {
            assertEquals("", TemplateEngine.resolve("", emptyData()));
        }

        @Test
        @DisplayName("aplica formato currency a variable numérica")
        void appliesCurrencyFormat() {
            PrintData data = dataOf("total", 5375000L);
            String result = TemplateEngine.resolve("{total}", data, "currency");
            assertEquals("5.375.000", result);
        }

        @Test
        @DisplayName("aplica formato uppercase a variable de texto")
        void appliesUppercaseFormat() {
            PrintData data = dataOf("nombre", "juan pérez");
            String result = TemplateEngine.resolve("{nombre}", data, "uppercase");
            assertEquals("JUAN PÉREZ", result);
        }

        @Test
        @DisplayName("mezcla texto fijo con variable con formato")
        void mixedTextAndFormattedVariable() {
            PrintData data = dataOf("total", 1000000L);
            String result = TemplateEngine.resolve("Total: {total}", data, "currency");
            assertEquals("Total: 1.000.000", result);
        }
    }

    // ── pad() ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("pad(text, width, align)")
    class PadTests {

        @Test
        @DisplayName("alineación izquierda rellena con espacios a la derecha")
        void alignLeftPadsRight() {
            assertEquals("abc       ", TemplateEngine.pad("abc", 10, "left"));
        }

        @Test
        @DisplayName("alineación derecha rellena con espacios a la izquierda")
        void alignRightPadsLeft() {
            assertEquals("       abc", TemplateEngine.pad("abc", 10, "right"));
        }

        @Test
        @DisplayName("alineación central distribuye espacios en ambos lados")
        void alignCenterDistributesPadding() {
            String result = TemplateEngine.pad("abc", 10, "center");
            assertEquals(10, result.length());
            assertTrue(result.contains("abc"));
            assertEquals("   abc    ", result);
        }

        @Test
        @DisplayName("texto más largo que el ancho se trunca")
        void textLongerThanWidthIsTruncated() {
            assertEquals("abcde", TemplateEngine.pad("abcdefghij", 5, "left"));
        }

        @Test
        @DisplayName("texto exactamente del mismo ancho no se modifica")
        void textExactWidthUnchanged() {
            assertEquals("abcde", TemplateEngine.pad("abcde", 5, "left"));
        }

        @Test
        @DisplayName("texto null se trata como vacío")
        void nullTextTreatedAsEmpty() {
            assertEquals("     ", TemplateEngine.pad(null, 5, "left"));
        }

        @Test
        @DisplayName("align null usa alineación izquierda por defecto")
        void nullAlignUsesLeft() {
            assertEquals("abc  ", TemplateEngine.pad("abc", 5, null));
        }

        @Test
        @DisplayName("sobrecarga sin align usa izquierda por defecto")
        void overloadWithoutAlignUsesLeft() {
            assertEquals("abc  ", TemplateEngine.pad("abc", 5));
        }
    }

    // ── separator() ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("separator(char, width)")
    class SeparatorTests {

        @Test
        @DisplayName("genera N repeticiones del carácter dado")
        void generatesCorrectLength() {
            String result = TemplateEngine.separator('-', 10);
            assertEquals("----------", result);
            assertEquals(10, result.length());
        }

        @Test
        @DisplayName("funciona con carácter '='")
        void worksWithEqualsChar() {
            assertEquals("========", TemplateEngine.separator('=', 8));
        }

        @Test
        @DisplayName("ancho cero devuelve string vacío")
        void zeroWidthReturnsEmpty() {
            assertEquals("", TemplateEngine.separator('-', 0));
        }

        @Test
        @DisplayName("todos los caracteres son el mismo")
        void allCharsAreTheSame() {
            String result = TemplateEngine.separator('*', 5);
            assertTrue(result.chars().allMatch(c -> c == '*'));
        }
    }
}
