package lia.gsi.ssh;

/**
 * Copyright (c) 2004, National Research Council of Canada All rights reserved. Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions: The above copyright
 * notice(s) and this licence appear in all copies of the Software or substantial portions of the Software, and that both the above copyright notice(s) and this license appear in
 * supporting documentation. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF THIRD PARTY RIGHTS. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS INCLUDED IN THIS NOTICE BE LIABLE FOR ANY CLAIM, OR
 * ANY DIRECT, INDIRECT, SPECIAL OR CONSEQUENTIAL DAMAGES, OR ANY DAMAGES WHATSOEVER (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWSOEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN AN ACTION OF CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OF THE SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. Except as contained in
 * this notice, the name of a copyright holder shall NOT be used in advertising or otherwise to promote the sale, use or other dealings in this Software without specific prior
 * written authorization. Title to copyright in this software and any associated documentation will at all times remain with copyright holders.
 */
// (Changes (c) CCLRC 2006)
// (Changes Adi.Muraru 2007 - avoid GUIs dependencies, and rely on environment to get the proxy location)

import com.sshtools.j2ssh.authentication.*;
import com.sshtools.j2ssh.io.ByteArrayReader;
import com.sshtools.j2ssh.io.ByteArrayWriter;
import com.sshtools.j2ssh.io.UnsignedInteger32;
import org.globus.common.CoGProperties;
import org.globus.gsi.CredentialException;
import org.globus.gsi.GSIConstants;
import org.globus.gsi.GlobusCredentialException;
import org.globus.gsi.X509Credential;
import org.globus.gsi.gssapi.GSSConstants;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.gsi.gssapi.GlobusGSSManagerImpl;
import org.globus.gsi.gssapi.auth.HostAuthorization;
import org.gridforum.jgss.ExtendedGSSContext;
import org.ietf.jgss.*;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GSIAuthenticationClient extends SshAuthenticationClient {

    private static Logger logger = Logger.getLogger(GSIAuthenticationClient.class.getName());

    GSSCredential gsscredential;

    public GSIAuthenticationClient() throws GSSException, IOException {

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

        String x509UserProxy = System.getProperty("X509_USER_PROXY");
        if (x509UserProxy == null) {
            x509UserProxy = System.getenv("X509_USER_PROXY");
            if (x509UserProxy != null)
                System.setProperty("X509_USER_PROXY", x509UserProxy);
        }
        if (x509UserProxy == null)
            x509UserProxy = CoGProperties.getDefault().getProxyFile();
        if (!new File(x509UserProxy).isFile())
            throw new IOException("User proxy certificate not found in environment");

        logger.info("Using proxy certificate:" + x509UserProxy);

        try {
            gsscredential = createUserCredential(x509UserProxy);
        } catch (GlobusCredentialException | CredentialException e) {
            throw new IOException("Could not load user proxy certificate from:" + x509UserProxy);
        }
        if (gsscredential == null) {
            throw new IOException("User credential not initialized !Could not load user proxy certificate. Check your environmen if you have X509_USER_CERT proxy set up");
        }
    }

    public static GSSCredential createUserCredential(String x509UserProxy) throws GlobusCredentialException, GSSException, CredentialException {
        if (x509UserProxy != null) {
            X509Credential gcred = new X509Credential(x509UserProxy);
            GSSCredential cred = new GlobusGSSCredentialImpl(gcred, GSSCredential.INITIATE_ONLY);
            return cred;
        }
        X509Credential gcred = X509Credential.getDefaultCredential();
        GSSCredential cred = new GlobusGSSCredentialImpl(gcred, GSSCredential.INITIATE_ONLY);
        return cred;

    }

    public final String getMethodName() {
        return "gssapi";
    }

    public void reset() {
    }

    public void authenticate(AuthenticationProtocolClient authenticationprotocolclient, String s) throws IOException, TerminatedStateException {
        try {
            logger.finest("Registering gss-ssh return messages.");
            authenticationprotocolclient.registerMessage(com.sshtools.j2ssh.authentication.SshMsgUserauthGssapiResponse.class, 60);
            authenticationprotocolclient.registerMessage(com.sshtools.j2ssh.authentication.SshMsgUserauthGssapiToken.class, 61);
            authenticationprotocolclient.registerMessage(com.sshtools.j2ssh.authentication.SshMsgUserauthGssapiError.class, 64);
            authenticationprotocolclient.registerMessage(com.sshtools.j2ssh.authentication.SshMsgUserauthGssapiErrtok.class, 65);
            logger.finest("Sending gssapi user auth request.");
            ByteArrayWriter bytearraywriter = new ByteArrayWriter();
            bytearraywriter.writeUINT32(new UnsignedInteger32(1L));
            byte abyte0[] = GSSConstants.MECH_OID.getDER();
            bytearraywriter.writeBinaryString(abyte0);
            logger.finest("Username:" + getUsername());
            SshMsgUserAuthRequest sshmsguserauthrequest = new SshMsgUserAuthRequest(getUsername(), s, "gssapi", bytearraywriter.toByteArray());
            authenticationprotocolclient.sendMessage(sshmsguserauthrequest);
            logger.finest("Receiving user auth response:");
            SshMsgUserauthGssapiResponse sshmsguserauthgssapiresponse = (SshMsgUserauthGssapiResponse) authenticationprotocolclient.readMessage(60);
            ByteArrayReader bytearrayreader = new ByteArrayReader(sshmsguserauthgssapiresponse.getRequestData());
            byte abyte1[] = bytearrayreader.readBinaryString();
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "Mechanism requested: " + GSSConstants.MECH_OID);
                logger.log(Level.FINEST, "Mechanism selected: " + new Oid(abyte1));
                logger.log(Level.FINEST, "Verify that selected mechanism is GSSAPI.");
            }
            if (!GSSConstants.MECH_OID.equals(new Oid(abyte1))) {
                logger.warning("Mechanism do not match!");
                throw new IOException("Mechanism do not match!");
            }
            logger.finest("Creating GSS context base on grid credentials.");
            GlobusGSSManagerImpl globusgssmanagerimpl = new GlobusGSSManagerImpl();

            HostAuthorization gssAuth = new HostAuthorization(null);
            GSSName targetName = gssAuth.getExpectedName(null, hostname);

            GSSContext gsscontext = globusgssmanagerimpl.createContext(targetName, new Oid(abyte1), gsscredential, GSSCredential.INDEFINITE_LIFETIME - 1);
            gsscontext.requestCredDeleg(true);
            gsscontext.requestMutualAuth(true);
            gsscontext.requestReplayDet(true);
            gsscontext.requestSequenceDet(true);
            // MOD
            // gsscontext.requestConf(false);
            gsscontext.requestConf(true);

            Object type = GSIConstants.DELEGATION_TYPE_LIMITED;
            gsscontext.requestCredDeleg(false);
            ((ExtendedGSSContext) gsscontext).setOption(GSSConstants.DELEGATION_TYPE, type);

            logger.finest("Starting GSS token exchange.");
            byte abyte2[] = new byte[0];
            do {
                if (gsscontext.isEstablished())
                    break;
                byte abyte3[] = gsscontext.initSecContext(abyte2, 0, abyte2.length);
                if (abyte3 != null) {
                    ByteArrayWriter bytearraywriter1 = new ByteArrayWriter();
                    bytearraywriter1.writeBinaryString(abyte3);
                    SshMsgUserauthGssapiToken sshmsguserauthgssapitoken = new SshMsgUserauthGssapiToken(bytearraywriter1.toByteArray());
                    authenticationprotocolclient.sendMessage(sshmsguserauthgssapitoken);
                }
                if (!gsscontext.isEstablished()) {
                    SshMsgUserauthGssapiToken sshmsguserauthgssapitoken1 = (SshMsgUserauthGssapiToken) authenticationprotocolclient.readMessage(61);
                    ByteArrayReader bytearrayreader1 = new ByteArrayReader(sshmsguserauthgssapitoken1.getRequestData());
                    abyte2 = bytearrayreader1.readBinaryString();
                }
            } while (true);
            logger.log(Level.FINEST, "Sending gssapi exchange complete.");
            SshMsgUserauthGssapiExchangeComplete sshmsguserauthgssapiexchangecomplete = new SshMsgUserauthGssapiExchangeComplete();
            authenticationprotocolclient.sendMessage(sshmsguserauthgssapiexchangecomplete);
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "Context established.\nInitiator : " + gsscontext.getSrcName() + "\nAcceptor  : " + gsscontext.getTargName() + "\nLifetime  : "
                        + gsscontext.getLifetime() + "\nIntegrity   : " + gsscontext.getIntegState() + "\nConfidentiality   : " + gsscontext.getConfState() + "\nAnonymity : "
                        + gsscontext.getAnonymityState());
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Got Exception: ", t);
            throw new TerminatedStateException(AuthenticationProtocolState.FAILED);
        }
    }

    public Properties getPersistableProperties() {
        Properties properties = new Properties();
        return properties;
    }

    public void setPersistableProperties(Properties properties) {
    }

    public boolean canAuthenticate() {
        return true;
    }

}
