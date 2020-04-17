package com.example.authendance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.zxing.Result;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

import static android.Manifest.permission.CAMERA;

public class CodeScanner extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private static final int REQUEST_CAMERA = 1;
    private static final String TAG = "CODE_SCAN";
    private ZXingScannerView scannerView;

    private FirebaseAuth fAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (checkPermission()) {
            Toast.makeText(CodeScanner.this, "Please scan the QR code now.", Toast.LENGTH_SHORT).show();
        } else {
            requestCameraPermission();
        }

    }



    //Checks if camera permission has already been granted
    private boolean checkPermission() {
        return (ContextCompat.checkSelfPermission(CodeScanner.this, CAMERA) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission for camera required.")
                    .setMessage("This permission is required to scan QR codes.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(CodeScanner.this, new String[]{CAMERA}, REQUEST_CAMERA);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .create()
                    .show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{CAMERA}, REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Authendance needs the camera to scan QR codes. Please enable camera permission in settings.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (checkPermission()) {
            if (scannerView == null) {
                scannerView = new ZXingScannerView(this);
                setContentView(scannerView);
            }
            scannerView.setResultHandler(this);
            scannerView.startCamera();
        } else {
            requestCameraPermission();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scannerView.stopCamera();
    }

    //Shows alert dialog when QR code is scanned
    @Override
    public void handleResult(Result result) {

        Intent intent = getIntent();
        Bundle data = intent.getExtras();
        String moduleName = data.getString("MOD_NAME");
        String qrCode = data.getString("QR_CODE");
        String docID = data.getString("MOD_ID");
        String studentID = data.getString("STU_ID");

        Log.d(TAG, "Result: " + result.getText());
        Log.d(TAG, "module name: " + moduleName);
        Log.d(TAG, "qrCode: " + qrCode);
        Log.d(TAG, "Student ID: " + studentID);

        assert qrCode != null;
        if (qrCode.equals(result.getText())) {

            String currentDate = java.text.DateFormat.getDateInstance().format(new Date());

            DocumentReference documentReference = db.collection("School")
                    .document("0DKXnQhueh18DH7TSjsb")
                    .collection("Attendance")
                    .document(docID)
                    .collection("Date")
                    .document(currentDate);

            documentReference.update("students_attended", FieldValue.arrayUnion(studentID));

            AlertDialog.Builder builder = new AlertDialog.Builder(CodeScanner.this);
            builder.setTitle("Attendance Authenticated!");
            builder.setMessage("Your attendance for " + moduleName + " on " + currentDate + " has been recorded.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { Intent intent = new Intent(CodeScanner.this, StudentActivity.class);
                        startActivity(intent);
                    }
                });
            AlertDialog alert = builder.create();
            alert.show();
        }
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(CodeScanner.this);
            builder.setTitle("Invalid QR code");
            builder.setMessage("This code is invalid.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(CodeScanner.this, StudentActivity.class);
                    startActivity(intent);
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }
}