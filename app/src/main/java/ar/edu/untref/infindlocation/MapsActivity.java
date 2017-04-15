package ar.edu.untref.infindlocation;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.maps.android.SphericalUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.google.maps.android.SphericalUtil.computeDistanceBetween;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private Location currentLocation;
    private LatLng currentLatLng;
    private static final String TAG = "MapsActivity";
    private String exitAreaDateTime;
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private Polygon area;
    ArrayList<LatLng> polygonPoints;
    List<LatLng> puntos = new LinkedList<LatLng>();
    int contadorDePuntos=0;
    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5);
    ScheduledFuture<?> monitor;

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
        int numberOfPoints = this.puntos.size();
        int nextPoint;
        LatLng pointA, pointB;
        for (int i=0; i<numberOfPoints; i++){
            pointA = this.puntos.get(i);
            nextPoint = i+1;
            if(nextPoint < numberOfPoints){
                pointB = this.puntos.get(nextPoint);
            }
            else{
                pointB = this.puntos.get(0);
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
        mMap.clear();
        puntos.clear();
        mMap.setOnMapClickListener(this);
        Button boton;
        boton = (Button) findViewById(R.id.btnListo);
        boton.setVisibility(View.VISIBLE);
        boton = (Button) findViewById(R.id.btnAddPoints);
        boton.setVisibility(View.INVISIBLE);
    }

    public void sendMail(){
        Bundle bundle = getIntent().getExtras();
        final String notificationEmail = bundle.getString("notificationEmail");
        Log.v(TAG, "ENTRE A TESTMAIL");
        Log.v(TAG, "Notification Email: " + notificationEmail);
        String url = "https://infindlocation.herokuapp.com/notifications";
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response).getJSONObject("form");
                            String site = jsonResponse.getString("site"),
                                    network = jsonResponse.getString("network");
                            System.out.println("Site: "+site+"\nNetwork: "+network);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                }
        ) {
            @Override
            public byte[] getBody() throws AuthFailureError, UnsupportedEncodingException {
                JSONObject jsonBody = new JSONObject();
                JSONObject notificationJSON = new JSONObject();

                try {
                    jsonBody.put("email",notificationEmail);
                    jsonBody.put("subject","Estas afuera del area seleccionada");
                    jsonBody.put("key","JxnuLWwqAii5cT4k6iSHFmrvZ3s8zoNiAU4GmpkQz6mDA6d8bDK5aoXJHfpEQmCa");
                    jsonBody.put("time",exitAreaDateTime);
                    jsonBody.put("latitude",currentLocation.getLatitude());
                    jsonBody.put("longitude",currentLocation.getLongitude());
                    jsonBody.put("distance", getOffsetDistanceToPolygon() + " meters");
                    notificationJSON.put("notification",jsonBody);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Log.v(TAG, "JSON: " + notificationJSON.toString().getBytes("utf-8"));
                return notificationJSON.toString().getBytes("utf-8");
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type","application/json; charset=utf-8");
                return headers;
            }
        };
        requestQueue.add(postRequest);
        //requestQueue.start();
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
            Log.d("Puntos de prueba", Double.toString(punto.latitude) + " | " + Double.toString(punto.longitude));
            contadorDePuntos++;
        }
        else{
            Log.d("Estado: ","Todos los puntos fueron agregados");
        }
    }

    Runnable monitorear = new Runnable() {
        @Override
        public void run() {
            Log.v(TAG, "Entre a monitorear area");
            try{
                Date date = new Date();
                currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                currentLatLng = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
                exitAreaDateTime = dateFormat.format(date).toString();
                boolean adentroDelArea = contains(currentLatLng);
                if (!adentroDelArea){
                    playSound();
                    sendMail();
                }
                Log.v(TAG, "Current LAT : " + currentLocation.getLatitude() + " Current LONG: " + currentLocation.getLongitude());
                Log.v(TAG, "Estoy adentro del area? : " + adentroDelArea);
            }
            catch (SecurityException e){
                e.printStackTrace();
            }

        }
    };
    public void startMonitoring(View view){
        Log.v(TAG, "Entre a start monitoring (area)");
        monitor = scheduledThreadPoolExecutor.scheduleWithFixedDelay(monitorear,1,10, TimeUnit.SECONDS);
        Button boton;
        boton = (Button) findViewById(R.id.btnStopMonitoring);
        boton.setVisibility(View.VISIBLE);
        boton = (Button) findViewById(R.id.btnStartMonitoring);
        boton.setVisibility(View.INVISIBLE);
    }

    public void stopMonitoring(View view){
        Log.v(TAG, "Entre a stop monitoring (area)");
        monitor.cancel(true);
        Button boton;
        boton = (Button) findViewById(R.id.btnStopMonitoring);
        boton.setVisibility(View.INVISIBLE);
        boton = (Button) findViewById(R.id.btnStartMonitoring);
        boton.setVisibility(View.VISIBLE);
    }

    //Returns the closest polygon point from current location
    public LatLng getClosestPoint(){
        LatLng closestPoint = puntos.get(0);
        LatLng currentPosition = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
        double distancia= computeDistanceBetween(currentPosition,puntos.get(0));
        int index = 0;
        for (int i = 0; i<puntos.size();i++){
            if(computeDistanceBetween(currentPosition,puntos.get(i))<distancia){
                distancia = computeDistanceBetween(currentPosition,puntos.get(i));
                closestPoint = puntos.get(i);
                index = i;
            }
        }
        return closestPoint;
    }

    //Returns the distance to closest point
    public Double getDistanceToClosestPoint(){
        LatLng closestPoint = getClosestPoint();
        double distance= computeDistanceBetween(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()),closestPoint);
        return distance;
    }

    public int getOffsetDistanceToPolygon(){
        double distance=999999999;
        String strSegment="";
        for (int i=0; i<puntos.size(); i++){
            if (i==puntos.size()-1){
                LatLng[] segment = new LatLng[]{puntos.get(puntos.size()-1),puntos.get(0)};
                LatLng p = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
                strSegment = "Segmento: " + Integer.toString(puntos.size()-1) + " - " + Integer.toString(0);
                if(getDistanceFromPointToSegment(p,segment,strSegment)<distance){
                    distance = getDistanceFromPointToSegment(p,segment,strSegment);
                }
            }
            else{
                LatLng[] segment = new LatLng[]{puntos.get(i),puntos.get(i+1)};
                LatLng p = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
                strSegment = "Segmento: " + Integer.toString(i) + " - " + Integer.toString(i+1);
                if(getDistanceFromPointToSegment(p,segment,strSegment)<distance){
                    distance = getDistanceFromPointToSegment(p,segment,strSegment);

                }
            }
        }
        Log.d("hjk Ubicacion Actual", currentLocation.getLatitude() + " | " + currentLocation.getLongitude());
        Log.d("hjk Distancia Poligono", Integer.toString((int) distance));
        return (int) distance;
    }

    public Double getDistanceFromPointToSegment(LatLng p, LatLng[] segment, String segmento){
        Log.d("hjk Calc distancia", segmento);

        double distancia = -1;
        //AB is the segment
        LatLng a = segment[0];
        LatLng b = segment[1];

        Log.d("hjk Distancia PA", Double.toString(computeDistanceBetween(p,a)));
        Log.d("hjk Distancia PB", Double.toString(computeDistanceBetween(p,b)));
        Log.d("hjk Distancia AB", Double.toString(computeDistanceBetween(a,b)));

        //Angle between PA - AB
        double alpha=
                Math.acos(
                (Math.pow(computeDistanceBetween(p,a),2)+
                   Math.pow(computeDistanceBetween(a,b),2)-
                        Math.pow(computeDistanceBetween(p,b),2))
                            /
                            ((2)*computeDistanceBetween(p,a)*computeDistanceBetween(a,b))

        );
        //Angle between PB - AB
        double beta=
                Math.acos(
                (Math.pow(computeDistanceBetween(p,b),2)+
                        Math.pow(computeDistanceBetween(a,b),2)-
                            Math.pow(computeDistanceBetween(p,a),2))
                                /
                                ((2)*computeDistanceBetween(p,b)*computeDistanceBetween(a,b))

        );
        Log.d("hjk alpha", Double.toString(alpha));
        Log.d("hjk beta", Double.toString(beta));
        //if both angles are lower than 90 then point is somewhere between segment
        if (alpha<(Math.PI/2) && beta<(Math.PI/2)) {
            distancia = (computeDistanceBetween(p,a)/Math.sin(alpha));
        }
        //If Beta is greater or equal than 90 it takes the distance PB
        else if(alpha<(Math.PI/2) && beta>=(Math.PI/2)){
            distancia = (computeDistanceBetween(p,b));
        }
        //Takes the disctance PA
        else if(alpha>=(Math.PI/2) && beta<(Math.PI/2)){
            distancia = (computeDistanceBetween(p,a));
        }
        else{
            distancia = -1.0;
        }
        Log.d("hjk Distancia", Double.toString(distancia));
        return distancia;
    }
}
