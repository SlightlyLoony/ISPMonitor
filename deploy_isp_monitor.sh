#! /bin/bash

# copy all the deployment files over to Beast...
scp out/artifacts/ISPMonitor.jar beast:/apps/ispmon
scp ISP-monitor.service beast:/apps/ispmon
scp isp_monitor_config.json beast:/apps/ispmon
scp logging.properties beast:/apps/ispmon

# execute commands on Beast
# get to the app directory
# set mode and owner on files that stay in that directory
# copy the service wrapper (if it has changed) to the systemd/system directory, change its mode and owner
# bounce the ISP-monitor service
ssh tom@ntp << RUN_ON_BEAST
cd /apps/ispmon
sudo chown ispmon:ispmon ISPMonitor.jar
sudo chmod ug+xrw ISPMonitor.jar
sudo chown ispmon:ispmon isp_monitor_config.json
sudo chmod ug+xrw isp_monitor_config.json
sudo chown ispmon:ispmon logging.properties
sudo chmod ug+xrw logging.properties
sudo cp -u ISP-monitor.service /etc/systemd/system
sudo chown ispmon:ispmon /etc/systemd/system/ISP-monitor.service
sudo chmod ug+xrw /etc/systemd/system/ISP-monitor.service
sudo systemctl stop ISP-monitor.service
sudo systemctl daemon-reload
sudo systemctl enable ISP-monitor.service
sudo systemctl start ISP-monitor.service
RUN_ON_BEAST
