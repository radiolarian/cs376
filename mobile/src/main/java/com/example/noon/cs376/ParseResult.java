package com.example.noon.cs376;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by estra on 10/30/2017.
 */

@Entity
public class ParseResult
{
    @PrimaryKey(autoGenerate = true)
    public int uid;

    //@ColumnInfo(name = "error_code")
    @Ignore
    //@ColumnInfo
    public ParseErrorCodes errorCode = ParseErrorCodes.ERROR;

    @ColumnInfo
    public String data = "";

    @ColumnInfo
    public float envNoise = -1f; //-1 means error by default

    @ColumnInfo
    public Date timestamp; //automatically populates when obj created

    public enum ParseErrorCodes
    {
        SUCCESS,
        NO_AUDIO,
        CANCELLED,
        ERROR
    }

    public ParseResult(ParseErrorCodes errorCode, String data, float envNoise)
    {
        this.errorCode = errorCode;
        this.data = data;
        this.envNoise = envNoise;
        this.timestamp = Calendar.getInstance().getTime();
    }

    public ParseResult(String data)
    {
        this.data = data;
    }
}