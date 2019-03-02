// ISportStepInterface.aidl
package com.today.step.lib;

interface ISportStepInterface {
    /**
     * 获取当前时间运动步数
     */
     int getCurrentTimeSportStep();

     /**
      * 获取所有步数列表，json格式，如果数据过多建议在线程中获取，否则会阻塞UI线程
      */
     String getTodaySportStepArray();
}
