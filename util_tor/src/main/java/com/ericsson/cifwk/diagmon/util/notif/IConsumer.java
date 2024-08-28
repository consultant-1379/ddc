package com.ericsson.cifwk.diagmon.util.notif;

public interface IConsumer {
    void detach() throws Exception; //NOPMD
    void ping();
}
