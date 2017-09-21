[[Home](index.md)]  [Documentation]  [[Performance Tests](perf-disk-to-disk.md)]

[[FDT & DDCopy](doc-fdt-ddcopy.md)]   [[Examples](doc-examples.md)]  [[Security](doc-security.md)]   [User's Extensions]    [[System Tuning](doc-system-tuning.md)]


### User's Extensions
FDT allows to load user defined classes for Pre and Post - Processing of file transfers.
This functionality can be used to easily interface FDT with mass storage systems and to implement any additional Access Control List (ACL) to the files transfered by FDT.

It can also be used for packing, compression or customized integrity check.

The user can define its own syntax for managing files on different MS systems and the implementation for the Pre/Post Processing interfaces allows the user to define the mechanism to perform local staging or to move the transfered files to a MS system after they are transfered by FDT.

The two procedures act as filters for the source and destination fields in the FDT syntax.

The list of files to be transfered, the destination directory and the GSI authentication are passed to the class implementing the PreProcessing interface. If the FDT is used without the GSI authentication the Subject will be a null parameter. Based on the user defined syntax, the implementation can initiate local staging for the files . This can be done in one or multiple threads. It can verify the credentials for the authenticated user to access the files. It can also be used to perform data compression on the files to be transfered.
In case the final destination for the files is a MS system on the remote site, the preProcessing implementation should change the destination with a temporary directory on the remote system. The naming scheme for it is used by the PostProcessing implementation to start moving the files to the MS system after the FDT transfer is done. The Post Processing implementation can also be used to verify the user's credentials to write into the MS system to uncompress data, or make an integrity check on the MS system. If the Pre/Post processing classes are used to interface FDT with a MS system, they should modify and act only on files using the user's defined syntax in the name. They should not modify the naming scheme for local files.

The preProcessing filters must implement **lia.util.net.copy.filters.Preprocessor** interface and the postProcessing filters must implement **lia.util.net.copy.filters.Postprocessor** interface. The functionality of these interfaces may be extended in the future.


**Preprocessor.java**
```
package lia.util.net.copy.filters;

import javax.security.auth.Subject;

public interface Preprocessor {
public void preProcessFileList(ProcessorInfo processorInfo, Subject peerSubject) throws Exception;
}
```

**Postprocessor.java**
```
package lia.util.net.copy.filters;

import javax.security.auth.Subject;

public interface Postprocessor {
public void postProcessFileList(ProcessorInfo processorInfo, Subject peerSubject) throws Exception;
}
```

**ProcessorInfo.java**
```
package lia.util.net.copy.filters;

public class ProcessorInfo {
public String[] fileList;
public String destinationDir;
    /**
     * @since 0.9.25
     */
    public InetAddress remoteAddress;
    /**
     * @since 0.9.25
     */
    public int remotePort;
    /**
     * @since 0.9.25
     */
    public boolean recursive;
    /**
     * Non-null on writer side <b>ONLY</b>.
     * Gives access to the transfer map of an FDT session.
     * Key - the final file name (including the destination directory) for a FileSession
     * Value - the FileSession
     *
     * @see FileSession
     * @since 0.10.0
     */
    public Map<string,> fileSessionMap;
}
```

### Example
We provide a simple example in using this functionality to help users in implementing customized filters.
In this example the pre/postProcessing classes are used to compress a list of files before sending and to decompress them at the destination.

To run the example please download the **FDTZipFilterExample.tar.gz** and follow these steps:

1) Untar the archive and go to FDTZipFilterExample directory. The directory already
contains the fdt.jar archive.

2) Go to FDTZipFilterExample directory and use compile.sh script ( javac must be in the $PATH)
to compile the filters.
```
$./compile.sh
```
3) To start the FDT server with PostZipFilter already enabled use the startFDTServer.sh script
```
$./startFDTServer.sh
```
4) To start FDT client and enable PreZipFilter the startFDTClient.sh may be used
```
$./startFDTClient.sh -c localhost -d /home/test dataToTransfer
```

The **dataToTransfer** file will be first zipped in **dataTransfer.zip** by the **PreZipFilter** and it's name will be changed in the ProcessorInfo and returned to the FDT client. Then the **dataToTransfer.zip** will be transfered to the destination where the **PostZipFilter** will uzip it and delete the zip file.
>
