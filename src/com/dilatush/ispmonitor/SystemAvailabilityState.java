package com.dilatush.ispmonitor;

/**
 * Holds the state of the availability of a subsystem, updated periodically by reports of the current state of the subsystem.
 * <p>Instances of this class are mutable and <i>not</i> threadsafe.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class SystemAvailabilityState {

    private final int downDelay;
    private final int upDelay;

    private SystemAvailability rawState;
    private SystemAvailability state;
    private int downCount;
    private int upCount;


    /**
     * Creates a new instance of {@link SystemAvailabilityState} with the specified starting state and no delays in state changes.  A non-zero up
     * delay specifies the number of sequential updates reporting up before the processed state changes to up.  Similarly, a non-zero down delay
     * specifies the number of sequential updates reporting down before the processed state changes to down.  An update of unknown immediately changes
     * the processed state to unknown.
     *
     * @param _startingState the starting state for this instance
     */
    public SystemAvailabilityState( final SystemAvailability _startingState ) {
        rawState  = _startingState;
        state     = _startingState;
        downDelay = 0;
        upDelay   = 0;
    }


    /**
     * Creates a new instance of {@link SystemAvailabilityState} with the specified starting state and the specified delays in state changes.
     *
     * @param _startingState the starting state for this instance
     */
    public SystemAvailabilityState( final SystemAvailability _startingState, final int _upDelay, final int _downDelay ) {
        rawState  = _startingState;
        state     = _startingState;
        downDelay = _downDelay;
        upDelay   = _upDelay;
    }


    /**
     * Returns the processed state, which (depending on how this instance is configured) may be different than the raw state.
     *
     * @return the processed state
     */
    public SystemAvailability getState() {
        return state;
    }


    /**
     * Returns the raw state, which (depending on how this instance is configured) may be different than the processed state.
     *
     * @return the raw state
     */
    public SystemAvailability getRawState() {
        return rawState;
    }


    /**
     * Update the state of this subsystem with the specified current state. The raw state will be immediately updated, but the processed state may
     * not be, depending on whether up delays or down delays have been specified in this instance.
     *
     * @param _currentState the observed current state of this subsystem
     */
    public void update( final SystemAvailability _currentState ) {

        // if our raw state changes, clear the counters...
        if( (_currentState != rawState) || (_currentState == SystemAvailability.UNKNOWN) ) {
            upCount = 0;
            downCount = 0;
        }

        rawState = _currentState;

        if( rawState != state ) {

            switch( rawState ) {

                case UP:
                    if( upCount < upDelay ) {
                        upCount++;
                    } else {
                        state = rawState;
                    }
                    break;

                case DOWN:
                    if( downCount < downDelay ) {
                        downCount++;
                    } else {
                        state = rawState;
                    }
                    break;

                case UNKNOWN:
                    state = rawState;
                    break;
            }
        }
    }
}
