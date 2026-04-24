package py.com.zafiro.printer.print;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para PrintData.
 *
 * @author gcabello on 21/04/2026.
 */
@DisplayName("PrintData")
class PrintDataTest {

    // ── getValue() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getValue(key)")
    class GetValueTests {

        @Test
        @DisplayName("retorna el valor de un campo String")
        void returnsStringValue() {
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .add("empresa", "Zafiro S.A.")
                    .build());
            assertEquals("Zafiro S.A.", data.getValue("empresa"));
        }

        @Test
        @DisplayName("retorna el valor de un campo numérico como String")
        void returnsNumberAsString() {
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .add("total", 5375000)
                    .build());
            assertEquals("5375000", data.getValue("total"));
        }

        @Test
        @DisplayName("retorna string vacío para clave inexistente")
        void returnsEmptyForMissingKey() {
            PrintData data = new PrintData(Json.createObjectBuilder().build());
            assertEquals("", data.getValue("campo_inexistente"));
        }

        @Test
        @DisplayName("retorna string vacío para campo null en JSON")
        void returnsEmptyForNullJson() {
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .addNull("vacio")
                    .build());
            assertEquals("", data.getValue("vacio"));
        }

        @Test
        @DisplayName("retorna 'true' para campo booleano true")
        void returnsTrueForBooleanTrue() {
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .add("activo", true)
                    .build());
            assertEquals("true", data.getValue("activo"));
        }

        @Test
        @DisplayName("retorna 'false' para campo booleano false")
        void returnsFalseForBooleanFalse() {
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .add("activo", false)
                    .build());
            assertEquals("false", data.getValue("activo"));
        }
    }

    // ── getValue(key, format) ────────────────────────────────────────────────

    @Nested
    @DisplayName("getValue(key, format)")
    class GetValueWithFormatTests {

        @Test
        @DisplayName("formato 'currency' aplica separador de miles con punto")
        void currencyFormatAppliesThousandSeparator() {
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .add("total", 5375000)
                    .build());
            assertEquals("5.375.000", data.getValue("total", "currency"));
        }

        @Test
        @DisplayName("formato 'currency' para valores pequeños")
        void currencyFormatSmallNumber() {
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .add("precio", 85000)
                    .build());
            assertEquals("85.000", data.getValue("precio", "currency"));
        }

        @Test
        @DisplayName("formato 'currency2' incluye dos decimales con coma")
        void currency2FormatIncludesDecimals() {
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .add("precio", 1500)
                    .build());
            assertEquals("1.500,00", data.getValue("precio", "currency2"));
        }

        @Test
        @DisplayName("formato 'number' aplica separador de miles")
        void numberFormatAppliesThousandSeparator() {
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .add("cantidad", 1250)
                    .build());
            assertEquals("1.250", data.getValue("cantidad", "number"));
        }

        @Test
        @DisplayName("formato 'uppercase' convierte a mayúsculas")
        void uppercaseFormatConverts() {
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .add("cliente", "juan pérez")
                    .build());
            assertEquals("JUAN PÉREZ", data.getValue("cliente", "uppercase"));
        }

        @Test
        @DisplayName("formato null devuelve el valor sin transformar")
        void nullFormatReturnsRaw() {
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .add("ruc", "80123456-7")
                    .build());
            assertEquals("80123456-7", data.getValue("ruc", null));
        }

        @Test
        @DisplayName("formato desconocido devuelve el valor sin transformar")
        void unknownFormatReturnsRaw() {
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .add("campo", "valor")
                    .build());
            assertEquals("valor", data.getValue("campo", "formato_inventado"));
        }

        @Test
        @DisplayName("formato currency sobre string no numérico devuelve el valor original")
        void currencyOnNonNumericReturnsRaw() {
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .add("campo", "texto")
                    .build());
            assertEquals("texto", data.getValue("campo", "currency"));
        }
    }

    // ── getArray() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getArray(key)")
    class GetArrayTests {

        @Test
        @DisplayName("retorna el JsonArray para clave existente")
        void returnsJsonArray() {
            JsonArray items = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder().add("codigo", "P-001").add("precio", 15000).build())
                    .add(Json.createObjectBuilder().add("codigo", "P-002").add("precio", 25000).build())
                    .build();
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .add("items", items)
                    .build());

            JsonArray result = data.getArray("items");
            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("retorna null para clave inexistente")
        void returnsNullForMissingKey() {
            PrintData data = new PrintData(Json.createObjectBuilder().build());
            assertNull(data.getArray("items"));
        }

        @Test
        @DisplayName("array vacío retorna JsonArray de tamaño 0")
        void emptyArrayReturnsEmpty() {
            PrintData data = new PrintData(Json.createObjectBuilder()
                    .add("items", Json.createArrayBuilder().build())
                    .build());
            JsonArray result = data.getArray("items");
            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    // ── getValueFromRow() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getValueFromRow(row, key, format)")
    class GetValueFromRowTests {

        private JsonObject buildRow() {
            return Json.createObjectBuilder()
                    .add("codigo",      "P-001")
                    .add("descripcion", "Notebook")
                    .add("cantidad",    2)
                    .add("precio",      2500000)
                    .add("subtotal",    5000000)
                    .build();
        }

        @Test
        @DisplayName("retorna el valor del campo dentro del row")
        void returnsFieldValue() {
            assertEquals("Notebook", PrintData.getValueFromRow(buildRow(), "descripcion", null));
        }

        @Test
        @DisplayName("retorna valor numérico como string")
        void returnsNumericFieldAsString() {
            assertEquals("2", PrintData.getValueFromRow(buildRow(), "cantidad", null));
        }

        @Test
        @DisplayName("aplica formato currency al valor del campo")
        void appliesFormatToField() {
            assertEquals("2.500.000", PrintData.getValueFromRow(buildRow(), "precio", "currency"));
        }

        @Test
        @DisplayName("retorna string vacío para campo inexistente en el row")
        void returnsEmptyForMissingField() {
            assertEquals("", PrintData.getValueFromRow(buildRow(), "iva", null));
        }

        @Test
        @DisplayName("retorna string vacío si el row es null")
        void returnsEmptyForNullRow() {
            assertEquals("", PrintData.getValueFromRow(null, "campo", null));
        }
    }

    // ── applyFormat() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("applyFormat(raw, format)")
    class ApplyFormatTests {

        @Test
        @DisplayName("valor vacío siempre devuelve vacío sin importar el formato")
        void emptyValueReturnsEmpty() {
            assertEquals("", PrintData.applyFormat("", "currency"));
        }

        @Test
        @DisplayName("cero con formato currency devuelve '0'")
        void zeroWithCurrencyReturnsZero() {
            assertEquals("0", PrintData.applyFormat("0", "currency"));
        }

        @Test
        @DisplayName("número decimal con currency2 redondea correctamente")
        void decimalWithCurrency2() {
            assertEquals("1.234,57", PrintData.applyFormat("1234.567", "currency2"));
        }
    }
}
