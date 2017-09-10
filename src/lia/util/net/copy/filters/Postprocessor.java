/*
 * $Id$
 */
package lia.util.net.copy.filters;

import javax.security.auth.Subject;

/**
 * Base class used to implement post filters plugins in FDT. The <code>Postprocessor</code>
 * is called after a FDT session finishes.
 *
 * @author ramiro
 */
public interface Postprocessor {
    public void postProcessFileList(ProcessorInfo processorInfo, Subject peerSubject, Throwable downCause, String downMessage) throws Exception;
}
