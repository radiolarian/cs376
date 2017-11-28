package com.example.noon.cs376;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverter;

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
    public Long timestamp; //automatically populates when obj created

    //need typeconverters since DB cant store date objs
    @TypeConverter
    public Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public Long dateToTimestamp(Date date) {
        if (date == null) {
            return null;
        } else {
            return date.getTime();
        }
    }

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
        this.timestamp = dateToTimestamp(Calendar.getInstance().getTime());
    }

    public ParseResult(String data)
    {
        this.data = data;
    }
}