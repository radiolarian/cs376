package com.example.noon.cs376;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * Created by estra on 11/11/2017.
 */

@Dao
public interface WozDao {
    @Insert
    void insert(WozResult wozResult);

    @Insert
    void insertAll(WozResult... wozResults);

    @Delete
    void delete(WozResult wozResult);

    @Query("SELECT * FROM wozresult")
    List<WozResult> getWozResults();
}
