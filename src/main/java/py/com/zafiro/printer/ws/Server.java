package py.com.zafiro.printer.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.com.zafiro.printer.Main;
import py.com.zafiro.printer.print.PrintConfig;
import py.com.zafiro.printer.print.PrintData;
import py.com.zafiro.printer.print.PrintRenderer;
import py.com.zafiro.printer.print.PrinterUtility;
import py.com.zafiro.printer.utils.CommandExecutor;
import py.com.zafiro.printer.utils.Config;
import py.com.zafiro.printer.utils.DevPrinter;
import py.com.zafiro.printer.utils.NetworkUtilities;

import static py.com.zafiro.printer.ws.Message.*;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Session;
import java.io.IOException;
import java.util.Calendar;

/**
 * Created by gcabello on 21/04/2026.
 */
public class Server {

    final static Logger log = LoggerFactory.getLogger(Server.class.getName());

    public static void message(Message message, Session session) throws IOException, EncodeException {
        log.info("message: " + message.toString());
        if (message.getJson().containsKey(Field.CMD) && message.getJson().containsKey(Field.ID)) {
            Message response;
            if (message.getJson().getString(Field.CMD).equals(Command.PRINT)) {
                response = Server.print(message);
            } else if (message.getJson().getString(Field.CMD).equals(Command.STATUS)) {
                response = Server.getStatus(message);
            } else if (message.getJson().getString(Field.CMD).equals(Command.SERIAL)) {
                response = Server.getSerial(message);
            } else if (message.getJson().getString(Field.CMD).equals(Command.MAC)) {
                response = Server.getMac(message);
            } else if (message.getJson().getString(Field.CMD).equals(Command.IP)) {
                response = Server.getIp(message);
            } else if (message.getJson().getString(Field.CMD).equals(Command.HOSTNAME)) {
                response = Server.getHostname(message);
            } else if (message.getJson().getString(Field.CMD).equals(Command.VERSION)) {
                response = Server.getVersion(message);
            } else if (message.getJson().getString(Field.CMD).equals(Command.REGISTER)) {
                response = Server.register(message);
//            } else if (message.getJson().getString(Message.CMD).equals(Message.RENAME)) {
//                response = Server.rename(message);
            } else if (message.getJson().getString(Field.CMD).equals(Command.REMOVE)) {
                response = Server.remove(message);
            } else if (message.getJson().getString(Field.CMD).equals(Command.COMMAND)) {
                response = Server.executeCommand(message);
            } else {
                response = new Message(Json.createObjectBuilder()
                        .add(Field.CMD, message.getJson().getString(Field.CMD))
                        .add(Field.MESSAGE, Status.NOT_FOUND)
                        .build());
            }
            log.info("message response: " + response.toString());
            session.getBasicRemote().sendObject(response);
        }
    }

    public static Message register() {
        return register(null);
    }

    public static Message register(Message message) {
        JsonObjectBuilder job = Json.createObjectBuilder()
                .add(Field.CMD, Command.REGISTER)
                .add(Field.MESSAGE, Status.OK)
                .add(Field.SERIAL, Config.getInstanse().getSerial())
                .add(Field.ON_LINE, WSClient.getInstance().isOpen())
                .add(Field.TOKEN, Config.getInstanse().getToken())
                .add(Field.MAC, NetworkUtilities.getInstanse().getHardwareAddress().replace(":", ""))
                .add(Field.IP, NetworkUtilities.getInstanse().getInetAddress())
                .add(Field.HOSTNAME, NetworkUtilities.getInstanse().getHostName())
                .add(Field.VERSION, Main.VERSION)
                .add(Field.STATUS, DevPrinter.getInstanse().getStatusMessage());
        if (message != null) {
            job.add(Field.ID, message.getJson().getJsonNumber(Field.ID).longValue());
            if (message.getJson().containsKey(Field.CLIENT)) {
                job.add(Field.CLIENT, message.getJson().getString(Field.CLIENT));
            }
        } else {
            job.add(Field.ID, Calendar.getInstance().getTimeInMillis());
        }
        Message response = new Message(job.build());
        return response;
    }

