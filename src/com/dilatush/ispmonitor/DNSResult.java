package com.dilatush.ispmonitor;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class DNSResult {

    public final DNSResultHandler handler;
    public final DNSResultType    type;
    public final long             actualTimeMS;
    public final int              actualTries;
    public final String           ip;


    public DNSResult( final DNSResultHandler _handler, final DNSResultType _type, final long _actualTimeMS, final int _actualTries, final String _ip ) {
        handler      = _handler;
        type         = _type;
        actualTimeMS = _actualTimeMS;
        actualTries  = _actualTries;
        ip           = _ip;
    }


    public String toString() {
        return "Queried " + ip + ", " + type + ", actual time: " + actualTimeMS + "ms; tries: " + actualTries;
    }
}
