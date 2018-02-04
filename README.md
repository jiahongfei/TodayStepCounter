# Android计步模块（类似微信运动，支付宝计步，今日步数）

[Android计步模块优化（今日步数）](http://www.jianshu.com/p/cfc2a200e46d)

[Android计步模块优化（今日步数）V2.0.0](https://www.jianshu.com/p/1b53937150ad)

![图片源于网络.png](http://upload-images.jianshu.io/upload_images/4158487-ef235914605842d1.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

> 针对计步模块进行优化[TodayStepCounterV2.0.0](https://github.com/jiahongfei/TodayStepCounter).
Android搞计步真是坑爹，每天都能收到很多用户进行投诉，于是我对投诉进行分析整理出几个优化点进行优化。

> [第一篇Android计步模块优化（今日步数）](https://www.jianshu.com/p/cfc2a200e46d)

> [Github TodayStepCounter](https://github.com/jiahongfei/TodayStepCounter)

![目录.png](http://upload-images.jianshu.io/upload_images/4158487-7d83f4039efda845.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

### 优化点
1. 计步器可以根据需要记录多天步数。
2. 增加根据时间返回步数列表接口。
3. 修改Android4.4以下计步算法，[开源算法](https://github.com/finnfu/stepcount)。
4. 增加使用文案。

#### 1.计步器可以根据需要记录多天步数
第一版数据库中只保存的是当天的以时间分隔的步数。

由于后台Service不联网上传数据需要打开App进行上传。有很多用户晚上忘记上传步数了导致第二天早上看前一天的步数少了很多，其实是晚上没有打开app上传步数。
由于这个问题我增加数据库可以记录多天步数，第二天用户早上打开App会上传当天和前一天的步数。

看如下代码：
```
public class TodayStepService extends Service implements Handler.Callback {
    private static final String TAG = "TodayStepService";
    /**数据库中保存多少天的运动数据*/
    private static final int DB_LIMIT = 2;
    ......
}
```
如上代码`DB_LIMIT`为数据库中保存几天的运动数据，我们可以直接修改。

#### 2. 增加根据时间返回步数列表接口
由于数据库中存储的是多天的数据，那么我们就有需要根据时间来查询某一天的运动数据，或者一段时间间隔的运动数据。

我提供了如下接口来获得。
```
interface ISportStepInterface {
    /**
     * 获取当前时间运动步数
     */
     int getCurrentTimeSportStep();

     /**
      * 获取所有步数列表，json格式，如果数据过多建议在线程中获取，否则会阻塞UI线程
      */
     String getTodaySportStepArray();

    /**
     * 根据时间获取步数列表
     *
     * @param dateString 格式yyyy-MM-dd
     * @return
     */
     String getTodaySportStepArrayByDate(String date);

     /**
      * 根据时间和天数获取步数列表
      * 例如：
      * startDate = 2018-01-15
      * days = 3
      * 获取 2018-01-15、2018-01-16、2018-01-17三天的步数
      *
      * @param startDate 格式yyyy-MM-dd
      * @param days
      * @return
      */
      String getTodaySportStepArrayByStartDateAndDays(String date, int days);
}
```
#### 3. 修改Android4.4以下计步算法，[开源算法](https://github.com/finnfu/stepcount)
Android4.4以下（不包括4.4）版本没有计步协处理器，只能通过加速度传感器进行获取，而且计步Service必须保证在后台存活，之前找的记步算法也是开源的，但是在很多低端手机上计步非常不准确，总是少了很多步数，有的彻底不记步，于是又在github上重新找了一个算法，虽然这个算法还是有问题，但是比之前的好多了，这个算法需要自己进行优化，由于时间太紧了我也就直接用了，有时间的话还是要好好看看源码，优化一下。

[开源算法](https://github.com/finnfu/stepcount)这个是源码，如果有大神对他进行优化，非常欢迎和我进行讨论。

####4. 增加使用文案。
增加文案也是没办法的办法了，由于用户不懂规则，或者不会设置后台自启动，所以要增加文案，教用户如何使用计步器。

例如：

1. 一些不能后台的手机需要告诉用户每天早上打开一次app才可以正常计步。
2. 每天晚上走完需要打开app进行上传步数，通知栏上的步数是本地的不是服务器上的。
3. 如果手机用加速度传感器进行计步，需要在文案上增加如何使app计步模块在后台自启动，防止被第三方安全软件杀掉，等等。

### 测试代码
```
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.stepArrayButton: {
                //获取所有步数列表
                if (null != iSportStepInterface) {
                    try {
                        String stepArray = iSportStepInterface.getTodaySportStepArray();
                        mStepArrayTextView.setText(stepArray);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            case R.id.stepArrayButton1:{
                //根据时间来获取步数列表
                if (null != iSportStepInterface) {
                    try {
                        String stepArray = iSportStepInterface.getTodaySportStepArrayByDate("2018-01-19");
                        mStepArrayTextView.setText(stepArray);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            case R.id.stepArrayButton2:{
                //获取多天步数列表
                if (null != iSportStepInterface) {
                    try {
                        String stepArray = iSportStepInterface.getTodaySportStepArrayByStartDateAndDays("2018-01-20", 6);
                        mStepArrayTextView.setText(stepArray);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            default:
                break;
        }
    }
```
