# Adding lib directory contents to a local maven repository then later we could use them in fdt building
# Maven should be installed already on machine (i.e. use sudo apt-get install maven)

#Globus dependencies
mvn install:install-file -Dfile="lib/globus/gss-2.2.0.jar" -DgroupId=org.globus.gsi.gssapi -DartifactId=gss -Dversion=2.2.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/globus/io-2.2.0.jar" -DgroupId=org.globus.io -DartifactId=io -Dversion=2.2.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/globus/jsse-2.2.0.jar" -DgroupId=org.globus.jsse -DartifactId=jsse -Dversion=2.2.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/globus/myproxy-2.2.0.jar" -DgroupId=org.globus.myproxy -DartifactId=myproxy -Dversion=2.2.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/globus/ssl-proxies-2.2.0.jar" -DgroupId=org.globus -DartifactId=ssl-proxies -Dversion=2.2.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/globus/gridftp-2.2.0.jar" -DgroupId=org.globus -DartifactId=gridftp -Dversion=2.2.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/globus/gram-2.2.0.jar" -DgroupId=org.globus -DartifactId=gram -Dversion=2.2.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/globus/axisg-2.2.0.jar" -DgroupId=org.globus -DartifactId=axisg -Dversion=2.2.0 -Dpackaging=jar
# END of Globus dependencies

#SSH Tools dependencies
mvn install:install-file -Dfile="lib/gsi-sshterm/TransferAPIClient.jar" -DgroupId=org.globusonline -DartifactId=TransferAPIClient -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/gsi-sshterm/SSHVnc.jar" -DgroupId=com.sshtools.sshvnc -DartifactId=SSHVnc -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/gsi-sshterm/SSHTerm-1.0.0.jar" -DgroupId=com.sshtools.sshterm -DartifactId=SSHTerm -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/gsi-sshterm/ShiFT.jar" -DgroupId=com.sshtools.shift -DartifactId=ShiFT -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/gsi-sshterm/putty-pk-1.1.0.jar" -DgroupId=com.sshtools.ext -DartifactId=putty-pk -Dversion=1.1.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/gsi-sshterm/SecureTunneling.jar" -DgroupId=com.sshtools.tunnel -DartifactId=SecureTunneling -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/gsi-sshterm/not-yet-commons-ssl-0.3.11.jar" -DgroupId=org.apache.commons -DartifactId=not-yet-commons-ssl -Dversion=0.3.11 -Dpackaging=jar
mvn install:install-file -Dfile="lib/gsi-sshterm/ncsa-lcrypto-146.jar" -DgroupId=edu.illinois.ncsa -DartifactId=ncsa-lcrypto -Dversion=1.4.6 -Dpackaging=jar
mvn install:install-file -Dfile="lib/gsi-sshterm/libbrowser.jar" -DgroupId=uk.ac.rl.esc.browser -DartifactId=libbrowser -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/gsi-sshterm/jlirc-unix-soc.jar" -DgroupId=org.lirc.socket -DartifactId=jlirc-unix-soc -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/gsi-sshterm/j2ssh-core-0.2.7.jar" -DgroupId=com.sshtools.core -DartifactId=j2ssh-core -Dversion=0.2.7 -Dpackaging=jar
mvn install:install-file -Dfile="lib/gsi-sshterm/j2ssh-common-0.2.7.jar" -DgroupId=com.sshtools.common -DartifactId=j2ssh-common -Dversion=0.2.7 -Dpackaging=jar
mvn install:install-file -Dfile="lib/gsi-sshterm/filedrop.jar" -DgroupId=net.iharder.dnd -DartifactId=filedrop -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/gsi-sshterm/commons-compress-1.2.jar" -DgroupId=org.apache.commons -DartifactId=commons-compress -Dversion=1.2.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/gsi-sshterm/bcprov-jdk15on-1.50.jar" -DgroupId=org.bouncycastle -DartifactId=bcprov -Dversion=1.50.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/gsi-sshterm/BCGSS.jar" -DgroupId=edu.illinois.ncsa -DartifactId=BCGSS -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile="lib/gsi-sshterm/swing-layout-1.0.3.jar" -DgroupId=org.jdesktop -DartifactId=swing-layout -Dversion=1.0.3 -Dpackaging=jar
#mvn install:install-file -Dfile="bbbb" -DgroupId=aaaaa -DartifactId=BCGSS -Dversion=1.0.0 -Dpackaging=jar

echo "FINISHED INSTALLING LOCAL DEPENDENCIES"