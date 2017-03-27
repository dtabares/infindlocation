package ar.edu.untref.infindlocation;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private Location currentLocation;
    private LatLng currentLatLng;
    private static final String TAG = "MapsActivity";
    private Polygon area;
    ArrayList<LatLng> polygonPoints;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    protected void addPoints(){

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
            Log.v(TAG, "entre");
            PolygonOptions polygonOptions = new PolygonOptions()
                    .strokeColor(Color.RED)
                    .fillColor(Color.argb(20, 255, 0, 0));

            List puntos = this.generarPuntosDePrueba();
            Iterator<LatLng> it = puntos.iterator();

            while(it.hasNext()){
                polygonOptions.add(it.next());
            }
            area = mMap.addPolygon(polygonOptions);


        }
        catch (SecurityException e){
            Log.v(TAG, "no entre");
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
}
