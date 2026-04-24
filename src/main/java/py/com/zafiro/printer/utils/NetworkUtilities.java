package py.com.zafiro.printer.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author gcabello
 */
public class NetworkUtilities {

    private String ipAddress;
    private String macAddress;
    private String hostName;
    private int port = 80;
    private static NetworkUtilities instanse;

    private NetworkUtilities() {
        try {
            InetAddress localAddress;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(Config.getInstanse().getPingHostName(), port));
                localAddress = socket.getLocalAddress();
                this.ipAddress = localAddress.getHostAddress();
            }
            this.hostName = InetAddress.getLocalHost().getHostName();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localAddress);
            byte[] mac = networkInterface.getHardwareAddress();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
            }
            this.macAddress = sb.toString();
        } catch (IOException ex) {
            Logger.getLogger(NetworkUtilities.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static NetworkUtilities getInstanse() {
        if (instanse == null) {
            instanse = new NetworkUtilities();
        }
        return instanse;
    }

    public NetworkUtilities refresh() {
        instanse = new NetworkUtilities();
        return instanse;
    }


    public String getHostName() {
        return this.hostName;
    }

    public String getHardwareAddress() {
        return this.macAddress;
    }

    public String getInetAddress() {
        return this.ipAddress;
    }
}
