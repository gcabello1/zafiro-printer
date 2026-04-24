package py.com.zafiro.printer.ws;

import jakarta.websocket.EncodeException;
import jakarta.websocket.EndpointConfig;

/**
 * Created by gcabello on 21/04/2026.
 */
public class Encoder implements jakarta.websocket.Encoder.Text<Message> {

    @Override
    public String encode(Message message) throws EncodeException {

        return message.getJson().toString();
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
