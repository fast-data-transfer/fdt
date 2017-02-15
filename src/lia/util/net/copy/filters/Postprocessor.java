/*
 * $Id: Postprocessor.java 348 2007-08-16 14:02:52Z ramiro $
 */
package lia.util.net.copy.filters;

import javax.security.auth.Subject;

/**
 * 
 * Base class used to implement post filters plugins in FDT. The <code>Postprocessor</code>
 * is called after a FDT session finishes.
 * 
 * @author ramiro
 */
public interface Postprocessor {
    public void postProcessFileList(ProcessorInfo processorInfo, Subject peerSubject, Throwable downCause, String downMessage) throws Exception;
}
