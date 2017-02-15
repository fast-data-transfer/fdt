//----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Jul 19, 2005
 */
package lia.gsi.authz;

import java.io.IOException;

import org.globus.gsi.gssapi.auth.AuthorizationException;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;

public class GridMapAuthorization  extends LocalMappingAuthorization {

	public GridMapAuthorization() throws IOException {
		GridMap.getGridMap();
	}

	public String getLocalID(GSSContext context, String host) {
		try {
			String dn = context.getSrcName().toString();
			String name = GridMap.getGridMap().getUserID(dn);
			
			if (name == null) {
				throw new AuthorizationException("No local mapping for " + dn);
			}
			return name;
		} catch (IOException e) {
			return null;
		} catch (GSSException e) {
			return null;
		}
	}
	
	
}
