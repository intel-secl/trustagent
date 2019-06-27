#!/bin/bash
#Analysis tboot log for tpm1.2 DA mode & tpm2.0 case. Called from module_analysis.sh
# Usage: ./module_analysis_da.sh   (reads from txt-stat output)
#        ./module_analysis_da.sh  file1  (reads from previously saved output in file1)
TXTSTAT=$(which txt-stat 2>/dev/null)
TXTSTAT=${TXTSTAT:-"/usr/sbin/txt-stat"}

# store the location of pcr_event_log
INFILE_PCR_EVENTLOG=${INFILE_PCR_EVENTLOG:-/opt/trustagent/var/ramfs/pcr_event_log}
INFILE_TCB_MEASUREMENT_SHA256="/var/log/trustagent/measurement.sha256"

# 2.0 outputs to /opt/trustagent/var/measureLog.xml
OUTFILE=${OUTFILE:-/opt/trustagent/var/measureLog.xml}
# 1.2 outputs to measureLog.xml in current directory
#OUTFILE=measureLog.xml
BLANK2="  "
BLANK4="    "
BLANK6="      "
BLANK8="        "
txt_status=0
round=0

xDigest="EOF"
xPcrIndex="EOF"
xType="EOF"
xBank="EOF"
xName="EOF"
declare -a xDigestArray
declare -a xPcrIndexArray
declare -a xTypeArray
declare -a xNameArray
declare -a xBankArray
modIndex=0

declare -a AlgIdArray

section1EndNum=0
startNum=0
endNum=0

###pcrBank : pcrNumber : name : value ###
xml_module()
{
  echo "$BLANK6<module>"
  echo "$BLANK8<pcrBank>$1</pcrBank>"
  echo "$BLANK8<pcrNumber>$2</pcrNumber>"
  echo "$BLANK8<name>$3</name>"
  echo "$BLANK8<value>$4</value>"
  echo "$BLANK6</module>"
}

# get the PCR values from /opt/trustagent/var/ramfs/pcr_event_log
generate_pcr_measurelog ()
{
  if [ -f "$INFILE_PCR_EVENTLOG" ];then
    while read line; do
      if [ -z "$line" ];then
        continue
      fi
      num=0
      for word in $line; do
        if [ $num -eq 0 ];then
          PCR_BANK="$word"
        elif [ $num -eq 1 ];then
          PCR_NUM="$word"
        elif [ $num -eq 2 ];then
          NAME="$word"
        elif [ $num -eq 3 ];then
          VALUE="$word"
        fi
        let "num++"
      done
      xml_module "$PCR_BANK" "$PCR_NUM" "$NAME" "$VALUE"
    done
  fi
}

get_line_number()
{
  local num="`$INFILE | grep -n "$1" | awk -F: '{print $1}'`"
  if [ ${#num} -gt 0 ];then
    echo $num
  else
    echo 0
  fi
}

getName()
{
  lType=$1
  lRound=$2

  case $lType in
  0x401) echo PCR_MAPPING;;
  0x402) echo HASH_START;;
  0x403) echo COMBINED_HASH;;
  0x404) echo MLE_HASH;;
  0x40a) echo BIOSAC_REG_DATA;;
  0x40b) echo CPU_SCRTM_STAT;;
  0x40c) echo LCP_CONTROL_HASH;;
  0x40d) echo ELEMENTS_HASH;;
  0x40e) echo STM_HASH;;
  0x40f) echo OSSINITDATA_CAP_HASH;;
  0x410) echo SINIT_PUBKEY_HASH;;
  0x411) echo LCP_HASH;;
  0x412) echo LCP_DETAILS_HASH;;
  0x413) echo LCP_AUTHORITIES_HASH;;
  0x414) echo NV_INFO_HASH;;
  0x416) echo EVTYPE_KM_HASH;;
  0x417) echo EVTYPE_BPM_HASH;;
  0x418) echo EVTYPE_KM_INFO_HASH;;
  0x419) echo EVTYPE_BPM_INFO_HASH;;
  0x41a) echo EVTYPE_BOOT_POL_HASH;;
  0x4ff) echo CAP_VALUE;;
  0x501)
    case "$lRound" in
    0 | 1) echo tb_policy;;
    2) echo vmlinuz;;
    3) echo initrd;;
    4) echo asset-tag;;
    *) echo 0x501;;
    esac;;
  *) echo $lType;;
  esac
}

getBankById()
{
  lAlgId=$1

  case $lAlgId in
  4) echo SHA1 ;;
  11) echo SHA256 ;;
  *) echo $lAlgId;;
  esac
}

getBank()
{
  lRound=$1
  lBankCount=$2

  lAlgId="${AlgIdArray["$((lRound%lBankCount))"]}"
  echo `getBankById $lAlgId`
}

