
package lia.util.net.copy.filters;

import javax.security.auth.Subject;


public interface Preprocessor {
    public void preProcessFileList(ProcessorInfo processorInfo, Subject peerSubject) throws Exception; 
}
