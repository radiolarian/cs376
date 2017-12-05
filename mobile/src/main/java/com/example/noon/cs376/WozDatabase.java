package com.example.noon.cs376;

/**
 * Created by estra on 11/11/2017.
 */

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {WozResult.class}, version = 1, exportSchema = false)
public abstract class WozDatabase extends RoomDatabase {
    public abstract WozDao wozDao();
}

