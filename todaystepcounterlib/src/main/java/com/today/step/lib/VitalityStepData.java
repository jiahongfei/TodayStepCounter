package com.today.step.lib;

import android.os.Parcel;
import android.os.Parcelable;

import com.litesuits.orm.db.annotation.Column;
import com.litesuits.orm.db.annotation.PrimaryKey;
import com.litesuits.orm.db.annotation.Table;
import com.litesuits.orm.db.enums.AssignType;

import java.io.Serializable;


@Table("vitalitystepdata")
public class VitalityStepData implements Serializable,Parcelable {

    // 指定自增，每个对象需要有一个主键
    @PrimaryKey(AssignType.AUTO_INCREMENT)
    private int id;

    @Column("date")
    private long date;
    @Column("step")
    private long step;

    public VitalityStepData(){

    }

    protected VitalityStepData(Parcel in) {
        id = in.readInt();
        date = in.readLong();
        step = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeLong(date);
        dest.writeLong(step);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<VitalityStepData> CREATOR = new Creator<VitalityStepData>() {
        @Override
        public VitalityStepData createFromParcel(Parcel in) {
            return new VitalityStepData(in);
        }

        @Override
        public VitalityStepData[] newArray(int size) {
            return new VitalityStepData[size];
        }
    };

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getStep() {
        return step;
    }

    public void setStep(long step) {
        this.step = step;
    }

    @Override
    public String toString() {
        return "date : " + date + "   step : " + step;
    }
}
