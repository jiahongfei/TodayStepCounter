# Android计步模块（类似微信运动，支付宝计步，今日步数）

[Android计步模块优化（今日步数）](http://www.jianshu.com/p/cfc2a200e46d)

[Android计步模块优化（今日步数）V2.0.0](https://www.jianshu.com/p/1b53937150ad)

![图片源于网络.png](http://upload-images.jianshu.io/upload_images/4158487-ef235914605842d1.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

### 功能
1. 返回当天运动步数
2. 内部自动切换计步算法，适配所有手机
3. 通过AIDL对外暴露接口
4. 采用单独进程计步

### 优化点
1. 适配Android8.0系统
3. TYPE_ACCELEROMETER和TYPE_STEP_COUNTER传感器自动切换
4. 只提供当天的步数数据
5. 解决一些bug
6. 对关键位置增加日志信息（日志系统底层需要自己实现）

[开源算法](https://github.com/finnfu/stepcount)这个是源码，如果有大神对他进行优化，非常欢迎和我进行讨论。

#### 问题
1. 用户后台保活（对于加速度传感器必须后台保活），每个手机都不一样无法提供通用的标准操作
2. 早上打开一次，计步器会开始计步
3. 重启手机需要打开app，否则步数丢失
4. 如果遇到当天步数不准，或者不记步，需要重启手机，android计步协处理器会出现bug
5. 会有部分清零和极大值出现，这也是由于android计步协处理器出现问题导致的
6. 卸载app步数会清空，归零。

