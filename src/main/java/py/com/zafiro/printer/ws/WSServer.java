package py.com.zafiro.printer.ws;

import jakarta.websocket.EncodeException;
import jakarta.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by gcabello on 21/04/2026.
 */
public class WSServer {

    private static WSServer server;
    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

    private WSServer() {
    }

    public static WSServer getInstance() {
        if (server == null) {
            server = new WSServer();
        }
        return server;
    }

    public void addSession(Session session) {
        sessions.add(session);
    }

    public void removeSession(Session session) {
        sessions.remove(session);
    }

    public void pingAll() {
        byte[] pingString = "ping".getBytes();
        ByteBuffer pingData = ByteBuffer.allocate(pingString.length);
        pingData.put(pingString).flip();
        for (Session session : sessions) {
            try {
                session.getBasicRemote().sendPing(pingData);
//                session.getAsyncRemote().sendPing(pingData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        pingData.clear();
    }

    public void sendToAll(Message message) {
        for (Session session : sessions) {
            try {
                session.getBasicRemote().sendObject(message);
            } catch (IOException | EncodeException e) {
                e.printStackTrace();
            }
        }
    }
}
