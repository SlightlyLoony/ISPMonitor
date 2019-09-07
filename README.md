#ISP Monitor
Part of a system for monitoring the state of two Internet Service Provider (ISP) connections (primary and secondary), and automatically switching between them as the ISP connections go up or down.

The entire system includes the following components:
- **Primary ISP modem**: This modem provides the connectivity to the primary ISP.  In the author's case, this is an Xfinity-provided gigabit cable modem.
- **Secondary ISP modem**: This modem provides the connectivity to the secondary ISP.  In the author's case, this is a Cradlepoint CBA850 Branch LTE adapter, which is an LP6 LTE modem.
- **Border Router**: This is the router at the edge of the local network, connected to both of the ISP modems.  In the author's case, this is a MikroTik RouterBoard 1100AHx2 router.  The router contains the public key for an SSH user ("HMFIC"), and it has three scripts that can be run remotely (via SSH):
  - *set_primary*: sets the default route to the primary ISP modem, and returns "SUCCESS" or "ERROR".
  - *set_secondary*: sets the default route to the secondary ISP modem, and returns "SUCCESS" or "ERROR".
  - *get_isp*: returns "PRIMARY" if the default route is currently set to the primary ISP modem, "SECONDARY" if the default route is currently set to the secondary ISP modem, or "ERROR" if neither.
  
  The router must have two interfaces dedicated to the ISP connections (one for the primary, one for the secondary).  In addition it must be configured with routes for the DNS servers used for testing, such that the primary ISP's DNS servers are routed ONLY to the primary ISP's modem, and the secondary ISP's DNS servers are routed ONLY to the secondary ISP's modem.
- **Linux Server**: this box hosts the ISPMonitor program, which runs as a systemd service.  In the author's case, this is a Dell PowerEdge T640.
- **ISPMonitor**: a Java program that uses dig to monitor the state of the ISP connections.  When appropriate, it also runs router scripts (via SSH) to change the router's default route to an ISP that is up.  This program normally runs as the user *isp_mon*, which has a private key for SSH into the router.

##Configuration
*ISPMonitor's* configuration is handled by the JSON file *isp_monitor_config.json*.  Its fields are documented below:
- **name**: The name of the ISPMonitor's post office in the message-oriented programming system.
- **secret**: The encryption secret for the message-oriented programming system.  This has no default and must be configured through the central post office manager.
- **queueSize**: The maximum number of outgoing messages that may be queued for delivery to the central post office.
- **cpoHost**: The name of the computer hosting the central post office.
- **cpoPort**: The TCP port that the central post office is listening on.
- **monitorInterval**: The interval (in seconds) between published monitor messages from ISPMonitor.
- **minTestInterval**: The minimum interval, in seconds, between tests of a DNS server.
- **maxTestInterval**: The maximum interval, in seconds, between tests of a DNS server.
- **primary**: Specification of the primary ISP connection.
  - **name**: The user-readable name of the primary ISP.
  - **dns1**: The dotted-form IP address (like 23.23.23.23) of a DNS server that can be used to test connectivity to the primary ISP.
  - **dns2**: The dotted-form IP address (like 23.23.23.23) of another DNS server that can be used to test connectivity to the primary ISP.
- **secondary**: Specification of the secondary ISP connection.
  - **name**: The user-readable name of the secondary ISP.
  - **dns1**: The dotted-form IP address (like 23.23.23.23) of a DNS server that can be used to test connectivity to the secondary ISP.
  - **dns2**: The dotted-form IP address (like 23.23.23.23) of another DNS server 
- **DNSDigger**: Specification of the DNSDigger configuration.
  - **domains**: Comma-separated list of domain names that can be used for testing DNS server accessibility and functionality.  These should be popular, unlikely-to-be-abandoned domain names (like google.com, wikipedia.org, etc.).
  - **statisticsPeriod**: The period that DNSDigger should keep availability statistics for, in milliseconds (86,400,000 is one day).
  - **tries**: The number of times that DNSDigger should try to "dig" a DNS server before concluding that the server is down or inaccessible.  Note that each additional try potentially adds one second to the test time.