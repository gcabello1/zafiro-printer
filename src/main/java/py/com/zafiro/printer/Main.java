/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package py.com.zafiro.printer;

import static io.undertow.Handlers.resource;

import io.undertow.Undertow;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;


import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import jakarta.servlet.ServletException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import py.com.zafiro.printer.utils.Config;
import py.com.zafiro.printer.utils.DevPrinter;
import py.com.zafiro.printer.ws.ServerEndpoint;
import py.com.zafiro.printer.ws.WSClient;
import py.com.zafiro.printer.ws.jobs.PrinterScheduler;

/**
 * @author gcabello
 */
public class Main {

    private static final char[] STORE_PASSWORD = "password".toCharArray();
    public static final String VERSION = "0.10.0";
    final static Logger log = LoggerFactory.getLogger(Main.class.getName());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {

        //        try {
        //17 5
        //16 1
//        StringBuilder s = new StringBuilder();
//        s.append((char) 27).append((char) 64);
//        s.append((char) 27).append((char) 120).append((char) 0);
//        s.append((char) 27).append((char) 33).append((char) 5);
//        s.append((char) 27).append((char) 51).append((char) 15);
//            s.append("0....5....1....5....2....5....3....5....\n");
//            s.append((char) 27).append((char) 33).append((char) 17);
//            s.append("0....5....1....5....2....5....3....5....\n");
//            s.append("001-001-0000005\n");


//            Right
//            s.append((char) 27).append((char) 97).append((char) 0);
//            s.append("Hola\n");
//            Center
//            s.append((char) 27).append((char) 97).append((char) 1);
//            s.append("Hola\n");
//            Left
//        s.append((char) 27).append((char) 113);
//        s.append((char) 27).append((char) 97).append((char) 2);
//            s.append("Hola\n");


//        s.append((char) 27).append((char) 33).append((char) 5);
//        s.append((char) 27).append((char) 51).append((char) 12);
//        s.append(generarNumero(4960));
//        s.append((char) 27).append((char) 51).append((char) 25);
//        s.append((char) 27).append((char) 33).append((char) 17);
//        s.append((char) 27).append((char) 116).append((char) 18);
//        s.append("\n");
//        s.append("PUERTOS Y ESTIBAJES SA (Puerto Fenix)\n");
//        s.append("Mercedes Benz OM352\n");
//        s.append("\n");
//        for (int i = 0; i < 255; i++) {
//            s.append((char) 27).append((char) 116).append((char) i);
//            s.append(i + " ñ " + (char) 164 + " " + (char) 165 + " ");
//        }

//        s.append("\n");
//        s.append((char) 27).append((char) 33).append((char) 5);
//        s.append("9/11/2015\n");

//        s.append(UUID.randomUUID());
//        System.out.println(s);
//
//        String base64 = Base64.encodeBytes(s.toString().getBytes());
//
//        System.out.println(base64);
//
//        Printer printer = new Printer();
//        printer.setText(base64);
//        printer.setPrinter("TM-U325");

//        Printer printer = new Printer();
//        StringBuilder s = new StringBuilder();
//        s.append((char) 27).append((char) 33).append((char) 0);
//        s.append("0....5....1....5....2....5....3....5....4....5....5....5....6....5....7....5....\n");
//        s.append((char) 27).append((char) 33).append((char) 1);


//        s.append((char) 27).append((char) 64);
//        s.append((char) 27).append((char) 120).append((char) 0);
//        s.append((char) 27).append((char) 33).append((char) 1);
//        s.append((char) 27).append((char) 51).append((char) 20);
//        s.append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\n");
//        s.append((char) 27).append((char) 51).append((char) 22);
//        s.append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\n");


//        s.append((char) 27).append((char) 64);
//        s.append((char) 27).append((char) 120).append((char) 0);
//        s.append((char) 27).append((char) 33).append((char) 5);
//        s.append((char) 27).append((char) 51).append((char) 25);
//        s.append("0....5....1....5....2....5....3....\n");
//        s.append("0....5....1....5....2....5....3....\n");
//        s.append("0....5....1....5....2....5....3....\n");
//        s.append("0....5....1....5....2....5....3....\n");
//        s.append("0....5....1....5....2....5....3....\n");
//        s.append("5....5....1....5....2....5....3....\n");

//        s.append((char) 27).append((char) 33).append((char) 65);
//        s.append((char) 27).append((char) 51).append((char) 50);
//        s.append("0....5....1....5....2....5....3....\n");


//        String sFactura = //"\u001B\u0033\u0025"
//                + "\u001B\u0021\u0005"
//                + "\u001B\u0030"
//                s.toString();
//                + "\u001B\u0064\u0001";


//        System.out.println("0x10");
//        System.out.println(s);

//        String base64 = Base64.encodeBytes("Hola Mundo!\n".getBytes());
//        String base64 = Base64.encodeBytes(sFactura.getBytes());
//        String base64 = "G0AgG3gAIBshBSAbMxkKCgogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIDNhYjhmYzM2LWE3NjctNDE0ZC05OWY0LWIxN2ZjZTVlYWE4NwoKCgoKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgMDAxLTAwMS0wMDAwMjAxCgoKCgogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAwNSBkZSBub3ZpZW1icmUgZGUgMjAxNSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBYICAgICAgICAgICAgICAgICAgICAgICAgICAzMC82MC85MAoKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBTb2xhciBTLkEuCgogICAgICAgICAgICAgICAgICAgICAgICAgODAwMDIyODMtMQoKICAgICAgICAgICAgICAgICAgICAgICAgIE5vbWJyZSBkZSBsYSBjYWxsZSBjb24gc3UgbnVtZXJvICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICs1OTUgKDk3MSkgOTM4LTMzNwoKCgoKICAgICAgICAgICAgIDEsMzMzICBTZXJ2aWNpb3MgcHJlc3RhZG9zIGVuIGVsIG1lcyBkZSBvY3R1YnJlIGRlbCAyMDEyICAgICAgICAgICAgICAgICAgICAgICAgICAyMDAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIDI2NwoKICAgICAgICAgICAgIDEsMjAwICBTZXJ2aWNpb3MgcHJlc3RhZG9zIGVuIGVsIG1lcyBkZSBub3ZpZW1icmUgZGVsIDIwMTIgICAgICAgICAgICAgICAgICAyLjAwMC4wMDAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIDIuNDAwLjAwMAogICAgICAgICAgICAgICAgICAgIFNlcnZpY2lvcyBwcmVzdGFkb3MgZW4gZWwgbWVzIGRlIG5vdmllbWJyZSBkZWwgMjAxMgogICAgICAgICAgICAgICAgICAgIFNlcnZpY2lvcyBwcmVzdGFkb3MgZW4gZWwgbWVzIGRlIG5vdmllbWJyZSBkZWwgMjAxMgogICAgICAgICAgICAgICAgICAgIFNlcnZpY2lvcyBwcmVzdGFkb3MgZW4gZWwgbWVzIGRlIG5vdmllbWJyZSBkZWwgMjAxMgoKICAgICAgICAgICAgIDAsNzUwICBTZXJ2aWNpb3MgcHJlc3RhZG9zIGVuIGVsIG1lcyBkZSBub3ZpZW1icmUgZGVsIDIwMTIgICAgICAgICAgICAgICAgICAyLjAwMC4wMDAgICAgICAgICAgICAgICAgICAgICAgICAgMS41MDAuMDAwCgogICAgICAgICAgICAgMCw1MTIgIFNlcnZpY2lvcyBwcmVzdGFkb3MgZW4gZWwgbWVzIGRlIG5vdmllbWJyZSBkZWwgMjAxMiAgICAgICAgICAgICAgICAgIDIuMDAwLjAwMCAgICAgICAxLjAyNC4wMDAKCgoKCgoKCgoKCgoKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgMS4wMjQuMDAwICAgICAgICAgMS41MDAuMDAwICAgICAgICAgICAgIDIuNDAwLjI2NwobMx4KICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgY3VhdHJvIG1pbGxvbmVzIG5vdmVjaWVudG9zIHZlaW50aWN1YXRybyBtaWwgZG9zY2llbnRvcyBzZXNlbnRhIHkgc2VpcwogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgNC45MjQuMjY3CgoKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgNzEuNDI5ICAgICAgICAgICAgICAgICAgICAgICAgIDIxOC4yMDYgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgMjg5LjYzNQo=";
//        printer.setText(base64);
//        printer.setPrinter("TM-U325D");
//        PrinterUtility.getInstance().print(printer);
        log.info("Server started");
        Options options = new Options();

        options.addOption("d", true, "dev.properties file");
        options.addOption("c", true, "config.properties file");
        options.addOption("p", true, "Path of dev and config fieles");

        CommandLineParser parser = new DefaultParser();

        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("p") && !cmd.hasOption("c") && !cmd.hasOption("d")) {
            String path = cmd.getOptionValue("p");
            if (path.endsWith("//")) {
                Config.getInstanse(cmd.getOptionValue("p") + "config.properties");
                DevPrinter.getInstanse(cmd.getOptionValue("p") + "dev.properties");
            } else {
                Config.getInstanse(cmd.getOptionValue("p") + System.getProperty("file.separator") + "config.properties");
                DevPrinter.getInstanse(cmd.getOptionValue("p") + System.getProperty("file.separator") + "dev.properties");
            }
        } else {
            if (cmd.hasOption("d")) {
                DevPrinter.getInstanse(cmd.getOptionValue("d"));
            } else {
                DevPrinter.getInstanse("dev.properties");
            }

            if (cmd.hasOption("c")) {
                Config.getInstanse(cmd.getOptionValue("c"));
            } else {
                Config.getInstanse("config.properties");
            }
        }


