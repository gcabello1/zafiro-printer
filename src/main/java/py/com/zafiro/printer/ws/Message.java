package py.com.zafiro.printer.ws;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.StringWriter;

/**
 * Created by gcabello on 21/04/2026.
 */
public class Message {

    public static class Status {
        public static Long OK = 200L; //200 OK
        public static Long NOT_FOUND = 404L; //404 Not Found
        public static Long BAD_REQUEST = 400L; //400 Bad Request
        public static Long UNAVAILABLE = 503L; //503 Service Unavailable
        public static Long SERVER_ERROR = 500L; //500 Internal Server Error
    }

    public static class Field {
        public static String ID = "id";
        public static String CMD = "cmd";
        public static String CONFIG = "config";   // JSON template de impresión
        public static String DATA = "data";       // JSON con los datos a imprimir
        public static String PRINTER = "printer";
        public static String VERSION = "version";
        public static String ON_LINE = "online";
        public static String MESSAGE = "message";
        public static String STATUS = "status";
        public static String SERIAL = "serial";
        public static String MAC = "mac";
        public static String IP = "ip";
        public static String HOSTNAME = "hostname";
        public static String TOKEN = "token";
        public static String CLIENT = "client";
        // Privado
        public static String COMMAND = "command";
    }

    public static class Command {
        public static String VERSION = "version";
        public static String HOSTNAME = "hostname";
        public static String IP = "ip";
        public static String MAC = "mac";
        public static String SERIAL = "serial";
        public static String STATUS = "status";
        public static String REGISTER = "register";
        public static String PRINT = "print";
        public static String REMOVE = "remove";
        // Privado
        public static String COMMAND = "command";
    }


    private JsonObject json;

    public Message(JsonObject json) {
        this.json = json;
    }

    public JsonObject getJson() {
        return json;
    }

    public void setJson(JsonObject json) {
        this.json = json;
    }

    @Override
    public String toString() {
        StringWriter writer = new StringWriter();
        Json.createWriter(writer).write(json);
        return writer.toString();
    }
}
