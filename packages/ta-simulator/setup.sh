#!/bin/bash

TA_SIMULATOR_HOME=/opt/trustagent-simulator/
TA_SIMULATOR_LOGS=$TA_SIMULATOR_HOME/logs
mkdir -p $TA_SIMULATOR_HOME
mkdir $TA_SIMULATOR_LOGS

echo "Installing pre-requisites........."

yum install -y gcc
yum install -y python python-devel 
yum install -y python-pip

pip=$(which pip 2>/dev/null)
if [ -z "$pip" ]; then
  python get-pip.py &> /dev/null
fi

pip=$(which pip 2>/dev/null)
if [ -z "$pip" ]; then
  echo "cannot install pip command: please install pip manually"
  exit 1
fi

pip install --upgrade pypi  2>/dev/null
pip install jprops  2>/dev/null
pip install flask 2>/dev/null
pip install flask_basicauth  2>/dev/null
pip install uuid 2>/dev/null
pip install requests 2>/dev/null
pip install psutil 2>/dev/null
pip install futures 2>/dev/null

echo "Completed pre-req installation"

HTTP=$http_proxy
HTTPS=$https_proxy
export http_proxy=
export https_proxy=

cp ./* $TA_SIMULATOR_HOME/ -r
cd $TA_SIMULATOR_HOME
read -p "Enter the TA ip (ex: 10.1.2.3):" TA_IP
TA_IP=https://$TA_IP:1443
read -p "Enter the TA username:" TA_USER
read -p "Enter the TA pass:" TA_PASS

echo "Setting up environment............"
curl -u $TA_USER:$TA_PASS -H "Accept:application/json" $TA_IP/v2/host -k -v > ./repository/host_info.json
curl -u $TA_USER:$TA_PASS -H "Accept:application/pkix-cert" $TA_IP/v2/aik -k -v > ./repository/aik.cert
curl -X POST -u $TA_USER:$TA_PASS  -H "Accept:application/xml" -H "Content-Type:application/json" $TA_IP/v2/tpm/quote -k -d '{"nonce":"+c4ZEmco4aj1G5dTXQvjIMGFd44=","pcrs":[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23],"pcrbanks":["SHA1"]}' > ./repository/quote.xml

echo "Done"

export http_proxy=$HTTP
export http_proxy=$HTTPS
echo "Trust Agent Simulator installation completed"
