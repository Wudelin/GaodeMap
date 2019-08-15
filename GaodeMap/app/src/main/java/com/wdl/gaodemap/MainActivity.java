package com.wdl.gaodemap;

import android.os.Bundle;


import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity implements GeocodeSearch.OnGeocodeSearchListener
{

    private MapView mMapView = null;
    private GeocodeSearch geocodeSearch = null;
    private AMap aMap;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mMapView = findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        if (aMap == null)
            aMap = mMapView.getMap();

        geocodeSearch = new GeocodeSearch(this);
        geocodeSearch.setOnGeocodeSearchListener(this);


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // name表示地址，第二个参数表示查询城市，中文或者中文全拼，citycode、adcode
                GeocodeQuery query = new GeocodeQuery("鼓浪屿", "厦门");
                geocodeSearch.getFromLocationNameAsyn(query);
            }
        });


    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i)
    {

    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i)
    {
        if (geocodeResult == null) return;
        List<GeocodeAddress> mList = geocodeResult.getGeocodeAddressList();
        if (mList != null && !mList.isEmpty())
        {
            LatLonPoint point = mList.get(0).getLatLonPoint();
            if (point != null)
            {
                double lat = point.getLatitude();
                double lon = point.getLongitude();
                // Log.e("wdl", lat + " , " + lon);
                Toast.makeText(MainActivity.this, lat + " , " + lon, Toast.LENGTH_LONG).show();
                LatLng latLng = new LatLng(lat, lon);
                aMap.addMarker(new MarkerOptions().position(latLng).title(mList.get(0).getProvince()).snippet("DefaultMarker"));
            }
        }
    }
}
