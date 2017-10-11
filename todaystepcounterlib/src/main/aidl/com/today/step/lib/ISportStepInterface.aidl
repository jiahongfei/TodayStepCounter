// ISportStepInterface.aidl
package com.today.step.lib;

interface ISportStepInterface {
    /**
     * 获取当前时间运动步数
     */
     int getCurrentTimeSportStep();

     /**
      * 获取当天步数列表，json格式
      */
     String getTodaySportStepArray();
}
