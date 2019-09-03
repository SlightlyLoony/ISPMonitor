:local gatewayStatus [:tostr [/ip route get [:pick [find dst-address=0.0.0.0/0 active=yes] 0] gateway-status]]
:local i [:find $gatewayStatus " reachable via  " -1]
:local interface
:if ($i > 1) do={
    :set interface [:pick $gatewayStatus ($i +  16) 255]
}
:if ($interface = "ether1") do={
    :put "PRIMARY"
} else={
    :if ($interface = "ether2") do={
        :put "SECONDARY"
    } else={
        :put "ERROR"
    }
}
:set interface
:set i
:set gatewayStatus
