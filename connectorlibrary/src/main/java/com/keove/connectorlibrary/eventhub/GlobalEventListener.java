package com.keove.connectorlibrary.eventhub;

/**
 *
 * Created by cagriozdes on 12/24/16.
 */

public interface GlobalEventListener {

    public abstract void onGlobalEvent(String eventName, Object data, boolean flag, int code);

}
