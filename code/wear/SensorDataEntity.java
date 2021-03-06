package ch.epfl.esl.sportstracker;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

//Annotation for Room library to recognize the entity
@Entity
public class SensorDataEntity {
    //Different types of sensors
    public final static int HEART_RATE = 0;
    public final static int LATITUDE = 1;
    public final static int LONGITUDE = 2;

    //Primary key to access the row of SQLite table for the entity SensorData
    @PrimaryKey(autoGenerate = true)
    public int uid;
    //Different coloumn for different attributes of the entity SensorData
    @ColumnInfo
    public long timestamp;
    @ColumnInfo
    public int type;
    @ColumnInfo
    public double value;
}
