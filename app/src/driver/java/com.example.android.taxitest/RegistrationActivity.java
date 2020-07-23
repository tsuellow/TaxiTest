package com.example.android.taxitest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.ColorUtils;
import androidx.core.text.HtmlCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Html;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.android.taxitest.connection.MySingleton;
import com.example.android.taxitest.utils.MiscellaneousUtils;
import com.google.android.material.textfield.TextInputLayout;
import com.skydoves.balloon.ArrowOrientation;
import com.skydoves.balloon.Balloon;
import com.skydoves.balloon.BalloonAnimation;
import com.skydoves.balloon.TextForm;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class RegistrationActivity extends AppCompatActivity {

    EditText firstName, lastName, phoneNr, carDescription, nrPlate;
    TextInputLayout loFirstName, loGender, loDob, loPhone, loNrPlate;
    AutoCompleteTextView gender;
    EditText dob;
    TextView textPhotoFace, textPhotoCar, textWarning;
    CheckBox sharePhone, agreeToTerms;

    ImageView photoFace, photoCar, infoFirst, infoLast, infoPlate, infoDesc, infoSharePhone;
    Button register, readTerms;
    ScrollView svParent;

    Date dateOfBirth;
    SharedPreferences preferences;

    int REQUEST_TAKE_FACE=1011;
    int REQUEST_TAKE_CAR=1111;
    int REQUEST_CODE=222;

    private static final String TAG = "RegistrationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_registration);
        getSupportActionBar().setTitle("Welcome to TaxiTest");
        initViews();

        preferences= PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        buildTooltips(infoFirst,"using your real name will increase\n the trust of your clients",0.9f);
        buildTooltips(infoLast,"adding a last name will increase\n the trust of your clients",0.9f);
        buildTooltips(infoPlate,"i.e. ES123567 or M891011",0.90f);
        buildTooltips(infoDesc,"i.e. red KIA or new Corolla silver",0.90f);
        buildTooltips(infoSharePhone,"optional, your phone will only\n be shared with clients you have\n traveled with",0.90f);

        //gender
        List<String> sexes= Arrays.asList("male","female");
        ArrayAdapter<String> occupationAdapter=new ArrayAdapter<String>(RegistrationActivity.this,
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

        //dob
        dob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal=Calendar.getInstance();
                cal.set(2000,0,1);
                Date defaultDate=cal.getTime();
                displayDatePickerDialog(defaultDate);
                dob.setError(null);
            }
        });

        // check permissions CONSIDER MOVING
        ActivityCompat.requestPermissions(RegistrationActivity.this, new String[]{READ_EXTERNAL_STORAGE,ACCESS_FINE_LOCATION, WRITE_EXTERNAL_STORAGE, RECORD_AUDIO,CAMERA},112);


        phoneNr.addTextChangedListener(new PhoneNumberFormattingTextWatcher("NI"));

        //photo face
        photoFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "onClick: we should be here");
                        dispatchTakePictureIntent(REQUEST_TAKE_FACE);
                    } else {
                        Log.d(TAG, "onClick: nor here");
                        String[] permissionRequested = {CAMERA};
                        ActivityCompat.requestPermissions(RegistrationActivity.this,permissionRequested,REQUEST_CODE);
                    }

                } else {
                    Log.d(TAG, "onClick: not here");
                    dispatchTakePictureIntent(REQUEST_TAKE_FACE);

                }
            }


        });

        //photo car
        photoCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        dispatchTakePictureIntent(REQUEST_TAKE_CAR);

                    } else {
                        String[] permissionRequested = {CAMERA};
                        ActivityCompat.requestPermissions(RegistrationActivity.this,permissionRequested,REQUEST_CODE);
                    }

                } else {
                    dispatchTakePictureIntent(REQUEST_TAKE_CAR);
                }
            }


        });

        readTerms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog dialog=makeTermsOfUseDialog(RegistrationActivity.this);
                dialog.show();
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSubmit();
            }
        });

    }

    public void initViews(){
        firstName=(EditText) findViewById(R.id.et_first_name);
        lastName=(EditText) findViewById(R.id.et_last_name);
        phoneNr=(EditText) findViewById(R.id.et_phone);
        carDescription=(EditText) findViewById(R.id.et_car_desc);
        nrPlate=(EditText) findViewById(R.id.et_plate);
        dob=(EditText) findViewById(R.id.et_dob);
        gender=(AutoCompleteTextView) findViewById(R.id.actv_gender);
        photoFace=(ImageView) findViewById(R.id.iv_photo_face);
        photoCar=(ImageView) findViewById(R.id.iv_photo_car);
        register=(Button) findViewById(R.id.bt_register);
        textPhotoFace=(TextView) findViewById(R.id.tv_face);
        textPhotoCar=(TextView) findViewById(R.id.tv_car);
        sharePhone=(CheckBox) findViewById(R.id.cb_share_phone);
        agreeToTerms=(CheckBox) findViewById(R.id.cb_terms);
        readTerms=(Button) findViewById(R.id.bt_terms);

        loFirstName=(TextInputLayout) findViewById(R.id.lo_first_name);
        loGender=(TextInputLayout) findViewById(R.id.lo_gender);
        loDob =(TextInputLayout) findViewById(R.id.lo_dob);
        loPhone=(TextInputLayout) findViewById(R.id.lo_phone);
        loNrPlate=(TextInputLayout) findViewById(R.id.lo_plate);

        infoFirst=(ImageView) findViewById(R.id.iv_info_first_name);
        infoLast=(ImageView) findViewById(R.id.iv_info_last_name);
        infoPlate=(ImageView) findViewById(R.id.iv_info_nr_plate);
        infoDesc=(ImageView) findViewById(R.id.iv_info_car_desc);
        infoSharePhone =(ImageView) findViewById(R.id.iv_info_share_phone);
        textWarning=(TextView) findViewById(R.id.tv_warning);

        svParent=(ScrollView) findViewById(R.id.sv_parent);
    }

    private void buildTooltips(ImageView view, String text, float arrow){
        final Balloon balloonFirst=new Balloon.Builder(getApplicationContext())
                .setArrowSize(10)
                .setArrowOrientation(ArrowOrientation.TOP)
                .setArrowVisible(true)
                .setWidthRatio(0.7f)
                .setPadding(4)
                .setTextSize(12f)
                .setArrowPosition(arrow)
                .setAlpha(0.9f)
                .setText(text)
                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorSelected))
                .setBalloonAnimation(BalloonAnimation.FADE)
                .setAutoDismissDuration(4000)
                .build();

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                balloonFirst.showAlignBottom(view);
            }
        });

    }

    private void displayDatePickerDialog(Date date){
        AlertDialog.Builder mBuilder=new AlertDialog.Builder(RegistrationActivity.this);
        View mView=getLayoutInflater().inflate(R.layout.dialog_date_picker,null);
        final DatePicker mDatePicker=(DatePicker) mView.findViewById(R.id.dp_date_picker);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        mDatePicker.init(year,month,day,null);

        Button mOk=(Button) mView.findViewById(R.id.btn_ok_dp);
        Button mCancel=(Button) mView.findViewById(R.id.btn_cancel_dp);
        mBuilder.setTitle("date of birth");
        mBuilder.setView(mView);

        final AlertDialog dialog=mBuilder.create();
        //button to dismiss dialog
        mOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal=Calendar.getInstance();
                int year, month, day;
                year=mDatePicker.getYear();
                month=mDatePicker.getMonth();
                day=mDatePicker.getDayOfMonth();
                cal.set(year,month,day);
                dob.setText(MiscellaneousUtils.getShortDateString(cal));
                dateOfBirth=MiscellaneousUtils.getRoundDate(cal.getTime());
                dialog.dismiss();
            }
        });

        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    enum Size{FULL, MED, THUMB}

    private void dispatchTakePictureIntent(int type) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile;
            photoFile = imageFile(type,Size.FULL);
            Log.d(TAG, "dispatchTakePictureIntent: "+photoFile.getAbsolutePath());
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getResources().getString(R.string.file_provider),
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent,type);
                Log.d(TAG,  photoURI.toString());

            }

        }

    }

    private File imageFile(int requestType, Size size){
        String name=requestType==REQUEST_TAKE_FACE?"face":"car";
        name=size==Size.FULL?name+"_full":size==Size.MED?name+"_med":name+"_thumb";
        File photoFile;
        photoFile = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), ""+name+".jpg");
        return photoFile;
    }

    //code to frame and display pic
    private void takePic(int code){
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile(code,Size.FULL).getAbsolutePath());
        Bitmap cropImg = MiscellaneousUtils.makeSquaredImage(bitmap);

        savePhotoThumbMed(code, cropImg);
        if (imageFile(code,Size.FULL).exists()){
            imageFile(code,Size.FULL).delete();
        }
    }

    private void savePhotoThumbMed(int code,final Bitmap bitmap) {
        try {
            File thumbFile = imageFile(code,Size.THUMB);
            File mediumFile = imageFile(code,Size.MED);

            if (thumbFile.exists()){
                thumbFile.delete();
            }
            if (mediumFile.exists()){
                mediumFile.delete();
            }

            FileOutputStream thumbOut = new FileOutputStream(thumbFile);
            FileOutputStream mediumOut = new FileOutputStream(mediumFile);

            Bitmap thumb = Bitmap.createScaledBitmap(bitmap, 96, 96, false);
            Bitmap medium = Bitmap.createScaledBitmap(bitmap, 1000, 1000, false);

            mBase64= MiscellaneousUtils.base64Bitmap(thumb);


            thumb.compress(Bitmap.CompressFormat.JPEG, 100, thumbOut);
            medium.compress(Bitmap.CompressFormat.JPEG, 100, mediumOut);

            thumbOut.flush();
            mediumOut.flush();

            thumbOut.close();
            mediumOut.close();
            Log.d("ThumbMed saved", "Thumb and Medium ok");
        } catch (IOException ex) {
            ex.printStackTrace();
            Log.d("ThumbMed saved", "Thumb Medium IOException");
        }
    }

    private void setPicture(int code){
        File medium=imageFile(code, Size.MED);
        String clientMedium=medium.getAbsolutePath();
        ImageView view=code==REQUEST_TAKE_FACE?photoFace:photoCar;
        if (medium.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(clientMedium);
            view.setImageBitmap(bitmap);
        }else{
            view.setImageResource(R.drawable.clooney);
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_TAKE_FACE || requestCode == REQUEST_TAKE_CAR) && resultCode == RESULT_OK) {
            takePic(requestCode);
            setPicture(requestCode);
        }
    }

    //check field input validity
    private boolean checkIsEmpty(EditText editView, TextInputLayout layout){
        if (editView.getText().toString().trim().isEmpty()){
            //layout.setErrorEnabled(true);
            editView.setError("input required");
            layout.setHintTextColor(ColorStateList.valueOf(Color.RED));
            return false;
        }
        loFirstName.setErrorEnabled(false);
        return true;
    }

    private boolean checkPhoneNumber(){
        if (phoneNr.getText().toString().length()==0){
            loPhone.setErrorEnabled(false);
            return true;
        }
        String phoneText= MiscellaneousUtils.depuratePhone(phoneNr.getText().toString());
        try{
            String checkText=phoneText.replaceFirst("^0+(?!$)", "");
            Long.parseLong(checkText);
            if (checkText.length()>=11){
                loPhone.setErrorEnabled(false);
                return true;
            }else{
                //loPhone.setErrorEnabled(false);
                phoneNr.setError("invalid phone number");
                return false;
            }
        }catch(Exception e){
            Toast.makeText(getApplicationContext(),phoneText,Toast.LENGTH_SHORT).show();
            //loPhone.setErrorEnabled(false);
            phoneNr.setError("phone number faulty");
            return  false;
        }
    }

    private boolean checkPhotos(int code, TextView textView){
        if(imageFile(code,Size.MED).exists()){
            textView.setTextColor(ContextCompat.getColor(getApplicationContext(),R.color.colorSelected));
            return true;
        }else{
            textView.setTextColor(Color.RED);
            return false;
        }
    }

    private boolean checkAgreed(){
        if (!agreeToTerms.isChecked()){
            agreeToTerms.setTextColor(Color.RED);
            return false;
        }
        return true;
    }

    public boolean checkAllEntries(){
        boolean checkFirst  =checkIsEmpty(firstName,loFirstName);
        boolean checkGender =checkIsEmpty(gender,loGender);
        boolean checkDob   =checkIsEmpty(dob,loDob);
        boolean checkPlate =checkIsEmpty(nrPlate,loNrPlate);
        boolean checkPhone =checkPhoneNumber();
        boolean checkFace  =checkPhotos(REQUEST_TAKE_FACE,textPhotoFace);
        boolean checkCar   =checkPhotos(REQUEST_TAKE_CAR,textPhotoCar);
        boolean checkAgree =checkAgreed();
        return (checkFirst&&
                checkGender&&
                checkDob&&
                checkPlate&&
                checkPhone&&
                checkFace&&
                checkCar&&
                checkAgree);
    }



    public void onSubmit(){
        if (checkAllEntries()){
            JSONObject json=writeJson();
            if (json!=null){
                registerUser(json,RegistrationActivity.this);
                //send json
            }
        }else{
            textWarning.setVisibility(View.VISIBLE);
            svParent.fullScroll(View.FOCUS_UP);
        }
    }

    String mBase64;
    public JSONObject writeJson(){
        try {
            String lastNameStr=firstName.getText()==null?"":lastName.getText().toString();
            String phoneStr=phoneNr.getText()==null?"":MiscellaneousUtils.depuratePhone(phoneNr.getText().toString());
            String carDescStr=carDescription.getText()==null?"":carDescription.getText().toString();

            JSONObject json=new JSONObject();
            json.put("firstName",firstName.getText().toString());
            json.put("lastName",lastNameStr);
            json.put("dob",MiscellaneousUtils.getDateString(dateOfBirth));
            json.put("gender",gender.getText().toString().substring(0,1));
            json.put("phone",phoneStr);
            json.put("sharePhone",sharePhone.isChecked()?1:0);
            json.put("nrPlate",nrPlate.getText().toString());
            json.put("carDesc",carDescStr);
            json.put("timestamp",MiscellaneousUtils.getDateString(new Date()));
            json.put("photo",mBase64);
            return json;

        }catch (JSONException e){
            e.printStackTrace();
            return null;
        }
    }


    //sync to server

    public void registerUser(final JSONObject backupJson, final Context context){
        final Dialog loadingDialog=makeRegistrationDialog(context,true,false,"null");
        loadingDialog.show();

        JsonObjectRequest jsonObjectRequest;

        jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, Constants.SERVER_URL + "register_driver.php", backupJson, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");

                            if (res.equals("OK")) {
                                register.setEnabled(false);
                                int id=response.getInt("taxiId");
                                SharedPreferences.Editor editor=preferences.edit();
                                editor.putString("taxiId",CustomUtils.getOwnStringId(id));
                                editor.apply();
                                if (loadingDialog.isShowing())
                                loadingDialog.dismiss();
                                Dialog successDialog=makeRegistrationDialog(context,false,false,CustomUtils.getOwnStringId(id));
                                successDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialogInterface) {
                                        Intent i = new Intent(RegistrationActivity.this, EntryActivityDriver.class);
                                        startActivity(i);
                                    }
                                });
                                successDialog.show();
                                //all ok display id
                            }else{
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


    public Dialog makeRegistrationDialog(Context context, final boolean loading, final boolean error, String id){
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
            title="Registration ongoing";
            text="Please wait while the registration process finishes";
            imageView.setVisibility(View.GONE);
            loadingView.setVisibility(View.VISIBLE);
        }else{
            imageView.setVisibility(View.VISIBLE);
            loadingView.setVisibility(View.GONE);
            if (!error){
                title="Registration complete";
                text= "You have been registered successfully.<br>Your user ID is "+"<b>" + id + "</b> <br>This ID will be useful if you ever lose your phone on a ride";
                imageView.setImageResource(R.drawable.confirm);
                imageView.setColorFilter(ContextCompat.getColor(context, R.color.colorGreen), android.graphics.PorterDuff.Mode.MULTIPLY);
            }else{
                title="Error";
                text= "An error occurred while registering.<br> Please make sure your internet connection is working and try again later";
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
                    Intent i = new Intent(RegistrationActivity.this, EntryActivityDriver.class);
                    startActivity(i);
                }
                dialog.dismiss();
            }
        });
        return dialog;
    }

    public Dialog makeTermsOfUseDialog(Context context){
        final Dialog dialog=new Dialog(context);
        dialog.setContentView(R.layout.dialog_register);
        TextView titleView=dialog.findViewById(R.id.tv_title_dialog);
        TextView textView=dialog.findViewById(R.id.tv_text);
        ImageView imageView=(ImageView) dialog.findViewById(R.id.iv_check);
        ProgressBar loadingView=(ProgressBar) dialog.findViewById(R.id.pb_loading);
        Button closeBtn=dialog.findViewById(R.id.bt_close);

        imageView.setVisibility(View.GONE);
        loadingView.setVisibility(View.GONE);

        titleView.setText("Terms of Use");
        textView.setText("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod " +
                "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis " +
                "nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis " +
                "aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat " +
                "nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui " +
                "officia deserunt mollit anim id est laborum.");

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        return dialog;
    }


}
