package py.com.zafiro.printer.ws.jobs;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Properties;

/**
 * Created by gcabello on 21/04/2026.
 */
public class PrinterScheduler {

    private static PrinterScheduler printerScheduler;

    private Scheduler scheduler;

    private PrinterScheduler() {
        try {
            StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
            Properties prop = new Properties();
            prop.put("org.quartz.threadPool.threadCount","2");
            schedulerFactory.initialize(prop);
            scheduler = schedulerFactory.getScheduler();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static PrinterScheduler getInstance() {
        if (printerScheduler == null) {
            printerScheduler = new PrinterScheduler();
        }
        return printerScheduler;
    }

//    public Scheduler getScheduler() {
//        return scheduler;
//    }

    public void startPing() {

        JobDetail job = JobBuilder.newJob(Ping.class)
                .withIdentity("ping")
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("5s")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(5).repeatForever()).build();
        scheduleJob(job, trigger);
    }

    public void stopPing() {
        try {
            scheduler.deleteJob(new JobKey("ping"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void startStatus() {
        JobDetail job = JobBuilder.newJob(Status.class)
                .withIdentity("status")
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("1s")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(1).repeatForever()).build();
        scheduleJob(job, trigger);
    }

    public void stopStatus() {
        try {
            scheduler.deleteJob(new JobKey("status"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void scheduleJob(JobDetail job, Trigger trigger) {
        try {
            scheduler.scheduleJob(job, trigger);
            if (!scheduler.isStarted()) {
                scheduler.start();
            }
        } catch (SchedulerException ex) {
            ex.printStackTrace();
        }
    }

}
