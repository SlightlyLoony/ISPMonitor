package com.dilatush.ispmonitor;

/**
 * Enumerates all the possible events that can occur to the ISPMonitor {@link StateMachine}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public enum EventType {

    Heartbeat                       ( null ),
    Start                           ( null ),
    SSHResult                       ( SSHResult.class ),
    DNSResult                       ( DNSResult.class ),
    ISPAvailabilityChanged          ( ISP.class ),
    ISPChangeNeeded                 ( EdgeRouter.class ),
    PostOfficeTest                  ( POTestResult.class ),
    RouterISP                       ( ISPChoice.class );

    public final Class payloadClass;

    private EventType( final Class _payloadClass ) {
        payloadClass = _payloadClass;
    }
}
