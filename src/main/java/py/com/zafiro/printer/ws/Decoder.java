package py.com.zafiro.printer.ws;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.websocket.DecodeException;
import jakarta.websocket.EndpointConfig;
import java.io.StringReader;

/**
 * Created by gcabello on 21/04/2026.
 */
public class Decoder implements jakarta.websocket.Decoder.Text<Message> {

    @Override
    public Message decode(String s) throws DecodeException {
        JsonObject json = Json.createReader(new StringReader(s)).readObject();
        return new Message(json);
    }

    @Override
    public boolean willDecode(String s) {
        try {
            Json.createReader(new StringReader(s)).read();
            return true;
        } catch (JsonException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
