/*
 * $Id$
 */
package lia.util.net.copy.transport;

/**
 * 
 * Exception used to signal protcol exception at the FDT Application level
 * Usually this kind of Exception happen if there is a BUG inside FDT protocol
 * or someone is trying to hijack the established FDT Session
 * 
 * @author ramiro
 *
 */
public class FDTProcolException extends Exception {
    
    private static final long serialVersionUID = 4606073777542177510L;
    
    public FDTProcolException() {
        super();
    }
    
    public FDTProcolException(String message) {
        super(message);
    }
 
    public FDTProcolException(String message, Throwable cause) {
        super(message, cause);
    }

    public FDTProcolException(Throwable cause) {
        super(cause);
    }

}
