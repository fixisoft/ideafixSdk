IdeaFIX vs Quickfix/J benchmarks
===========

Pre-requisites 
==============

* JDK 17+
* Gradle 8.0+
* Maven 2.6+
* Python 3 (for analysis)

Here are the included folders :

* ```ideafix_client``` the benchmark IdeaFIX client
* ```ideafix_server``` the benchmark IdeaFIX server
* ```ideafix_direct_client``` the garbage-free benchmark IdeaFIX client
* ```ideafix_direct_server``` the garbage-free benchmark IdeaFIX server
* ```quickfix_client``` the benchmark QuickFIX/J client
* ```quickfix_server``` the benchmark QuickFIX/J client
* ```analysis``` the python script analysing performances

Methodology
===========

As the focus of trading applications is on response times, this benchmark method 
involves writing a client that sends orders (NewSingleOrder type 35=D) and receives
2 executions reports (type 35=8), one acknowledgement and one file report.

After that, the client sends another order which then triggers 2 execution report and so on 
until the timer is expired. 

It's a simple but realistic ping-pong setup that is very suitable for end-to-end latency measurement.
It allows for a platform-neutral, generic comparison between FIX engines.

Analysing message history leads to a measure of the distribution of round trip times (RTT).
This includes mean, standard deviation, percentiles etc.

For the moment, only IdeaFIX and QuickFIX/J are listed. there are 6 FIX 
Applications, 3 clients and 3 servers. 

Getting started
===============

To run a benchmark of IdeaFIX for the default period (10 min), run command :

```./ideafix_bench.sh```

To run the benchmark for only 1m :

```./ideafix_bench.sh -d 60```

Similarly you can run  :

```./quickfix_bench.sh```

Or :

```./quickfix_bench.sh 60```

Note IdeaFIX runs best on server specs with several cores available as it's using
more parallelism than QuickFIX/J. Also generating a high-frequency message history
can be demanding for the hard drive. It's best to have a NVMe SSD model with lots of
space available. Be careful with long benchmark runs !

When using script ```ideafix_bench.sh```, enable flag -g to run the garbage-free ideaFIX 
benchmarks. Here's the command usage :

```
./ideafix_bench.sh -h
Run a ideaFIX benchmark for a given duration, benchmark directory and tag. Requires sudo rights
Usage: ./ideafix_bench.sh -d duration -s benchmarkDir -t tag -g
	-d duration of benchmark run (default is 600)
	-s message root directory location (default is ./ideafix_bench) WARNING directory is cleared at each run
	-t tag to look for, usually to id a transaction (default is 35=D)
	-g to use the low gc version of ideafix (default is false)
```

The analysis requires python 3+. the script can be found in the analysis folder :

```
./analyse_message_history.py --help
usage: analyse_message_history.py [-h] [--resolution RESOLUTION] [--sampleCount SAMPLECOUNT] [--warmup WARMUP] [--tag TAG]
                                  [--useHeaderTimestamp | --no-useHeaderTimestamp]
                                  filename

compute RTT by resampling message history. provide a HDR histogram and percentiles thanks to a bootstrap method.

positional arguments:
  filename              message history to process

options:
  -h, --help            show this help message and exit
  --resolution RESOLUTION
                        define the sampling resolution in nanoseconds (default is 5000000ns = 5ms)
  --sampleCount SAMPLECOUNT
                        define the number of samples, higher means better estimates (default is 10000)
  --warmup WARMUP       define the warmup period to ignore during sampling (default is 60s)
  --tag TAG             defines the tag to look for (default is 35=D)
  --useHeaderTimestamp, --no-useHeaderTimestamp
                        use header timestamp (a long number before separator ';', faster and more precise)
```

For example, the following command :

```
./analyse_message_history.py 
/mnt/data/ideafix_bench/ideafix_data/ideafix_client/IdeaFixClientBenchmark_INITIATOR_FIX.4.4_clientBenchmark_serverBenchmark_SIMPLE_OM.xml/20240416-00_00-23_59_59@Europe_Paris/outgoing/outgoing_session.1.fix 
--useHeaderTimestamp --warmup 240 --sampleCount 30000
```

gave the following results on my testing hardware :

IdeaFIX
-------