    public static Message print(Message message) {
        log.info("print");
        JsonObjectBuilder job;
        JsonObject json = message.getJson();

        // Validar campos requeridos
        if (!json.containsKey(Field.CONFIG) ||
            !json.containsKey(Field.DATA)   ||
            !json.containsKey(Field.PRINTER)) {
            log.warn("print bad request — faltan campos config, data o printer");
            job = Json.createObjectBuilder()
                    .add(Field.ID,      json.getJsonNumber(Field.ID))
                    .add(Field.CMD,     Command.PRINT)
                    .add(Field.MESSAGE, Status.BAD_REQUEST);
            return new Message(job.build());
        }

        String printerUuid = json.getString(Field.PRINTER);

        // Verificar que la impresora esté online
        if (!DevPrinter.getInstanse().getStatus(printerUuid)) {
            log.warn("Impresora no disponible: {}", printerUuid);
            job = Json.createObjectBuilder()
                    .add(Field.ID,      json.getJsonNumber(Field.ID).longValue())
                    .add(Field.CMD,     Command.PRINT)
                    .add(Field.MESSAGE, Status.UNAVAILABLE)
                    .add(Field.PRINTER, printerUuid);
            return new Message(job.build());
        }

        // Construir config y data
        PrintConfig config = new PrintConfig(json.getJsonObject(Field.CONFIG));
        PrintData   data   = new PrintData(json.getJsonObject(Field.DATA));

        // Renderizar bytes según tipo de impresora
        byte[] bytes = PrintRenderer.render(config, data);
        if (bytes == null) {
            log.error("PrintRenderer retornó null para la impresora {}", printerUuid);
            job = Json.createObjectBuilder()
                    .add(Field.ID,      json.getJsonNumber(Field.ID).longValue())
                    .add(Field.CMD,     Command.PRINT)
                    .add(Field.MESSAGE, Status.SERVER_ERROR)
                    .add(Field.PRINTER, printerUuid);
            return new Message(job.build());
        }

        // Enviar a la impresora
        boolean ok = PrinterUtility.getInstance().print(bytes, printerUuid);
        long status = ok ? Status.OK : Status.SERVER_ERROR;

        job = Json.createObjectBuilder()
                .add(Field.ID,      json.getJsonNumber(Field.ID).longValue())
                .add(Field.CMD,     Command.PRINT)
                .add(Field.MESSAGE, status)
                .add(Field.PRINTER, printerUuid);
        if (json.containsKey(Field.TOKEN)) {
            job.add(Field.TOKEN, json.getString(Field.TOKEN));
        }

        log.info("print finalizado — status: {}, impresora: {}", status, printerUuid);
        return new Message(job.build());
    }

    public static Message remove(Message message) {
        JsonObjectBuilder job;
        if (!message.getJson().getString(Field.PRINTER).isEmpty()
                && DevPrinter.getInstanse().getPrinter(message.getJson().getString(Field.PRINTER)) != null) {
            DevPrinter.getInstanse().removePrinter(message.getJson().getString(Field.PRINTER));
            job = Json.createObjectBuilder()
                    .add(Field.ID, message.getJson().getJsonNumber(Field.ID))
                    .add(Field.CMD, Command.REMOVE)
                    .add(Field.MESSAGE, Status.OK);
        } else {
            job = Json.createObjectBuilder()
                    .add(Field.ID, message.getJson().getJsonNumber(Field.ID))
                    .add(Field.CMD, Command.REMOVE)
                    .add(Field.MESSAGE, Status.BAD_REQUEST);
        }
        if (message.getJson().containsKey(Field.CLIENT)) {
            job.add(Field.CLIENT, message.getJson().getString(Field.CLIENT));
        }
        Message response = new Message(job.build());
        return response;
    }

