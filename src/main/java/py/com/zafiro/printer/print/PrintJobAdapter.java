package py.com.zafiro.printer.print;

import javax.print.event.PrintJobEvent;

/**
 * Created by gcabello on 21/04/2026.
 */
public class PrintJobAdapter extends javax.print.event.PrintJobAdapter {

    @Override
    public void printJobCompleted(PrintJobEvent e) {
//        System.out.println("Print job complete");
    }

    @Override
    public void printDataTransferCompleted(PrintJobEvent e) {
//        System.out.println("Document transfered to printer");
    }

    @Override
    public void printJobRequiresAttention(PrintJobEvent e) {
//        System.out.println("Print job requires attention");
//        System.out.println("Check printer: out of paper?");
    }

    @Override
    public void printJobFailed(PrintJobEvent e) {
//        System.out.println("Print job failed");

    }

    @Override
    public void printJobCanceled(PrintJobEvent e) {
//        System.out.println("Print Cancelado");

    }

    @Override
    public void printJobNoMoreEvents(PrintJobEvent e) {
//        System.out.println("No mas eventos de impresion.");
    }
}
