package py.com.zafiro.printer.ws.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.com.zafiro.printer.ws.WSClient;
import py.com.zafiro.printer.ws.WSServer;

/**
 * Created by gcabello on 21/04/2026.
 */
public class Ping implements Job {

    final static Logger log = LoggerFactory.getLogger(Ping.class.getName());

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (WSClient.getInstance().isOpen()) {
            WSClient.getInstance().ping();
        } else {
            // volver a conectar en caso de no estar conectado
            WSClient.getInstance().connect();
        }
        WSServer.getInstance().pingAll();
        try {
            this.finalize();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
