package edu.ncsu.store.persistence;

import org.apache.log4j.Logger;

public class Profiler {

    /* Keep all loggers transient so that they are not passed over RMI call */
    private final transient static Logger logger = Logger.getLogger(Profiler.class);

    public enum Event {

        GET_INDEX_READ("GET read from index file"),
        GET_DATA_READ("GET read from data file"),
        PUT_DATA_WRITE("PUT write to data file"),
        PUT_INDEX_WRITE("PUT write to index file"),
        PUT_INDEX_READ("PUT read from index file");

        private final String message;

        private Event(String msg) {
            this.message = msg;
        }

        public String getMessage() {
            return message;
        }

    }

    public static void logTimings(Event eventType, long before, long after) {
        logger.debug(eventType.getMessage() + " took " + (after - before) + "ms");
    }
}
