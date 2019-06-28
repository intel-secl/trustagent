#!/bin/bash

if [ "$1" = "start" ]
then
    python master_deploy.py --start &>> ../logs/tagent-sim.log &
elif [ "$1" = "stop" ]
then
    python master_deploy.py --stop &>> ../logs/tagent-sim.log
elif [ "$1" = "restart" ]
then
    python master_deploy.py --stop &>> ../logs/tagent-sim.log
    sleep 2
    python master_deploy.py --start &>> ../logs/tagent-sim.log &
elif [ "$1" = "create-all-hosts" ]
then
    python master_deploy.py --create_all_host &>> ../logs/tagent-sim.log &
elif [ "$1" = "create-all-flavors" ]
then
    python master_deploy.py --create_all_flavors &>> ../logs/tagent-sim.log &
elif [ "$1" = "get-stats" ]
then
    python master_deploy.py --get_stats  
else
    echo "   ####### Trustagent Simulator ####### "
    echo " tagent-sim.sh start              : To start simulator "
    echo " tagent-sim.sh stop               : To stop simulator"
    echo " tagent-sim.sh restart            : To restart simulator"
    echo " tagent-sim.sh create-all-hosts   : To create all hosts"
    echo " tagent-sim.sh create-all-flavors : To create all flavors"
    echo " tagent-sim.sh get-stats          : To get performance stats"
fi
