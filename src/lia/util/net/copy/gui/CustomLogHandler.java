package lia.util.net.copy.gui;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * A custom class designed to handle the logging messages to be also outputed to the StatusBar of the gui
 *
 * @author cipsm
 */
public class CustomLogHandler extends Handler {

    private final StatusBar status;
    private final MyCustomFormatter formatter;

    public CustomLogHandler(final StatusBar status) {
        super();
        this.status = status;
        formatter = new MyCustomFormatter();
    }

    private void setStatus(String status) {
        if (this.status == null) return;
        this.status.addText(status);
    }

    /* (non-API documentation)
     * @see java.util.logging.Handler#publish(java.util.logging.LogRecord)
     */
    public void publish(LogRecord record) {
        // ensure that this log record should be logged by this Handler
        if (!isLoggable(record))
            return;
        setStatus(formatter.format(record));
    }

    /* (non-API documentation)
     * @see java.util.logging.Handler#flush()
     */
    public void flush() {
    }

    /* (non-API documentation)
     * @see java.util.logging.Handler#close()
     */
    public void close() throws SecurityException {
    }

    class MyCustomFormatter extends Formatter {

        public MyCustomFormatter() {
            super();
        }

        public String format(LogRecord record) {

            // Create a StringBuffer to contain the formatted record
            // start with the date.
            StringBuffer sb = new StringBuffer();

            sb.append("<font color=#ff0000>");

            // Get the date from the LogRecord and add it to the buffer
            Date date = new Date(record.getMillis());
            sb.append(date.toString());
            sb.append(" ");

            // get the name of the class
            sb.append(record.getSourceClassName());
            sb.append(" ");

            // get the name of the method
            sb.append(record.getSourceMethodName().replace("<", "[").replace(">", "]"));
            sb.append("\n");

            // Get the level name and add it to the buffer
            sb.append(record.getLevel().getName());
            sb.append(": ");

            // Get the formatted message (includes localization
            // and substitution of paramters) and add it to the buffer
            sb.append(formatMessage(record).replace("<", "[").replace(">", "]"));
            sb.append("\n");

            sb.append("</font>");

            return sb.toString();
        }
    }

} // end of class CustomLogHandler
