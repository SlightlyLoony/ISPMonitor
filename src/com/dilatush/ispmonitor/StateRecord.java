package com.dilatush.ispmonitor;

/**
 * Simple POJO to hold a record of the state of a DNS server.
 * <p>Instances of this class are immutable and threadsafe.</p>
 */
class StateRecord {
    public final SystemAvailability systemAvailability;
    public final long timestamp;


    /**
     * Creates a new instance of this class with the given attributes.
     *
     * @param _systemAvailability  the state (UP or DOWN) of the DNS server
     */
    public StateRecord( final SystemAvailability _systemAvailability ) {
        systemAvailability = _systemAvailability;
        timestamp = System.currentTimeMillis();
    }
}
