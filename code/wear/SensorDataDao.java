package ch.epfl.esl.sportstracker;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SensorDataDao {
    //Implementation of the queries to use to access the database

    @Query("SELECT * FROM SensorDataEntity WHERE type = :sensorType ORDER BY timestamp DESC")
    List<SensorDataEntity> getAllValues(int sensorType);

    @Insert
    void insertSensorDataEntityList(List<SensorDataEntity> sensorDataEntityList);

    @Query("DELETE FROM SensorDataEntity")
    void deleteAll();
}
