#ISPMonitor Design Notes
The nature of ISPMonitor dictates that a lot of asynchronous activity is going on, all the time.  In addition there is some activity that is time based.  This makes a clean, simple design highly desirable.  That design is what this document describes.
##State Machine
The ISPMonitor's state machine (an instance of StateMachine) is the heart of ISPMonitor.  It accepts asynchronous events that are processed in the order that they're received, updates its internal state as needed, and performs actions as the state machine requires.  
##Event
This enum enumerates all of the events that affect the ISPMonitor state machine.  Note that events aren't necessarily indicative of a state change.  For example, an ISPState event simply notes the current state of an ISP connection, which normally will be the same state that it was on the last ISPState event.  The state machine is responsible for edge detection on such events.
##Single-threaded "executor"
This class (STExecutor) runs in its own thread.  Users of an instance of this class send it messages requesting the performance of a task (which could be anything at all).  These tasks are assumed to take more time than the calling thread would like to block for, but are limited (via timeouts, for example) to some reasonable duration, such as a few seconds.  The STExecutor instance will execute those tasks in the order that they're received.  Upon completion, the task may invoke a callback function contained within the task to inform the sender of the results.  This callback function may directly send events to the state machine.