/*
 * $Id$
 */
package lia.gsi.net;

import lia.gsi.authz.LocalMappingAuthorization;

import javax.security.auth.Subject;
import java.net.Socket;

/**
 * Extended Socket with an optional Subject attribute. The Subject field may contain additional Principal such as: GridID,UserLocalPrincipal
 *
 * @author Adrian Muraru
 */
public class Peer {
    private final Socket socket;
    private final LocalMappingAuthorization authorization;

    public Peer(Socket socket, LocalMappingAuthorization authz) {
        this.socket = socket;
        this.authorization = authz;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public Subject getPeerSubject() {
        return this.authorization == null ? null : this.authorization.getPeerSubject();
    }

}