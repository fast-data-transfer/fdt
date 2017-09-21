[[Home](index.md)]  [Documentation]  [[Performance Tests](perf-disk-to-disk.md)]

[[FDT & DDCopy](doc-fdt-ddcopy.md)]   [[Examples](doc-examples.md)]  [[Security](doc-security.md)]    [[User's Extensions](doc-user-extensions.md)]   [System Tuning]


### System Settings
##### Linux

We suggest to use newer linux distributions, or if this is not possible, update at least the kernel(2.6.20+). The newer kernels provide adequate TCP settings. We suggest to use the following settings to improve the TCP throughput, especially over long RTT links:
1. Increase the TCP buffers (newer kernels have this buffers in creased by default). Add the following lines in /etc/sysctl.conf to make the changes permanent accross reboots:

```net.core.rmem_max = 8388608```

```net.core.wmem_max = 8388608```

```net.ipv4.tcp_rmem = 4096 87380 8388608```

```net.ipv4.tcp_wmem = 4096 65536 8388608```

```net.core.netdev_max_backlog = 250000```

```net.ipv4.tcp_no_metrics_save = 1```

```net.ipv4.tcp_moderate_rcvbuf = 1```

After adding them just run the following commabd as root:
    
```#sysctl -p /etc/sysctl.conf```

The settings above will set a maximum of 8 MBytes buffers.
we suggest to use at least 4Mbytes maximum TCP buffers and a maximum of 16Mbytes should be enough. You should use reasonable values. Don't set very high values for this parameters, and especially don't set the same value for all the fields in net.ipv4.tcp_*. It's also a good practice to have the same value for net.core.r(w)mem_max with the last value in the net.ipv4.tcp_r(w)mem. Do not modify the net.ipv4.tcp_mem parameter. It is computed by the kernel at the system boot.

2. The TCP congestion protocol (if available). To check if it is available for your kernel version:

```$/sbin/sysctl net.ipv4.tcp_congestion_control```

Set it to cubic if kernel version 2.6.20+, and to scalable if older kernels.

```#sysctl -w net.ipv4.tcp_congestion_control=cubic```

To make this persistent accross reboots add the following line in /etc/sysctl.conf

```net.ipv4.tcp_congestion_control=cubic```

You may try to experiment various TCP stacks. You can list all of them using:

```$/sbin/sysctl net.ipv4.tcp_available_congestion_control```

3. Increase txqueuelen size your ethernet card

```#ifconfig eth2 txqueuelen 50000```

4. If your network infrastructure supports jumbo frames you may set the MTU size to 9000. Please notice that this setting might broke your AFS installation (newer versions of OpenAFS supports jumbo frames)

```#ifconfig eth2 mtu 9000```

You may also try to disable the TCP timestamps. On some kernel versions this setting disables the automatic window scalling:

```#sysctl -w net.ipv4.tcp_timestamps=0```

To make this setting permanent add the following line in /etc/sysctl.conf:

```net.ipv4.tcp_timestamps=0```

##### OpenSolaris

We obtained good results on OpenSolaris using these setting for the TCP buffers:

```ndd -set /dev/tcp tcp_max_buf 8388608```

```ndd -set /dev/tcp tcp_cwnd_max 4194304```

```ndd -set /dev/tcp tcp_xmit_hiwat 524288```

```ndd -set /dev/tcp tcp_recv_hiwat 524288```

This page will include other settings or operating systems in the near future.

For further comments and suggestions please send an email to: support-fdt@monalisa.cern.ch
