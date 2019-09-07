package com.dilatush.ispmonitor;

/**
 * Simple POJO to hold a record of the state of a DNS server.
 * <p>Instances of this class are immutable and threadsafe.</p>
 */
class StateRecord {
    public final State state;
    public final long timestamp;


    /**
     * Creates a new instance of this class with the given attributes.
     *
     * @param _state  the state (UP or DOWN) of the DNS server
     */
    public StateRecord( final State _state ) {
        state = _state;
        timestamp = System.currentTimeMillis();
    }
}
