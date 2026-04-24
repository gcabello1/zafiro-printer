package py.com.zafiro.printer.ws;

import io.undertow.websockets.jsr.DefaultWebSocketClientSslProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.com.zafiro.printer.utils.Config;

import javax.net.ssl.SSLContext;
import jakarta.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

/**
 * Created by gcabello on 21/04/2026.
 */
public class WSClient {

    private Session session;
    private static WebSocketContainer container;
    private static URI server;
    private static WSClient client;
    final static Logger log = LoggerFactory.getLogger(WSClient.class.getName());

    private WSClient() {
//        try {
//            DefaultWebSocketClientSslProvider.setSslContext(SSLContext.getDefault());
//            container = ContainerProvider.getWebSocketContainer();
//        } catch (NoSuchAlgorithmException ex) {
//            ex.printStackTrace();
//        }
    }

    public static WSClient getInstance() {
        if (client == null) {
            try {
                server = new URI(Config.getInstanse().getServer() + "?t=" + Config.getInstanse().getToken());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            client = new WSClient();
        }
        return client;
    }

    public void ping() {
        try {
            String pingString = "ping";
            ByteBuffer pingData = ByteBuffer.allocate(pingString.getBytes().length);
            pingData.put(pingString.getBytes()).flip();
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendPing(pingData);
//                session.getAsyncRemote().sendPing(pingData);
            } else {
                this.connect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(Message message) {
        log.info("Message: " + message.toString());
        try {
            session.getBasicRemote().sendObject(message);
        } catch (IOException | EncodeException e) {
            e.printStackTrace();
        }
    }

    public boolean isOpen() {
        return session != null && session.isOpen();
    }


    public void connect() {
        this.connect(server);
    }

    public void connect(String uri) {
        try {
            server = new URI(uri);
            connect(server);
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        }
    }

    public void connect(URI uri) {
        log.info("Connect: " + uri.toString());
        try {
            this.server = uri;
            DefaultWebSocketClientSslProvider.setSslContext(SSLContext.getDefault());
            container = ContainerProvider.getWebSocketContainer();
            session = container.connectToServer(ClientEndpoint.class, server);

        } catch (IOException | NoSuchAlgorithmException | DeploymentException ex) {
            log.info(ex.getMessage());
//            System.out.println(ex);
        }
    }

}
