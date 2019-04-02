#!/bin/bash
# makebin-auto.sh - creates a self-extracting installer
# how it works:

# workspace is typically "target" and must contain the files to package in the installer including the setup script
workspace="${1}"
if [ ! -d "$workspace" ]; then workspace="./"; fi

# check for the makeself tool
makeself=`which makeself`
if [ -z "$makeself" ]; then
    echo "Missing makeself tool"
    exit 1
fi

jars=`ls ./share/lib-host-connector-1.0.jar && ls ./share/mtwilson-flavor-ws-v2-4.0-SNAPSHOT.jar`
if [ -z "$jars" ]; then
    echo "Missing dependency jars"
    exit 1
fi

$makeself --notemp "$workspace" ./ta-simulator.bin "TA Simulator Installer - `date`" ./setup.sh
