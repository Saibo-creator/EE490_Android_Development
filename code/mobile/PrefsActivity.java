package ch.epfl.esl.sportstracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class PrefsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String LAST_ACTIVITY_TIMESTAMP = "LAST_ACTIVITY_TIMESTAMP";

    private static final int MAX_UPDATE_INTERVAL = 14;
    private static final int MIN_UPDATE_INTERVAL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.key_enable_reminders))) {
            // The reminders checkbox has changed
        } else if (key.equals(getString(R.string.key_reminder_days))) {
            // The reminders delay has changed

            // Flag to correct (or not) the user input
            boolean updateIntervalSetting = false;
            int newIntervalDaysInt = 3; // Default number of days
            String newIntervalDaysStr = sharedPreferences.getString(key, String.valueOf(newIntervalDaysInt));

            // Try to parse the value as an integer
            try {
                newIntervalDaysInt = Integer.parseInt(newIntervalDaysStr);
            } catch (NumberFormatException e) {
                newIntervalDaysStr = null;
            }

            // Check if the result is valid
            if (newIntervalDaysStr == null || newIntervalDaysStr.isEmpty() || newIntervalDaysStr.matches("\\D")) {
                updateIntervalSetting = true;
                Toast.makeText(this, String.format(getString(R.string.invalid_days_value_defaulting_to_initial), newIntervalDaysInt), Toast.LENGTH_SHORT).show();
            } else {
                // If valid, check restrict to acceptable range
                if (newIntervalDaysInt > MAX_UPDATE_INTERVAL) {
                    updateIntervalSetting = true;
                    newIntervalDaysInt = MAX_UPDATE_INTERVAL;
                } else if (newIntervalDaysInt < MIN_UPDATE_INTERVAL) {
                    updateIntervalSetting = true;
                    newIntervalDaysInt = MIN_UPDATE_INTERVAL;
                }

                // If out of bounds, warn the user
                if (updateIntervalSetting) {
                    Toast.makeText(this, String.format(getString(R.string.invalid_days_value_out_of_bounds), newIntervalDaysInt), Toast.LENGTH_SHORT).show();
                }

            }

            // Save the modified value (if needed)
            if (updateIntervalSetting) {
                sharedPreferences.edit().putString(key, Integer.toString(newIntervalDaysInt)).apply();
            }
        }

        AlarmUpdater.updateNotificationSystem(this);
    }
}