#main
if [ ! -f "$TXTSTAT" ]; then
  echo "<measureLog>" >$OUTFILE
  echo "$BLANK2<txt>" >>$OUTFILE
  echo "$BLANK2$BLANK2<txtStatus>0</txtStatus>" >>$OUTFILE
  echo "$BLANK2$BLANK2<modules>" >>$OUTFILE
  if [ -f "$INFILE_PCR_EVENTLOG" ];then
    generate_pcr_measurelog < "$INFILE_PCR_EVENTLOG" >>$OUTFILE
  fi
  echo "$BLANK2$BLANK2</modules>" >>$OUTFILE
  echo "$BLANK2</txt>" >>$OUTFILE
  echo "</measureLog>" >>$OUTFILE
  exit 0
else
  TXTSTAT="sudo -n $TXTSTAT"
fi

if [ -n "$1" ]; then INFILE="cat $1"; else INFILE="$TXTSTAT"; fi

txt_status=3
#start xml file generation, continue only when $txt_status = 3
echo "<measureLog>" >$OUTFILE
echo "$BLANK2<txt>" >>$OUTFILE
txt_measured_launch="`$INFILE | grep 'TXT measured launch: TRUE'`"
secrets_flag_set="`$INFILE | grep 'secrets flag set: TRUE'`"
if [ ${#txt_measured_launch} -eq 27 -a ${#secrets_flag_set} -eq 24 -a $txt_status -eq 3 ];then
  echo "$BLANK2$BLANK2<txtStatus>3</txtStatus>" >>$OUTFILE
else
  echo "$BLANK2$BLANK2<txtStatus>0</txtStatus>" >>$OUTFILE
  echo "$BLANK2</txt>" >>$OUTFILE
  echo "</measureLog>" >>$OUTFILE
  exit 0
fi

x501i=0
eventLineNumbersArray="$(get_line_number 'TCG Event:')"
for eventLineNumber in $eventLineNumbersArray; do
  xPcrIndex="$($INFILE | sed -n "$((eventLineNumber+1)) p" | awk -F: '{print $3}' | sed "s/ //g" | sed "s/\t//g")"
  xType="$($INFILE | sed -n "$((eventLineNumber+2)) p" | awk -F: '{print $3}' | sed "s/ //g" | sed "s/\t//g")"
  if [[ $xType == "0x501" ]]; then
    xName="$(getName $xType $x501i)"
    let "x501i++"
  else
    xName="$(getName $xType)"
  fi
  count="$($INFILE | sed -n "$((eventLineNumber+3)) p" | awk -F: '{print $3}' | sed "s/ //g" | sed "s/\t//g")"
  for (( i=1; i<=$count; i++ )); do
    xBank="$($INFILE | sed -n "$((eventLineNumber+2+(2*$i))) p" | awk -F: '{print $2}' | sed "s/ //g" | sed "s/\t//g")"
    xDigest="$($INFILE | sed -n "$((eventLineNumber+3+(2*$i))) p" | awk -F: '{print $2}' | sed "s/ //g" | sed "s/\t//g")"
    xPcrIndexArray[$modIndex]=$xPcrIndex
    xTypeArray[$modIndex]=$xType
    xDigestArray[$modIndex]=$xDigest
    xNameArray[$modIndex]=$xName
    xBankArray[$modIndex]=$xBank
    let "modIndex++"
  done
done

#output all modules
echo "$BLANK2$BLANK2<modules>" >>$OUTFILE
# write the PCR values to the measure log
if [ -f "$INFILE_PCR_EVENTLOG" ];then
  generate_pcr_measurelog < "$INFILE_PCR_EVENTLOG" >>$OUTFILE
fi

for((g=0;g<${#xDigestArray[*]};g++));do
  if [ "${xTypeArray[$g]}" != 0x3 -a "${xPcrIndexArray[$g]}" != 255 ]; then
    xml_module "${xBankArray[$g]}" "${xPcrIndexArray[$g]}" "${xNameArray[$g]}" "${xDigestArray[$g]}" >>$OUTFILE
  fi
done

### looks for tcb measurement hash in /var/log/trustagent/measurement.sha1, adds
### as a module to OUTFILE
if [ -f "$INFILE_TCB_MEASUREMENT_SHA256" ]; then
  measurement_name="tbootxm"
  measurement=$(cat "$INFILE_TCB_MEASUREMENT_SHA256")
  xml_module "SHA256" "19" "$measurement_name" "$measurement" >>$OUTFILE
fi

echo "$BLANK2$BLANK2</modules>" >>$OUTFILE
echo "$BLANK2</txt>" >>$OUTFILE
echo "</measureLog>" >>$OUTFILE
