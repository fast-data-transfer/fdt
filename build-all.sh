if command -v yum > /dev/null 2>&1; then
    OS="Centos"
    CMD=yum
elif command -v zypper > /dev/null 2>&1; then
    OS="OpenSuse"
    CMD=zypper
else
    OS="Debian"
    CMD=apt-get
fi
if command -v mvn > /dev/null 2>&1; then
    echo "Maven already installed "
    echo "Detected OS $OS, using command $CMD"
else
    echo "Maven will be installed "
    sudo $CMD install maven
fi
echo "Addling libraries to local maven repository"
./add-lib-to-local-maven.sh


mvn clean install

