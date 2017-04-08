package ar.edu.untref.infindlocation;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    EditText emailText;
    Button goToMap;
    Button saveEmail;
    private static final String TAG = "MainActivity";
    private int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
    private int MY_PERMISSIONS_REQUEST_INTERNET;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    String notificationEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "tiene permisos");

        } else {
            Log.v(TAG, "no tiene permisos");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "tiene permisos de internet");

        } else {
            Log.v(TAG, "no tiene permisos de internet");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.INTERNET},
                    MY_PERMISSIONS_REQUEST_INTERNET);
        }

        sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        notificationEmail = sharedPreferences.getString("email","");
        Log.v(TAG, "saved email: " + notificationEmail);
        if (notificationEmail.length() > 0){
            emailText = (EditText) findViewById(R.id.editText);
            emailText.setText(notificationEmail);
        }
        goToMap = (Button) findViewById(R.id.button4);
        goToMap.setOnClickListener(new View.OnClickListener() {
            //TO DO: Si no hay direccion seteada pedir que se setee una
            @Override
            public void onClick(View v) {
                if (notificationEmail.isEmpty()){
                    notificationEmail = sharedPreferences.getString("email","");
                }
                Intent intent = new Intent(MainActivity.this,MapsActivity.class);
                Log.v(TAG, "email a pasar en el intent: " + notificationEmail);
                intent.putExtra("notificationEmail",notificationEmail);
                startActivity(intent);
            }
        });

    }

    public void saveEmail(View view){
        emailText = (EditText) findViewById(R.id.editText);
        editor = sharedPreferences.edit();
        editor.putString("email",emailText.getText().toString());
        editor.commit();
        Log.v(TAG, "entre a saveEmail");
        Log.v(TAG, "email: " + emailText.getText().toString());

        //TODO : Mostrar cartel de que se guardo con exito
    }

}
