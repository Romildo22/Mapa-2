package com.android.mapa;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private LatLng latLngAtualGlobal;
    private LatLng latAux;
    private LatLng latAux1;
    private GoogleMap mMap;
    private String[] permissoes = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE
    };
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Button linha_reta;
    private DatabaseReference mDatabase;
    private ImageButton btn_stop, btn_play;
    private DatabaseReference firebaseRef;
    private double longitudeGlobal;
    private double latitudeGlobal;
    private double latituteBANCO;
    private double longitudeBANCO;
    private boolean arm = false;
    private boolean aux1 = false;
    private boolean teste = false;
    private String hora_atual2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_mapa);

        btn_play = findViewById(R.id.btn_play);
        btn_stop = findViewById(R.id.btn_stop);

        firebaseRef = FirebaseDatabase.getInstance().getReference();

        //Validar permissões
        Permissoes.validarPermissoes(permissoes, this, 1);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        //Instanciando as referencias do banco de dados realtime
        mDatabase = FirebaseDatabase.getInstance().getReference();

        rotas();

        btnsBanco();
    }

    //método padrão para incluir/mostrar o mapa na aplicação
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Permissoes.validarPermissoes(permissoes, this, 1);
        mMap = googleMap;
        //metodo que pega a localização em tempo real do usuario
        mMap.setMyLocationEnabled(true);

        location();
        userLocation();
        pontos();
    }
    public void location(){
        LatLng casa = new LatLng(-3.737745, -38.554165);
        mMap.addMarker(new MarkerOptions().position(casa).title("Minha casa"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(casa,17));
    }
    public void userLocation(){
        //Objeto responsável por gerenciar a localização do usuário
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                //dentro do metodo setMyLocationEnabled possui esses dados
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                //mMap.clear();
                LatLng localUsuario = new LatLng(latitude, longitude);
                //Toast.makeText(MapsActivity.this, "local" + localUsuario, Toast.LENGTH_SHORT).show();
                latLngAtualGlobal = localUsuario;
                latitudeGlobal = latitude;
                longitudeGlobal = longitude;
                //mMap.addMarker(new MarkerOptions().position(localUsuario).title("Meu local"));
                //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(localUsuario, 15));

                //Metodos para o banco de dados
                LocationData locationData = new LocationData(latitude,longitude);
               // mDatabase.child("Location").child(String.valueOf(new Date().getTime())).setValue(locationData);

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

        /*
         * 1) Provedor da localização
         * 2) Tempo mínimo entre atualizacões de localização (milesegundos)
         * 3) Distancia mínima entre atualizacões de localização (metros)
         * 4) Location listener (para recebermos as atualizações)
         * */
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0,
                    locationListener
            );
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int permissaoResultado : grantResults) {

            //permission denied (negada)
            if (permissaoResultado == PackageManager.PERMISSION_DENIED) {
                //Alerta
                alertaValidacaoPermissao();
            } else if (permissaoResultado == PackageManager.PERMISSION_GRANTED) {
                //Recuperar localizacao do usuario

                /*
                 * 1) Provedor da localização
                 * 2) Tempo mínimo entre atualizacões de localização (milesegundos)
                 * 3) Distancia mínima entre atualizacões de localização (metros)
                 * 4) Location listener (para recebermos as atualizações)
                 * */
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            0,
                            0,
                            locationListener
                    );
                }

            }
        }

    }

    private void alertaValidacaoPermissao() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissões Negadas");
        builder.setMessage("Para utilizar o app é necessário aceitar as permissões");
        builder.setCancelable(false);
        builder.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    public void rotas(){
        LatLng casa = new LatLng(-3.737745, -38.554165);
        linha_reta = findViewById(R.id.btnLinha);
        linha_reta.setOnClickListener(V -> {

            if(latLngAtualGlobal != null) {
                PolygonOptions polygonOptions = new PolygonOptions();
                polygonOptions.add(casa);
                polygonOptions.add(latLngAtualGlobal);
                polygonOptions.strokeWidth(10);

                mMap.addPolygon(polygonOptions);

                //pontos();
            }else{ Toast.makeText(this, "espere um pouco enquanto verificamos sua localização", Toast.LENGTH_SHORT).show();}

        });
    }

    public void btnsBanco(){
            btn_play.setOnClickListener(V -> {
                arm = true;
                aux1 = true;

                if(latLngAtualGlobal != null) {
                atualizacoes(arm);
                Toast.makeText(this, "Armazenando a localização no Banco de dado", Toast.LENGTH_SHORT).show();

                    teste = true;
                }
                else{ Toast.makeText(this, "espere um pouco enquanto verificamos sua localização", Toast.LENGTH_SHORT).show();}
            });

        btn_stop.setOnClickListener(V->{
            arm = false;
            aux1 = false;

            if(latLngAtualGlobal != null) {
            atualizacoes(arm);
            Toast.makeText(this, "Atualizaçõs encerradas", Toast.LENGTH_SHORT).show();
            teste = false;}
            else{ Toast.makeText(this, "espere um pouco enquanto verificamos sua localização", Toast.LENGTH_SHORT).show();}
        });
    }

    public void atualizacoes(boolean arm){

            new Thread() {
                public void run() {
                    if(arm) {
                        armazenarBanco(arm);
                    }
                }
            }.start();
    }

    public void recuperarLocal(){

        DatabaseReference local = firebaseRef.child("Mapa").child("Location").child(hora_atual2);
        Query reqLocal = local.orderByChild(hora_atual2);

        reqLocal.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Log.d("resultado","onDataChange"+ dataSnapshot.toString());
                Dados local =  dataSnapshot.getValue(Dados.class);
                assert local != null;
                Log.d("resultado2","localLAT "+local.getLatitude());
                Log.d("resultado2","localLONG "+local.getLongitude());
               // Log.d("resultado2","localTESTE "+local.latitude);
               // Log.d("resultado2","localTESTE2 "+local.longitude);
                latituteBANCO = local.getLatitude();
                longitudeBANCO = local.getLongitude();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void pontos(){
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (teste) {
                    LatLng rastro = new LatLng(latituteBANCO, longitudeBANCO);
                    CircleOptions circleOptions = new CircleOptions();
                    circleOptions.center(rastro);
                    circleOptions.strokeWidth(0);
                    circleOptions.radius(6);
                    circleOptions.fillColor(Color.argb(135, 26, 163, 255));
                    mMap.addCircle(circleOptions);
                }
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    0,
                    locationListener
            );
        }
    }

    public void armazenarBanco(boolean arm) {

            while (arm) {
                if(!aux()){
                    break;
                }
                @SuppressLint("SimpleDateFormat") SimpleDateFormat date_format_hora = new SimpleDateFormat("hh:mm:ss");
                Date data = new Date();
                Calendar cal = Calendar.getInstance();
                cal.setTime(data);
                Date data_atual = cal.getTime();
                String hora_atual = date_format_hora.format(data_atual);
                hora_atual2 = hora_atual;

                //Metodos para o banco de dados
                LatLng locationData = new LatLng(latitudeGlobal, longitudeGlobal);
                mDatabase.child("Mapa").child("Location").child(hora_atual).setValue(locationData);
                latAux = locationData;

                recuperarLocal();

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            0,
                            0,
                            locationListener
                    );
                }
                try {
                    Thread.sleep(3500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        public boolean aux(){
         boolean aux = aux1;
         return aux;
        }
    }
