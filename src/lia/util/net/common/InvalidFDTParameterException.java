/*
 * $Id: InvalidFDTParameterException.java 346 2007-08-16 13:48:25Z ramiro $
 */
package lia.util.net.common;

/**
 * 
 * Used to signal various exception/errors related to FDT config/params
 *  
 * @author ramiro
 */
public class InvalidFDTParameterException extends Exception {

	private static final long serialVersionUID = -4780995072523010199L;
	public InvalidFDTParameterException() {
        super();
    }
    
    public InvalidFDTParameterException(String message) {
        super(message);
    }
 
    public InvalidFDTParameterException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFDTParameterException(Throwable cause) {
        super(cause);
    }
}
