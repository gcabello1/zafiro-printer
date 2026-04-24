package py.com.zafiro.printer.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.com.zafiro.printer.ws.Message;

/**
 * Created by gcabello on 21/04/2026.
 */
public class DevPrinter {

    final static Logger log = LoggerFactory.getLogger(DevPrinter.class.getName());
    private static DevPrinter instanse;
    private static Properties prop;
    private static Boolean remove = false;

    private String fileName;

    private static Map<String, Boolean> devStatus = new HashMap<>();

    private DevPrinter(String fileName) {
        this.fileName = fileName;
        prop = new Properties();
        InputStream is = null;
        try {
            File f = new File(fileName);
            is = new FileInputStream(f);
        } catch (Exception e) {
            is = null;
        }
        try {
            if (is == null) {
                is = DevPrinter.class.getResourceAsStream(fileName);
            }

            prop.load(is);
        } catch (Exception e) {

        }
        loadStatus();
    }

    private static void loadStatus() {
        Set<Object> printers = prop.keySet();
        for (Object printer : printers) {
            String dev = prop.getProperty((String) printer);
            File file = new File(dev);
            if (file.canWrite()) {
                devStatus.put((String) printer, true);
            } else {
                devStatus.put((String) printer, false);
            }
        }
    }

    public static DevPrinter getInstanse() {
        if (instanse == null) {
            instanse = new DevPrinter("dev.properties");
        }
        return instanse;
    }

    public static DevPrinter getInstanse(String path) {
        if (instanse == null) {
            instanse = new DevPrinter(path);
        }
        return instanse;
    }

    public boolean update() {
        newDevices();
        Set<String> names = devStatus.keySet();
        boolean status = false;
        for (String name : names) {
            boolean currentStatus = getStatus(name);
            if (currentStatus != devStatus.get(name)) {
                status = true;
                devStatus.replace(name, currentStatus);
            }
        }
        if (remove) {
            remove = false;
            return true;
        }
        return status;
    }

    public JsonArrayBuilder getStatusMessage() {
        JsonArrayBuilder json = Json.createArrayBuilder();
        for (Map.Entry<String, Boolean> entry : devStatus.entrySet()) {
            json.add(Json.createObjectBuilder()
                    .add(Message.Field.ID, entry.getKey())
                    .add(Message.Field.ON_LINE, entry.getValue().booleanValue())
                    .add(Message.Field.PRINTER, getPrinter(entry.getKey().toString())));
        }
        return json;

    }

    public String getPrinter(String name) {
        String path = prop.getProperty(name);
        if (path == null) {
            return "";
        } else {
            return path;
        }
    }

    public Boolean getStatus(String name) {
//        log.info("get status");
        //Se busca el Sistema operativo con el que se esta trabajando
        String osName = System.getProperty("os.name").toLowerCase();
        //Se detecta que es Windows y se busca si tiene la impresora conectada por PrintServiceLookup
        if (osName.contains("win")) {
//            log.info("Windows status=true");
            // TODO: Buscar una alternativa a solo retornar True para windows
            return true;
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
//            log.info("Linux status");
            String printerPath = getPrinter(name);
            if (printerPath.isEmpty()) {
                return false;
            } else if (printerPath.startsWith("/dev/")) {
//                log.info("/dev/ printer");
                if (Files.isWritable(Paths.get(getPrinter(name)))) {
//                    log.info("status=true");
                    return true;
                }
            } else {
//                log.info("Linux CUPS status = true");
                // TODO: Buscar alternativa para CUPS
                return true;
            }
        } else {
//            log.info("Not Win or Linux status = false");
            return false;
        }
//        log.info("default status = false");
        return false;
    }

    private void newDevices() {
        //Se trae la lista de todas las impresoras activas
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        //Se busca el Sistema operativo con el que se esta trabajando
        String osName = System.getProperty("os.name").toLowerCase();
        //Se detecta que es Windows y se busca si tiene la impresora conectada por PrintServiceLookup
        if (osName.contains("win")) {
            if (services.length > 0) {
                //Se toma todas las impresoras activas y se guarda en el archivo
                for (int i = 0; services.length > i; i++) {
                    if (!prop.contains(services[i].getName())) {
                        String uuid = UUID.randomUUID().toString();
                        prop.put(uuid, services[i].getName());
                        devStatus.put(uuid, !getStatus(uuid));
                        save();
                    }
                }
            }
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            // Impresoras RAW
            File[] files = new File(Config.getInstanse().getDevPath()).listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!prop.contains(file.getAbsolutePath())) {
                        String uuid = UUID.randomUUID().toString();
                        prop.put(uuid, file.getAbsolutePath());
                        devStatus.put(uuid, !getStatus(uuid));
                        save();
                    }
                }
            }
            // Busquedar de impresoras configuradas por CUPS
            if (services.length > 0) {
                //Se toma todas las impresoras activas y se guarda en el archivo
                for (int i = 0; services.length > i; i++) {
                    if (!prop.contains(services[i].getName())) {
                        String uuid = UUID.randomUUID().toString();
                        prop.put(uuid, services[i].getName());
                        devStatus.put(uuid, !getStatus(uuid));
                        save();
                    }
                }
            }
        }
    }

    private void addPrinter(String name, String dev) {
        prop.put(name, dev);
        devStatus.put(name, !getStatus(name));
        save();
    }

    public void removePrinter(String name) {
        prop.remove(name);
        devStatus.remove(name);
        remove = true;
        save();
    }

    private void save() {
        try {
            File f = new File(fileName);
            OutputStream out = new FileOutputStream(f);
            prop.store(out, "Devices");
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
