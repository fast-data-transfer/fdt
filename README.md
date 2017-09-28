FDT is an Application for Efficient Data Transfers which is capable of reading and writing at disk speed over wide area networks (with standard TCP). It is written in Java, runs an all major platforms and it is easy to use.

FDT is based on an asynchronous, flexible multithreaded system and is using the capabilities of the Java NIO libraries. Its main features are:

Streams a dataset (list of files) continuously, using a managed pool of buffers through one or more TCP sockets.
Uses independent threads to read and write on each physical device
Transfers data in parallel on multiple TCP streams, when necessary
Uses appropriate-sized buffers for disk I/O and for the network
Restores the files from buffers asynchronously
Resumes a file transfer session without loss, when needed
FDT can be used to stream a large set of files across the network, so that a large dataset composed of thousands of files can be sent or received at full speed, without the network transfer restarting between files.

[MORE...](https://fast-data-transfer.github.io/fdt/)

![alt tag](http://monalisa.cern.ch/FDT/img/FDT_diagram.png)
