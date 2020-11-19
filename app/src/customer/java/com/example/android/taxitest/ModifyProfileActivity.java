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
import com.example.android.taxitest.utils.MiscellaneousUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ModifyProfileActivity extends RegistrationActivity {

    String firstNameStr,lastNameStr,genderStr,phoneNrStr,prioritiesStr,photoFacePathStr;
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
        prioritiesStr=preferences.getString("prio","");
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

    @Override
    public JSONObject writeJson() {
        try {
            String lastNameStr=lastName.getText()==null?"":lastName.getText().toString();
            String phoneStr=phoneNr.getText()==null?"":MiscellaneousUtils.depuratePhone(phoneNr.getText().toString());

            JSONObject json=new JSONObject();
            json.put("id",MiscellaneousUtils.getNumericId(preferences.getString("taxiId","0")));
            json.put("firstName",firstName.getText().toString());
            json.put("lastName",lastNameStr);
            json.put("dob",MiscellaneousUtils.getDateString(dateOfBirth));
            json.put("gender",gender.getText().toString().substring(0,1));
            json.put("phone",phoneStr);
            json.put("sharePhone",sharePhone.isChecked()?1:0);
            json.put("extra","");
            json.put("timestamp",MiscellaneousUtils.getDateString(new Date()));
            json.put("photo",mBase64);
            return json;

        }catch (JSONException e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void registerUser(JSONObject backupJson, Context context) {
        final Dialog loadingDialog=makeRegistrationDialog(context,true,false,"null");
        loadingDialog.show();

        JsonObjectRequest jsonObjectRequest;

        jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, Constants.SERVER_URL + "update_client.php", backupJson, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");

                            if (res.equals("OK")) {
                                register.setEnabled(false);
                                savePrefs();
                                if (loadingDialog.isShowing())
                                    loadingDialog.dismiss();
                                Dialog successDialog=makeRegistrationDialog(context,false,false,preferences.getString("taxiId","0"));
                                successDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialogInterface) {
                                        onBackPressed();
                                    }
                                });
                                successDialog.show();
                                //all ok display id
                            }else{
                                Toast.makeText(context,"Modification failed: "+res,Toast.LENGTH_LONG).show();
                                //error on server side fix
                            }
                            //notification
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(context,"json failed",Toast.LENGTH_LONG).show();

                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        if (loadingDialog.isShowing())
                            loadingDialog.dismiss();
                        Dialog errorDialog=makeRegistrationDialog(context,false,true,"null");
                        errorDialog.show();

                    }
                });
        MySingleton.getInstance(context).addToRequestQueue(jsonObjectRequest);
    }

    @Override
    public Dialog makeRegistrationDialog(Context context, boolean loading, boolean error, String id) {
        final Dialog dialog=new Dialog(context);
        dialog.setContentView(R.layout.dialog_register);
        TextView titleView=dialog.findViewById(R.id.tv_title_dialog);
        TextView textView=dialog.findViewById(R.id.tv_text);
        ImageView imageView=(ImageView) dialog.findViewById(R.id.iv_check);
        ProgressBar loadingView=(ProgressBar) dialog.findViewById(R.id.pb_loading);
        Button closeBtn=dialog.findViewById(R.id.bt_close);
        String title;
        String text;
        if(loading){
            title="Submission ongoing";
            text="Please wait while data submission process finishes";
            imageView.setVisibility(View.GONE);
            loadingView.setVisibility(View.VISIBLE);
        }else{
            imageView.setVisibility(View.VISIBLE);
            loadingView.setVisibility(View.GONE);
            if (!error){
                title="Submission complete";
                text= "You have changed your profile data successfully.<br>Your user ID is still "+"<b>" + id + "</b> <br>This ID will be useful if you ever lose your phone on a ride";
                imageView.setImageResource(R.drawable.confirm);
                imageView.setColorFilter(ContextCompat.getColor(context, R.color.colorGreen), android.graphics.PorterDuff.Mode.MULTIPLY);
            }else{
                title="Error";
                text= "An error occurred while sending the data.<br> Please make sure your internet connection is working and try again later";
                imageView.setImageResource(R.drawable.close);
                imageView.setColorFilter(ContextCompat.getColor(context, R.color.colorRed), android.graphics.PorterDuff.Mode.MULTIPLY);
            }
        }
        textView.setText(Html.fromHtml(text));
        titleView.setText(title);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!loading && !error) {
                    Intent i = new Intent(ModifyProfileActivity.this, SettingsActivity.class);
                    startActivity(i);
                }
                dialog.dismiss();
            }
        });
        return dialog;
    }
}
