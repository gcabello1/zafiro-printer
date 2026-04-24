package py.com.zafiro.printer.ws;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.com.zafiro.printer.utils.DevPrinter;

import static py.com.zafiro.printer.ws.Message.*;

import jakarta.json.Json;
import jakarta.websocket.*;
import java.io.IOException;
import java.util.Calendar;

/**
 * Created by gcabello on 21/04/2026.
 */

@jakarta.websocket.ClientEndpoint(decoders = Decoder.class, encoders = Encoder.class)
public class ClientEndpoint {

    static final Logger log = LoggerFactory.getLogger(ClientEndpoint.class.getName());

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        log.info("onClose");
        Message message = new Message(Json.createObjectBuilder()
                .add(Field.ID, Calendar.getInstance().getTimeInMillis())
                .add(Field.CMD, Command.STATUS)
                .add(Field.ON_LINE, WSClient.getInstance().isOpen())
                .add(Field.MESSAGE, Status.OK)
                .add(Field.STATUS, DevPrinter.getInstanse().getStatusMessage())
                .build());
        WSServer.getInstance().sendToAll(message);
    }

    @OnError
    public void onError(Throwable error) {
        log.info("onError");
//        System.out.println("ERROR");
//        System.out.println(error);
    }

    @OnMessage
    public void onPongMessage(PongMessage message) {
//        System.out.println(String.valueOf(message.getApplicationData().array()));
    }

    @OnMessage
    public void onMessage(Message message, Session session) {
        log.info("onMessage: " + message.toString());
        try {
            Server.message(message, session);
        } catch (IOException | EncodeException e) {
            e.printStackTrace();
            log.info(e.getMessage());
        }
//        System.out.println(session.isSecure());
//        System.out.println(message);
    }

    @OnOpen
    public void onOpen(Session session) {
        log.info("onOpen");
        Message message = new Message(Json.createObjectBuilder()
                .add(Field.ID, Calendar.getInstance().getTimeInMillis())
                .add(Field.CMD, Command.STATUS)
                .add(Field.ON_LINE, WSClient.getInstance().isOpen())
                .add(Field.MESSAGE, Status.OK)
                .add(Field.STATUS, DevPrinter.getInstanse().getStatusMessage())
                .build());
        WSServer.getInstance().sendToAll(message);
        try {
            session.getBasicRemote().sendObject(Server.register());
        } catch (IOException | EncodeException e) {
            e.printStackTrace();
        }
    }
}
