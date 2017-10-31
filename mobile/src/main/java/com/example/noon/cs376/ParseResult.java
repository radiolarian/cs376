package com.example.noon.cs376;

/**
 * Created by estra on 10/30/2017.
 */

public class ParseResult
{
    public ParseErrorCodes errorCode;
    public String data;

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
}