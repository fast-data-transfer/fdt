package lia.util.net.copy.gui;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * A custom PrintStream that is designed to capture println and output it into the status panel as well
 * @author cipsm
 *
 */
public class CustomPrintStream extends PrintStream {

	private final StatusBar status;
	private final String color;
	
	public CustomPrintStream(final StatusBar status, OutputStream out, String color) {
		super(out);
		this.status = status;
		this.color = color;
	}
	
	private void setStatus(String status) {
    	if (this.status == null) return;
    	status = status.replace("<", "[").replace(">", "]");
   		this.status.addText("<font color="+color+">"+status+"</font>");
    }
	
	public void println(String string) {
		super.print(string.toCharArray());
		setStatus(string);
		print("\n");
	}
	
	public void println() {
		super.println();
		setStatus("\n");
	}
	
    public void print(String string) {
		super.print(string);
		setStatus(string);
    }
	
} // end of class CustomPrintStream

