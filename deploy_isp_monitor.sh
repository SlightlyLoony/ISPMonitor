#! /bin/bash

# copy all the deployment files over to the NTP server...
scp out/artifacts/NTPMonitor.jar ntp:/apps/ntpmon
scp NTP-monitor.service ntp:/apps/ntpmon
scp ntpconfig.json ntp:/apps/ntpmon
scp logging.properties ntp:/apps/ntpmon
scp runNTPMon.sh ntp:/apps/ntpmon

# execute commands on the NTP server
# get to the app directory
# set mode and owner on files that stay in that directory
# copy the service wrapper (if it has changed) to the systemd/system directory, change its mode and owner
# bounce the NTP-monitor service
ssh tom@ntp << RUN_ON_NTP
cd /apps/ntpmon
sudo chown ntpmon:ntpmon NTPMonitor.jar
sudo chmod ug+xrw NTPMonitor.jar
sudo chown ntpmon:ntpmon ntpconfig.json
sudo chmod ug+xrw ntpconfig.json
sudo chown ntpmon:ntpmon logging.properties
sudo chmod ug+xrw logging.properties
sudo chown ntpmon:ntpmon runNTPMon.sh
sudo chmod ug+xrw runNTPMon.sh
sudo cp -u NTP-monitor.service /etc/systemd/system
sudo chown ntpmon:ntpmon /etc/systemd/system/NTP-monitor.service
sudo chmod ug+xrw /etc/systemd/system/NTP-monitor.service
sudo systemctl stop NTP-monitor.service
sudo systemctl daemon-reload
sudo systemctl enable NTP-monitor.service
sudo systemctl start NTP-monitor.service
RUN_ON_NTP
