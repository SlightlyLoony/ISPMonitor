package com.dilatush.ispmonitor;

/**
 * Simple POJO to contain information about the result of a command that controls a systemd service.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
/* package-private */  class ServiceActionInfo {
    /* package-private */ final String              hostName;
    /* package-private */ final String              serviceName;
    /* package-private */ final RemoteServiceAction action;
    /* package-private */ final RemoteServiceActionResult result;


    /* package-private */ServiceActionInfo( final String _hostName, final String _serviceName,
                                            final RemoteServiceAction _action, final RemoteServiceActionResult _result ) {
        hostName    = _hostName;
        serviceName = _serviceName;
        action      = _action;
        result      = _result;
    }


    public String toString() {
        return serviceName + " " + action + ": " + result;
    }
}
