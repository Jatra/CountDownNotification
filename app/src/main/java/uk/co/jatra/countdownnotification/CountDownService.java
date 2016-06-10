package uk.co.jatra.countdownnotification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.util.concurrent.TimeUnit;

public class CountDownService extends Service {

    public static final String COUNTDOWNSERVICE_RESULT_EXTRA = "countDownServiceResultExtra";
    public static final int DEFAULT_RESERVATION_HOLD_TIME = 60 * 15 * 1000;
    public static final String CANCEL_SCAN = "cancelScan";
    public static final int KILL_SERVICE_REQUEST_CODE = 666;
    public static final String RESERVATION_HOLD_TIME_EXTRA = "reservationHoldTimeExtra";
    public static final String OPEN_ACTIVITY_CLASSNAME_EXTRA = "openActivityClassnameExtra";
    private static final int NOTIFICATION_HOLD_ID = 101;
    private static final int NOTIFICATION_TIMED_OUT_ID = 102;
    private static final String STOP_SCAN_ACTION = "stopScanAction";
    private Notification notification;
    private PendingIntent pendingOpenIntent;
    private NotificationCompat.Action action;
    private Handler handler;
    private Runnable notificationUpdater;
    private String notificationText;
    private long timeLeft;
    private long expirationTime;
    private Class targetActivity;

    public CountDownService() {
    }

    public static void startCountDownService(Context context, Class classOfActivityToLaunch, long timeToLiveMillis) {
        Intent serviceIntent = new Intent(context, CountDownService.class);
        serviceIntent.putExtra(RESERVATION_HOLD_TIME_EXTRA, timeToLiveMillis);
        serviceIntent.putExtra(OPEN_ACTIVITY_CLASSNAME_EXTRA, classOfActivityToLaunch);
        context.startService(serviceIntent);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getBooleanExtra(CANCEL_SCAN, false)) {
            cancelScan();
            return START_NOT_STICKY;
        }

        targetActivity = (Class) intent.getSerializableExtra(OPEN_ACTIVITY_CLASSNAME_EXTRA);
        pendingOpenIntent = getOpenPendingIntent(Result.OPENED);

        expirationTime = System.currentTimeMillis() + intent.getLongExtra(RESERVATION_HOLD_TIME_EXTRA, DEFAULT_RESERVATION_HOLD_TIME);
        updateTimeLeft();


        PendingIntent cancelPendingIntent = getCancelPendingIntent();
        action = new NotificationCompat.Action.Builder(android.R.drawable.ic_media_pause, "Cancel scan", cancelPendingIntent)
                .build();

        notification = createHoldingNotification();
        startForeground(NOTIFICATION_HOLD_ID, notification);

        notificationUpdater = createNotificationUpdater();
        handler = createHandler();
        handler.post(notificationUpdater);

        return START_STICKY;
    }

    private PendingIntent getCancelPendingIntent() {
        Intent cancelIntent = new Intent(this, CountDownService.class);
        cancelIntent.putExtra(CANCEL_SCAN, true);
        return PendingIntent.getService(this, KILL_SERVICE_REQUEST_CODE, cancelIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent getOpenPendingIntent(Result result) {
        Intent openIntent = new Intent(this, targetActivity);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openIntent.putExtra(COUNTDOWNSERVICE_RESULT_EXTRA, result);
        return PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void updateOpenIntent(Result result) {
        getOpenPendingIntent(result);
    }

    @NonNull
    private Runnable createNotificationUpdater() {
        return new Runnable() {
            @Override
            public void run() {
                updateTimeLeft();
                if (timeLeft <= 0) {
                    sendHoldTimedOutNotification();
                    cancelScan();
                } else {
                    updateNotification();
                    long updateTime = timeLeft > TimeUnit.MINUTES.toMillis(2) ? TimeUnit.MINUTES.toMillis(1) : TimeUnit.SECONDS.toMillis(1);
                    handler.postDelayed(this, updateTime);
                }
            }
        };
    }

    private void sendHoldTimedOutNotification() {
        updateOpenIntent(Result.TIMED_OUT);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notification = createHoldTimedOutNotification();
        mNotificationManager.notify(NOTIFICATION_TIMED_OUT_ID, notification);
    }

    private Handler createHandler() {
        HandlerThread thread = new HandlerThread(this.getClass().getSimpleName());
        thread.start();
        return new Handler(thread.getLooper());
    }

    private void updateNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notification = createHoldingNotification();
        mNotificationManager.notify(NOTIFICATION_HOLD_ID, notification);
    }

    private void cancelScan() {
        handler.removeCallbacks(notificationUpdater);
        broadcastScanStop();
//        stopSelf();
    }

    private void broadcastScanStop() {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(new Intent(STOP_SCAN_ACTION));
    }

    private void updateTimeLeft() {
        timeLeft = (expirationTime - System.currentTimeMillis()) / 1000;
        notificationText = timeLeft > 120 ? String.format("Time left to start reservation: %d minutes", timeLeft / 60) : String.format("Time left to start reservation: %d seconds", timeLeft);
    }

    private Notification createHoldingNotification() {
        return new NotificationCompat.Builder(this)
                .setContentTitle("Counting down")
                .setContentText(notificationText)
                .setSmallIcon(android.R.drawable.ic_media_ff)
                .setContentIntent(pendingOpenIntent)
                .setAutoCancel(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setGroup("jatra")
                .setGroupSummary(true)
                .addAction(action)
                .build();
    }

    private Notification createHoldTimedOutNotification() {
        return new NotificationCompat.Builder(this)
                .setContentTitle("Timed out")
                .setContentText("timed out")
                .setSmallIcon(android.R.drawable.ic_media_ff)
                .setContentIntent(pendingOpenIntent)
                .setAutoCancel(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(false)
                .setOnlyAlertOnce(true)
                .setGroup("jatra")
                .setGroupSummary(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Does not support binding");
    }

    enum Result {
        OPENED,
        CANCELLED,
        TIMED_OUT
    }

}
