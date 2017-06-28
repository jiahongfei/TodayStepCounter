package com.today.step.lib;

import org.apache.log4j.Logger;

import de.mindpipe.android.logging.log4j.LogConfigurator;

/**
 * Created by jiahongfei on 2017/6/27.
 */

public class Log4j {

    private static Logger logger = null;
    private static boolean sIsDebug = false;

    public Log4j(Class clazz, String logFileName){
        LogConfigurator logConfigurator = new LogConfigurator();
        logConfigurator.setFileName(logFileName);
        logConfigurator.setMaxFileSize(1024 * 1024 * 500);
        logConfigurator.setImmediateFlush(true);
        logConfigurator.configure();
        logger = Logger.getLogger(clazz);
    }

    public void d(String message) {
        if (sIsDebug)
            logger.debug( message);
    }

    public void i( String message) {
        if (sIsDebug)
            logger.info(message);
    }

    public void w( String message) {
        if (sIsDebug)
            logger.warn(message);
    }

    public static void e(String message) {
        if (sIsDebug)
            logger.error(message);
    }


}