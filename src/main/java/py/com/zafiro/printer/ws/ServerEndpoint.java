/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package py.com.zafiro.printer.ws;

import jakarta.websocket.*;
import java.io.IOException;

/**
 * @author gcabello
 */
@jakarta.websocket.server.ServerEndpoint(value = "/ws", encoders = Encoder.class, decoders = Decoder.class)
public class ServerEndpoint {

    @OnMessage
    public void message(Message message, Session session) {
        try {
            Server.message(message, session);
        } catch (IOException | EncodeException e) {
            e.printStackTrace();
        }
    }

    @OnError
    public void onError(Throwable error) {
    }

    @OnOpen
    public void onOpen(Session session) {
        try {
            session.getBasicRemote().sendObject(Server.register());
        } catch (IOException | EncodeException e) {
            e.printStackTrace();
        }
        WSServer.getInstance().addSession(session);
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        WSServer.getInstance().removeSession(session);
//        System.out.println(session.getId() + " " + closeReason.getCloseCode() + " " + closeReason.getReasonPhrase());
    }
}
