#!/bin/bash

# define action usage commands
usage() { echo "Usage: $0 [-v \"version\"]" >&2; exit 1; }

# set option arguments to variables and echo usage on failures
version=
while getopts ":v:" o; do
  case "${o}" in
    v)
      version="${OPTARG}"
      ;;
    \?)
      echo "Invalid option: -$OPTARG"
      usage
      ;;
    *)
      usage
      ;;
  esac
done

if [ -z "$version" ]; then
  echo "Version not specified" >&2
  exit 2
fi

changeVersionCommand="mvn versions:set -DnewVersion=${version}"
changeParentVersionCommand="mvn versions:update-parent -DallowSnapshots=true -DparentVersion=${version}"
mvnInstallCommand="mvn clean install"

(cd features && $changeVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven version on \"features\" folder" >&2; exit 3; fi
(cd features && $changeParentVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven parent versions in the \"features\" folder" >&2; exit 3; fi
(cd features/mtwilson-trustagent-version && $changeVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven version on \"features/mtwilson-trustagent-version\" folder" >&2; exit 3; fi
(cd features/mtwilson-trustagent-version && $changeParentVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven parent versions in the \"features/mtwilson-trustagent-version\" folder" >&2; exit 3; fi
(cd packages && $changeVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven version on \"packages\" folder" >&2; exit 3; fi
(cd packages && $changeParentVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven parent versions in the \"packages\" folder" >&2; exit 3; fi
(cd packages/tboot-linux && $changeVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven version on \"packages/tboot-linux\" folder" >&2; exit 3; fi
(cd packages/tboot-linux && $changeParentVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven parent versions in the \"packages/tboot-linux\" folder" >&2; exit 3; fi
(cd packages/trustagent-zip && $changeVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven version on \"packages/trustagent-zip\" folder" >&2; exit 3; fi
sed -i 's/\(TBOOTXM_VERSION="\).*\("\)/\1'${version}'\2/g' packages/trustagent-linux/src/build/get-dependencies.sh
if [ $? -ne 0 ]; then echo "Failed to change version in \"packages/trustagent-linux/src/build/get-dependencies.sh\"" >&2; exit 3; fi
(cd packages/trustagent-linux && $changeVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven version on \"packages/trustagent-linux\" folder" >&2; exit 3; fi
(cd packages/trustagent-windows && $changeVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven version on \"packages/trustagent-windows\" folder" >&2; exit 3; fi
(cd packages/trustagent-windows/src/service && $changeVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven version on \"packages/trustagent-windows/src/service\" folder" >&2; exit 3; fi
(cd packages/trustagent-windows/src/systemtray && $changeVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven version on \"packages/trustagent-windows/src/systemtray\" folder" >&2; exit 3; fi
(cd packages/trustagent-docker && $changeVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven version on \"packages/trustagent-docker\" folder" >&2; exit 3; fi
sed -i 's/\-[0-9\.]*\(\-SNAPSHOT\|\(\-\|\.zip$\|\.bin$\|\.jar$\)\)/-'${version}'\2/g' build.targets
if [ $? -ne 0 ]; then echo "Failed to change versions in \"build.targets\" file" >&2; exit 3; fi
sed -i 's/\-[0-9\.]*\(\-SNAPSHOT\|\(\-\|\.zip$\|\.bin$\|\.jar$\)\)/-'${version}'\2/g' build.targets.windows
if [ $? -ne 0 ]; then echo "Failed to change versions in \"build.targets.windows\" file" >&2; exit 3; fi
