package com.example.android.taxitest;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.example.android.taxitest.connection.DataPart;
import com.example.android.taxitest.utils.CustomTextView;
import com.example.android.taxitest.utils.MiscellaneousUtils;
import com.google.android.gms.common.util.IOUtils;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.CAMERA;

public class RegistrationActivityDriver extends RegistrationActivityBasic {

    EditText carDescription, nrPlate;
    AutoCompleteTextView vehicleType;
    TextInputLayout loNrPlate, loVehicleType;
    public TextView textPhotoCar;
    ImageView photoCar, infoPlate, infoDesc;
    CardView cardCar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildTooltips(infoPlate,"i.e. ES123567 or M891011",0.90f);
        buildTooltips(infoDesc,"i.e. red KIA or new Corolla silver",0.90f);

        //photo car
        photoCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(),"wait while camera opens", Toast.LENGTH_SHORT).show();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        dispatchTakePictureIntent(REQUEST_TAKE_CAR);

                    } else {
                        String[] permissionRequested = {CAMERA};
                        ActivityCompat.requestPermissions(mCurrActivity,permissionRequested,REQUEST_CODE);
                    }

                } else {
                    dispatchTakePictureIntent(REQUEST_TAKE_CAR);
                }
            }
        });

        //vehicle type
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
    }

    @Override
    public void setActivities() {
        mCurrActivity=RegistrationActivityDriver.this;
        mNextActivity=EntryActivityDriver.class;
    }

    @Override
    public void initViews() {
        super.initViews();
        carDescription=(EditText) findViewById(R.id.et_car_desc);
        nrPlate=(EditText) findViewById(R.id.et_plate);
        photoCar=(ImageView) findViewById(R.id.iv_photo_car);
        textPhotoCar=(CustomTextView) findViewById(R.id.tv_car);
        vehicleType=(AutoCompleteTextView) findViewById(R.id.actv_type);

        loNrPlate=(TextInputLayout) findViewById(R.id.lo_plate);
        loVehicleType=(TextInputLayout) findViewById(R.id.lo_type);
        infoPlate=(ImageView) findViewById(R.id.iv_info_nr_plate);
        infoDesc=(ImageView) findViewById(R.id.iv_info_car_desc);
        cardCar=(CardView) findViewById(R.id.cv_border_color_car);
    }

    @Override
    public void setPicture(int code) {
        File medium=imageFile(code, Size.MED);
        String clientMedium=medium.getAbsolutePath();
        ImageView view=code==REQUEST_TAKE_FACE?photoFace:photoCar;
        if (medium.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(clientMedium);
            view.setImageBitmap(bitmap);
        }else{
            int placeholder=code==REQUEST_TAKE_FACE?R.drawable.clooney:R.drawable.taxi_photo;
            view.setImageResource(placeholder);
        }
    }
    public static String getVehicleTypeCode(String name){
        switch (name){
            case "taxi":
                return "taxi";
            case "tuk-tuk":
                return "caponera";
            case "bike-taxi":
                return "bicitaxi";
            case "minibus":
                return "minibus";
            case "motorbike":
                return "moto";
            default:
                return "otro";
        }
    }

    @Override
    public void savePrefs() {
        SharedPreferences.Editor editor=preferences.edit();
        editor.putString("firstname",firstName.getText().toString());
        editor.putString("lastname",lastName.getText().toString());
        editor.putString("gender",gender.getText().toString().contentEquals("male")?"m":"f");
        editor.putString("phone",phoneNr.getText()==null?"": MiscellaneousUtils.depuratePhone(phoneNr.getText().toString()));
        editor.putString("nrplate",nrPlate.getText().toString());
        editor.putString("cardesc",carDescription.getText().toString());
        editor.putString("residencecity",city.getText().toString());
        editor.putString("vehicletype",vehicleType.getText().toString());
        editor.putString("photofacepath",imageFile(REQUEST_TAKE_FACE, Size.MED).getAbsolutePath());
        editor.putString("photocarpath",imageFile(REQUEST_TAKE_CAR, Size.MED).getAbsolutePath());
        editor.putLong("dob",dateOfBirth.getTime());
        editor.putBoolean("sharephone",sharePhone.isChecked());
        editor.putBoolean("agreetoterms",agreeToTerms.isChecked());
        editor.apply();
    }

    @Override
    public JSONObject writeJson() {
        try {
            String lastNameStr=firstName.getText()==null?"":lastName.getText().toString();
            String phoneStr=phoneNr.getText()==null?"":MiscellaneousUtils.depuratePhone(phoneNr.getText().toString());
            String carDescStr=carDescription.getText()==null?"":carDescription.getText().toString();
            String cityStr=city.getText()==null?"":city.getText().toString();

            JSONObject json=new JSONObject();
            json.put("firstName",firstName.getText().toString());
            json.put("lastName",lastNameStr);
            json.put("dob",MiscellaneousUtils.getDateString(dateOfBirth));
            json.put("gender",gender.getText().toString().substring(0,1));
            json.put("phone",phoneStr);
            json.put("city",cityStr);
            json.put("vehicleType",getVehicleTypeCode(vehicleType.getText().toString()));
            json.put("sharePhone",sharePhone.isChecked()?1:0);
            json.put("nrPlate",nrPlate.getText().toString());
            json.put("carDesc",carDescStr);
            json.put("timestamp",timestamp);
//            byte[] thumbBytes =MiscellaneousUtils.readFileToBytes(imageFile(REQUEST_TAKE_FACE,Size.THUMB));
//            json.put("thumb",thumbBytes);
//            byte[] faceBytes =MiscellaneousUtils.readFileToBytes(imageFile(REQUEST_TAKE_FACE,Size.MED));
//            json.put("photoFace",faceBytes);
//            byte[] carBytes =MiscellaneousUtils.readFileToBytes(imageFile(REQUEST_TAKE_CAR,Size.MED));
//            json.put("photoCar",carBytes);
//            Log.d("volley",json.toString());
            return json;

        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void setSubmissionParams() {
        requestMethod= Request.Method.POST;
        apiExtension="driver";
    }

    @Override
    public boolean checkAllEntries() {
        boolean checkCar  =checkPhotos(REQUEST_TAKE_CAR,cardCar);
        boolean checkType =checkIsEmpty(vehicleType,loVehicleType);
        return (super.checkAllEntries() && checkCar && checkType);
    }

    @Override
    public Map<String, DataPart> getSubmittableFiles() {
        Map<String, DataPart> files=new HashMap<>();
        files.put("FACE",new DataPart(timestamp+".jpg",MiscellaneousUtils.readFileToBytes(imageFile(REQUEST_TAKE_FACE,Size.MED)),"image/jpeg"));
        files.put("CAR",new DataPart(timestamp+".jpg",MiscellaneousUtils.readFileToBytes(imageFile(REQUEST_TAKE_CAR,Size.MED)),"image/jpeg"));
        files.put("THUMB",new DataPart("THUMB.jpg",MiscellaneousUtils.readFileToBytes(imageFile(REQUEST_TAKE_FACE,Size.THUMB)),"image/jpeg"));
        return files;
    }

}
