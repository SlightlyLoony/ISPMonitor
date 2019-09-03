#ISP Monitor
Part of a system for monitoring the state of two Internet Service Provider (ISP) connections (primary and secondary), and automatically switching between them as the ISP connections go up or down.

The entire system includes the following components:
- **Primary ISP modem**: This modem provides the connectivity to the primary ISP.  In the author's case, this is an Xfinity-provided gigabit cable modem.
- **Secondary ISP modem**: This modem provides the connectivity to the secondary ISP.  In the author's case, this is a Cradlepoint CBA850 Branch LTE adapter, which is an LP6 LTE modem.
- **Border Router**: This is the router at the edge of the local network, connected to both of the ISP modems.  In the author's case, this is a MikroTik RouterBoard 1100AHx2 router.  The router contains the public key for an SSH user ("HMFIC"), and it has three scripts that can be run remotely (via SSH):
  - *set_primary*: sets the default route to the primary ISP modem, and returns "SUCCESS" or "ERROR".
  - *set_secondary*: sets the default route to the secondary ISP modem, and returns "SUCCESS" or "ERROR".
  - *get_isp*: returns "PRIMARY" if the default route is currently set to the primary ISP modem, "SECONDARY" if the default route is currently set to the secondary ISP modem, or "ERROR" if neither. 
- **Linux Server**: this box hosts the ISPMonitor program, which runs as a systemd service.  In the author's case, this is a Dell PowerEdge T640.
- **ISPMonitor**: a Java program that uses dig to monitor the state of the ISP connections.  When appropriate, it also runs router scripts (via SSH) to change the router's default route to an ISP that is up.  This program normally runs as the user *isp_mon*, which has a private key for SSH into the router.