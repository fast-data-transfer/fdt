
package lia.util.net.copy.filters;

import javax.security.auth.Subject;


public interface Postprocessor {
    public void postProcessFileList(ProcessorInfo processorInfo, Subject peerSubject, Throwable downCause, String downMessage) throws Exception;
}
