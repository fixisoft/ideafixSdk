#!/bin/bash
#
# This script updates and install all the requirements to run ideaFIX benchmarks on debian 12(-based) distros
# There are some extras, like using debian backports for the latest kernels, or even custom-build tkg kernels.
# These kernels include some of the most-respected kernel patches for low-latency applications.
#

# general purpose upgrade of a debian distro
sudo apt-get update
sudo apt-get upgrade
sudo apt-get dist-upgrade

# add debian backports
# echo "deb http://deb.debian.org/debian bookworm-backports main" > /etc/apt/sources.list.d/backport.list
# sudo apt-get install linux-*-6.7.12+bpo-amd64

# dependencies for benchmark scripts
sudo apt-get install zip bc 

# dependencies for analysis scripts
sudo apt-get install python-is-python3 python3-matplotlib

# dependencies to monitor and optimise benchmark 
sudo apt-get install htop iotop sysstat linux-cpupower

# JAVA dependencies to run IdeaFIX. using SDKMAN!
curl -s "https://get.sdkman.io" | bash
source "/home/debian/.sdkman/bin/sdkman-init.sh"
sdk install java #23.0.2-graal
sdk install gradle 
sdk install maven

# download and unzip IdeaFIX SDK
cd ~
wget http://www.fixisoft.com/ideafixSdk-1.4.1.zip
unzip ideafixSdk-1.4.1.zip

# run benchmarks
# cd ideafixSdk-1.4.1
# ./install.sh
# cd benchmark
# ./ideafix_bench.sh

# switching to performance cpu governor
# sudo cpupower frequency-set -g performance

# git is required for tkg kernel
# apt-get install git

# cloning repo
# git clone https://github.com/Frogging-Family/linux-tkg.git

# to figure out the intel arch code name to select during kernel configuration
# cat /sys/devices/cpu/caps/pmu_name


