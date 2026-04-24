package py.com.zafiro.printer.print;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.com.zafiro.printer.utils.DevPrinter;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Envía bytes ya generados (ESC/POS o ESC/P) a la impresora destino.
 * La generación de los bytes es responsabilidad de EscPosBuilder / EscPBuilder
 * a través de PrintRenderer.
 *
 * @author gcabello
 */
public class PrinterUtility {

    private static final Logger log = LoggerFactory.getLogger(PrinterUtility.class);

    private static volatile PrinterUtility instance;

    private PrinterUtility() {}

    public static PrinterUtility getInstance() {
        if (instance == null) {
            synchronized (PrinterUtility.class) {
                if (instance == null) {
                    instance = new PrinterUtility();
                }
            }
        }
        return instance;
    }

    /**
     * Envía los bytes dados a la impresora identificada por printerUuid.
     *
     * @param data        bytes ESC/POS o ESC/P listos para imprimir
     * @param printerUuid UUID de la impresora en dev.properties
     * @return true si la impresión fue exitosa
     */
    public boolean print(byte[] data, String printerUuid) {
        String printerPath = DevPrinter.getInstanse().getPrinter(printerUuid);

        if (printerPath == null) {
            log.error("Impresora no encontrada para UUID: {}", printerUuid);
            return false;
        }

        if (printerPath.startsWith("/dev/")) {
            return printToDev(data, printerPath);
        } else {
            return printToCups(data, printerPath);
        }
    }

    /**
     * Imprime directamente en un dispositivo /dev/ (Linux raw).
     */
    private boolean printToDev(byte[] data, String devicePath) {
        log.info("Imprimiendo en dispositivo raw: {}", devicePath);
        try (FileOutputStream fos = new FileOutputStream(devicePath);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.ISO_8859_1)) {
            osw.write(new String(data, StandardCharsets.ISO_8859_1));
            osw.flush();
            log.info("Impresión /dev/ completada: {}", devicePath);
            return true;
        } catch (IOException e) {
            log.error("Error al imprimir en {}: {}", devicePath, e.getMessage());
            return false;
        }
    }

    /**
     * Imprime usando la API CUPS / javax.print (Windows y Linux con CUPS).
     */
    private boolean printToCups(byte[] data, String printerName) {
        log.info("Imprimiendo en CUPS/Windows: {}", printerName);

        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        if (services.length == 0) {
            log.error("No hay impresoras disponibles en el sistema.");
            return false;
        }

        PrintService targetService = null;
        for (PrintService service : services) {
            if (service.getName().equals(printerName)) {
                targetService = service;
                break;
            }
        }

        if (targetService == null) {
            log.error("Impresora CUPS no encontrada: {}", printerName);
            return false;
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
            Doc doc = new SimpleDoc(bais, flavor, null);
            PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
            DocPrintJob job = targetService.createPrintJob();
            job.addPrintJobListener(new PrintJobAdapter());
            job.print(doc, attrs);
            log.info("Impresión CUPS completada: {}", printerName);
            return true;
        } catch (PrintException e) {
            log.error("Error al imprimir en CUPS {}: {}", printerName, e.getMessage());
            return false;
        }
    }

    /**
     * Retorna la lista de nombres de impresoras disponibles en el sistema.
     */
    public java.util.List<String> getPrinters() {
        java.util.List<String> result = new java.util.ArrayList<>();
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService service : services) {
            result.add(service.getName());
        }
        return result;
    }
}
