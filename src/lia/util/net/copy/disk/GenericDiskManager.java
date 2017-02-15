/*
 * $Id$
 */
package lia.util.net.copy.disk;

import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import lia.util.net.common.AbstractFDTIOEntity;
import lia.util.net.copy.FDTSession;

/**
 * 
 * Master class for both Read/Write managers
 * 
 * @author ramiro
 * 
 */
abstract class GenericDiskManager extends AbstractFDTIOEntity {

    protected final SortedSet<FDTSession> sessions = Collections.synchronizedSortedSet(new TreeSet<FDTSession>());

    public boolean removeSession(FDTSession fdtSession, String downMessage, Throwable downCause) {
        if(sessions.remove(fdtSession)) {
            fdtSession.close(downMessage, downCause);
            return true;
        }
        
        return false;
    }
    
    public boolean addSession(FDTSession fdtSession) {
        return sessions.add(fdtSession);
    }
    
    public final int sessionsSize() {
        return sessions.size(); 
    }
    
    public Set<FDTSession> getSessions() {
        return sessions;
    }
    
    public long getSize() {
        return -1;
    }
}
