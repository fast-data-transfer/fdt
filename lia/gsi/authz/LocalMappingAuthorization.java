package lia.gsi.authz;

import javax.security.auth.Subject;

import org.globus.gsi.gssapi.auth.Authorization;
import org.globus.gsi.gssapi.auth.AuthorizationException;
import org.globus.gsi.jaas.GlobusPrincipal;
import org.globus.gsi.jaas.JaasGssUtil;
import org.globus.gsi.jaas.UserNamePrincipal;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;


public abstract class  LocalMappingAuthorization extends Authorization {
	
	Subject peerSubject=null;
	
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
