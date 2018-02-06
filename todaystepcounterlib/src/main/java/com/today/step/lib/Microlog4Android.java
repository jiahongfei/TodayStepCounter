package com.today.step.lib;

import android.content.Context;

import com.google.code.microlog4android.LoggerFactory;
import com.google.code.microlog4android.appender.FileAppender;
import com.google.code.microlog4android.config.PropertyConfigurator;

/**
 * @author :  jiahongfei
 * @email : jiahongfeinew@163.com
 * @date : 2018/2/6
 * @desc :
 */

public class Microlog4Android {

    private static final com.google.code.microlog4android.Logger logger = LoggerFactory.getLogger();

    public void configure(Context context){
        if(null != logger) {
            PropertyConfigurator.getConfigurator(context).configure();
            FileAppender appender = (FileAppender) logger.getAppender(1);
            appender.setAppend(true);
            logger.addAppender(appender);
        }
    }

    public void error(Object message){
        if(null != logger) {
            logger.error(message);
        }
    }

}
