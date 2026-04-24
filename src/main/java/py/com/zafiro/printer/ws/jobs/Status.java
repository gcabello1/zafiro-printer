package py.com.zafiro.printer.ws.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.com.zafiro.printer.utils.DevPrinter;
import py.com.zafiro.printer.ws.Message;

import static py.com.zafiro.printer.ws.Message.*;

import py.com.zafiro.printer.ws.WSClient;
import py.com.zafiro.printer.ws.WSServer;

import jakarta.json.Json;
import java.util.Calendar;

/**
 * Created by gcabello on 21/04/2026.
 */
public class Status implements Job {

    final static Logger log = LoggerFactory.getLogger(Status.class.getName());

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (WSClient.getInstance().isOpen()) {
            if (DevPrinter.getInstanse().update()) {
                log.info("DevPrinter update");
                Message message = new Message(Json.createObjectBuilder()
                        .add(Field.ID, Calendar.getInstance().getTimeInMillis())
                        .add(Field.CMD, Command.STATUS)
                        .add(Field.ON_LINE, WSClient.getInstance().isOpen())
                        .add(Field.MESSAGE, Message.Status.OK)
                        .add(Field.STATUS, DevPrinter.getInstanse().getStatusMessage())
                        .build());
                WSClient.getInstance().send(message);
                WSServer.getInstance().sendToAll(message);
            }
        } else {
            if (DevPrinter.getInstanse().update()) {
                log.info("DevPrinter update");
                Message message = new Message(Json.createObjectBuilder()
                        .add(Field.ID, Calendar.getInstance().getTimeInMillis())
                        .add(Field.CMD, Command.STATUS)
                        .add(Field.ON_LINE, WSClient.getInstance().isOpen())
                        .add(Field.MESSAGE, Message.Status.OK)
                        .add(Field.STATUS, DevPrinter.getInstanse().getStatusMessage())
                        .build());
                WSServer.getInstance().sendToAll(message);
            }
        }
        try {
            this.finalize();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
