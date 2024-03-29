package nl.speyk.nifi.microsoft.graph.processors.utils;

public final class CalendarAttributes {

    //Microsoft allows max 4 concurrent tasks on a mailbox
    public final static int GRAPH_MAILBOX_CONCURRENCY_LIMIT = 4;

    //We want to retry in case of the following errors
    public final static int GRAPH_HTTP_TO_MANY_REQUESTS = 429;
    public final static int GRAPH_HTTP_SERVICE_UNAVAILABLE = 503;
    public final static int GRAPH_HTTP_GATEWAY_TIMEOUT = 504;

    //Something is wrong with the given json format
    public final static int GRAPH_BAD_REQUEST = 400;

    //flow file attribute keys returned after reading the response
    public final static String EXCEPTION_CLASS = "invokeMSGraph.java.exception.class";
    public final static String EXCEPTION_MESSAGE = "invokeMSGraph.java.exception.message";

    //Compounded key(partition/row) for storing events in a distributed map cache
    public final static String PARTITION_KEY = "event";
}