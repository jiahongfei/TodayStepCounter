package com.today.step;

import android.app.Application;

import com.today.step.lib.Logger;
import com.today.step.lib.UploadSportStepNetwork;
import com.today.step.lib.UploadSportStepResponse;

/**
 * Created by jiahongfei on 2017/6/26.
 */

public class UploadSportStepNetworkImpl extends UploadSportStepNetwork {

    public UploadSportStepNetworkImpl(Application application) {
        super(application);
    }

    @Override
    public void postSportStepNum(String sportStepJson, UploadSportStepResponse uploadSportStepResponse) {
        Logger.e("","request : " + sportStepJson);
        uploadSportStepResponse.onSuccess("上传成功");
    }
}
