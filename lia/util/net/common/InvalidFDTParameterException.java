
package lia.util.net.common;


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
