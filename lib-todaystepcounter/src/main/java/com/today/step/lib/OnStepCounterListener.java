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
     * 步数清零监听，由于跨越0点需要重新计步
     */
    void onStepCounterClean();

}
