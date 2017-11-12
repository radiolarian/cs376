package com.example.noon.cs376;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

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

    public enum ParseErrorCodes
    {
        SUCCESS,
        NO_AUDIO,
        CANCELLED,
        ERROR
    }

    public ParseResult(ParseErrorCodes errorCode, String data)
    {
        this.errorCode = errorCode;
        this.data = data;
    }

    public ParseResult(String data)
    {
        this.data = data;
    }
}