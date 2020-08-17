package com.example.android.taxitest.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public abstract class CommsDao {

    //comm methods

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertComm(CommRecordObject commRec);

    @Query("update commsTable set firstName=:firstName, lastname=:lastName, gender=:gender, dob=:dob, collar=:collar, reputation=:reputation where commId=:commId")
    public abstract void updatePersonalData(String commId, String firstName, String lastName, String gender,long dob, String collar, double reputation);

    @Query("update commsTable set photoFacePath=:photoFacePath where commId=:commId")
    public abstract void updatePhoto(String commId, String photoFacePath);

    @Query("update commsTable set commStatus=:status where commId=:commId")
    public abstract void updateStatus(String commId, int status);

    @Query("delete from commsTable where commStatus=0")
    public abstract void deleteEmptyComms();

    @Query("select * from commsTable where commStatus!=0 and (firstName like :searchString " +
            "or lastName like :searchString or barrioFrom like :searchString or barrioTo like :searchString) " +
            "order by timestamp desc")
    public abstract LiveData<List<CommRecordObject>> getPastComms(String searchString);

    //msj methods

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertMsj(MsjRecordObject msjRec);

    @Query("update msjTable set received=:received where msjId=:msjId")
    public abstract void updateReceived(String msjId, long received);

    @Query("update msjTable set played=:played where msjId=:msjId")
    public abstract void updatePlayed(String msjId, long played);

    @Query("update msjTable set heard=:heard where msjId=:msjId")
    public abstract void updateHeard(String msjId, long heard);

    @Query("update msjTable set msjStatus=:msjStatus where msjId=:msjId")
    public abstract void updateMsjStatus(String msjId, int msjStatus);

    @Query("select * from msjTable where commId=:commId order by timestamp desc")
    public abstract List<MsjRecordObject> getCommMsjs(String commId);
}
