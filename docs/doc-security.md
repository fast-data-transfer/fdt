[[Home](index.md)]   [Documentation]  [[Performance Tests](perf-disk-to-disk.md)]

[[FDT & DDCopy](doc-fdt-ddcopy.md)]   [[Examples](doc-examples.md)]   [Security]   [[User's Extensions](doc-user-extensions.md)]    [[System Tuning](doc-system-tuning.md)]

### FDT Security
FDT provides several security schemes to allow sending and receiving files over public networks.
The FDT architecture allows to "plug-in" external security APIs and to use them for client authentication and authorization. The current version supports:

* SSH channels

* GSI-SSH [ NGS GSI-SSHTerm: http://www.grid-support.ac.uk/content/view/81/62/ ]

* Globus-GSI [CoG JGlobus: http://dev.globus.org/wiki/CoG_JGlobus_1.4 ]

`Please note that FDT distribution does not include these security packages. The user should download the libraries for preferred API from its source. See below the instructions to install different security libraries.`

There are four security modes that one can set when transferring files with FDT:
##### **1. Source IP address filtering**

In this mode the server activates a simple IP-based firewall where each source IP is checked against the list of allowed IPs. In this mode no user authentication is done.

By default FDT starts allowing clients from any destination.

To enable this mode, pass the "-f" option when starting FDT server:
-f <allowedIPsList> , where allowedIPsList: A list of IP addresses allowed to connect to the server. 
Multiple IP addresses may be separated by ':'.
You can use CIDR notation to specify an entire subnet.
    
`However, please note that this mode does not enable any privacy or confidentiality on client-server control channel and it may be subject to source IP spoofing.`

IP filtering can be used together with other authentication schemes.

##### **2. Using SSH channels to securely start remote FDT client/server**
This mode is enabled when you use "SCP syntax" to transfer files with FDT. In this mode the local client starts on-the-fly an instance of FDT server, using an SSH connection to pass the start-up command to the remote machine. It is required the server system runs a ssh demon and the user has a valid shell account. The FDT server will accept data connections from only this client and will exit when the transfer finishes.
When both the source and destination are remote (i.e. the client uses a third-party machine to initiate the transfer) a different SSH connection is made for the client and server in order to start them on the specified machines. (the remote hosts should already have running an OpenSSH compatible SSH server).
During the transfer, the control channels with the remote hosts are kept open and the status messages are streamed back to the user console.

Example:
Using local FDT client to transfer files to/from remote hosts:

```
fdt /path/to/file1 user@hostname:/path/to/file2
```

```
fdt user@hostname:/path/to/file1 /path/to/file2
```

3rd party transfers (start both FDT client and server remotely):

```
fdt user1@hostname1:/path/to/file1 user2@hostname2:/path/to/file2
```

##### **3. GSI-SSH mode**

In this mode the FDT client can use the local GRID security credentials (i.e. proxy certificate) to authenticate to a remote GSI-extended SSH server.

`N.B. In this case, the authentication and authorization is deferred to the GSI-OpenSSH server, which means that any user allowed to connect to this server is also allowed to start FDT client/server.`

The hosts involved in the transfer have to fulfil the following requirements:
* the remote hosts need to have installed a GSI-Enabled OpenSSH server;
This is usually distributed in the current major grid-middleware software : VDT,gLITE.
See http://grid.ncsa.uiuc.edu/ssh/ for more details on how this server can be manually installed and configured.
* the machine running the FDT client needs to have an Grid-UI interface loaded:
The proxy certificate used to authenticate to the remote GSI-SSH server is searched in the following order:
    1. in the path specified by the X509_USER_PROXY environment variable
    2. in the default /tmp/x509up_u<uid> location

* there are additional libraries that need to be appended to FDT client CLASSPATH:
1. Download FDT :

```
[~/fdt]> wget http://monalisa.cern.ch/FDT/lib/fdt.jar
```

2. Download gsi-sshterm libs:

```
[~/fdt]> wget http://www.grid-support.ac.uk/files/gsissh/GSI-SSHTerm-0.79.zip
[~/fdt]> unzip GSI-SSHTerm-0.79.zip
```

3. Set the CLASSPATH

```
[~/fdt]>export GSISSHLIBS=`find GSI-SSHTerm-0.79/lib/ -name "*.jar" -printf "$PWD/%p:"`
[~/fdt]>export CLASSPATH=$PWD/fdt.jar:$GSISSHLIBS
```

4. Set FDT command alias:

```
[~/fdt]> alias fdt="java lia.util.net.copy.FDT"
```

This mode is similar to the previous one in the way the remote FDT instances are started.
You have to pass **-gsissh** option to instruct FDT to use Grid credentials.

Example:
Using local FDT client to transfer files to/from remote hosts:

```
fdt -gssish /path/to/file1 user@hostname:/path/to/file2
fdt -gsissh user@hostname:/path/to/file1 /path/to/file2
```

3rd party transfers (start both FDT client and server remotely):

```
fdt -gsissh user1@host1:/path/to/file1 user2@host2:/path/to/file2
```

##### **4. GSI-enabled FDT server**

This mode offers a more flexible way to authenticate and authorize users in Grid environments. The control channel between FDT clients and FDT server is secured using Globus GSI API. Mutual authentication is performed between FDT clients and servers.
To explicitly set this mode you have to download the Globus JGlobus libraries (see below) , set the CLASSPATH variable accordingly and pass the -gsi parameter when starting the FDT clients and FDT server

**Server side:**
The FDT server have to be started using a pair of X509 public/private keys. The search path for these files is:
* X509_SERVICE_CERT and X509_SERVICE_KEY properties passed to the java virtual machine
* X509_HOST_CERT and X509_HOST_KEY environment variables
* /etc/grid-security/hostcert.pem and /etc/grid-security/hostkey.pem files

`Note:
It is highly recommended to start the FDT server using an unprivileged account. Usually the host certificates are read protected from unprivileged accounts. In this case you should consider running the FDT server with different service private/public key files.`

The clients connecting to the server are authenticated using the current environment setup on the server side: CAs certificates, CAs certificate revocation lists directory:
    **default location:** /etc/grid-security/certificates
    override with X509_CERT_DIR environment variable
By default, the authorization of users is based on grid-mapfile file available in the current Globus installation:
    **default** /etc/grid-security/grid-mapfile or 
   Override with GRIDMAP java property or environment variable
Other authorization modules may be plugged-in in the FDT server by specifying : -Dgsi.authz.Authorization=customAuthzPluginClass

**Client side:**

The machine running the FDT client needs to have an Grid-UI environment loaded:
The proxy certificate used to authenticate to the remote GSI-enabled FDT server is searched in the following paths:
in the path specified by the X509_USER_PROXY environment variable

in the default /tmp/x509up_u<uid> location

###### **4.1. Setup client and server environment**

1. Download Globus GSI (both client and server):

```
[~/fdt]> wget http://www-unix.globus.org/cog/distribution/1.4/cog-jglobus-1.4-bin.tar.gz
[~/fdt]> tar -xzvf cog-jglobus-1.4-bin.tar.gz
```

2. Setup CLASSPATH

```
[~/fdt]> export JGSILIBS=`find cog-jglobus-1.4/lib/ -name "*.jar" -printf "$PWD/%p:"`
[~/fdt]> export CLASSPATH=$PWD/fdt.jar:$JGSILIBS
```

###### **4.2. Start FDT server**

Start FDT server using /home/fdt/fdtcert.pem and/home/fdt/fdtkey.pem credentials:

```
[~/fdt]> java -DX509_SERVICE_KEY=/home/fdt/fdtkey.pem -DX509_SERVICE_CERT=/home/fdt/fdtcert.pem lia.util.net.copy.FDT -gsi [server_options]
```

The server is using the X509_CERT_DIR environment variable as CAs certificates and CRLs location and default /etc/grid-security/grid-mapfile file to authorize users.

###### **4.3. Start FDT client:**

```
[~/fdt]> java lia.util.net.copy.FDT -gsi [client_options]
```
