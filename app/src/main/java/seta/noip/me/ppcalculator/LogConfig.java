package seta.noip.me.ppcalculator;

/**
 * Just static configuration to control logging.
 * Based on https://stackoverflow.com/a/2448327
 *
 * if (MyDebug.LOG) {
 if (condition) Log.i(...);
 }

 Now when you set MyDebug.LOG to false, the compiler will strip out all code inside such checks (since it is a static final, it knows at compile time that code is not used.)
 * Created by vachi on 24-Nov-17.
 */

class LogConfig {
    static final boolean LOG = true;
}
