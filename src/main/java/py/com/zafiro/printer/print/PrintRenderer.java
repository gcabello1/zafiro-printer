package py.com.zafiro.printer.print;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.com.zafiro.printer.print.builder.EscPBuilder;
import py.com.zafiro.printer.print.builder.EscPosBuilder;

/**
 * Orquestador principal del motor de impresión.
 *
 * Recibe un PrintConfig (template) y un PrintData (datos),
 * selecciona el builder adecuado según config.type y retorna
 * los bytes listos para enviar a la impresora.
 *
 * Uso:
 *   byte[] bytes = PrintRenderer.render(config, data);
 *   PrinterUtility.getInstance().print(bytes, printerUuid);
 */
public class PrintRenderer {

    private static final Logger log = LoggerFactory.getLogger(PrintRenderer.class);

    private PrintRenderer() {}

    /**
     * Genera los bytes de impresión a partir del config y los datos.
     *
     * @param config configuración del template (tipo, ancho, secciones)
     * @param data   datos a inyectar en el template
     * @return bytes ESC/POS o ESC/P listos para enviar a la impresora,
     *         o null si el tipo de impresora no es reconocido
     */
    public static byte[] render(PrintConfig config, PrintData data) {
        if (config == null) {
            log.error("PrintConfig es null.");
            return null;
        }
        if (data == null) {
            log.error("PrintData es null.");
            return null;
        }

        String type = config.getType();
        log.info("Renderizando documento — tipo: {}, ancho: {}", type, config.getWidth());

        if (config.isThermal()) {
            log.info("Usando EscPosBuilder (térmica).");
            return new EscPosBuilder(config, data).build();
        }

        if (config.isMatricial()) {
            log.info("Usando EscPBuilder (matricial).");
            return new EscPBuilder(config, data).build();
        }

        log.error("Tipo de impresora desconocido: '{}'. Use 'thermal' o 'matricial'.", type);
        return null;
    }
}
