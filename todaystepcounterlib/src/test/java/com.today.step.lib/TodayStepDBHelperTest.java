package com.today.step.lib;

import android.app.Application;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.List;

/**
 * @author :  jiahongfei
 * @email : jiahongfeinew@163.com
 * @date : 2018/1/22
 * @desc :
 */
@RunWith(MyRobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class TodayStepDBHelperTest {

    private static final String SPORT_DATE = "sportDate";
    private static final String STEP_NUM = "stepNum";

    private ITodayStepDBHelper mTodayStepDBHelper;

    @Before
    public void before(){

        Application application = RuntimeEnvironment.application;

        System.out.println(application);

        mTodayStepDBHelper = TodayStepDBHelper.factory(application);

    }

    @Test
    public void insert(){

        //10天
        for (int i = 0; i<10; i++){
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(DateUtils.getDateMillis("2018-01-01","yyyy-MM-dd"));
            if(4 == i || 5 == i || 7 == i || 8 == i){
                continue;
            }
            calendar.add(Calendar.DAY_OF_YEAR,i);
            //每天存储5次步数
            for (int j = 0; j<5; j++){
                TodayStepData todayStepData = new TodayStepData();
                todayStepData.setToday(DateUtils.dateFormat(calendar.getTimeInMillis(),"yyyy-MM-dd"));
                todayStepData.setStep(100*j);
                Calendar hourCalendar = Calendar.getInstance();
                hourCalendar.setTimeInMillis(DateUtils.getDateMillis("2018-01-01 08:00","yyyy-MM-dd HH:mm"));
                hourCalendar.add(Calendar.HOUR_OF_DAY,j);
                todayStepData.setDate(hourCalendar.getTimeInMillis());
                mTodayStepDBHelper.insert(todayStepData);
            }
        }

        List<TodayStepData> todayStepDataList = mTodayStepDBHelper.getQueryAll();
        JSONArray jsonArray = getSportStepJsonArray(todayStepDataList);
        System.out.println(jsonArray.toString());

        todayStepDataList = mTodayStepDBHelper.getStepListByDate("2018-01-01");
        jsonArray = getSportStepJsonArray(todayStepDataList);
        System.out.println(jsonArray.toString());

        todayStepDataList = mTodayStepDBHelper.getStepListByStartDateAndDays("2018-01-03",3);
        jsonArray = getSportStepJsonArray(todayStepDataList);
        System.out.println(jsonArray.toString());



//        mTodayStepDBHelper.clearCapacity("2018-01-10",7);
//
//        todayStepDataList = mTodayStepDBHelper.getQueryAll();
//        jsonArray = getSportStepJsonArray(todayStepDataList);
//        Systemhttp://pa.mokous.com/share/renewal/esb_plus_award_notice.html.out.println(jsonArray.toString());


    }


    private JSONArray getSportStepJsonArray(List<TodayStepData> todayStepDataArrayList){
        JSONArray jsonArray = new JSONArray();
        if (null == todayStepDataArrayList || 0 == todayStepDataArrayList.size()) {
            return jsonArray;
        }
        for (int i = 0; i < todayStepDataArrayList.size(); i++) {
            TodayStepData todayStepData = todayStepDataArrayList.get(i);
            try {
                JSONObject subObject = new JSONObject();
                subObject.put(TodayStepDBHelper.TODAY, todayStepData.getToday());
                subObject.put(SPORT_DATE, DateUtils.dateFormat(todayStepData.getDate(),"yyyy-MM-dd HH:mm"));
                subObject.put(STEP_NUM, todayStepData.getStep());
                jsonArray.put(subObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jsonArray;
    }
}
