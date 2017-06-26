package com.today.step.lib;

/**
 * Created by jiahongfei on 2017/6/26.
 */

public interface UploadSportStepResponse {

    void onSuccess(String response);

    void onFails(String error);
}
