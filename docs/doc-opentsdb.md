[[Home](index.md)]   [Documentation]  [[Performance Tests](perf-disk-to-disk.md)]

[[FDT & DDCopy](doc-fdt-ddcopy.md)]   [[Examples](doc-examples.md)]   [[Security](doc-security.md)]   [[User's Extensions](doc-user-extensions.md)]    [[System Tuning](doc-system-tuning.md)]    [ FDT Monitoring ]

### FDT Monitoring
FDT provides self monitoring possibility. FDT can send metrics to the OpenTSDB server and these metrics can be used to draw FDT dashboard using Grafana tool.

In order to start monitoring and sending metrics to OpenTSDB server user has to specify OpenTSDB server address and port number for FDT using commandline argument *-opentsdb*
Optional parameter is *-fdtTAG* which allows user to specify cistom tag for all metrics from that specific FDT.
If user is using proxy server then additional Java arguments has to be passed to provide proxy host and proxy port. 
At this moment no authentication is implemented for proxy and OpenTSDB.

#### Examples

*Monitor net test metrics to specified OpenTSDB server:*

SERVER2
```
java -jar fdt.jar -opentsdb <opentsdb-ip:port> -nettest
```
SERVER1
```
java -jar fdt.jar -opentsdb <opentsdb-ip:port> -nettest -c $SERVER2
```
*Monitor net test metrics to specified OpenTSDB server with specific tag:*
SERVER2
```
java -jar fdt.jar -opentsdb <opentsdb-ip:port> -nettest -fdtTAG <tag>
```
SERVER1
```
java -jar fdt.jar -opentsdb <opentsdb-ip:port> -nettest -c $SERVER2 -fdtTAG <tag>
```
*Monitor net test metrics to specified OpenTSDB server and using http proxy server:*
SERVER2
```
java -Dhttp.proxyHost=<host> -Dhttp.proxyPort=<port> -jar fdt.jar -opentsdb <opentsdb-ip:port> -nettest
```
SERVER1
```
java -Dhttp.proxyHost=<host> -Dhttp.proxyPort=<port> -jar fdt.jar -opentsdb <opentsdb-ip:port> -nettest -c $SERVER2
```
