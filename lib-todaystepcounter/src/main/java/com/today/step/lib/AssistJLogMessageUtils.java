package com.today.step.lib;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.List;
import java.util.logging.Logger;

/**
 * @author :  wcd
 * @email : WANGCHUNDONG719@pingan.com.cn
 * @date : 2019-1-23 11:49
 * @desc : 我想要在每次打印log的时候，知道：1）是否息屏 2)手机电量 3)用户的经纬度
 */
 class AssistJLogMessageUtils {
    public static int getBattery(Context context){
        BatteryManager batteryManager = (BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);
        int battery = -1;
        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.LOLLIPOP){
            battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        return battery;
    }
    public static boolean getScreenState(Context context){
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm.isScreenOn();
    }
    public static Location getLocation(Context context){
        Location location = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission( context, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission( context, Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED){
            return location;
        }
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        //2.获取位置提供器，GPS或是NetWork
        List<String> providers = locationManager.getProviders( true );
        if (!providers.contains( LocationManager.NETWORK_PROVIDER )&&!providers.contains( LocationManager.GPS_PROVIDER )) {
            return location;
        }
        String locationProvider = null;
        if(providers.contains( LocationManager.NETWORK_PROVIDER )){
            locationProvider = LocationManager.NETWORK_PROVIDER;
        }else if(providers.contains( LocationManager.GPS_PROVIDER )){
            locationProvider = LocationManager.GPS_PROVIDER;
        }
        if(locationProvider==null)return  location;
        location = locationManager.getLastKnownLocation(locationProvider);
        return location;

    }
}
