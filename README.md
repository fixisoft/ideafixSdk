# ideafixSdk

## Welcome to ideaFIX SDK !

IdeaFIX is an uncompromising, easy-to-use, ultra low latency FIX Engine.

The SDK is divided into 2 main sections :

1. examples
2. benchmarks

FIX is the standard protocol for financial applications, similar to what HTTP is for the web.

Script ```debian_setup_script.sh``` install dependencies and dowload latest binaries 
on a fresh debian-based linux distribution. Install on other environments can be derived without 
too much trouble. 

Please visit www.fixisoft.com if you're looking for more information !

## Get started on Windows
For simplicity, current documentation assumes UNIX conventions. 
Only minor changes are required to run the entire SDK on Windows :

1. Use a good package manager. [Scoop](https://scoop.sh/) does a great job
2. Install Cygwin ```scoop install cygwin``` to run bash scripts
3. Install Python  & matploblib ```scoop install python``` ```pip install matplotlib``` to run analysis scripts
4. Run the benchmarks in TCP mode ```ideafix_bench.sh -p```. Only recent versions of Windows support UNIX Domain Sockets
