/*
 * $Id$
 */
package lia.gsi.authz;

import javax.security.auth.Subject;

import org.globus.gsi.gssapi.auth.Authorization;
import org.globus.gsi.gssapi.auth.AuthorizationException;
import org.globus.gsi.gssapi.jaas.GlobusPrincipal;
import org.globus.gsi.gssapi.JaasGssUtil;
import org.globus.gsi.gssapi.jaas.UserNamePrincipal;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;

/**
 * 
 * Extend org.globus.gsi.gssapi.auth.Authorization base-class to also provide the localID mapping of the client.<br>
 * This is based on PDP interface from GT4 but in the same time keep the backward-compatibily with  GT2 interfaces<br>
 * The default authorization plugin can be changed at runtime with other implementation of this class by passing: 
 * <i>-Dgsi.authz.Authorization=authzclass</i> property.<br>
 * By default the GridMapAuthorization is used
 * 
 * @author Adrian Muraru
 * @date 30.01.2007
 * 
 */
public abstract class  LocalMappingAuthorization extends Authorization {
	
	Subject peerSubject=null;
	/**
	 * Interface for authorization mechanisms on server side
	 * The authorization is performed once the connection was authenticated.
	 * @return: local userID mapping for the given peer (ussualy based on DN) or "null" is the user is not authorized
	 */
	public abstract String  getLocalID(GSSContext context, String host) ;
	
	public void authorize(GSSContext context, String host) throws AuthorizationException {
		
		String localID = this.getLocalID(context, host);
		if (localID==null){
			String srcName;
			try {
				srcName = context==null?"":context.getSrcName().toString();
			} catch (GSSException e) {
				srcName="";
			}
			throw new AuthorizationException("No local mapping for :"+srcName);
		}
		peerSubject =new Subject(); 
	    GlobusPrincipal nm;
		try {
			nm = JaasGssUtil.toGlobusPrincipal(context.getSrcName());
		} catch (GSSException e) {
			throw new AuthorizationException("Cannot get peer DN");
		}
	    peerSubject.getPrincipals().add(nm);
	    peerSubject.getPrincipals().add(new UserNamePrincipal(localID));
	}
	
	public Subject getPeerSubject() {
		return this.peerSubject;
	}
	
}
