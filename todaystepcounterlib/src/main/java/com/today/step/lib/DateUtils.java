package com.today.step.lib;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Description: 时间工具类（时间格式转换方便类）
 */
class DateUtils {

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat();

    /**
     * 返回一定格式的当前时间
     *
     * @param pattern "yyyy-MM-dd HH:mm:ss E"
     * @return
     */
    public static String getCurrentDate(String pattern) {
        SIMPLE_DATE_FORMAT.applyPattern(pattern);
        Date date = new Date(System.currentTimeMillis());
        String dateString = SIMPLE_DATE_FORMAT.format(date);
        return dateString;

    }

    public static long getDateMillis(String dateString, String pattern) {
        long millionSeconds = 0;
        SIMPLE_DATE_FORMAT.applyPattern(pattern);
        try {
            millionSeconds = SIMPLE_DATE_FORMAT.parse(dateString).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }// 毫秒

        return millionSeconds;
    }

    /**
     * 格式化输入的millis
     *
     * @param millis
     * @param pattern yyyy-MM-dd HH:mm:ss E
     * @return
     */
    public static String dateFormat(long millis, String pattern) {
        SIMPLE_DATE_FORMAT.applyPattern(pattern);
        Date date = new Date(millis);
        String dateString = SIMPLE_DATE_FORMAT.format(date);
        return dateString;
    }

    /**
     * 将dateString原来old格式转换成new格式
     *
     * @param dateString
     * @param oldPattern yyyy-MM-dd HH:mm:ss E
     * @param newPattern
     * @return oldPattern和dateString形式不一样直接返回dateString
     */
    public static String dateFormat(String dateString, String oldPattern,
                                    String newPattern) {
        long millis = getDateMillis(dateString, oldPattern);
        if (0 == millis) {
            return dateString;
        }
        String date = dateFormat(millis, newPattern);
        return date;
    }

}
