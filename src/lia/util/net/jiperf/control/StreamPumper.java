/*
 * $Id: StreamPumper.java 361 2007-08-16 14:55:41Z ramiro $
 */
package lia.util.net.jiperf.control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * 
 * This will be kept for history :). 
 * The entire package lia.util.net.jiperf is the very first version of FDT. It
 * started as an Iperf-like test for Java.
 * 
 * @author ramiro
 */
public class StreamPumper implements Runnable {

    private BufferedReader in;
    private StreamConsumer consumer = null;
    private PrintWriter out = new PrintWriter(System.out);
    private static final int SIZE = 1024;

    public StreamPumper(InputStream in, PrintWriter writer) {
        this(in);
        out = writer;
    }

    public StreamPumper(InputStream in) {
        this.in = new BufferedReader(new InputStreamReader(in), SIZE);
    }

    public StreamPumper(InputStream in, StreamConsumer consumer) {
        this(in);
        this.consumer = consumer;
    }

    public StreamPumper(InputStream in, PrintWriter writer,
                        StreamConsumer consumer) {
        this(in);
        this.out = writer;
        this.consumer = consumer;
    }

    public void run() {
        try {
            String s = in.readLine();
            while (s != null) {
                consumeLine(s);
                if (out != null) {
                    out.println(s);
		    out.println("<DONE>");
                    out.flush();
                }

                s = in.readLine();
            }
        } catch (IOException e) {
            // do nothing
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    public void flush() {
        out.flush();
    }

    public void close() {
        flush();
        out.close();
    }

    private void consumeLine(String line) {
        if (consumer != null) {
            consumer.consumeLine(line);
        }
    }
}
