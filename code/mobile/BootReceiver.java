package ch.epfl.esl.sportstracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "Boot finshed, set new alarm");
        AlarmUpdater.updateNotificationSystem(context);
    }
}
