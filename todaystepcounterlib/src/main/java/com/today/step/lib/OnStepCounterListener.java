package com.today.step.lib;

/**
 * Created by jiahongfei on 2017/6/30.
 */

public interface OnStepCounterListener {

    /**
     * 用于显示步数
     * @param step
     */
    void onChangeStepCounter(int step);

    /**
     * 用于保存数据
     * @param step
     * @param millisecond
     */
    void onSaveStepCounter(int step, long millisecond);

}
