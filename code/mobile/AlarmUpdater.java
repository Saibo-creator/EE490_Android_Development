package ch.epfl.esl.sportstracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

class AlarmUpdater {

    static final String TAG = "AlarmUpdater";

    static void updateNotificationSystem(Context context) {
        Log.v(TAG, "Setting up the AlarmManager");

        // Get the AlarmManager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        assert alarmManager != null; // Guarantees that alarmManager isn't null

        // Create the intent which will be triggered when the alarm expires
        // If an alarm exists with the same Intent, it will be canceled
        Intent intentForService = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intentForService, PendingIntent.FLAG_CANCEL_CURRENT);

        // Check if we need an alarm at all
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean reminders = sharedPref.getBoolean(context.getString(R.string.key_enable_reminders), true);
        if (reminders) {
            // Get the delay between two activities and the last activity time
            long updateIntervalDays = Integer.parseInt(sharedPref.getString(context.getString(R.string.key_reminder_days), "3"));
            long lastTimestampMs = PreferenceManager.getDefaultSharedPreferences(context).getLong(PrefsActivity.LAST_ACTIVITY_TIMESTAMP, Long.MAX_VALUE);

            // Set the alarm
            long alarmTimestamp = lastTimestampMs + updateIntervalDays * AlarmManager.INTERVAL_DAY;
            alarmManager.set(AlarmManager.RTC, alarmTimestamp, pendingIntent);

            Log.v(TAG, "Set alarm in " + (alarmTimestamp - System.currentTimeMillis()) / 1000. + " seconds");
        } else {
            alarmManager.cancel(pendingIntent);
        }
    }
}
