#!/bin/bash
#default duration is 10min
DEFAULT_DURATION=600 
DEFAULT_BENCHMARK_DIR="./ideafix_bench"
DEFAULT_TAG="35=D"
DEFAULT_USE_LOW_GC=true
IDEAFIX_CLIENT="ideafix_client"
IDEAFIX_LOW_GC_CLIENT="ideafix_direct_client"
IDEAFIX_SERVER="ideafix_server"
IDEAFIX_LOW_GC_SERVER="ideafix_direct_server"

helpFunction()
{
   echo "Run a ideaFIX benchmark for a given duration, benchmark directory and tag. Requires sudo rights"
   echo "Usage: $0 -d duration -s benchmarkDir -t tag -g"
   echo -e "\t-d duration of benchmark run (default is $DEFAULT_DURATION)"
   echo -e "\t-s message root directory location (default is $DEFAULT_BENCHMARK_DIR) WARNING directory is cleared at each run"
   echo -e "\t-t tag to look for, usually to id a transaction (default is $DEFAULT_TAG)"
   echo -e "\t-g to use the low gc version of ideafix (default is false)"
   exit 1 # Exit script after printing help
}


while getopts "d:s:t:gh" opt
do
   case "$opt" in
      d ) duration="$OPTARG" ;;
      s ) benchmarkDir="$OPTARG" ;;
      t ) tag="$OPTARG" ;;
      g ) useLowGc=true ;;
      h ) helpFunction ;;
      ? ) helpFunction ;;
   esac
done


if [ "$useLowGc" = true ]
then
   echo "-g defined using low gc client $IDEAFIX_LOW_GC_CLIENT and server $IDEAFIX_LOW_GC_SERVER" 
   client=$IDEAFIX_LOW_GC_CLIENT
   server=$IDEAFIX_LOW_GC_SERVER
else
   echo "-g not defined using default client $IDEAFIX_CLIENT and server $IDEAFIX_SERVER" 
   client=$IDEAFIX_CLIENT
   server=$IDEAFIX_SERVER
fi
# use default is duration is empty
if [ -z "$duration" ] 
then
   echo "No duration specified, using default duration ${DEFAULT_DURATION}s"
   duration=$DEFAULT_DURATION
fi

# use default is benchmarkDir is empty
if [ -z "$benchmarkDir" ] 
then
   echo "No benchmarkDir specified, using default benchmarkDir $DEFAULT_BENCHMARK_DIR"
   benchmarkDir=$DEFAULT_BENCHMARK_DIR
fi

# use default is tag is empty
if [ -z "$tag" ] 
then
   echo "No tag specified, using default tag $DEFAULT_TAG (NEW_SINGLE_ORDER)"
   tag=$DEFAULT_TAG
fi

#display input variables
echo "running benchmark for $duration seconds, store dir is $benchmarkDir, looking for tag $tag ..."

#re-create the benchmark folder
echo "re-create benchmark folder $benchmarkDir ..."
rm -rf $benchmarkDir
mkdir $benchmarkDir

#rebuild 
echo "re-build $server and $client ..."
gradle $server:clean
gradle $client:clean
gradle $server:distTar
gradle $client:distTar

echo "kill all java processes (requires sudo rights) ..."
#kill all the java processes
sudo killall java

#unpack in benchmark folder
echo "unpack in benchmark folder $benchmarkDir ..."
tar -xvf ./$server/build/distributions/$server.tar -C $benchmarkDir
tar -xvf ./$client/build/distributions/$client.tar -C $benchmarkDir


#enter benchmark folder, start server wait 5s and start client  
cd $benchmarkDir || exit

echo "start $server ..."
./$server/bin/$server  &
server_pid=$!
sudo ionice -c 1 -n 4 -p $server_pid
echo "wait 5s ..."
sleep 5
echo "start $client ..."
./$client/bin/$client  &
client_pid=$!
sudo ionice -c 1 -n 4 -p $client_pid

#let the script, server and client run for a certain duration
echo "wait ${duration}s ..."
sleep $duration
echo "kill $server ..."
kill $server_pid
echo "wait 5s ..."
sleep 5
echo "kill $client ..."
kill $client_pid

echo "extract the number of tags $tag in outgoing client messages located in $benchmarkDir ..."
incoming=$(find . -path './ideafix_data/ideafix_client*' -name outgoing_session.1.fix)
n=$(tr < "$incoming" '\01' '\n' | grep -c "35=D")
echo "number of transaction is $n in ${duration}s"

if [ "$n" -eq 0 ] ; then
   echo "there were no transactions cannot compute RTT"
   exit 1	
fi
#display the average period (or RTT) in seconds. the lower, the better (good latencies)
period=$(bc <<< "scale=9; $duration/$n")
echo "average period in seconds is $period"

