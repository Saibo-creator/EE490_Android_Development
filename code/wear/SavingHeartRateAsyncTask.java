package ch.epfl.esl.sportstracker;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

public class SavingHeartRateAsyncTask extends AsyncTask<List<Integer>, Void, Void> {

    private SportTrackerRoomDatabase db;

    SavingHeartRateAsyncTask(SportTrackerRoomDatabase db) {
        this.db = db;
    }

    @SafeVarargs
    @Override
    protected final Void doInBackground(List<Integer>... lists) {
        List<Integer> hrValueList = lists[0];
        List<SensorDataEntity> sensorDataEntityList = new ArrayList<SensorDataEntity>();
        for (Integer hrValue : hrValueList) {
            SensorDataEntity sensorData = new SensorDataEntity();
            sensorData.timestamp = System.nanoTime();
            sensorData.type = SensorDataEntity.HEART_RATE;
            sensorData.value = hrValue;
            sensorDataEntityList.add(sensorData);
        }
        db.sensorDataDao().insertSensorDataEntityList(sensorDataEntityList);
        return null;
    }
}
