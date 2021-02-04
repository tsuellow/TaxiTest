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
import com.example.android.taxitest.connection.DataPart;
import com.example.android.taxitest.connection.MySingleton;
import com.example.android.taxitest.utils.MiscellaneousUtils;
import com.google.android.gms.common.util.IOUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifyProfileActivity extends RegistrationActivityDriver {

    String firstNameStr,lastNameStr,genderStr,phoneNrStr,nrPlateStr,carDescStr,photoFacePathStr,
            photoCarPathStr,cityStr,vehicleTypeStr;
    boolean sharePhoneVal, agreeToTermsVal;
    long dobLong;

    TextView salute;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPrefData();
        populateEditBoxes();
        getSupportActionBar().setTitle("Edit profile");
        salute=(TextView) findViewById(R.id.tv_salute);
        salute.setText("User Data");
        register.setText("Submit");
    }

    public void getPrefData(){
        firstNameStr=preferences.getString("firstname","Fulanito");
        lastNameStr=preferences.getString("lastname","de Tal");
        genderStr=preferences.getString("gender","m");
        phoneNrStr=preferences.getString("phone","");
        nrPlateStr=preferences.getString("nrplate","");
        carDescStr=preferences.getString("cardesc","");
        cityStr=preferences.getString("residencecity","");
        vehicleTypeStr=preferences.getString("vehicletype","");
        photoFacePathStr=preferences.getString("photofacepath",null);
        photoCarPathStr=preferences.getString("photocarpath",null);
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
        //vehicle type
        vehicleType.setText(vehicleTypeStr);
        List<String> types= Arrays.asList("taxi","tuk-tuk","bike-taxi","motorbike","minibus","other");
        ArrayAdapter<String> typeAdapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, types);
        vehicleType.setAdapter(typeAdapter);
        vehicleType.setKeyListener(null);
        vehicleType.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                ((AutoCompleteTextView) view).showDropDown();
                vehicleType.setError(null);
                return false;
            }
        });
        phoneNr.setText(phoneNrStr);
        nrPlate.setText(nrPlateStr);
        carDescription.setText(carDescStr);
        Calendar cal=Calendar.getInstance();
        cal.setTime(new Date(dobLong));
        dob.setText(MiscellaneousUtils.getShortDateString(cal));
        sharePhone.setChecked(sharePhoneVal);
        agreeToTerms.setChecked(agreeToTermsVal);
        File faceFile=imageFile(REQUEST_TAKE_FACE,Size.MED);
        Bitmap faceBitmap= BitmapFactory.decodeFile(faceFile.getAbsolutePath());
        photoFace.setImageBitmap(faceBitmap);
        File carFile=imageFile(REQUEST_TAKE_CAR,Size.MED);
        Bitmap carBitmap= BitmapFactory.decodeFile(carFile.getAbsolutePath());
        photoCar.setImageBitmap(carBitmap);
        mBase64= MiscellaneousUtils.base64Bitmap(BitmapFactory.decodeFile(imageFile(REQUEST_TAKE_FACE,Size.THUMB).getAbsolutePath()));
    }

    @Override
    public Map<String, DataPart> getSubmittableFiles() {
        Map<String, DataPart> files=new HashMap<>();
        files.put("FACE",new DataPart(timestamp+".jpg",MiscellaneousUtils.readFileToBytes(imageFile(REQUEST_TAKE_FACE,Size.MED)),"image/jpeg"));
        files.put("CAR",new DataPart(timestamp+".jpg",MiscellaneousUtils.readFileToBytes(imageFile(REQUEST_TAKE_CAR,Size.MED)),"image/jpeg"));
        files.put("THUMB",new DataPart(MiscellaneousUtils.getNumericId(preferences.getString("taxiId","0"))+".jpg",MiscellaneousUtils.readFileToBytes(imageFile(REQUEST_TAKE_FACE,Size.THUMB)),"image/jpeg"));
        return files;
    }

    @Override
    public Map<String, String> getHeaders() {
        Map<String, String> headers=new HashMap<>();
        headers.put("token",preferences.getString("token",""));
        return headers;
    }

    //overwritten for register button
    @Override
    public void setSubmissionParams() {
        requestMethod= Request.Method.PATCH;
        apiExtension="driver/"+MiscellaneousUtils.getNumericId(preferences.getString("taxiId","0"));

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
