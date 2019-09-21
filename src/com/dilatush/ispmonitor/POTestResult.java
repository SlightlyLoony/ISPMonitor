package com.dilatush.ispmonitor;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class POTestResult {

    /* package-private */ final SystemAvailability availability;
    /* package-private */ final String             postOffice;


    public POTestResult( final SystemAvailability _availability, final String _postOffice ) {
        availability = _availability;
        postOffice   = _postOffice;
    }
}
