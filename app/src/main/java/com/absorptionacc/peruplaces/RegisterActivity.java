package com.absorptionacc.peruplaces;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etNombres, etApellidos, etFechaNacimiento, etPaisOrigen, etDireccion;
    private Button btnRegistrar;

    // Instancia de FirebaseFirestore
    private FirebaseFirestore db;
    private String email;
    private String uid;

    // Variable para almacenar el correo electrónico

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        // Obtener referencias de los EditText, TextView y Button
        etNombres = findViewById(R.id.etNombres);
        etApellidos = findViewById(R.id.etApellidos);
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento);
        etPaisOrigen = findViewById(R.id.etPaisOrigen);
        etDireccion = findViewById(R.id.etDireccion);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        TextView tvCorreo = findViewById(R.id.tvCorreo);

        // Configurar el click listener del botón Registrar
        btnRegistrar.setOnClickListener(v -> registrarUsuario());

        // Recuperar los datos del Intent
        Intent intent = getIntent();
        if (intent != null) {
            email = intent.getStringExtra("EMAIL");
            uid = intent.getStringExtra("UID");

            // Actualizar el TextView con el correo electrónico
            if (email != null) {
                tvCorreo.setText("Correo:" + email); // Asegúrate de que email no sea null
            } else {
                tvCorreo.setText("Correo no disponible"); // Manejo en caso de que email sea null
            }

            Log.d("RegisterActivity", "Email recibido: " + email + ", UID: " + uid);
        }

        // Inicializar FirebaseFirestore
        db = FirebaseFirestore.getInstance();
    }

    private void registrarUsuario() {
        // Obtener los valores de los EditText
        String nombres = etNombres.getText().toString().trim();
        String apellidos = etApellidos.getText().toString().trim();
        String fechaNacimiento = etFechaNacimiento.getText().toString().trim();
        String paisOrigen = etPaisOrigen.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();

        // Obtener el UID del intent
        String uid = getIntent().getStringExtra("UID");
        if (uid == null) {
            Toast.makeText(this, "Error: UID es null", Toast.LENGTH_SHORT).show();
            return; // No continuar si el UID es null
        }

        // Validar que no haya campos vacíos
        if (TextUtils.isEmpty(nombres) || TextUtils.isEmpty(apellidos) || TextUtils.isEmpty(fechaNacimiento) ||
                TextUtils.isEmpty(paisOrigen) || TextUtils.isEmpty(direccion)) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear un mapa con los datos del usuario
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("nombres", nombres);
        usuario.put("apellidos", apellidos);
        usuario.put("fecha_nacimiento", fechaNacimiento);
        usuario.put("pais_origen", paisOrigen);
        usuario.put("direccion", direccion);
        usuario.put("email", email);

        // Agregar los datos del usuario a Firestore con el UID como identificador del documento
        db.collection("usuarios").document(uid)
                .set(usuario)
                .addOnSuccessListener(aVoid -> {
                    // Éxito al agregar los datos
                    Toast.makeText(RegisterActivity.this, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show();
                    // Puedes agregar aquí cualquier acción adicional después de registrar al usuario
                })
                .addOnFailureListener(e -> {
                    // Error al agregar los datos
                    Toast.makeText(RegisterActivity.this, "Error al registrar usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        setResult(RESULT_OK);
        finish();
    }
}