package com.example.android.taxitest;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Html;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.android.taxitest.connection.MySingleton;
import com.example.android.taxitest.utils.CustomTextView;
import com.example.android.taxitest.utils.MiscellaneousUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifyProfileActivity extends RegistrationActivityClient {

    String firstNameStr,lastNameStr,genderStr,phoneNrStr,prioritiesStr,photoFacePathStr,cityStr;
    boolean sharePhoneVal, agreeToTermsVal;
    long dobLong;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPrefData();
        populateEditBoxes();
        getSupportActionBar().setTitle("Edit Profile");
        salute=(CustomTextView) findViewById(R.id.tv_salute);
        salute.setText("Personal\nData");
        register.setText("Submit");
    }

    public void getPrefData(){
        firstNameStr=preferences.getString("firstname","Fulanito");
        lastNameStr=preferences.getString("lastname","de Tal");
        genderStr=preferences.getString("gender","m");
        phoneNrStr=preferences.getString("phone","");
        prioritiesStr=preferences.getString("prio","");
        cityStr=preferences.getString("residencecity","");
        photoFacePathStr=preferences.getString("photofacepath",null);
        dobLong=preferences.getLong("dob",0);
        dateOfBirth=new Date(dobLong);
        sharePhoneVal=preferences.getBoolean("sharephone",false);
        agreeToTermsVal =preferences.getBoolean("agreetoterms",true);
    }

    public void populateEditBoxes(){
        firstName.setText(firstNameStr);
        lastName.setText(lastNameStr);
        if (genderStr.contentEquals("m")){
            gender.setText("male");
        }else{
            gender.setText("female");
        }
        //gender
        List<String> sexes= Arrays.asList("male","female");
        ArrayAdapter<String> occupationAdapter=new ArrayAdapter<String>(ModifyProfileActivity.this,
                android.R.layout.simple_list_item_1, sexes);
        gender.setAdapter(occupationAdapter);
        gender.setKeyListener(null);
        gender.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                ((AutoCompleteTextView) view).showDropDown();
                gender.setError(null);
                return false;
            }
        });
        city.setText(cityStr);
        ArrayAdapter<String> cityAdapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, cities.getCityDropdown());
        city.setAdapter(cityAdapter);
        city.setKeyListener(null);
        city.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                ((AutoCompleteTextView) view).showDropDown();
                city.setError(null);
                return false;
            }

        });
        phoneNr.setText(phoneNrStr);
        priorities.setText(prioritiesStr);
        Calendar cal=Calendar.getInstance();
        cal.setTime(new Date(dobLong));
        dob.setText(MiscellaneousUtils.getShortDateString(cal));
        sharePhone.setChecked(sharePhoneVal);
        agreeToTerms.setChecked(agreeToTermsVal);
        File faceFile=imageFile(REQUEST_TAKE_FACE,Size.MED);
        Bitmap faceBitmap= BitmapFactory.decodeFile(faceFile.getAbsolutePath());
        photoFace.setImageBitmap(faceBitmap);
        mBase64= MiscellaneousUtils.base64Bitmap(BitmapFactory.decodeFile(imageFile(REQUEST_TAKE_FACE,Size.THUMB).getAbsolutePath()));
    }

    //overwritten for register button
    @Override
    public void setSubmissionParams() {
        requestMethod= Request.Method.PATCH;
        apiExtension="client/"+MiscellaneousUtils.getNumericId(preferences.getString("taxiId","0"));
    }

    @Override
    public Map<String, String> getHeaders() {
        Map<String, String> headers=new HashMap<>();
        headers.put("token",preferences.getString("token",""));
        return headers;
    }

    //overwritten for register button
    @Override
    public int doOnSuccess(JSONObject json) throws JSONException {
        return MiscellaneousUtils.getNumericId(preferences.getString("taxiId","0"));
    }


    @Override
    public void setActivities() {
        mCurrActivity=ModifyProfileActivity.this;
        mNextActivity=SettingsActivity.class;
    }

    @Override
    public void defineRegistrationDialogStrings(String id) {
        titleOngoing="Submission ongoing";
        textOngoing="Please wait while data submission process finishes";
        titleSuccess="Submission complete";
        textSuccess="You have changed your profile data successfully.<br>Your user ID is still "+"<b>" + id + "</b> <br>This ID will be useful if you ever lose your phone";
        titleError="Error";
        textError="An error occurred while sending the data.<br> Please make sure your internet connection is working and try again later";
    }


}
