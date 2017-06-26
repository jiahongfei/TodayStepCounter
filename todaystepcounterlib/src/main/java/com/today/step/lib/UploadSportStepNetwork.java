package com.today.step.lib;

import android.app.Application;

/**
 * Created by jiahongfei on 2017/6/26.
 */

public class UploadSportStepNetwork {

    public UploadSportStepNetwork(Application application){

    }

    public void postSportStepNum(String sportStepJson,UploadSportStepResponse uploadSportStepResponse){
        try {
            throw new Exception("如果想上传步数需要继承这个方法");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
