/*
 * $Id: Peer.java 621 2010-09-03 14:31:24Z ramiro $
 */
package lia.gsi.net;

import java.net.Socket;

import javax.security.auth.Subject;

import lia.gsi.authz.LocalMappingAuthorization;

/**
 * Extended Socket with an optional Subject attribute. The Subject field may contain additional Principal such as: GridID,UserLocalPrincipal
 * @author Adrian Muraru
 */
public class Peer {
	private final Socket socket;
	private final LocalMappingAuthorization authorization;
	
	public Peer(Socket socket, LocalMappingAuthorization authz) {
		this.socket=socket;
		this.authorization = authz;		
	}
	
	public Socket getSocket() {
		return this.socket;
	}
	
	public Subject  getPeerSubject() {
		return this.authorization==null? null: this.authorization.getPeerSubject();
	}
	
}