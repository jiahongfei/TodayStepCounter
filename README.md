# Android计步模块（类似微信运动，支付宝计步，今日步数）

[介绍请查看简书点击这里](http://www.jianshu.com/p/cfc2a200e46d)

#### 接入方法
1.先下载计步demo TodayStepCounter<br>
2.demo项目结构如下图：<br>
![TodayStepCounter项目结构图.png](http://upload-images.jianshu.io/upload_images/4158487-33c3d03eb306c583.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
由图可见*todaystepcounterlib*是计步模块封装好的Module，它对外提供的接口就是*ISportStepInterface.aidl*<br>
3.如何接入：<br>
查看对外接口*ISportStepInterface.aidl*如下代码：<br>
```
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
```
查看使用代码*MainActivity.java*，里面关键代码有注释非常简单<br>
```
public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";
    private static final int REFRESH_STEP_WHAT = 0;
    //循环取当前时刻的步数中间的间隔时间
    private long TIME_INTERVAL_REFRESH = 500;
    private Handler mDelayHandler = new Handler(new TodayStepCounterCall());
    private int mStepSum;
    private ISportStepInterface iSportStepInterface;
    private TextView mStepArrayTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化计步模块
        TodayStepManager.init(getApplication());
        mStepArrayTextView = (TextView)findViewById(R.id.stepArrayTextView);
        //开启计步Service，同时绑定Activity进行aidl通信
        Intent intent = new Intent(this, TodayStepService.class);
        startService(intent);
        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                //Activity和Service通过aidl进行通信
                iSportStepInterface = ISportStepInterface.Stub.asInterface(service);
                try {
                    mStepSum = iSportStepInterface.getCurrentTimeSportStep();
                    updateStepCount();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
     mDelayHandler.sendEmptyMessageDelayed(REFRESH_STEP_WHAT, TIME_INTERVAL_REFRESH);

            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        }, Context.BIND_AUTO_CREATE);
    }
    class TodayStepCounterCall implements Handler.Callback{
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH_STEP_WHAT: {
                    //每隔500毫秒获取一次计步数据刷新UI
                    if (null != iSportStepInterface) {
                        int step = 0;
                        try {
                            step = iSportStepInterface.getCurrentTimeSportStep();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        if (mStepSum != step) {
                            mStepSum = step;
                            updateStepCount();
                        }
                    }
                 mDelayHandler.sendEmptyMessageDelayed(REFRESH_STEP_WHAT, TIME_INTERVAL_REFRESH);
                    break;
                }
            }
            return false;
        }
    }
    private void updateStepCount() {
        Log.e(TAG,"updateStepCount : " + mStepSum);
        TextView stepTextView = (TextView)findViewById(R.id.stepTextView);
        stepTextView.setText(mStepSum + "步");
    }
    public void onClick(View view){
        switch (view.getId()){
            case R.id.stepArrayButton:{
                //显示当天计步数据详细，步数对应当前时间
                if(null != iSportStepInterface){
                    try {
                        String stepArray = iSportStepInterface.getTodaySportStepArray();
                        mStepArrayTextView.setText(stepArray);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            default:break;
        }
    }
}
```
