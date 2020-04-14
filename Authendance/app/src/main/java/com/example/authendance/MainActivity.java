package com.example.authendance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "login";
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String EMAIL = "email";
    public static final String PASSWORD = "password";

    private EditText emailField;
    private EditText passwordField;
    private String emailContent;
    private String passwordContent;

    private FirebaseAuth fAuth;
    private FirebaseFirestore db;
    private FirebaseAuth.AuthStateListener fAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        Button signInBtn = findViewById(R.id.signInBtn);
        final TextView forgotPasswordText = findViewById(R.id.forgotPasswordText);

        db = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();
        fAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser fUser = firebaseAuth.getCurrentUser();
                //User is logged in
                if(fUser != null) {
                    Log.d(TAG, "user found");
                }
                //User is logged out
                else {
                    Log.d(TAG, "user not found");
                }
            }
        };

        forgotPasswordText.setVisibility(View.INVISIBLE);

        signInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailField.getText().toString();
                String password = passwordField.getText().toString();

                //If email and password fields are properly filled in
                if(validateEmail() && validatePassword()) {
                    fAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if(task.isSuccessful()) {
                                        Log.d(TAG, "login successful");
                                        saveData();

                                        //User's UID is retrieved to find their record in the database
                                        String uid = Objects.requireNonNull(fAuth.getCurrentUser()).getUid();

                                        //Reference to current user's record in the database
                                        DocumentReference userRef = db.collection("School")
                                                .document("0DKXnQhueh18DH7TSjsb")
                                                .collection("User")
                                                .document(uid);

                                        //This code retrieves the value of the "user_type" field in the record to determine if the user is a student, teacher or admin
                                        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if(task.isSuccessful()) {
                                                    DocumentSnapshot document = task.getResult();
                                                    if(document != null) {
                                                        String userType = document.getString("user_type");
                                                        Toast.makeText(MainActivity.this, "Welcome, " + document.getString("name"), Toast.LENGTH_SHORT).show();

                                                        assert userType != null;
                                                        switch(userType) {
                                                            case "Admin":
                                                                Intent adminIntent = new Intent(MainActivity.this, AdminActivity.class);
                                                                adminIntent.putExtra("FULL_NAME", document.getString("name"));
                                                                startActivity(adminIntent);
                                                                break;
                                                            case "Teacher":
                                                                Intent teacherIntent = new Intent(MainActivity.this, TeacherActivity.class);
                                                                teacherIntent.putExtra("FULL_NAME", document.getString("name"));
                                                                startActivity(teacherIntent);
                                                                break;
                                                            case "Student":
                                                                Intent studentIntent = new Intent(MainActivity.this, StudentActivity.class);
                                                                studentIntent.putExtra("FULL_NAME", document.getString("name"));
                                                                startActivity(studentIntent);
                                                                break;
                                                            default:
                                                                Log.d(TAG, "User type could not be determined");
                                                        }
                                                    }
                                                    else {
                                                        Log.d(TAG, "Document not found");
                                                    }
                                                }
                                            }
                                        });
                                    }
                                    else {
                                        Toast.makeText(MainActivity.this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                                        forgotPasswordText.setVisibility(View.VISIBLE);
                                        //Log.d(TAG, task.getException().getMessage());
                                    }
                                }
                            });
                }
            }
        });

        forgotPasswordText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, PasswordReset.class);
                startActivity(intent);
            }
        });

        loadData();
        updateFields();
    }

    @Override
    protected void onStart() {
        super.onStart();
        fAuth.addAuthStateListener(fAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        fAuth.removeAuthStateListener(fAuthListener);
    }

    //Ensures email and password fields are filled out correctly before login
    private boolean validateEmail() {
        String email = emailField.getText().toString().trim();

        if (email.isEmpty()) {
            emailField.setError("Please enter your email address");
            emailField.requestFocus();
            return false;
        }

        else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError("Please enter a valid email address");
            emailField.requestFocus();
            return false;
        }
        else {
            emailField.setError(null);
            return true;
        }
    }

    private boolean validatePassword() {
        String password = passwordField.getText().toString().trim();

        if(password.isEmpty()) {
            passwordField.setError("Please enter password");
            passwordField.requestFocus();
            return false;
        }
        else {
            passwordField.setError(null);
            return true;
        }
    }

    //Saves email and password in the text fields
    public void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(EMAIL, emailField.getText().toString());
        editor.putString(PASSWORD, passwordField.getText().toString());

        editor.apply();

        Log.d(TAG, "Email: " + emailField.getText().toString() + " and password " + passwordField.getText().toString() + " saved");
    }

    public void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        emailContent = sharedPreferences.getString(EMAIL, "");
        passwordContent = sharedPreferences.getString(PASSWORD, "");
    }

    public void updateFields() {
        emailField.setText(emailContent);
        passwordField.setText(passwordContent);
    }

    /*@Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("emailField", emailField.getText().toString());
        outState.putString("passwordField", passwordField.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        emailField.setText(savedInstanceState.getString("emailField"));
        passwordField.setText(savedInstanceState.getString("passwordField"));
    }*/

}
