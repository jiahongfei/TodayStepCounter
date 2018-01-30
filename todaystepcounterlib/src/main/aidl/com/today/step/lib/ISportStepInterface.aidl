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

    /**
     * 根据时间获取步数列表
     *
     * @param dateString 格式yyyy-MM-dd
     * @return
     */
     String getTodaySportStepArrayByDate(String date);

     /**
      * 根据时间和天数获取步数列表
      * 例如：
      * startDate = 2018-01-15
      * days = 3
      * 获取 2018-01-15、2018-01-16、2018-01-17三天的步数
      *
      * @param startDate 格式yyyy-MM-dd
      * @param days
      * @return
      */
      String getTodaySportStepArrayByStartDateAndDays(String date, int days);
}
