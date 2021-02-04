package com.example.android.taxitest;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.android.taxitest.utils.MiscellaneousUtils;
import com.google.android.gms.common.util.IOUtils;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;

public class RegistrationActivityClient extends RegistrationActivityBasic {
    EditText priorities;
    public ImageView infoPrio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildTooltips(infoPrio,"optional, i.e. speed, price, security\n or that the driver is good looking",0.90f);
    }

    @Override
    public void setActivities() {
        mCurrActivity=RegistrationActivityClient.this;
        mNextActivity=EntryActivityCustomer.class;
    }

    @Override
    public void initViews() {
        super.initViews();
        priorities =(EditText) findViewById(R.id.et_prio);
        infoPrio =(ImageView) findViewById(R.id.iv_info_prio);
    }

    @Override
    public void savePrefs() {
        SharedPreferences.Editor editor=preferences.edit();
        editor.putString("firstname",firstName.getText().toString());
        editor.putString("lastname",lastName.getText().toString());
        editor.putString("gender",gender.getText().toString().contentEquals("male")?"m":"f");
        editor.putString("phone",phoneNr.getText()==null?"": MiscellaneousUtils.depuratePhone(phoneNr.getText().toString()));
        editor.putString("prio",priorities.getText().toString());
        editor.putString("residencecity",city.getText().toString());
        editor.putString("photofacepath",imageFile(REQUEST_TAKE_FACE, Size.MED).getAbsolutePath());
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
            String cityStr=city.getText()==null?"":city.getText().toString();
            String prios=priorities.getText()==null?"":priorities.getText().toString();

            JSONObject json=new JSONObject();
            json.put("firstName",firstName.getText().toString());
            json.put("lastName",lastNameStr);
            json.put("dob",MiscellaneousUtils.getDateString(dateOfBirth));
            json.put("gender",gender.getText().toString().substring(0,1));
            json.put("phone",phoneStr);
            json.put("city",cityStr);
            json.put("sharePhone",sharePhone.isChecked()?1:0);
            json.put("extra",prios);
            json.put("timestamp",MiscellaneousUtils.getDateString(new Date()));
            return json;

        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
