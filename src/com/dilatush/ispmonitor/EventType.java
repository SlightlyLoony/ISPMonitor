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
    PrimaryISPDNS1State             ( SystemAvailability.class ),
    PrimaryISPDNS2State             ( SystemAvailability.class ),
    SecondaryISPDNS1State           ( SystemAvailability.class ),
    SecondaryISPDNS2State           ( SystemAvailability.class ),
    PrimaryISPRawState              ( SystemAvailability.class ),
    SecondaryISPRawState            ( SystemAvailability.class ),
    PrimaryISPAvailabilityChanged   ( SystemAvailability.class ),
    SecondaryISPAvailabilityChanged ( SystemAvailability.class ),
    CPOQueryTimeout                 ( null ),
    POConnected                     ( String.class ),   // MOP post office name
    PODisconnected                  ( String.class ),   // MOP post office name
    RouterISP                       ( ISPUsed.class ),
    RouterSet                       ( ISPUsed.class );

    public final Class payloadClass;

    private EventType( final Class _payloadClass ) {
        payloadClass = _payloadClass;
    }
}
