package py.com.zafiro.printer.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by gcabello on 21/04/2026.
 */
public class Config {

    private static final String ADDRESS = "address";
    private static final String DEFAULT_ADDRESS = "0.0.0.0";
    private static final String SERVER = "server";
    private static final String DEFAULT_SERVER = "wss://kava.com.py/api/printer";
    private static final String PING = "ping";
    private static final String DEFAULT_PING = "kava.com.py";
    private static final String TOKEN = "token";
    private static final String DEFAULT_TOKEN = "AUTH";
    private static final String DEV_PATH = "devpath";
    private static final String DEFAULT_DEV_PATH = "/dev/lp/by-id";
    private static final String CPU_INFO = "cpuinfo";
    private static final String DEFAULT_CPU_INFO = "/proc/cpuinfo";
    private static final String PASSWORD = "password";
    private static final String DEFAULT_PASSWORD = "admin";
    private static Config instanse;
    private String filaName;


    private Config(String filaName) {
        this.filaName = filaName;
    }

    public static Config getInstanse() {
        if (instanse == null) {
            instanse = new Config("config.properties");
        }
        return instanse;
    }

    public static Config getInstanse(String filaName) {
        if (instanse == null) {
            instanse = new Config(filaName);
        }
        return instanse;
    }


    public String getAddress() {
        if (getInit().containsKey(ADDRESS)) {
            return getInit().getProperty(ADDRESS);
        } else {
            return DEFAULT_ADDRESS;
        }
    }


    public String getServer() {
        if (getInit().containsKey(SERVER)) {
            return getInit().getProperty(SERVER);
        } else {
            return DEFAULT_SERVER;
        }
    }

    public String getPingHostName() {
        if (getInit().containsKey(PING)) {
            return getInit().getProperty(PING);
        } else {
            return DEFAULT_PING;
        }
    }

    public String getToken() {
        if (getInit().containsKey(TOKEN)) {
            return getInit().getProperty(TOKEN);
        } else {
            return DEFAULT_TOKEN;
        }
    }

    public String getPassword() {
        if (getInit().containsKey(PASSWORD)) {
            return getInit().getProperty(PASSWORD);
        } else {
            return DEFAULT_PASSWORD;
        }
    }

    public String getDevPath() {
        if (getInit().containsKey(DEV_PATH)) {
            return getInit().getProperty(DEV_PATH);
        } else {
            return DEFAULT_DEV_PATH;
        }
    }


    public String getSerial() {
        String file;
        if (getInit().containsKey(CPU_INFO)) {
            file = getInit().getProperty(CPU_INFO);
        } else {
            file = DEFAULT_CPU_INFO;
        }
        try {
            Stream<String> stream = Files.lines(Paths.get(file));
            List<String> inf;
            inf = stream
                    .filter(line -> line.startsWith("Serial"))
                    .collect(Collectors.toList());
            if (inf.size() > 0) {
                return inf.get(0).split(":")[1].trim();
            } else {
                return "";
            }

        } catch (IOException e) {
            return "";
        }
    }

    private Properties getInit() {
        Properties prop = new Properties();
        InputStream is = null;
        try {
            File f = new File(filaName);
            is = new FileInputStream(f);
        } catch (Exception e) {
            is = null;
        }
        try {
            if (is == null) {
                is = Config.class.getResourceAsStream(filaName);
            }
            if (is == null) {
                saveDefault();
            }
            prop.load(is);
        } catch (Exception e) {

        }
        return prop;
    }

    private void save(Properties prop) {
        try {
            File f = new File(filaName);
            OutputStream out = new FileOutputStream(f);
            prop.store(out, "Config");
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveDefault() {
        Properties prop = new Properties();
        prop.put(SERVER, DEFAULT_SERVER);
        prop.put(TOKEN, DEFAULT_TOKEN);
        prop.put(DEV_PATH, DEFAULT_DEV_PATH);
        prop.put(PING, DEFAULT_PING);
        save(prop);
    }
}
