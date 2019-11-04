package com.android.mapa;

public class LocationData {
    //para salvar o objeto no banco, os dados tem que ser publicos
    double latitude;
    double longitude;

    public LocationData(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
