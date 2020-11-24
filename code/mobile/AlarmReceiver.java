package ch.epfl.esl.sportstracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private final String TAG = this.getClass().getSimpleName();

    private String CHANNEL_ID = "";
    private int notificationId = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        CHANNEL_ID = context.getString(R.string.reminders);
        createNotificationChannel(context);

        Log.v(TAG, "Woken up by the AlarmManager");


        Intent intentMainActivity = new Intent(context, LoginActivity.class);
        intentMainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intentMainActivity, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID).setContentTitle(context.getString(R.string.lets_exercice)).setSmallIcon(R.drawable.ic_logo).setContentText(context.getString(R.string.notification_content)).setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true).setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notificationId, mBuilder.build());
    }


    // Taken (almost) directly from:
    // https://developer.android.com/training/notify-user/build-notification
    private void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.channel_name);
            String description = context.getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the
            // importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
