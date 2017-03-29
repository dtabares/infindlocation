package ar.edu.untref.infindlocation;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleMap.OnMapClickListener {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private Location currentLocation;
    private LatLng currentLatLng;
    private static final String TAG = "MapsActivity";
    private Polygon area;
    ArrayList<LatLng> polygonPoints;
    List<LatLng> puntos = new LinkedList<LatLng>();
    int contadorDePuntos=0;

    //Interfaz
    Button agregarPuntos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private List generarPuntosDePrueba(){

        polygonPoints = new ArrayList<LatLng>();
        polygonPoints.add(new LatLng(-34.598420, -58.492450));
        polygonPoints.add(new LatLng(-34.596133, -58.492847));
        polygonPoints.add(new LatLng(-34.593104, -58.487751));
        polygonPoints.add(new LatLng(-34.593864, -58.484522));
        polygonPoints.add(new LatLng(-34.600859, -58.486217));

        return polygonPoints;
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try{
            mMap.setMyLocationEnabled(true);
            currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            currentLatLng = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng,20));
            //Log.v(TAG, "entre");
        }
        catch (SecurityException e){
            //Log.v(TAG, "no entre");
            e.printStackTrace();
        }
    }

    //Returns all the points (in tuples) that create the polygon
    private ArrayList<LatLng[]> getPolygonEdges(){
        ArrayList<LatLng[]> edges = new ArrayList<>();
        LatLng[] latLngTuple;
        int numberOfPoints = this.polygonPoints.size();
        int nextPoint;
        LatLng pointA, pointB;
        for (int i=0; i<numberOfPoints; i++){
            pointA = this.polygonPoints.get(i);
            nextPoint = i+1;
            if(nextPoint < numberOfPoints){
                pointB = this.polygonPoints.get(nextPoint);
            }
            else{
                pointB = this.polygonPoints.get(0);
            }
            latLngTuple = new LatLng[2];
            latLngTuple[0] = pointA;
            latLngTuple[1] = pointB;
            edges.add(latLngTuple);
        }
        return edges;
    }

    //Function to check if the point is inside the polygon using the Ray Casting Algorigthm
    private boolean contains(LatLng point){

        LatLng pointA,pointB;
        // epsylon is used to make sure points are not on the same line as vertexes
        double epsylon = 0.00001;

        //huge is used to act as infinity if we divide by 0
        float huge = Float.MAX_VALUE;

        double mEdge, mPoint;

        // We start on the outside of the polygon
        boolean inside = false;

        for(LatLng[] edge: this.getPolygonEdges()){
            //Make sure A is the lower point of the edge
            pointA = edge[0];
            pointB = edge[1];

            if (pointA.latitude > pointB.latitude){
                pointA = edge[1];
                pointB = edge[0];
            }

            //Make sure point is not at same height as vertex
            if (point.latitude == pointA.latitude || point.latitude == pointB.latitude){
                point = new LatLng(point.latitude + epsylon, point.longitude);
            }

            if (point.latitude > pointB.latitude || point.latitude < pointA.latitude || point.longitude > Math.max(pointA.longitude, pointB.longitude)){
                //The horizontal ray does not intersect with the edge
                continue;
            }

            if (point.longitude < Math.min(pointA.longitude, pointB.longitude)){
                // The ray intersects with the edge
                inside = !inside;
                continue;
            }

            try{
                mEdge = (pointB.latitude - pointA.latitude) / (pointB.longitude - pointA.longitude);
            }
            catch (ArithmeticException E){
                mEdge = huge;
            }

            try{
                mPoint = (point.latitude - pointA.latitude) / (point.longitude - pointA.longitude);
            }
            catch (ArithmeticException E){
                mPoint = huge;
            }

            if (mPoint >= mEdge){
                //The ray intersects with the edge
                inside = !inside;
            }
        }

        return inside;
    }

    public void playSound(){
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            MediaPlayer mp = MediaPlayer.create(getApplicationContext(), notification);
            mp.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addPoints(View view){
        mMap.setOnMapClickListener(this);
        Button boton;
        boton = (Button) findViewById(R.id.btnListo);
        boton.setVisibility(View.VISIBLE);
        boton = (Button) findViewById(R.id.btnAddPoints);
        boton.setVisibility(View.INVISIBLE);
    }

    public void addPointsOK(View view){
        Button boton;
        boton = (Button) findViewById(R.id.btnListo);
        boton.setVisibility(View.INVISIBLE);
        boton = (Button) findViewById(R.id.btnAddPoints);
        boton.setVisibility(View.VISIBLE);
        //Dibujo el poligono
        PolygonOptions polygonOptions = new PolygonOptions()
                .strokeColor(Color.RED)
                .fillColor(Color.argb(20, 255, 0, 0));

        Iterator<LatLng> it = puntos.iterator();

        while(it.hasNext()){
            polygonOptions.add(it.next());
        }
        area = mMap.addPolygon(polygonOptions);
        contadorDePuntos=0;
        //Hay que sacar el MapOnClick listener no se como hacerlo
    }

    @Override
    public void onMapClick (LatLng punto)
    {
        if(contadorDePuntos<5) {
            CircleOptions circleOptions = new CircleOptions()
                    .center(punto)
                    .radius(0.1); // In meters
            Circle circle = mMap.addCircle(circleOptions);
            //Log.d("Latitud", Double.toString(punto.latitude));
            //Log.d("Longitud", Double.toString(punto.longitude));
            puntos.add(punto);
            contadorDePuntos++;
        }
        else{
            Log.d("Estado: ","Todos los puntos fueron agregados");
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {

    }

    public void startMonitoring(View view){
        playSound();
    }
}
