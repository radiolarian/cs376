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
public class WozResult
{
    public final static int VOLUME_CONDITION__SILENT = 0;
    public final static int VOLUME_CONDITION__MEDIUM = 1;
    public final static int VOLUME_CONDITION__NOISY = 2;
    public final static int CONVERSATION_CONDITION__SHREK = 0;
    public final static int CONVERSATION_CONDITION__TFIOS = 1;
    public final static int CONVERSATION_CONDITION__PB = 2;
    public final static int SPEAKER_VOLUME__LOW = 0;
    public final static int SPEAKER_VOLUME__CORRECT = 1;
    public final static int SPEAKER_VOLUME__HIGH = 2;


    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo
    public int participantId;

    @ColumnInfo
    public int trialNum;

    @ColumnInfo
    public boolean wearingWatch;

    @ColumnInfo
    public int volumeCondition;

    @ColumnInfo
    public int conversationCondition;

    @ColumnInfo
    public int speakerVolume;

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


    public WozResult(int ParticipantID, int TrialNum, boolean WearingWatch, int VolumeCondition, int ConversationCondition, int SpeakerVolume)
    {
        this.participantId = ParticipantID;
        this.trialNum = TrialNum;
        this.wearingWatch = WearingWatch;
        this.volumeCondition = VolumeCondition;
        this.conversationCondition = ConversationCondition;
        this.speakerVolume = SpeakerVolume;
        this.timestamp = dateToTimestamp(Calendar.getInstance().getTime());
    }

    public WozResult()
    {

    }
}