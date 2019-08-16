package com.wdl.gaodemap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;


import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.CameraPosition;
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
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.NameAlias;
import com.raizlabs.android.dbflow.sql.language.OrderBy;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;
import com.raizlabs.android.dbflow.structure.database.transaction.ITransaction;
import com.raizlabs.android.dbflow.structure.database.transaction.QueryTransaction;
import com.raizlabs.android.dbflow.structure.database.transaction.Transaction;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.List;
import java.util.Random;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity implements GeocodeSearch.OnGeocodeSearchListener, EasyPermissions.PermissionCallbacks
{

    private View view1;
    private EditText etAddress, etName;
    private MapView mMapView = null;
    private GeocodeSearch geocodeSearch = null;
    private FrameLayout fl;
    private AMap aMap;
    static final CameraPosition XIAMEN = new CameraPosition.Builder()
            .target(new LatLng(24.48405, 118.03394)).zoom(12).bearing(0).tilt(30).build();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        checkPermissions();


        fl = findViewById(R.id.cl_container);
        AMapOptions aOptions = new AMapOptions();
        aOptions.camera(XIAMEN);
        mMapView = new MapView(this, aOptions);
        mMapView.onCreate(savedInstanceState);

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        fl.addView(mMapView, params);
        if (aMap == null)
            aMap = mMapView.getMap();


        // 查询数据库-初始化
        query();


        geocodeSearch = new GeocodeSearch(this);
        geocodeSearch.setOnGeocodeSearchListener(this);


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view ->
        {
            view1 = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_content, null);
            etAddress = view1.findViewById(R.id.et_address);
            etName = view1.findViewById(R.id.et_student_name);
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setCancelable(false)
                    .setNegativeButton("确定", (dialogInterface, i) ->
                    {
                        String address = etAddress.getText().toString();
                        if (!TextUtils.isEmpty(address))
                        {
                            // name表示地址，第二个参数表示查询城市，中文或者中文全拼，citycode、adcode
                            GeocodeQuery query = new GeocodeQuery(address, "厦门");
                            geocodeSearch.getFromLocationNameAsyn(query);
                        }
                    }).setTitle("请输入具体位置").setView(view1).create();

            if (dialog != null && !dialog.isShowing())
                dialog.show();


        });


    }

    private void query()
    {
        //异步事务
        FlowManager.getDatabase(AppDataBase.class)
                .beginTransactionAsync(databaseWrapper ->
                {
                    List<Info> mList = SQLite.select().from(Info.class).queryList(databaseWrapper);
                    if (mList != null && !mList.isEmpty())
                    {
                        addMarkers(mList);
                    }

                }).build().execute();

    }

    private void addMarkers(List<Info> mList)
    {
        LatLng latLng;
        if (aMap != null)
        {
            for (Info info : mList)
            {
//                latLng = new LatLng(info.getLat(), info.getLat());
//                aMap.addMarker(new MarkerOptions().position(latLng).title(info.getName()).
//                        snippet(info.getAddressInfo()));
                addMarker(info);
            }
        }

    }

    @AfterPermissionGranted(0x01)
    private void checkPermissions()
    {
        String[] perms = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE};
        if (EasyPermissions.hasPermissions(this, perms))
        {

        } else
        {
            EasyPermissions.requestPermissions(this, "权限",
                    0x01, perms);
        }

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

                Info info = new Info();
                info.setName(etName.getText().toString());
                info.setAddressInfo(etAddress.getText().toString());
                info.setLat(lat);
                info.setLon(lon);
                info.save();

                addMarker(info);

//                aMap.addMarker(new MarkerOptions().position(latLng).title(etName.getText().toString()).
//                        snippet(etAddress.getText().toString()));
            }
        }
    }

    private void addMarker(Info info)
    {
        aMap.addMarker(new MarkerOptions().position(new LatLng(info.getLat(), info.getLon())).title(info.getName()).
                snippet(info.getAddressInfo()));
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms)
    {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms)
    {
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
}
