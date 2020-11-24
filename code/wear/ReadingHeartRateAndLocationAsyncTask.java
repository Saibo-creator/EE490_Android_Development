package ch.epfl.esl.sportstracker;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

public class ReadingHeartRateAndLocationAsyncTask extends AsyncTask<Void, Void, List<SensorDataEntity>> {

    private final SportTrackerRoomDatabase db;
    private final OnTaskCompletedListener onTaskCompletedListener;

    ReadingHeartRateAndLocationAsyncTask(OnTaskCompletedListener onTaskCompletedListener, SportTrackerRoomDatabase db) {
        this.onTaskCompletedListener = onTaskCompletedListener;
        this.db = db;
    }


    @Override
    protected List<SensorDataEntity> doInBackground(Void... voids) {
        List<SensorDataEntity> sensorDataEntityList = db.sensorDataDao().getAllValues(SensorDataEntity.HEART_RATE);
        sensorDataEntityList.addAll(db.sensorDataDao().getAllValues(SensorDataEntity.LONGITUDE));
        sensorDataEntityList.addAll(db.sensorDataDao().getAllValues(SensorDataEntity.LATITUDE));
        db.sensorDataDao().deleteAll();
        return sensorDataEntityList;
    }

    @Override
    protected void onPostExecute(List<SensorDataEntity> dbValues) {
        super.onPostExecute(dbValues);

        //Filter only HR values
        List<SensorDataEntity> hrValues = new ArrayList<SensorDataEntity>();
        hrValues.addAll(dbValues);
        hrValues.removeIf(p -> p.type != SensorDataEntity.HEART_RATE);

        RecordingActivity.hrArray = new ArrayList();
        for (int i = 0; i < hrValues.size(); i++) {
            RecordingActivity.hrArray.add((int) dbValues.get(i).value);
        }

        //Filter only location values
        List<SensorDataEntity> longitudeValues = new ArrayList<SensorDataEntity>();
        longitudeValues.addAll(dbValues);
        longitudeValues.removeIf(p -> p.type != SensorDataEntity.LONGITUDE);
        List<SensorDataEntity> latitudeValues = new ArrayList<SensorDataEntity>();
        latitudeValues.addAll(dbValues);
        latitudeValues.removeIf(p -> p.type != SensorDataEntity.LATITUDE);

        RecordingActivity.latitudeArray = new float[latitudeValues.size()];
        RecordingActivity.longitudeArray = new float[latitudeValues.size()];

        for (int i = 0; i < latitudeValues.size(); i++) {
            RecordingActivity.latitudeArray[i] = (float) latitudeValues.get(i).value;
        }
        for (int i = 0; i < longitudeValues.size(); i++) {
            RecordingActivity.longitudeArray[i] = (float) longitudeValues.get(i).value;
        }

        onTaskCompletedListener.onTaskCompleted();
    }
}
