package com.absorptionacc.peruplaces;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                // Verifica el estado de autenticación
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                Intent intent;
                if (user != null) {
                    // El usuario está autenticado, dirige directamente a HomeActivity
                    intent = new Intent(MainActivity.this, HomeActivity.class);
                } else {
                    // No hay usuario autenticado, dirige a LoginActivity
                    intent = new Intent(MainActivity.this, LoginActivity.class);
                }

                startActivity(intent);
                finish(); // Finaliza MainActivity para que el usuario no pueda volver a ella
            }
        };

        Timer tiempo = new Timer();
        tiempo.schedule(task, 1000); // Puedes ajustar el tiempo según lo necesites
    }
}
