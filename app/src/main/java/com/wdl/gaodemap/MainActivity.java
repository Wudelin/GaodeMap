package com.wdl.gaodemap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    // 0:通过按钮添加
    // 1:通过excel导入添加
    private int type = -1;

    private String type1Name = "";

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
                        type = 0;
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
            chooser();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 选择文件
     */
    private void chooser()
    {
        //导入格式为 .xls .xlsx
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/*");//设置类型
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 1);
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
        double lat;
        double lon;
        if (mList != null && !mList.isEmpty())
        {
            LatLonPoint point = mList.get(0).getLatLonPoint();
            if (point != null)
            {
                lat = point.getLatitude();
                lon = point.getLongitude();
                // Log.e("wdl", lat + " , " + lon);
                Toast.makeText(MainActivity.this, lat + " , " + lon, Toast.LENGTH_LONG).show();
                // LatLng latLng = new LatLng(lat, lon);

                // 按钮添加
                // 添加完成后重置类型
                if (type == 0)
                {
                    Info info = new Info();
                    info.setName(etName.getText().toString());
                    info.setAddressInfo(etAddress.getText().toString());
                    info.setLat(lat);
                    info.setLon(lon);
                    info.save();
                    addMarker(info);
                    type = -1;
                } else if (type == 1)
                {
                    Info info = SQLite
                            .select()
                            .from(Info.class)
                            .where(Info_Table.name.eq(type1Name))
                            .querySingle();

                    assert info != null;
                    info.setLat(lat);
                    info.setLon(lon);
                    info.update();
                    addMarker(info);
                    type = -1;
                }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null)
        {
            Log.e("wdl", "选择的文件Uri = " + data.toString());
            //通过Uri获取真实路径
            final String excelPath = getRealFilePath(this, data.getData());
            Log.e("wdl", "excelPath = " + excelPath);//    /storage/emulated/0/test.xls
            assert excelPath != null;
            if (excelPath.contains(".xls") && !excelPath.contains(".xlsx"))
            {
                //载入excel
                readExcel(excelPath);
            } else
            {
                return;
            }
        }
    }

    //读取Excel表
    private void readExcel(String excelPath)
    {
        try
        {
            File file = new File(excelPath);
            InputStream input = new FileInputStream(file);
            POIFSFileSystem fs = new POIFSFileSystem(input);
            // HSSFWorkbook相当于一个excel表

            HSSFWorkbook wb = new HSSFWorkbook(fs);
            // HSSFWorkbook代表一个工作簿
            HSSFSheet sheet = wb.getSheetAt(0);
            // Row代表一行
            Iterator<Row> rows = sheet.rowIterator();
            List<Info> mList = new ArrayList<>();
            while (rows.hasNext())
            {

                // Row代表一行
                HSSFRow row = (HSSFRow) rows.next();
                //每一行 = 新建一个学生
                Info stu = new Info();
                // cell代表一个单元格
                Iterator<Cell> cells = row.cellIterator();

                // cell代表一个单元格
                HSSFCell cell0 = (HSSFCell) cells.next();
                HSSFCell cell1 = (HSSFCell) cells.next();
                // 第一个单元格
                stu.setName(cell0.getStringCellValue());

                // 第二个单元格
                stu.setAddressInfo(cell1.getStringCellValue());

                stu.save();
                mList.add(stu);
            }


            mHandler.post(new Runnable()
            {

                @Override
                public void run()
                {

                    type = 1;
                    if (index < mList.size())
                    {
                        Info info = mList.get(index);
                        index += 1;
                        if (info != null)
                        {
                            type1Name = info.getName();
                            GeocodeQuery query = new GeocodeQuery(info.getAddressInfo(), "厦门");
                            geocodeSearch.getFromLocationNameAsyn(query);
                            mHandler.postDelayed(this, 1000);
                        }
                    }
                }
            });

            index = 0;

        } catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }


    /**
     * 根据Uri获取真实路径
     * <p/>
     * 一个android文件的Uri地址一般如下：
     * content://media/external/images/media/62026
     *
     * @param context
     * @param uri
     * @return
     */
    public static String getRealFilePath(final Context context, final Uri uri)
    {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme))
        {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme))
        {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor)
            {
                if (cursor.moveToFirst())
                {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1)
                    {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }


    private int index = 0;


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(@NonNull Message msg)
        {
            super.handleMessage(msg);
        }
    };



}
