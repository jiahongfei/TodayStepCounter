package com.andrjhf.lib.jlogger;

public class JLoggerConstant {

    /**
     * app热启动
     */
    public static final String JLOGGER_APP_HOT_START = "jlogger_app_hot_start";

    /**
     * 设备信息
     * map.put("BRAND", Build.BRAND);  //samsung
     * map.put("MANUFACTURER" , Build.MANUFACTURER);//samsung
     * map.put("MODEL" ,  Build.MODEL);//SM-G9500
     * map.put("PRODUCT" , Build.PRODUCT);//dreamqltezc
     * map.put("RELEASE" ,  android.os.Build.VERSION.RELEASE);//8.0.0
     * map.put("SDK_INT" ,  String.valueOf(Build.VERSION.SDK_INT));//26
     * map.put("APP_Version" ,  DeviceInfo.getAppVersion(context));
     * map.put("APP_Build" ,  DeviceInfo.getAppVersionCode(context));
     */
    public static final String JLOGGER_DEVICE_INFO = "jlogger_device_info";

    /**
     * Sensor.TYPE_STEP_COUNTER 传感器计步，定时获取传感器监听中的参数
     */
    public static final String JLOGGER_TYPE_STEP_COUNTER_TIMER = "jlogger_type_step_count_timer";
    /**
     * Sensor.TYPE_STEP_COUNTER 传感器计步,监听器构造中一些本地数据的展示
     */
    public static final String JLOGGER_TYPE_STEP_CONSTRUCTOR = "jlogger_type_step_constructor";
    /**
     * Sensor.TYPE_STEP_COUNTER 在onSensorChanged中回调，mCleanStep=true,调用了计步清零操作
     */
    public static final String JLOGGER_TYPE_STEP_CLEANSTEP = "jlogger_type_step_cleans_currStep_and_offsetStep";
    /**
     * Sensor.TYPE_STEP_COUNTER 在onSensorChanged中回调，判断出用户做了关机处理，做偏移量修改
     */
    public static final String JLOGGER_TYPE_STEP_SHUTDOWN = "jlogger_type_step_shutdown";
    /**
     * Sensor.TYPE_STEP_COUNTER 在onSensorChanged中回调，判断出sCurrStep < 0，进行容错处理，调用cleanStep（）
     */
    public static final String JLOGGER_TYPE_STEP_TOLERANCE = "jlogger_type_step_tolerance";
    /**
     * TodayStepService onCreate中，最初的展示到view上的步数
     */
    public static final String JLOGGER_SERVICE_INITIALIZE_CURRSTEP = "jlogger_service_initialize_currStep";
    /**
     * TodayStepService onStartCommand中，主进程复活后，首次连接从未挂掉的服务进程，传递的参数和此时服务进程记录的步数
     */
    public static final String JLOGGER_SERVICE_ONSTARTCOMMAND = "jlogger_service_onStartCommand";
    /**
     * TodayStepService onBind中，提供给主进程的步数
     */
    public static final String JLOGGER_SERVICE_ONBIND = "jlogger_service_onBind";
    /**
     * TodayStepService，主进程复活后，连接未挂掉的服务进程，Sensor.TYPE_STEP_COUNTER已经注册过
     */
    public static final String JLOGGER_SERVICE_TYPE_STEP_COUNTER_HADREGISTER = "jlogger_service_type_step_counter_hadRegister";
    /**
     * TodayStepService，注册Sensor.TYPE_STEP_COUNTER监听
     */
    public static final String JLOGGER_SERVICE_TYPE_STEP_COUNTER_REGISTER = "jlogger_service_type_step_counter_register";
    /**
     * TodayStepService，主进程复活后，连接未挂掉的服务进程，Sensor.TYPE_ACCELEROMETER已经注册过
     */
    public static final String JLOGGER_SERVICE_TYPE_ACCELEROMETER_HADREGISTER = "jlogger_service_type_accelerometer_hadregister";
    /**
     * TodayStepService，注册Sensor.TYPE_ACCELEROMETER监听
     */
    public static final String JLOGGER_SERVICE_TYPE_ACCELEROMETER_REGISTER = "jlogger_service_type_accelerometer_register";
    /**
     * TodayStepService，数据库插入数据
     */
    public static final String JLOGGER_SERVICE_INSERT_DB = "jlogger_service_insert_db";
    /**
     * TodayStepService，清除数据库数据
     */
    public static final String JLOGGER_SERVICE_CLEAN_DB = "jlogger_service_clean_db";
    /**
     * TodayStepService，设置传感器速率
     */
    public static final String JLOGGER_SERVICE_SENSORRATE_INVOKE = "jlogger_service_sensorrate_invoke";
    /**
     * TodayStepFragment，通过onBind与Service连接
     */
    public static final String JLOGGER_FRAGMENT_ONSERVICECONNECTED = "jlogger_fragment_onServiceConnected";
    /**
     * TodayStepFragment，service手动关闭了
     */
    public static final String JLOGGER_FRAGMENT_ONSERVICEDISCONNECTED = "jlogger_fragment_onServiceDisconnected";
    /**
     * TodayStepDetector，Sensor.TYPE_ACCELEROMETER，构造函数中，本地保存的数据
     */
    public static final String JLOGGER_TYPE_ACCELEROMETER_CONSTRUCTOR = "jlogger_type_accelerometer_constructor";
    /**
     * TodayStepDetector，Sensor.TYPE_ACCELEROMETER，零点清零
     */
    public static final String JLOGGER_TYPE_ACCELEROMETER_DATECHANGECLEANSTEP = "jlogger_type_accelerometer_dateChangeCleanStep";
    /**
     * TodayStepDetector，Sensor.TYPE_ACCELEROMETER，传感器计步，定时获取传感器监听中的参数
     */
    public static final String JLOGGER_TYPE_ACCELEROMETER_TIMER = "jlogger_type_accelerometer_timer";
    /**
     * MyOrdersActivity，用户进入保单页面
     */
    public static final String JLOGGER_USER_INTO_MYORDERSACTIVITY = "jlogger_user_into_MyOrdersActivity";
    /**
     * ModuleItemTypeHealthTopWidgetAndRun,用户
     * */
    public static final String JLOGGER_USER_INTO_HEALTHFRAGMENT = "jlogger_user_into_healthFragment";
    /**
     * 用户上传步数-成功
     * */
    public static final String JLOGGER_UP_STEP_SUCCESS = "jlogger_up_step_success";
    /**
     * 用户上传步数-失败
     * */
    public static final String JLOGGER_UP_STEP_FAILURE = "jlogger_up_step_failure";
    /**
     * 触发上传步数事件--点击上传按钮
     * */
    public static final String JLOGGER_UP_STEP_BUTTON = "jlogger_up_step_button";
    /**
     * 运动历史页面
     * */
    public static final String JLOGGER_UP_STEP_HISTORY = "jlogger_up_step_history";
    /**
     * 触发上传步数事件--健康页面下拉刷新
     * */
    public static final String JLOGGER_UP_STEP_PULLDOWN= "jlogger_up_step_pullDown";
    /**
     * 触发上传步数事件--边走边赚模块
     * */
    public static final String JLOGGER_UP_STEP_HEALTHSPORTSTEP= "jlogger_up_step_healthSportStep";
    /**
     * 触发上传步数事件--边走边赚模块--领取积分
     * */
    public static final String JLOGGER_UP_STEP_GET_INTEGRAL= "jlogger_up_step_get_integral";
    /**
     * 触发上传步数事件--健康首页初始化时
     * */
    public static final String JLOGGER_UP_STEP_FIRST_INTO_HEALTHFRAGMENT= "jlogger_up_step_first_into_healthFragment";
    /**
     * 触发上传步数事件--TodayStepFragment,5分钟循环上传
     * */
    public static final String JLOGGER_UP_STEP_CIRCULATION_FIVE= "jlogger_up_step_circulation_five";
    /**
     * 触发上传步数事件--TodayStepFragment,初始化时，上传
     * */
    public static final String JLOGGER_UP_STEP_TODAYSTEPFRAGMENT_ONACTIVITYCREATED= "jlogger_up_step_TodayStepFragment_onActivityCreated";
    /**
     * 触发上传步数事件--TodayStepFragment,event触发
     * */
    public static final String JLOGGER_UP_STEP_TODAYSTEPFRAGMENT_EVENT= "jlogger_up_step_TodayStepFragment_event";
    /**
     * SportAwardActivity --进入续保奖励页面
     * */
    public static final String JLOGGER_USER_INTO_SPORTAWARDACTIVITY= "jlogger_user_into_SportAwardActivity";
    /**
     * Sensor.TYPE_STEP_COUNTER 传感器计步，时间改变了清零，或者0点分隔回调
     * */
    public static final String JLOGGER_TYPE_STEP_COUNTER_DATECHANGECLEANSTEP= "jlogger_type_step_counter_dateChangeCleanStep";
    /**
     * 用户进行了登录操作
     * */
    public static final String JLOGGER_USER_LOGIN= "jlogger_user_login";
    /**
     * Sensor.TYPE_STEP_COUNTER 传感器计步,监听器构造中一些本地数据的展示
     * */
    public static final String JLOGGER_TYPE_STEP_SHUTDOWNBYSYSTEMRUNNINGTIME= "jlogger_type_step_shutdownBySystemRunningTime";
    /**
     * TodayStepService onDestroy
     * */
    public static final String JLOGGER_TODAYSTEPSERVICE_ONDESTROY= "jlogger_TodayStepService_onDestroy";
    /**
     * TodayStepService onUnbind
     * */
    public static final String JLOGGER_TODAYSTEPSERVICE_ONUNBIND= "jlogger_TodayStepService_onUnbind";
    /**
     * Sensor.TYPE_STEP_COUNTER 传感器计步,监听器构造中一些本地数据的展示
     * */
    public static final String JLOGGER_TYPE_STEP_SHUTDOWNBYCOUNTERSTEP= "jlogger_type_step_shutdownByCounterStep";
    /**
     * TodayStepFragment 上传数据库中的运动轨迹
     * */
    public static final String JLOGGER_TODAYSTEPFRAGMENT_POSTSPORTSTEPNUM= "jlogger_TodayStepFragment_postSportStepNum";
}
