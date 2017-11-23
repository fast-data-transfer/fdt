package lia.util.net.common;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author nlmills@g.clemson.edu
 */
public class NetloggerRecord {
    /**
     * the size of the data block read from the disk and posted to the network
     */
    private int block;
    /**
     * tcp buffer size (if 0 system defaults were used)
     */
    private int buffer;
    /**
     * the FTP rfc959 completion code of the transfer
     */
    private String code = "429";  // = "Connection closed; transfer aborted."
    /**
     * time the transfer completed
     */
    private Date completed = new Date();
    /**
     * the destination host
     */
    private InetAddress destination = Utils.getLoopbackAddress();
    /**
     * hostname of the server
     */
    private InetAddress host = Utils.getLoopbackAddress();
    /**
     * the total number of bytes transferred
     */
    private long nbytes;
    /**
     * time the transfer started
     */
    private Date start = completed;
    /**
     * the number of parallel TCP streams used in the transfer
     */
    private int streams;
    /**
     * the transfer type (RETR or STOR)
     */
    private String type = "RETR";

    public static String toULMDate(Date date) {
        // year month day hour minute second . millisecond
        SimpleDateFormat ulmFormat = new SimpleDateFormat("yyyyMMddHHmmss.SSS");
        // Date only has millisecond precision, so tack on zeros for microseconds
        String ulmDate = ulmFormat.format(date) + "000";

        return ulmDate;
    }

    public int getBuffer() {
        return buffer;
    }

    public void setBuffer(int buffer) {
        this.buffer = buffer;
    }

    public int getBlock() {
        return block;
    }

    public void setBlock(int block) {
        this.block = block;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Date getCompleted() {
        return completed;
    }

    public void setCompleted(Date completed) {
        this.completed = completed;
    }

    public InetAddress getDestination() {
        return destination;
    }

    public void setDestination(InetAddress destination) {
        this.destination = destination;
    }

    public InetAddress getHost() {
        return host;
    }

    public void setHost(InetAddress host) {
        this.host = host;
    }

    public long getNbytes() {
        return nbytes;
    }

    public void setNbytes(long nbytes) {
        this.nbytes = nbytes;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public int getStreams() {
        return streams;
    }

    public void setStreams(int streams) {
        this.streams = streams;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String toULMString() {
        String ulm = Utils.joinString(" ",
                "DATE=" + toULMDate(completed),
                "HOST=" + host.getHostName(),
                "PROG=fdt",
                //XXX what about NL.EVNT=FTP_INFO?
                "START=" + toULMDate(start),
                "BUFFER=" + buffer,
                "BLOCK=" + block,
                "NBYTES=" + nbytes,
                "STREAMS=" + streams,
                "DEST=[" + destination.getHostAddress() + "]",
                "TYPE=" + type,
                "CODE=" + code);

        return ulm;
    }

    @Override
    public String toString() {
        return toULMString();
    }
}