![](./benchmark_results/20240629_ideafix_local.png)
```
population mean = 11044.35 ns
sampled mean = 11669.74 ns
std dev. = 9880.71 ns
sample size = 568
Percentiles:
	1.00th percentile = 10404.54 ns
	5.00th percentile = 10460.21 ns
	50.00th percentile = 10618.85 ns
	90.00th percentile = 10802.60 ns
	95.00th percentile = 11776.01 ns
	99.00th percentile = 30476.53 ns
	99.99th percentile = 293768.70 ns
```
Results are excellent ! RTT distribution is centered, which means RTTs are predictable.
In fact, a major part of time is spent on the OS network stack. To measure local TCP RTT, [tcpping](https://github.com/josephcolton/tcpping) 
is very helpful.

On my testing hardware it gives  (30s warmup, 300s total run time) :

```
./tcpping localhost -s 30 -c 300 -d stat
TCP PING localhost (127.0.0.1) tcp port 443
--- localhost tcp ping statistics ---
270 pings, 270 success, 0 failed, 0.0% loss, total run time: 299101.532 ms
rtt min/ave/max/range/jitter = 0.031/0.032/0.045/0.014/0.001 ms

```

IdeaFIX is under 30µs that is **less** than plain tcpping RTT. It can be surprising at first but this is due the choice 
of (optimised) socket parameters in IdeaFIX

Nevertheless, this gives an indication that IdeaFIX is very close to the 
raw performance of the default linux network stack. Only a handful of microseconds
is spent parsing and marshalling FIX messages. 

The hardware and OS used is (```uname -a``` & ```hwinfo --short```) :

```
Linux desktop-pyp 6.7.12-tkg-eevdf #1 SMP PREEMPT_DYNAMIC TKG Wed Jun  5 09:53:13 CEST 2024 x86_64 GNU/Linux

cpu:                                                            
                       Intel(R) Core(TM) i7-6700 CPU @ 3.40GHz, 3700 MHz
                       Intel(R) Core(TM) i7-6700 CPU @ 3.40GHz, 3700 MHz
                       Intel(R) Core(TM) i7-6700 CPU @ 3.40GHz, 3700 MHz
                       Intel(R) Core(TM) i7-6700 CPU @ 3.40GHz, 3700 MHz
bridge:
                       Intel 100 Series/C230 Series Chipset Family PCI Express Root Port #2
                       Intel Z170 Chipset LPC/eSPI Controller
                       Intel 6th-10th Gen Core Processor PCIe Controller (x16)
                       Intel 100 Series/C230 Series Chipset Family PCI Express Root Port #17
                       Intel 100 Series/C230 Series Chipset Family PCI Express Root Port #4
                       Intel Xeon E3-1200 v5/E3-1500 v5/6th Gen Core Processor Host Bridge/DRAM Registers
                       Intel 100 Series/C230 Series Chipset Family PCI Express Root Port #3
memory:
                       32GiB System Memory
                       8GiB DIMM DDR4 Synchronous 2133 MHz (0.5 ns)
                       8GiB DIMM DDR4 Synchronous 2133 MHz (0.5 ns)
                       8GiB DIMM DDR4 Synchronous 2133 MHz (0.5 ns)
                       8GiB DIMM DDR4 Synchronous 2133 MHz (0.5 ns)
disk:
                       /dev/nvme0n1         SK hynix PC300 NVMe Solid State Drive 256GB
                       /dev/sda             ST1000DM003-1SB1
```

For comparison, here are Quickfix/J results on the same hardware :

Quickfix/J
----------
![](./benchmark_results/20240629_quickfix_local.png)

```
population mean = 62094.50 ns
sampled mean = 62272.54 ns
std dev. = 5235.52 ns
sample size = 101
Percentiles:
	1.00th percentile = 59638.64 ns
	5.00th percentile = 60455.92 ns
	50.00th percentile = 61501.06 ns
	90.00th percentile = 63390.65 ns
	95.00th percentile = 65030.88 ns
	99.00th percentile = 80076.84 ns
	99.99th percentile = 248514.67 ns
```

IdeaFIX is more than 5x faster !

Running on a server
===================

What happens when ideaFIX and quickfix run on a standard 8-core bare metal server ? 
Here are some results using OVH cloud provider :

IdeaFIX
-------

![](./benchmark_results/20240629_ideafix_ovh.png)

```
population mean = 10311.00 ns
sampled mean = 10325.82 ns
std dev. = 1439.49 ns
sample size = 608
Percentiles:
	1.00th percentile = 10090.84 ns
	5.00th percentile = 10132.97 ns
	50.00th percentile = 10273.73 ns
	90.00th percentile = 10425.89 ns
	95.00th percentile = 10491.78 ns
	99.00th percentile = 10689.75 ns
	99.99th percentile = 88788.29 ns
```

QuickFIX/J
----------

![](./benchmark_results/20240629_quickfix_ovh.png)

```
population mean = 60155.44 ns
sampled mean = 60341.26 ns
std dev. = 4656.07 ns
sample size = 104
Percentiles:
	1.00th percentile = 56689.39 ns
	5.00th percentile = 57100.56 ns
	50.00th percentile = 58578.18 ns
	90.00th percentile = 67866.25 ns
	95.00th percentile = 69053.48 ns
	99.00th percentile = 74685.56 ns
	99.99th percentile = 116496.26 ns
```

IdeaFIX is literally 6x faster than Quickfix/J on a server spec, and manage a stable 
10µs RTT without advanced OS tuning ! The additional cores help deal with the jitter caused 
by the gc, JIT compiler, OS scheduling and other sources of context switching.