package ch.epfl.esl.sportstracker;

import android.location.Location;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

public class SavingLocationAsyncTask extends AsyncTask<List<Location>, Void, Void> {

    private SportTrackerRoomDatabase db;

    SavingLocationAsyncTask(SportTrackerRoomDatabase db) {
        this.db = db;
    }

    @SafeVarargs

    @Override
    protected final Void doInBackground(List<Location>... lists) {
        List<Location> locationValueList = lists[0];
        List<SensorDataEntity> sensorDataEntityList = new ArrayList<SensorDataEntity>();

        for (Location locationValue : locationValueList) {
            long time = System.nanoTime();

            // Send Latitude
            SensorDataEntity sensorDataLat = new SensorDataEntity();
            sensorDataLat.timestamp = time;
            sensorDataLat.type = SensorDataEntity.LATITUDE;
            sensorDataLat.value = locationValue.getLatitude();
            sensorDataEntityList.add(sensorDataLat);

            // Send Longitude
            SensorDataEntity sensorDataLon = new SensorDataEntity();
            sensorDataLon.timestamp = time;
            sensorDataLon.type = SensorDataEntity.LONGITUDE;
            sensorDataLon.value = locationValue.getLongitude();
            sensorDataEntityList.add(sensorDataLon);
        }
        db.sensorDataDao().insertSensorDataEntityList(sensorDataEntityList);
        return null;
    }
}
