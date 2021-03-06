package ch.epfl.esl.sportstracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import java.util.ArrayList;
import java.util.List;

public class RecordingActivity extends WearableActivity implements SensorEventListener {

    public static final String STOP_ACTIVITY = "STOP_ACTIVITY";
    private final String TAG = this.getClass().getSimpleName();
    private SportTrackerRoomDatabase sportTrackerDB;

    private List<Integer> hrList = new ArrayList<Integer>();
    private List<Integer> hrListCopy = new ArrayList<>();
    static ArrayList<Integer> hrArray;
    private List<Location> locationList = new ArrayList<Location>();
    private List<Location> locationListCopy = new ArrayList<Location>();
    static float[] latitudeArray;
    static float[] longitudeArray;
    private int sizeListToSave = 10;


    private ConstraintLayout mLayout;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        if (checkSelfPermission("android.permission.BODY_SENSORS") == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{"android.permission.BODY_SENSORS"}, 0);
        }

        if (checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_DENIED || checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") == PackageManager.PERMISSION_DENIED || checkSelfPermission("android.permission.INTERNET") == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION", "android.permission.INTERNET"}, 0);
        }

        // Create instance of Sport Tracker Room DB
        sportTrackerDB = SportTrackerRoomDatabase.getDatabase(getApplicationContext());

        final SensorManager sensorManager = (SensorManager) getSystemService(MainActivity.SENSOR_SERVICE);
        Sensor hr_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        sensorManager.registerListener(this, hr_sensor, SensorManager.SENSOR_DELAY_UI);

        fusedLocationClient = new FusedLocationProviderClient(this);

        // Location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                TextView textViewLocation = findViewById(R.id.Location);

                double latitude = locationResult.getLastLocation().getLatitude();
                double longitude = locationResult.getLastLocation().getLongitude();

                textViewLocation.setText(String.format("Lat: %s \nLong: %s", latitude, longitude));

                // Save the data in case the tablet is not in range:
                // when the activity is over, we send the whole sensor database back
                locationList.add(locationResult.getLastLocation());
                if (!locationList.isEmpty()) {
                    locationListCopy.clear();
                    locationListCopy.addAll(locationList);
                    SavingLocationAsyncTask locationAsyncTask = new SavingLocationAsyncTask(sportTrackerDB);
                    locationAsyncTask.execute(locationListCopy);
                    locationList.clear();
                }

                // Send the data for live update on the tablet
                Intent intent = new Intent(RecordingActivity.this, WearService.class);
                intent.setAction(WearService.ACTION_SEND.LOCATION.name());
                intent.putExtra(WearService.LONGITUDE, longitude);
                intent.putExtra(WearService.LATITUDE, latitude);
                startService(intent);
            }
        };


        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sensorManager.unregisterListener(RecordingActivity.this);
                //Read HR and Location data
                OnTaskCompletedListener onTaskCompletedListener = new OnTaskCompletedListener() {
                    @Override
                    public void onTaskCompleted() {
                        //Send HR data and Location in the same intent
                        Intent intentSendHrLocation = new Intent(RecordingActivity.this, WearService.class);
                        intentSendHrLocation.setAction(WearService.ACTION_SEND.HEART_RATE_AND_LOCATION.name());
                        intentSendHrLocation.putIntegerArrayListExtra(WearService.HEART_RATE, hrArray);
                        intentSendHrLocation.putExtra(WearService.LONGITUDE, longitudeArray);
                        intentSendHrLocation.putExtra(WearService.LATITUDE, latitudeArray);
                        startService(intentSendHrLocation);
                        finish();
                    }
                };

                ReadingHeartRateAndLocationAsyncTask hrAsyncTask = new ReadingHeartRateAndLocationAsyncTask(onTaskCompletedListener, sportTrackerDB);
                hrAsyncTask.execute();
            }
        }, new IntentFilter(STOP_ACTIVITY));

        mLayout = findViewById(R.id.containerRecording);
        // Enables Always-on
        setAmbientEnabled();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        updateDisplay();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mLayout.setBackgroundColor(getResources().getColor(android.R.color.black, getTheme()));
        } else {
            mLayout.setBackgroundColor(getResources().getColor(android.R.color.white, getTheme()));
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        TextView textViewHR = findViewById(R.id.hrSensor);
        int heartRate = (int) event.values[0];
        if (textViewHR != null) textViewHR.setText(String.valueOf(event.values[0]));

        // Save the data in case the tablet is not in range:
        // when the activity is over, we send the whole sensor database back
        hrList.add(heartRate);
        if (!hrList.isEmpty()) {
            hrListCopy.clear();
            hrListCopy.addAll(hrList);
            SavingHeartRateAsyncTask hrAsyncTask = new SavingHeartRateAsyncTask(sportTrackerDB);
            hrAsyncTask.execute(hrListCopy);
            hrList.clear();
        }

        // Send the data for live update on the tablet
        Intent intent = new Intent(RecordingActivity.this, WearService.class);
        intent.setAction(WearService.ACTION_SEND.HEART_RATE.name());
        intent.putExtra(WearService.HEART_RATE, heartRate);
        startService(intent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest().setInterval(5).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
