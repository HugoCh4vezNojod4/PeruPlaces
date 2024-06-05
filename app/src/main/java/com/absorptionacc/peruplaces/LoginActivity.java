package com.absorptionacc.peruplaces;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class LoginActivity extends AppCompatActivity {
    private boolean isSpanish = true;
    private static final int RC_SIGN_IN = 123;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private EditText etEmail;
    private EditText etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAppLocale(isSpanish ? "es" : "en");
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("715491239290-vae0uic9emnshcb1bnc1ehmatn5q7gt0.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        Button btnAcceder = findViewById(R.id.btnAcceder);
        btnAcceder.setOnClickListener(v -> signInWithEmailPassword());

        Button btnRegistrar = findViewById(R.id.btnRegistrar);
        btnRegistrar.setOnClickListener(v -> signUpWithEmailPassword());

        Button btnLenguaje = findViewById(R.id.btnLenguaje);
        btnLenguaje.setOnClickListener(v -> changeLanguage());

        setupGoogleSignInButton();
    }

    private void setAppLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    private void changeLanguage() {
        isSpanish = !isSpanish;
        setAppLocale(isSpanish ? "es" : "en");
        recreate();
    }

    private void setupGoogleSignInButton() {
        Button signInWithGoogleButton = findViewById(R.id.btnSignInWithGoogle);
        signInWithGoogleButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void firebaseAuthWithGoogle(@NonNull GoogleSignInAccount acct) {
        Log.d("LoginActivity", "firebaseAuthWithGoogle:" + acct.getId());
        // Aquí se muestra cómo obtener el token ID y autenticarse con Firebase
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // La autenticación fue exitosa
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            String email = user.getEmail();
                            String uid = user.getUid();
                            verificarUsuarioExistente(user.getUid(), false);
                        }
                    } else {
                        Log.w("LoginActivity", "signInWithCredential:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInWithEmailPassword() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                verificarUsuarioExistente(user.getUid(), true);
                            }
                        } else {
                            // Manejo de errores, como credenciales incorrectas
                            Toast.makeText(LoginActivity.this, "Error al iniciar sesión: Verifique sus credenciales.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(LoginActivity.this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show();
        }
    }

    private void verificarUsuarioExistente(String uid, boolean isEmailPassword) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("usuarios").document(uid);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    // El usuario existe en Firestore, dirigir a HomeActivity
                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    finish();
                } else {
                    // El usuario no existe en Firestore
                    if (isEmailPassword) {
                        // Dirige al usuario a completar su perfil en Firestore
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            showRegister(user.getEmail(), uid);
                        }
                    } else {
                        // Si el inicio de sesión fue con Google pero no hay datos en Firestore
                        Toast.makeText(LoginActivity.this, "Complete su registro.", Toast.LENGTH_SHORT).show();
                        showRegister(null, uid); // Agregar esta línea para redirigir a RegisterActivity si no lo estás haciendo en otro lugar
                    }
                }
            } else {
                Log.e("LoginActivity", "Error al verificar el usuario en Firestore.", task.getException());
            }
        });
    }

    private void signUpWithEmailPassword() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Usuario creado exitosamente
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                String uid = user.getUid();
                                Log.d("LoginActivity", "Email: " + email + ", UID: " + uid);
                                // Redirigir al usuario a la pantalla de registro para completar su perfil
                                showRegister(email, uid);
                            }
                        } else {
                            // Manejar el error de "usuario ya existe" u otros errores
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                Toast.makeText(LoginActivity.this, "El correo electrónico ya está en uso por otra cuenta. Por favor, inicie sesión.", Toast.LENGTH_LONG).show();
                            } else {
                                // Manejar otros tipos de excepciones aquí
                                Log.e("LoginActivity", "Error en la autenticación: " + task.getException().getMessage());
                                Toast.makeText(LoginActivity.this, "Error en la autenticación: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            Toast.makeText(LoginActivity.this, "Por favor, ingrese correo electrónico y contraseña", Toast.LENGTH_SHORT).show();
        }
    }

    private static final int REQUEST_CODE_REGISTER = 1;

    private void showRegister(String email, String uid) {
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.putExtra("EMAIL", email);
        intent.putExtra("UID", uid);
        startActivity(intent);
    }

    private void showHome(String email, String password) {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("EMAIL", email);
        intent.putExtra("PASSWORD", password);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                } else {
                    Toast.makeText(this, "Cuenta de Google nula", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                Log.w("LoginActivity","signInWithGoogleFailed",e);
                Toast.makeText(this, "Error signing in with Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}

