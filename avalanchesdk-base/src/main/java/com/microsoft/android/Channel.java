package com.microsoft.android;


//TODO this class is just a dumb pipe that forwards
public class Channel {

    private static Channel sharedInstance = null;

    protected Channel() {}

    public static Channel getInstance() {
        if (sharedInstance == null) {
            sharedInstance = new Channel();
        }
        return sharedInstance;
    }

    public void handle(AvalancheDataInterface data) {
        //TODO forward data to Sending/Journalling pipeline
    }

    public void handleWithHighPriority(AvalancheDataInterface data) {
        //TODO forward data to sending/journalling pipeline but with higher prio
    }

}
