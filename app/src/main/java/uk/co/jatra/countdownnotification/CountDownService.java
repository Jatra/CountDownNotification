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

import java.util.concurrent.TimeUnit;

public class CountDownService extends Service {

    public static final int NOTIFICATION_ID = 101;
    public static final int DEFAULT_RESERVATION_HOLD_TIME = 60 * 15 * 1000;
    public static final String STOP_SERVICE = "stopService";
    public static final int KILL_SERVICE = 666;
    public static final String RESERVATION_HOLD_TIME_EXTRA = "reservationHoldTimeExtra";
    public static final String OPEN_ACTIVITY_CLASSNAME_EXTRA = "openActivityClassnameExtra";
    private Notification notification;
    private PendingIntent pendingOpenIntent;
    private NotificationCompat.Action action;
    private Handler handler;
    private Runnable updateNotification;
    private String notificationText;
    private long timeLeft;
    private long expirationTime;
    private Intent stopServiceIntent;

    public CountDownService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getBooleanExtra(STOP_SERVICE, false)) {
            stopHold();
            return START_NOT_STICKY;
        }

        stopServiceIntent = new Intent(this, CountDownService.class);
        long reservationHoldTime = intent.getLongExtra(RESERVATION_HOLD_TIME_EXTRA, DEFAULT_RESERVATION_HOLD_TIME);

        expirationTime = System.currentTimeMillis() + reservationHoldTime;
        updateTimeLeft();


        Class requestedClass = (Class) intent.getSerializableExtra(OPEN_ACTIVITY_CLASSNAME_EXTRA);
        Intent openIntent = new Intent(this, (requestedClass == null) ? MainActivity.class : requestedClass);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingOpenIntent = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        stopServiceIntent.putExtra(STOP_SERVICE, true);
        PendingIntent stopServicePendingIntent = PendingIntent.getService(this, KILL_SERVICE, stopServiceIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        action = new NotificationCompat.Action.Builder(android.R.drawable.ic_media_pause, "Cancel reservation", stopServicePendingIntent)
                .build();
        notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);

        updateNotification = createNotificationUpdater();
        handler = createHandler();
        handler.post(updateNotification);

        return START_STICKY;
    }

    @NonNull
    private Runnable createNotificationUpdater() {
        return new Runnable() {
            @Override
            public void run() {
                updateTimeLeft();
                if (timeLeft <= 0) {
                    stopHold();
                } else {
                    updateNotification();
                    long updateTime = timeLeft > TimeUnit.MINUTES.toMillis(2) ? TimeUnit.MINUTES.toMillis(1) : TimeUnit.SECONDS.toMillis(1);
                    handler.postDelayed(this, updateTime);
                }
            }
        };
    }

    private Handler createHandler() {
        HandlerThread thread = new HandlerThread(this.getClass().getSimpleName());
        thread.start();
        return new Handler(thread.getLooper());
    }

    private void updateNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notification = createNotification();
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void stopHold() {
        handler.removeCallbacks(updateNotification);
        //remove reservation.
        stopSelf();
    }

    private void updateTimeLeft() {
        timeLeft = (expirationTime - System.currentTimeMillis()) / 1000;
        notificationText = timeLeft > 120 ? String.format("Time left to start reservation: %d minutes", timeLeft / 60) : String.format("Time left to start reservation: %d seconds", timeLeft);
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this)
                .setContentTitle("Zipcar vehicle held")
                .setContentText(notificationText)
                .setSmallIcon(android.R.drawable.ic_media_ff)
                .setContentIntent(pendingOpenIntent)
                .setAutoCancel(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setGroup("GROUP")
                .setGroupSummary(true)
                .addAction(action)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