    public static Message getStatus(Message message) {
        JsonObjectBuilder job = Json.createObjectBuilder()
                .add(Field.ID, message.getJson().getJsonNumber(Field.ID).longValue())
                .add(Field.CMD, Command.STATUS)
                .add(Field.ON_LINE, WSClient.getInstance().isOpen())
                .add(Field.MESSAGE, Status.OK)
                .add(Field.STATUS, DevPrinter.getInstanse().getStatusMessage());
        if (message.getJson().containsKey(Field.CLIENT)) {
            job.add(Field.CLIENT, message.getJson().getString(Field.CLIENT));
        }
        Message response = new Message(job.build());
        return response;
    }

    public static Message getSerial(Message message) {
        JsonObjectBuilder job = Json.createObjectBuilder()
                .add(Field.ID, message.getJson().getJsonNumber(Field.ID).longValue())
                .add(Field.CMD, Command.SERIAL)
                .add(Field.MESSAGE, Status.OK)
                .add(Field.SERIAL, Config.getInstanse().getSerial());
        if (message.getJson().containsKey(Field.CLIENT)) {
            job.add(Field.CLIENT, message.getJson().getString(Field.CLIENT));
        }
        Message response = new Message(job.build());
        return response;
    }

    public static Message getMac(Message message) {
        JsonObjectBuilder job = Json.createObjectBuilder()
                .add(Field.ID, message.getJson().getJsonNumber(Field.ID).longValue())
                .add(Field.CMD, Command.MAC)
                .add(Field.MESSAGE, Status.OK)
                .add(Field.MAC, NetworkUtilities.getInstanse().getHardwareAddress());
        if (message.getJson().containsKey(Field.CLIENT)) {
            job.add(Field.CLIENT, message.getJson().getString(Field.CLIENT));
        }
        Message response = new Message(job.build());
        return response;
    }

    public static Message getIp(Message message) {
        JsonObjectBuilder job = Json.createObjectBuilder()
                .add(Field.ID, message.getJson().getJsonNumber(Field.ID).longValue())
                .add(Field.CMD, Command.IP)
                .add(Field.MESSAGE, Status.OK)
                .add(Field.IP, NetworkUtilities.getInstanse().getInetAddress());
        if (message.getJson().containsKey(Field.CLIENT)) {
            job.add(Field.CLIENT, message.getJson().getString(Field.CLIENT));
        }
        Message response = new Message(job.build());
        return response;
    }

    public static Message getHostname(Message message) {
        JsonObjectBuilder job = Json.createObjectBuilder()
                .add(Field.ID, message.getJson().getJsonNumber(Field.ID).longValue())
                .add(Field.CMD, Command.HOSTNAME)
                .add(Field.MESSAGE, Status.OK)
                .add(Field.HOSTNAME, NetworkUtilities.getInstanse().getHostName());
        if (message.getJson().containsKey(Field.CLIENT)) {
            job.add(Field.CLIENT, message.getJson().getString(Field.CLIENT));
        }
        Message response = new Message(job.build());
        return response;
    }

    public static Message getVersion(Message message) {
        JsonObjectBuilder job = Json.createObjectBuilder()
                .add(Field.ID, message.getJson().getJsonNumber(Field.ID).longValue())
                .add(Field.CMD, Command.VERSION)
                .add(Field.MESSAGE, Status.OK)
                .add(Field.VERSION, Main.VERSION);
        if (message.getJson().containsKey(Field.CLIENT)) {
            job.add(Field.CLIENT, message.getJson().getString(Field.CLIENT));
        }
        Message response = new Message(job.build());
        return response;
    }

    public static Message executeCommand(Message message) {
        JsonObjectBuilder job = Json.createObjectBuilder()
                .add(Field.ID, message.getJson().getJsonNumber(Field.ID))
                .add(Field.CMD, Command.COMMAND)
                .add(Field.COMMAND, CommandExecutor.execute(message.getJson().getString(Field.COMMAND)));
        if (message.getJson().containsKey(Field.CLIENT)) {
            job.add(Field.CLIENT, message.getJson().getString(Field.CLIENT));
        }
        Message response = new Message(job.build());
        return response;
    }
}
