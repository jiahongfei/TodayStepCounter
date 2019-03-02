package com.andrjhf.notification.api.compat;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;

public class NotificationApiCompat {

    private static final String TAG = "NotificationApiCompat";

    private final NotificationManager manager;
    private Notification mNotification;
    private final Notification.Builder mBuilder26;
    private final android.support.v4.app.NotificationCompat.Builder mBuilder25;

    public NotificationApiCompat(Builder builder) {
        manager = builder.manager;
        mNotification = builder.mNotification;
        mBuilder26 = builder.mBuilder26;
        mBuilder25 = builder.mBuilder25;
    }

    public Notification getNotificationApiCompat() {
        return mNotification;
    }

    public void startForeground(Service service, int id) {
        service.startForeground(id, mNotification);

    }

    public void stopForeground(Service service) {
        service.stopForeground(true);
    }

    public void notify(int id) {
        manager.notify(id, mNotification);
    }

    public void updateNotification( int id, String title, String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!TextUtils.isEmpty(text)) {
                mBuilder26.setContentText(text);
            }
            if (!TextUtils.isEmpty(title)) {
                mBuilder26.setContentTitle(title);
            }
            mNotification = mBuilder26.build();
        } else {
            if (!TextUtils.isEmpty(text)) {
                mBuilder25.setContentText(text);
            }
            if (!TextUtils.isEmpty(title)) {
                mBuilder25.setContentTitle(title);
            }
            mNotification = mBuilder25.build();
        }
        manager.notify(id, mNotification);

    }

    public static final class Builder {

        private Context mContext;
        private String mChannelId;
        private Notification mNotification;
        private final NotificationManager manager;
        private NotificationChannel mNotificationChannel;
        private Notification.Builder mBuilder26;
        private android.support.v4.app.NotificationCompat.Builder mBuilder25;

        public Builder(Context context, NotificationManager manager, String channelId, String channelName, int smallIcon) {
            mContext = context;
            mChannelId = channelId;
            this.manager = manager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationChannel = new NotificationChannel(mChannelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
                mBuilder26 = getChannelNotification(mContext, channelId);
                mBuilder26.setSmallIcon(smallIcon);
            } else {
                mBuilder25 = getNotification_25(mContext);
                mBuilder25.setSmallIcon(smallIcon);
            }
        }

        public Builder setPriority(int pri) {
//            mPriority = pri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            } else {
                mBuilder25.setPriority(pri);
            }
            return this;
        }

        public Builder setLargeIcon(Bitmap icon) {
//            mLargeIcon = icon;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mBuilder26.setLargeIcon(icon);
            } else {
                mBuilder25.setLargeIcon(icon);
            }
            return this;
        }

        public Builder setContentIntent(PendingIntent intent) {
//            mContentIntent = intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mBuilder26.setContentIntent(intent);
            } else {
                mBuilder25.setContentIntent(intent);
            }
            return this;
        }

        public Builder setTicker(CharSequence tickerText) {
//            mTickerText = tickerText;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mBuilder26.setTicker(tickerText);
            } else {
                mBuilder25.setTicker(tickerText);
            }
            return this;
        }

        public Builder setContentTitle(CharSequence title) {
//            mContentTitle = title;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mBuilder26.setContentTitle(title);
            } else {
                mBuilder25.setContentTitle(title);
            }
            return this;
        }

        public Builder setContentText(CharSequence text) {
//            mContentText = text;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mBuilder26.setContentText(text);
            } else {
                mBuilder25.setContentText(text);
            }
            return this;
        }

        public Builder setOngoing(boolean ongoing) {
//            mOngoing = ongoing;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mBuilder26.setOngoing(ongoing);
            } else {
                mBuilder25.setOngoing(ongoing);

            }
            return this;
        }

        public Builder setOnlyAlertOnce(boolean onlyAlertOnce) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mBuilder26.setOnlyAlertOnce(onlyAlertOnce);
            } else {
                mBuilder25.setOnlyAlertOnce(onlyAlertOnce);
            }
            return this;
        }

        public Builder setProgress(int max, int progress, boolean indeterminate) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mBuilder26.setProgress(max, progress, indeterminate);
            } else {
                mBuilder25.setProgress(max, progress, indeterminate);
            }
            return this;
        }

        public Builder setWhen(long when) {
//            mNotification.when = when;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mBuilder26.setWhen(when);
            } else {
                mBuilder25.setWhen(when);
            }
            return this;
        }

        public Builder setDefaults(int defaults) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                mBuilder26.setDefaults(defaults);
            } else {
                mBuilder25.setDefaults(defaults);
            }
            return this;
        }

        public Builder setAutoCancel(boolean autoCancel) {
//            setFlag(FLAG_AUTO_CANCEL, autoCancel);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mBuilder26.setAutoCancel(autoCancel);
            } else {
                mBuilder25.setAutoCancel(autoCancel);
            }
            return this;
        }

        /**
         * 大于等于Android 8.0 api>=26
         *
         * @param title
         * @param content
         * @return
         */
        @TargetApi(Build.VERSION_CODES.O)
        private Notification.Builder getChannelNotification(Context context, String channelId) {
            return new Notification.Builder(context, channelId);
        }

        /**
         * 小于Android 8.0 api<26
         *
         * @param title
         * @param content
         * @return
         */
        private android.support.v4.app.NotificationCompat.Builder getNotification_25(Context context) {
            return new NotificationCompat.Builder(context);
        }

        public NotificationApiCompat builder() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(mNotificationChannel);
                mNotification = mBuilder26.build();
            } else {
                mNotification = mBuilder25.build();
            }
            return new NotificationApiCompat(this);
        }
    }
}
