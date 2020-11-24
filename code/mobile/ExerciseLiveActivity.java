package ch.epfl.esl.sportstracker;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.graphics.Color.BLUE;
import static android.graphics.Color.RED;
import static android.graphics.Color.TRANSPARENT;

public class ExerciseLiveActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    public static final String RECEIVE_HEART_RATE = "RECEIVE_HEART_RATE";
    public static final String RECEIVE_LOCATION = "RECEIVE_LOCATION";
    public static final String RECEIVE_HEART_RATE_LOCATION = "RECEIVE_HEART_RATE_LOCATION";
    public static final String HEART_RATE = "HEART_RATE";
    public static final String LONGITUDE = "LONGITUDE";
    public static final String LATITUDE = "LATITUDE";
    public static final String HR_PLOT_WATCH = "HR Smart Watch";
    public static final String HR_PLOT_BELT = "HR Belt";
    private static final int MIN_HR = 40;
    private static final int MAX_HR = 200;
    private static final int NUMBER_OF_SECONDS = 50;
    private static XYPlot heartRatePlot;
    private final String TAG = this.getClass().getSimpleName();
    //private XYplotSeriesList xyPlotSeriesList;
    private BluetoothLeService mBluetoothLeService;
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean mConnected = false;

    private final SimpleXYSeries HRseriesWatch = new SimpleXYSeries(HR_PLOT_WATCH);
    private final SimpleXYSeries HRseriesBelt = new SimpleXYSeries(HR_PLOT_BELT);

    private  long startTime = System.currentTimeMillis() / 1000;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private ArrayList<Integer> hrDataArrayList = new ArrayList<>();

    private HeartRateBroadcastReceiver heartRateBroadcastReceiver;
    private LocationBroadcastReceiver locationBroadcastReceiver;

    private GoogleMap mMap;
    private DatabaseReference recordingRef;
    private Marker mapMarker;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_live);
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_DENIED || checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") == PackageManager.PERMISSION_DENIED || checkSelfPermission("android.permission.INTERNET") == PackageManager.PERMISSION_DENIED)) {
            requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION", "android.permission.INTERNET"}, 0);
        }

        // Acquire a reference to the system Location Manager
        final LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            } catch (Exception e) {
                Log.w(TAG, "Could not request location updates");
            }
        }

        // Obtain the SupportMapFragment and get notified when the map is
        // ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.GoogleMap);
        mapFragment.getMapAsync(this);

        Intent intentFromRec = getIntent();
        String userID = intentFromRec.getStringExtra(MyProfileFragment.USER_ID);
        String recID = intentFromRec.getStringExtra(NewRecordingFragment.RECORDIND_ID);

        // Android Plot
        heartRatePlot = findViewById(R.id.HRplot);
        configurePlot();

        // Initialize plot
        LineAndPointFormatter formatterRed = new LineAndPointFormatter(RED, TRANSPARENT, TRANSPARENT, null);
        LineAndPointFormatter formatterBlue = new LineAndPointFormatter(BLUE, TRANSPARENT, TRANSPARENT, null);

        formatterRed.getLinePaint().setStrokeWidth(8);
        formatterBlue.getLinePaint().setStrokeWidth(8);
        heartRatePlot.clear();
        heartRatePlot.addSeries(HRseriesWatch, formatterRed);
        heartRatePlot.addSeries(HRseriesBelt, formatterBlue);
        heartRatePlot.redraw();

        // Get recording information from Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference profileGetRef = database.getReference("profiles");
        recordingRef = profileGetRef.child(userID).child("recordings").child(recID);

        recordingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                TextView exerciseType = findViewById(R.id.exerciseTypeLive);
                exerciseType.setText(dataSnapshot.child("exercise_type").getValue().toString());
                TextView exerciseDatetime = findViewById(R.id.exerciseDateTimeLive);
                Long datetime = Long.parseLong(dataSnapshot.child("datetime").getValue().toString());
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss", Locale.getDefault());
                exerciseDatetime.setText(formatter.format(new Date(datetime)));
                String switchWatch = dataSnapshot.child("switch_watch").getValue().toString();
                String switchBelt = dataSnapshot.child("switch_hr_belt").getValue().toString();
                TextView hrWatch = findViewById(R.id.exerciseHRwatchLive);
                hrWatch.setText(switchWatch);
                TextView hrBelt = findViewById(R.id.exerciseHRbeltLive);
                hrBelt.setText(switchBelt);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                registerHeartRateService(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getIntExtra(BluetoothLeService.EXTRA_DATA, 0));
            }
        }
    };

    private void displayData(int intExtra) {

        TextView hrBelt = findViewById(R.id.exerciseHRbeltLive);
        hrBelt.setText(String.valueOf(intExtra));
        float time = System.currentTimeMillis() / 1000 - startTime;
        HRseriesBelt.addLast(time, intExtra);
        while (HRseriesBelt.size() > 0 && (time - HRseriesBelt.getX(0).longValue()) > NUMBER_OF_SECONDS) {
            HRseriesBelt.removeFirst();
            heartRatePlot.setDomainBoundaries(0, 0, BoundaryMode.AUTO);
        }

        heartRatePlot.redraw();

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Get the HR data back from the watch
        heartRateBroadcastReceiver = new HeartRateBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(heartRateBroadcastReceiver, new IntentFilter(RECEIVE_HEART_RATE));

        locationBroadcastReceiver = new LocationBroadcastReceiver();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(locationBroadcastReceiver, new IntentFilter(RECEIVE_LOCATION));

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(heartRateBroadcastReceiver);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationBroadcastReceiver);
        unregisterReceiver(mGattUpdateReceiver);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Lausanne
        double latitude = 46.5197;
        double longitude = 6.6323;

        // Add a marker in Lausanne and move the camera
        LatLng currentLocation = new LatLng(latitude, longitude);
        Log.e(TAG, "Current location: " + currentLocation);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
        if (mapMarker != null) {
            mapMarker.remove();
        }
        mapMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title(getString(R.string.current_location)));

        TextView longitudeTextView = findViewById(R.id.longitude);
        longitudeTextView.setText(String.valueOf(longitude));
        TextView latitudeTextView = findViewById(R.id.latitude);
        latitudeTextView.setText(String.valueOf(latitude));
    }

    private void configurePlot() {
        // Get background color from Theme
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);
        int backgroundColor = typedValue.data;
        // Set background colors
        heartRatePlot.setPlotMargins(0, 0, 0, 0);
        heartRatePlot.getBorderPaint().setColor(backgroundColor);
        heartRatePlot.getBackgroundPaint().setColor(backgroundColor);
        heartRatePlot.getGraph().getBackgroundPaint().setColor(backgroundColor);
        heartRatePlot.getGraph().getGridBackgroundPaint().setColor(backgroundColor);
        // Set the grid color
        heartRatePlot.getGraph().getRangeGridLinePaint().setColor(Color.GRAY);
        heartRatePlot.getGraph().getDomainGridLinePaint().setColor(Color.GRAY);
        // Set the origin axes colors
        heartRatePlot.getGraph().getRangeOriginLinePaint().setColor(Color.DKGRAY);
        heartRatePlot.getGraph().getDomainOriginLinePaint().setColor(Color.DKGRAY);
        // Set the XY axis boundaries and step values
        heartRatePlot.setRangeBoundaries(MIN_HR, MAX_HR, BoundaryMode.FIXED);
        heartRatePlot.setDomainBoundaries(0, NUMBER_OF_SECONDS - 1, BoundaryMode.FIXED);
        heartRatePlot.setRangeStepValue(9); // 9 values 40 60 ... 200
        heartRatePlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("#")); // Force the Axis to be integer
        heartRatePlot.setRangeLabel(getString(R.string.heart_rate));
    }

    public void stopRecordingOnWear(View view) {

        Intent intentStopRec = new Intent(ExerciseLiveActivity.this, WearService.class);
        intentStopRec.setAction(WearService.ACTION_SEND.STOPACTIVITY.name());
        intentStopRec.putExtra(WearService.ACTIVITY_TO_STOP, BuildConfig.W_recordingactivity);
        startService(intentStopRec);

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(this);

                // Retrieve HR data
                ArrayList<Integer> hrArray = intent.getIntegerArrayListExtra(HEART_RATE);

                // Convert the primitive float[] to a List of Float objects for Firebase
                // for the latitudes and longitudes
                float[] array = intent.getFloatArrayExtra(LATITUDE);
                List<Float> latArray = new ArrayList<>(array.length);
                for (float f : array) latArray.add(f);
                array = intent.getFloatArrayExtra(LONGITUDE);
                List<Float> lonArray = new ArrayList<>(array.length);
                for (float f : array) lonArray.add(f);

                // Upload everything in Firebase
                recordingRef.child("hr_watch").setValue(hrArray);
                recordingRef.child("loc_lat_watch").setValue(latArray);
                recordingRef.child("loc_lon_watch").setValue(lonArray);
            }
        }, new IntentFilter(RECEIVE_HEART_RATE_LOCATION));

        finish();
    }

    @Override
    public void onLocationChanged(Location location) {
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();

        LatLng currentLocation = new LatLng(latitude, longitude);
        Log.e(TAG, "Current location: " + currentLocation);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
        if (mapMarker != null) {
            mapMarker.remove();
        }
        mapMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title(getString(R.string.current_location)));

        TextView longitudeTextView = findViewById(R.id.longitude);
        longitudeTextView.setText(String.valueOf(longitude));
        TextView latitudeTextView = findViewById(R.id.latitude);
        latitudeTextView.setText(String.valueOf(latitude));
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }


    private class HeartRateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Show HR in a TextView
            int heartRateWatch = intent.getIntExtra(HEART_RATE, -1);
            TextView hrTextView = findViewById(R.id.exerciseHRwatchLive);
            hrTextView.setText(String.valueOf(heartRateWatch));
            float time = System.currentTimeMillis() / 1000 - startTime;
            HRseriesWatch.addLast(time, heartRateWatch);

            while (HRseriesWatch.size() > 0 && (time - HRseriesWatch.getX(0).longValue()) > NUMBER_OF_SECONDS) {
                HRseriesWatch.removeFirst();
                heartRatePlot.setDomainBoundaries(0, 0, BoundaryMode.AUTO);
            }

            // Update HR plot series
            /*xyPlotSeriesList.updateSeries(HR_PLOT_WATCH, heartRateWatch);
            XYSeries hrWatchSeries = new SimpleXYSeries(xyPlotSeriesList.getSeriesFromList(HR_PLOT_WATCH), SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED, HR_PLOT_WATCH);
            LineAndPointFormatter formatterPolar = xyPlotSeriesList.getFormatterFromList(HR_PLOT_WATCH);

            heartRatePlot.clear();
            heartRatePlot.addSeries(hrWatchSeries, formatterPolar);*/
            heartRatePlot.redraw();
        }
    }

    private class LocationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Update TextViews
            double longitude = intent.getDoubleExtra(LONGITUDE, -1);
            double latitude = intent.getDoubleExtra(LATITUDE, -1);

            TextView longitudeTextView = findViewById(R.id.longitude);
            longitudeTextView.setText(String.valueOf(longitude));

            TextView latitudeTextView = findViewById(R.id.latitude);
            latitudeTextView.setText(String.valueOf(latitude));

            // Update map
            LatLng currentLocation = new LatLng(latitude, longitude);
            Log.e(TAG, "Current location: " + currentLocation);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(currentLocation).title(getString(R.string.current_location)));
        }
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void registerHeartRateService(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService
                    .getCharacteristics();
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic
                    gattCharacteristic : gattCharacteristics) {
                uuid = gattCharacteristic.getUuid().toString();
                // Find heart rate measurement (0x2A37)
                if (SampleGattAttributes.lookup(uuid, "unknown")
                        .equals("Heart Rate Measurement")) {
                    Log.i(TAG, "Registering for HR measurement");
                    mBluetoothLeService.setCharacteristicNotification(
                            gattCharacteristic, true);
                }
            }
        }
    }
}
