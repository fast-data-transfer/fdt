[[Home](index.md)]  [Documentation]  [[Performance Tests](perf-disk-to-disk.md)]

[[FDT & DDCopy](doc-fdt-ddcopy.md)]   [Examples]   [[Security](doc-security.md)]    [[User's Extensions](doc-user-extensions.md)]    [[System Tuning](doc-system-tuning.md)]

### Examples

1. To send one file called "local.data" from the local system
directory to another computer
in the "/home/remoteuser/destiantionDir" folder, with default
parameters, there are two options:

- Client/Server mode

First,the FDT server needs to be started on the remote system. ( The defaultsettings will be used, which implies the default port, 54321, on boththeclient and the server ). -S is used to disable the standalone mode,which means that the server will stop after the session will finish

```
[remote computer]$ java -jar fdt.jar -S
```

Then,the client will be started on the local system specifying the sourcefile, the remote address (or hostname) where the server was started inthe previous step and the destination directory
        
```
[local computer]$ java -jar fdt.jar -c <remote_address> -d /home/remoteuser/destinationDir /home/localuser/local.data
```

OR

```
[local computer]$ java -jar fdt.jar -c <remote_address> -d destinationDir ./local.data
```

- Secure Copy (SCP) mode

In this mode the server will be started on the remote systemautomatically by the local FDT client using SSH.

```
[local computer]$ java -jar fdt.jar /home/localuser/local.data remoteuser@<remote_address>:/home/remoteuser/destinationDir
```

OR

```
[local computer]$ java -jar fdt.jar ./local.data remoteuser@<remote_address>:destinationDir
```

If the remoteuser parameter is not specified the local user, runningthe fdt command, will beused to login on the remote system

2. To get the content of an entire folder and all its children,
located in the user's home directory, the -r ( recursive
mode ) flag will be specified and also -pull to sink the data from the
server. In the Client/Server mode the access to the server will be
restricted to the local IP addresses only ( with -f flag ).

- Client/Server mode

Multiple addresses may be specfied using the -f flag using ':'. If theclient's IP address(es) is not specified in the allowed IP addressesthe connection will be closed. In the following command the server isstarted in standalone mode, which means that will continue to run afterthe session will finish. The transfer rate for every client sessionswill be limited to 4 MBytes/s

```
[remote computer]$ java -jar fdt.jar -f allowedIP1:allowedIP2 -limit 4M
```

OR

```
[remote computer]$ java -jar fdt.jar -f allowedIP1:allowedIP2 -limit 4096K
```

The command for the local client will be.

```
[local computer]$ java -jar fdt.jar -pull -r -c <remote_address>-d /home/localuser/localDir /home/remoteuser/remoteDir
```

OR

```
[local computer]$ java -jar fdt.jar -pull -r -c <remote_address> -d localDir remoteDir
```

- SCP mode

In this mode only the order of the parameters will be changed, and -ris the only argument that must be added ( -pull is implicit ). Sameauthentication policies apply as in the first example

```
[local computer]$ java -jar fdt.jar -r  remoteuser@<remote_address>:/home/remoteuser/remoteDir /home/localuser/localDir
```

OR

```
[local computer]$ java -jar fdt.jar -r remoteuser@<remote_address>:remoteDir localDir
```

3. To test the network connectivity a transfer here is an example
which transfers data from /dev/zero to /dev/null using 10 streams in
blocking mode, for both the server and the client with 8 MBytes
buffers. The server will stop after the test is finished

- Client/Server mode

```
[remote computer]$ java -jar fdt.jar -bio -bs 8M -f allowedIP -S
```

```
[local computer]$ java -jar fdt.jar -c <remote_address> -bio -P 10 -d /dev/null /dev/zero
```

- SCP mode

```
[local computer]$ java -jar fdt.jar -bio -P 10 /dev/zero remoteAddress:/dev/null
```

4. The user can also define a list of files ( a filename per line )
to be transfered. FDT will detect if the files are located on multiple
devices and will use a dedicated thread for each device.

```
[remote computer]$ java -jar fdt.jar -S
```

```
[local computer]$ java -jar fdt.jar -fl ./file_list.txt -c <remote_address> -d /home/remoteuser/destDir
```

5. To test the local read/write performance of the local disk the
DDCopy may be used.

- The following command will copy the entire partition
/dev/dsk/c0d1p1 to /dev/null reporting every 2 seconds ( the default )
the I/O speed

```
[local computer]$ java -cp fdt.jar lia.util.net.common.DDCopy if=/dev/dsk/c0d1p1 of=/dev/null
```

- To test the write speed of the file system using a 1GB file
read from /dev/zero the following command may be used. The operating
system will sync() the data to the disk. The data will be read/write
using 10MB buffers

```
[local computer]$ java -cp fdt.jar lia.util.net.common.DDCopy  if=/dev/zero of=/home/user/1GBTestFile bs=10M count=100 flags=NOSYNC
```

OR

```
[local computer]$ java -cp fdt.jar lia.util.net.common.DDCopy  if=/dev/zero of=/home/user/1GBTestFile bs=1M bn=10 count=100 flags=NOSYNC
```

- Launching FDT as Agent example:

```
java -jar fdt.jar -tp <transfer,ports,separated,by,comma> -p <portNo> -agent
```

- Sending coordinator message to the agent:

```
java -jar fdt.jar -dIP <destination-ip> -dp <destination-port> -sIP <source-ip> -p <source-port> -d /tmp/destination/files -fl /tmp/file-list-on-source.txt -coord
```
- Retrieving session log file. 

To retrieve session log file user needs to provide at least these parameters:

```
java -jar fdt.jar  -c <source-host> -d /tmp/destination/files -sID <session-ID>
```

- To retrieve list of files on custom path there is a custom mode which can be used.

```
java -jar fdt.jar  -c <source-host> -ls /tmp/
```




