package ch.epfl.esl.sportstracker;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {SensorDataEntity.class}, version = 1)
public abstract class SportTrackerRoomDatabase extends RoomDatabase {
    //Abstract class for inheritance: you don't implement the methods but you
    // can extend this class and implement them and add other features

    //Dao to associate to the database and use the queries implemented
    public abstract SensorDataDao sensorDataDao();

    //Instance of the database that will be used later
    private static SportTrackerRoomDatabase INSTANCE;

    //Constructor of the class. It's "synchronized" to avoid that concurrent
    // threads corrupts the instance.
    public static synchronized SportTrackerRoomDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context, SportTrackerRoomDatabase.class, "SportTrackerDB").build();
        }
        return INSTANCE;
    }

    //Method to destroy the instance of the database
    public static void destroyInstance() {
        INSTANCE = null;
    }
}
