package com.dilatush.ispmonitor;

/**
 * Enumerates all the possible events that can occur to the ISPMonitor {@link StateMachine}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public enum EventType {

    PrimaryISPDNS1State,
    PrimaryISPDNS2State,
    SecondaryISPDNS1State,
    SecondaryISPDNS2State,
    PrimaryISPRawState,
    SecondaryISPRawState,
    PrimaryISPAvailabilityChanged,
    SecondaryISPAvailabilityChanged,
}
