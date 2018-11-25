package com.keove.connectorlibrary.eventhub;

import android.app.Application;
import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 *
 * Created by cagriozdes on 12/24/16.
 */

public class GlobalEventApplication extends Application {


    HashMap<String,ArrayList<GlobalEventListener>> map = new HashMap<>();

    public static Context applicationContext = null;
}
