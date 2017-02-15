/*
 * Copyright 1999-2006 University of Chicago
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package lia.gsi.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import javax.security.auth.Subject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.common.CoGProperties;
import org.globus.gsi.GSIConstants;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.GlobusCredentialException;
import org.globus.gsi.gssapi.GSSConstants;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.gsi.gssapi.net.GssSocket;
import org.globus.gsi.gssapi.net.GssSocketFactory;
import org.globus.gsi.gssapi.net.impl.GSIGssSocket;
import org.globus.gsi.jaas.GlobusPrincipal;
import org.globus.gsi.jaas.JaasGssUtil;
import org.gridforum.jgss.ExtendedGSSContext;
import org.gridforum.jgss.ExtendedGSSManager;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;

// additional GSIGssSocketFactory that may be selected using
// -Dorg.globus.gsi.gssapi.net.provider
public class GSIGssSocketFactory extends GssSocketFactory {

	private static final int GSI_CONNECT_TIMEOUT = Integer.getInteger("GSI_CONNECT_TIMEOUT", 20000);

	private static final Log logger = LogFactory.getLog(GSIGssSocketFactory.class.getName());

	/**
	 * @param inetAddress :
	 *            the remote address
	 * @param port :
	 *            the remote port
	 * @param doDelegation :
	 *            if true, the client credential is delegated
	 * @param fullDelegation :
	 *            if doDelegation is set, this parameter specifies the type of delegation (FULL or LIMITED)
	 * @return the GSI-protected socket connected to the remote host
	 * @throws IOException
	 */
	public java.net.Socket createSocket(java.net.InetAddress inetAddress, int port, boolean doDelegation, boolean fullDelegation)
			throws IOException {
		// raw socket
		Socket socket = null;
		try {
			//override the search path for the CA directory: (GSI-SSHTERM writes in the ~/.globus/certificates so we don;t use this)
			//1) java X509_CERT_DIR property
			//2) override with X509_CERT_DIR env var
			//3) default /etc/grid-security/certificates
			String x509CertDir = System.getProperty("X509_CERT_DIR");
			if (x509CertDir == null) {
				x509CertDir = System.getenv("X509_CERT_DIR");
				if (x509CertDir == null)
					x509CertDir = "/etc/grid-security/certificates";
				System.setProperty("X509_CERT_DIR", x509CertDir);
			}

			String x509UserProxy = System.getenv("X509_USER_PROXY");
			if (x509UserProxy == null)
				x509UserProxy = CoGProperties.getDefault().getProxyFile();
			System.out.println("Trying " + x509UserProxy);
			GSSCredential credential = createUserCredential(x509UserProxy);
			if (credential == null) {
				throw new IOException("User credential not initialized !");
			}

			logger.info("createSocket() user credential is " + credential.getName());
			GSSManager manager = ExtendedGSSManager.getInstance();
			org.globus.gsi.gssapi.auth.GSSAuthorization gssAuth = org.globus.gsi.gssapi.auth.HostAuthorization.getInstance();
			GSSName targetName = gssAuth.getExpectedName(null, inetAddress.getCanonicalHostName());
			ExtendedGSSContext context = (ExtendedGSSContext) manager.createContext(targetName, GSSConstants.MECH_OID, credential,
					GSSContext.DEFAULT_LIFETIME);
			context.setOption(GSSConstants.GSS_MODE, GSIConstants.MODE_GSI);
			context.requestCredDeleg(doDelegation);
			if (doDelegation) {

				if (fullDelegation) {
					context.setOption(GSSConstants.DELEGATION_TYPE, GSIConstants.DELEGATION_TYPE_FULL);
				} else {
					context.setOption(GSSConstants.DELEGATION_TYPE, GSIConstants.DELEGATION_TYPE_LIMITED);
				}
			}

			SocketAddress socketAddress = new InetSocketAddress(inetAddress, port);
			socket = new Socket();
			socket.connect(socketAddress, GSI_CONNECT_TIMEOUT);
			GSIGssSocket gsiSocket = new GSIGssSocket(socket, context);
			gsiSocket.setUseClientMode(true);
			gsiSocket.setAuthorization(gssAuth);
			// Should be GSI_MODE ?
			gsiSocket.setWrapMode(GssSocket.SSL_MODE);
			gsiSocket.startHandshake();
			socket = gsiSocket;
		} catch (Throwable e) {
			if (socket != null) {
				try {
					socket.close();
				} catch (Exception e1) {
					logger.debug("Socket is already closed.");
				}
			}
			throw new IOException(e.toString());
		}
		// return the wrapped socket
		return socket;

	}

	/**
	 * Retrieve the Globus Subject from a GssSocket
	 * 
	 * @param socket
	 * @return javax.security.auth.Subject having a single Principal elementt : The Globus DN
	 * @throws GSSException
	 *             if the supplied socket is not a GssSocket or the Globus Credentials is not set on the socket
	 */
	public static Subject getLocalSubject(Socket socket) throws GSSException {

		if (!(socket instanceof GssSocket))
			throw new GSSException(GSSException.NO_CRED);

		GssSocket gssSocket;
		gssSocket = (GssSocket) socket;
		Subject mySubject = new Subject();
		GlobusPrincipal nm;
		try {
			nm = JaasGssUtil.toGlobusPrincipal(gssSocket.getContext().getSrcName());
		} catch (Throwable t) {
			throw new GSSException(GSSException.NO_CRED);
		}
		mySubject.getPrincipals().add(nm);
		return mySubject;
	}

	public Socket createSocket(Socket s, String host, int port, GSSContext context) {
		return new GSIGssSocket(s, context);
	}

	public Socket createSocket(String host, int port, GSSContext context) throws IOException {
		return new GSIGssSocket(host, port, context);
	}

	public static GSSCredential createUserCredential(String x509UserProxy) throws GlobusCredentialException, GSSException {
		if (x509UserProxy != null) {
			GlobusCredential gcred = new GlobusCredential(x509UserProxy);
			GSSCredential cred = new GlobusGSSCredentialImpl(gcred, GSSCredential.INITIATE_ONLY);
			return cred;
		}
		GlobusCredential gcred = GlobusCredential.getDefaultCredential();
		GSSCredential cred = new GlobusGSSCredentialImpl(gcred, GSSCredential.INITIATE_ONLY);
		return cred;

	}

	public static GSSCredential createUserCredential(String x509ServiceCert, String x509ServiceKey) throws GlobusCredentialException,
			GSSException {
		if (x509ServiceCert != null && x509ServiceKey != null) {
			GlobusCredential gcred = new GlobusCredential(x509ServiceCert, x509ServiceKey);
			GSSCredential cred = new GlobusGSSCredentialImpl(gcred, GSSCredential.INITIATE_ONLY);
			return cred;
		}

		GlobusCredential gcred = GlobusCredential.getDefaultCredential();
		GSSCredential cred = new GlobusGSSCredentialImpl(gcred, GSSCredential.INITIATE_ONLY);
		return cred;
	}

}
