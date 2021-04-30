#!/usr/bin/env sh

NIFI_URL="http://localhost:7777/nifi-api/system-diagnostics"
PROJECT_HOME="//Users/simon/IdeaProjects/nifi/nifi-nar-bundles/nifi-microsoft-graph-bundle/"
printf "Compiling..\n"

res=$(mvn clean install -T5 | grep ERROR)

if [ -n "$res" ]
then
  printf "Compiling failed: \n"
  echo "$res"
  exit
fi

printf "Done compiling.\n\n"

cp ${PROJECT_HOME}nifi-microsoft-graph-services-api-nar/target/nifi-microsoft-graph-services-api-nar-0.1.0.nar ~/nifi/nifi-1.13.2/extensions
cp ${PROJECT_HOME}nifi-microsoft-graph-services-nar/target/nifi-microsoft-graph-services-nar-0.1.0.nar ~/nifi/nifi-1.13.2/extensions
cp ${PROJECT_HOME}nifi-microsoft-graph-processors-nar/target/nifi-microsoft-graph-processors-nar-0.1.0.nar ~/nifi/nifi-1.13.2/extensions

echo "Restarting NiFi."

~/nifi/nifi-1.13.2/bin/nifi.sh restart > /dev/null 2>&1

printf "Waiting for NiFi to come alive.\n"

ret="0"
while [ $ret -ne "200" ]
do
    ret=$(curl -I -s $NIFI_URL -o /dev/null -w "%{http_code}\n")
    sleep 3
    echo "Still waiting.."
done

echo  "NiFi started."

exit 0
