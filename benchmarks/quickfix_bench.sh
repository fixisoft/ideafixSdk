#!/bin/bash
#default duration is 10min
DEFAULT_DURATION=600
if [ $# -eq 0 ]
  then
    echo "No argument specified Using default duration $DEFAULT_DURATION s"
    duration=$DEFAULT_DURATION
else
 duration=$1
fi

#re-create the benchmark folder
echo "re-create benchmark folder quickfix_bench ..."
rm -rf ./quickfix_bench
mkdir quickfix_bench

#rebuild 
echo "re-build quickfix_server and quickfix_client ..."
gradle quickfix_server:clean
gradle quickfix_client:clean
gradle quickfix_server:distTar
gradle quickfix_client:distTar

echo "kill all java processes ..."
#kill all the java processes
sudo killall java

#unpack in benchmark folder
echo "unpack in benchmark folder ..."
tar -xvf ./quickfix_server/build/distributions/quickfix_server.tar -C ./quickfix_bench
tar -xvf ./quickfix_client/build/distributions/quickfix_client.tar -C ./quickfix_bench


#enter benchmark folder, start server wait 5s and start client  
cd quickfix_bench

echo "start server ..."
nice -10 ./quickfix_server/bin/quickfix_server  &
server_pid=$!
sudo ionice -c 1 -n 4 -p $server_pid
echo "wait 5 s ..."
sleep 5
echo "start client ..."
nice -10 ./quickfix_client/bin/quickfix_client  &
client_pid=$!
sudo ionice -c 1 -n 4 -p $client_pid

#let the script, server and client run for a certain duration
echo "wait $duration s ..."
sleep $duration
echo "kill server ..."
kill $server_pid
echo "wait 5 s ..."
sleep 5
echo "kill client ..."
kill $client_pid

#extract the number of transactions

n=$(cat ./quickfix_data/quickfix_client/FIX.4.4-clientBenchmark-serverBenchmark.body | tr '\01' '\n' | grep -c "35=D")
echo "number of transaction is $n in $duration s"

#display the average period (or RTT) in seconds. the lower, the better (good latencies)
period=$(bc <<< "scale=9; $duration/$n")
echo "average period is $period s"

#analysis script requires messages to be separated by a return carriage
echo "create message history with return carriage after each message ..."
sed  '/\x018=FIX.4.4/ s//\x01\n8=FIX.4.4/g' ./quickfix_data/quickfix_client/FIX.4.4-clientBenchmark-serverBenchmark.body > ./quickfix_data/quickfix_client/FIX.4.4-clientBenchmark-serverBenchmark.lines.body


