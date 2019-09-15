package com.example.gps_maps;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Region;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.os.SystemClock;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

class ControlaPercurso {
    private Location CurrentLocation;
    private float distancia;
    private boolean Ativo;

    ControlaPercurso() {
        distancia = 0;
        Ativo = false;
    }

    public ControlaPercurso AtualizaPercurso(Location location) {
        if (CurrentLocation == null)
            CurrentLocation = location;

        distancia += CurrentLocation.distanceTo(location);
        CurrentLocation = location;

        return this;
    }

    public boolean getAtivo() {
        return Ativo;
    }

    public float getDistancia() {
        return distancia;
    }

    public void setAtivo(boolean ativo) {
        Ativo = ativo;

        if (Ativo) {
            distancia = 0;
            CurrentLocation = null;
        }
    }

    public double getLatitude(){
        return CurrentLocation == null ? 0 : CurrentLocation.getLatitude();
    }

    public double getLongitude(){
        return CurrentLocation == null ? 0 : CurrentLocation.getLongitude();
    }
}

public class MainActivity extends AppCompatActivity {

    private LocationManager locationManager;
    private LocationListener locationListener;

    private static final int REQUEST_PERMISSION_GPS = 1001;

    private TextView distanciaTextView;

    private Button botaoAtivarGPS;
    private Button botaoDesativarGPS;
    private Button botaoIniciarPercurso;
    private Button botaoFinalizarPercurso;
    private Button botaoConcederrPermissao;

    private ImageButton botaoPesquisa;

    private Chronometer chronometer;

    EditText pesquisaEditText;

    ControlaPercurso controlaPercurso = new ControlaPercurso();


    private void ControlaBotoes(boolean ConcederPermissao, boolean AtivaGPS, boolean DesativaGPS, boolean IniciaPercurso, boolean FinalizaPercurso, boolean Pesquisa) {

        botaoConcederrPermissao.setEnabled(ConcederPermissao);
        botaoAtivarGPS.setEnabled(AtivaGPS);
        botaoDesativarGPS.setEnabled(DesativaGPS);
        botaoIniciarPercurso.setEnabled(IniciaPercurso);
        botaoFinalizarPercurso.setEnabled(FinalizaPercurso);
        botaoPesquisa.setEnabled(Pesquisa);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        botaoAtivarGPS = findViewById(R.id.ativaGPSButton);
        distanciaTextView = findViewById(R.id.distanciaTextView);
        botaoFinalizarPercurso = findViewById(R.id.finalizarButton);
        botaoIniciarPercurso = findViewById(R.id.iniciarButton);
        botaoConcederrPermissao = findViewById(R.id.permissaoGPSButton);
        botaoDesativarGPS = findViewById(R.id.desativarGPSButton);
        botaoPesquisa = findViewById(R.id.buscaButtom);
        chronometer = findViewById(R.id.tempoChronometer);
        pesquisaEditText = findViewById(R.id.buscaEdit);

        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        locationListener =
                new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        if (!controlaPercurso.getAtivo())
                            return;

                        float distancia = controlaPercurso.AtualizaPercurso(location).getDistancia();

                        float metros = distancia % 1000;
                        float kilometros = distancia - metros;

                        distanciaTextView.setText(Math.round(kilometros) + "km " + Math.round(metros) + "m");
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {

                    }

                    @Override
                    public void onProviderEnabled(String provider) {

                    }

                    @Override
                    public void onProviderDisabled(String provider) {

                    }
                };

        botaoPesquisa.setOnClickListener((view) ->{
            if(pesquisaEditText.getText().length() == 0){
                Toast.makeText(this,
                        getString(
                                R.string.no_arguments_to_search
                        ),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Uri uri =
                    Uri.parse(
                            String.format(
                                    Locale.getDefault(),
                                    "geo:%f,%f?q=" + pesquisaEditText.getText(),
                                    controlaPercurso.getLatitude(),
                                    controlaPercurso.getLongitude()
                            )
                    );
            Intent intent =
                    new Intent (
                            Intent.ACTION_VIEW,
                            uri
                    );
            intent.setPackage("com.google.android.apps.maps");
            startActivity(intent);
        });

        botaoConcederrPermissao.setOnClickListener((view) -> {

            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    REQUEST_PERMISSION_GPS
            );

        });

        botaoAtivarGPS.setOnClickListener((view) -> {
            if (ActivityCompat.checkSelfPermission(
                    MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        500,
                        0,
                        locationListener
                );
            }

            ControlaBotoes(false, false, true, true, false, true);
        });

        botaoDesativarGPS.setOnClickListener((view) -> {
            locationManager.
                    removeUpdates(locationListener);

            ControlaBotoes(false, true, false, false, false, false);
        });

        botaoIniciarPercurso.setOnClickListener((view) -> {
            controlaPercurso.setAtivo(true);

            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();

            ControlaBotoes(false, false, false, false, true, true);
        });

        botaoFinalizarPercurso.setOnClickListener((view) -> {
            controlaPercurso.setAtivo(false);

            chronometer.stop();

            ControlaBotoes(false, false, true, true, false, true);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(
                MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) {

            ControlaBotoes(false, true, false, false, false, false);
        } else {

            ControlaBotoes(true, false, false, false, false, false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_GPS) {
            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED) {

                    ControlaBotoes(false, true, false, false, false,false);

                }
            } else {
                Toast.makeText(this,
                        getString(
                                R.string.no_gps_no_app
                        ),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