        WSClient.getInstance().connect();
//        WSClient.getInstance().connect("ws://localhost:8080/api/ws?k=AUTH");

        PrinterScheduler.getInstance().startPing();
        PrinterScheduler.getInstance().startStatus();

        PathHandler root = new PathHandler();
        ServletContainer container = ServletContainer.Factory.newInstance();

        String bindAddress = System.getProperty("bind.address", Config.getInstanse().getAddress());

        SSLContext sslContext = createSSLContext(loadKeyStore("server.keystore"), loadKeyStore("server.truststore"));

        Undertow.Builder builder = Undertow.builder()
                .addHttpListener(7612, bindAddress)
//                .addHttpListener(80, bindAddress)
//                .addHttpsListener(443, bindAddress, sslContext)
                .addHttpsListener(7613, bindAddress, sslContext);
        Undertow server = builder.setHandler(root).build();
        server.start();

        ///
        DeploymentInfo diWS = new DeploymentInfo()
                .setClassLoader(Main.class.getClassLoader())
                .setContextPath("/api")
                .addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME,
                        new WebSocketDeploymentInfo()
                                .setBuffers(new DefaultByteBufferPool(true, 20, 10, 12, 0))
                                .addEndpoint(ServerEndpoint.class)
                )
                .setDeploymentName("WebSocket");

        DeploymentManager managerWS = container.addDeployment(diWS);
        managerWS.deploy();

        try {
            root.addPrefixPath(diWS.getContextPath(), managerWS.start());
            root.addPrefixPath("/", resource(new ClassPathResourceManager(Main.class.getClassLoader(), Main.class.getPackage())).addWelcomeFiles("index.html"));
//            root.addPrefixPath("/", resource(new PathResourceManager(
//                    Paths.get("/opt/zafiro/printer/src/main/java/py/com/zafiro/printer"), 100))
//                    .addWelcomeFiles("index.html")
//                    .setDirectoryListingEnabled(true));
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    private static KeyStore loadKeyStore(String name) throws Exception {
        String storeLoc = System.getProperty(name);
        final InputStream stream;
        if (storeLoc == null) {
            stream = Main.class.getResourceAsStream(name);
        } else {
            stream = Files.newInputStream(Paths.get(storeLoc));
        }

        try {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(stream, password(name));
            return loadedKeystore;
        } finally {
            IoUtils.safeClose(stream);
        }
    }

    static char[] password(String name) {
        String pw = System.getProperty(name + ".password");
        return pw != null ? pw.toCharArray() : STORE_PASSWORD;
    }

    private static SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore) throws Exception {
        KeyManager[] keyManagers;
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password("key"));
        keyManagers = keyManagerFactory.getKeyManagers();

        TrustManager[] trustManagers = null;
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);

        return sslContext;
    }

    /*
     * Agregado p/ detener el servicio, cuando el printer se ejecuta como servicio
     */
    public static void stop(String args[]) {
        System.exit(0);
        //Thread.currentThread().interrupt();
    }
}
