package com.dilatush.ispmonitor;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ConnectivityTestResult {

    public final SystemAvailability            availability;
    public final String                        name;
    public final ConnectivityTestResultHandler handler;


    public ConnectivityTestResult( final ConnectivityTestResultHandler _handler, final SystemAvailability _availability, final String _name ) {
        handler      = _handler;
        availability = _availability;
        name         = _name;
    }


    public String toString() {
        return name + " is " + availability;
    }
}
