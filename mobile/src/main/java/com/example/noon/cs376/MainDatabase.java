package com.example.noon.cs376;

/**
 * Created by estra on 11/11/2017.
 */

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {ParseResult.class}, version = 2, exportSchema = false)
public abstract class MainDatabase extends RoomDatabase {
    public abstract MainDao mainDao();
}

