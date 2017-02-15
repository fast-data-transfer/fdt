package lia.gsi.net;

import java.net.Socket;

import javax.security.auth.Subject;

import lia.gsi.authz.LocalMappingAuthorization;


public class Peer {
	private Socket socket=null;
	private  LocalMappingAuthorization authorization = null;
	
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