package com.example.noon.cs376;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;

/**
 * Created by estra on 11/11/2017.
 */

@Dao
public interface MainDao {
    @Insert
    void insert(ParseResult parseResult);

    @Insert
    void insertAll(ParseResult... parseResults);

    @Delete
    void delete(ParseResult parseResult);
}
