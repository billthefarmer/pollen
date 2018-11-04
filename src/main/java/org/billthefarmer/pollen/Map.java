////////////////////////////////////////////////////////////////////////////////
//
//  Pollen UK - Android pollen forecast
//
//  Copyright (C) 2017	Bill Farmer
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//  Bill Farmer	 william j farmer [at] yahoo [dot] co [dot] uk.
//
///////////////////////////////////////////////////////////////////////////////

package org.billthefarmer.pollen;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.mylocation.SimpleLocationOverlay;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Map extends Activity {
    private final static String TAG = "Map";

    private final static String DATE = "date";
    private final static String POINTS = "points";
    private final static String LOCATION = "location";
    private final static String METADATA = "metadata";
    private final static String FORECAST = "forecast";
    private final static String LATITUDE = "latitude";
    private final static String LONGITUDE = "longitude";

    private final static String POLLEN =
            "aHR0cHM6Ly9zb2NpYWxwb2xsZW5jb3VudC5jby51ay9hcGkvcG9pbnRzLyUwNGQv" +
                    "JTAyZC8lMDJkP2xvY2F0aW9uPVslZiwlZl0mZGlzdGFuY2U9JWQmcGxhdGZvcm09" +
                    "bW9iaWxlJmhvdHNwb3RzPSVkJnVzZXI9JXMK";

    public final static String FORMAT =
            "yyyy-MM-dd'T'HH:mm:ss";

    private final static String DESCRIPTIONS[] =
            {
                    "Low", "Moderate", "High", "Very High"
            };

    private final static int IDS[] =
            {
                    R.drawable.ic_low, R.drawable.ic_moderate,
                    R.drawable.ic_high, R.drawable.ic_very_high
            };

    public final static int DELAY = 5000;
    public final static int DISTANCE = 50000;

    private TextView status;
    private TextView when;
    private MapView map;
    private IconListOverlay icons;
    private SimpleLocationOverlay simpleLocation;

    private Location last = null;
    private Location location = null;
    private LocationManager locationManager;

    // onCreate
    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        boolean dark = preferences.getBoolean(Pollen.DARK, true);

        if (dark)
            setTheme(R.style.AppDarkTheme);

        setContentView(R.layout.map);

        // Enable back navigation on action bar
        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        // Set the user agent
        Configuration.getInstance()
                .load(this, PreferenceManager.getDefaultSharedPreferences(this));

        // Get the text views
        status = findViewById(R.id.status);
        when = findViewById(R.id.when);

        // Get the map
        map = findViewById(R.id.map);
        if (map != null) {
            // Set up the map
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setBuiltInZoomControls(true);
            map.setMultiTouchControls(true);

            List<Overlay> overlayList = map.getOverlays();

            // Add the overlays
            CopyrightOverlay copyright =
                    new CopyrightOverlay(this);
            overlayList.add(copyright);
            copyright.setAlignBottom(true);
            copyright.setAlignRight(false);

            ScaleBarOverlay scale = new ScaleBarOverlay(map);
            overlayList.add(scale);
            scale.setAlignBottom(true);
            scale.setAlignRight(true);

            simpleLocation =
                    new SimpleLocationOverlay(this);
            overlayList.add(simpleLocation);

            icons = new IconListOverlay();
            overlayList.add(icons);
        }

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager)
                getSystemService(LOCATION_SERVICE);

        if (savedInstanceState != null) {
            double lat = savedInstanceState.getDouble(LATITUDE);
            double lng = savedInstanceState.getDouble(LONGITUDE);

            location = new Location(TAG);
            location.setLatitude(lat);
            location.setLongitude(lng);

            last = location;
        }

        if (location == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    Activity#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for Activity#requestPermissions for more details.
                    return;
                }
            }
            location =
                    locationManager
                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (location != null)
                last = location;
        }
    }

    // On resume
    @Override
    protected void onResume() {
        super.onResume();

        status.setText(R.string.locating);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return;
            }
        }
        location =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        IMapController mapController = map.getController();

        // Zoom map
        mapController.setZoom(7);

        // Get point
        GeoPoint point = new GeoPoint(52.561928, -1.464854);

        // Centre map
        mapController.setCenter(point);

        if (location != null) {
            loadData(location);
            last = location;

            // Get point
            point = new GeoPoint(location);

            // Update location
            simpleLocation.setLocation(point);
        }

        locationManager
                .requestSingleUpdate(LocationManager.GPS_PROVIDER,
                        new LocationListener() {
                            // onLocationChanged
                            @Override
                            public void onLocationChanged(Location location) {
                                if (last == null || location.distanceTo(last) > DISTANCE) {
                                    loadData(location);
                                    last = location;

                                    IMapController mapController = map.getController();

                                    // Get point
                                    GeoPoint point = new GeoPoint(location);

                                    // Update location
                                    simpleLocation.setLocation(point);
                                }
                            }

                            @Override
                            public void onStatusChanged(String provider,
                                                        int status, Bundle extras) {
                            }

                            @Override
                            public void onProviderEnabled(String provider) {
                            }

                            @Override
                            public void onProviderDisabled(String provider) {
                            }
                        }, null);
    }

    // onCreateOptionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map, menu);
        return true;
    }

    // onSaveInstanceState
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (location != null) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();

            outState.putDouble(LATITUDE, lat);
            outState.putDouble(LONGITUDE, lng);
        }
    }

    // onOptionsItemSelected
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Get id
        int id = item.getItemId();
        switch (id) {
            // Home
            case android.R.id.home:
                finish();
                break;

            // Date
            case R.id.action_date:
                onDateClick();
                break;

            default:
                return false;
        }

        return true;
    }

    // onDateClick
    private void onDateClick() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new
                DatePickerDialog(this, (view, year, month, day) -> {
            // Check connectivity before update
            ConnectivityManager manager = (ConnectivityManager)
                    getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();

            // Check connected
            if (info == null || !info.isConnected()) {
                if (status != null)
                    status.setText(R.string.no_connection);

                return;
            }

            // Check location
            if (location == null)
                return;

            double lat = location.getLatitude();
            double lng = location.getLongitude();

            String template = new
                    String(Base64.decode(POLLEN, Base64.DEFAULT));
            String url = String.format(Locale.getDefault(),
                    template,
                    year, month + 1, day,
                    lat, lng, 0, 0,
                    Build.SERIAL);

            LoadTask loadTask = new LoadTask();
            loadTask.execute(url);
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DATE));
        dialog.show();
    }

    // getDrawable
    @SuppressWarnings("deprecation")
    private Drawable getDrawable(String string) {
        int j = 0;
        int id = -1;
        for (String description : DESCRIPTIONS) {
            if (string.equals(description)) {
                id = IDS[j];
                break;
            }

            j++;
        }

        return getResources().getDrawable(id);
    }

    // loadData
    private void loadData(Location location) {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        // Check connectivity before update
        ConnectivityManager manager = (ConnectivityManager)
                getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();

        // Check connected
        if (info == null || !info.isConnected()) {
            if (status != null)
                status.setText(R.string.no_connection);

            long millis = preferences.getLong(DATE, -1);
            String pointString = preferences.getString(FORECAST, null);

            if (millis < 0 || pointString == null)
                return;

            try {
                JSONArray points = new JSONArray(pointString);

                List<IGeoPoint> pointList = new ArrayList<>();
                List<Drawable> iconList = new ArrayList<>();

                for (int i = 0; i < points.length(); i++) {
                    JSONObject point = points.getJSONObject(i);
                    JSONArray loc = point.getJSONArray(LOCATION);

                    double lat = loc.getDouble(0);
                    double lng = loc.getDouble(1);

                    IGeoPoint geoPoint = new GeoPoint(lat, lng);
                    pointList.add(geoPoint);

                    JSONObject metadata = point.getJSONObject(METADATA);
                    String forecast = metadata.getString(FORECAST);

                    Drawable icon = getDrawable(forecast);
                    iconList.add(icon);
                }

                icons.set(pointList, iconList);
                map.invalidate();

                DateFormat format =
                        DateFormat.getDateInstance(DateFormat.FULL);
                Date date = new Date(millis);
                String string = format.format(date);
                String updated = getString(R.string.updated);
                String text = String.format(updated, string);
                status.setText(text);
            } catch (Exception ignored) {
            }

            return;
        }

        Calendar today = Calendar.getInstance();
        int year = today.get(Calendar.YEAR);
        int month = today.get(Calendar.MONTH);
        int day = today.get(Calendar.DATE);

        double lat = location.getLatitude();
        double lng = location.getLongitude();

        String template = new String(Base64.decode(POLLEN, Base64.DEFAULT));
        String url = String.format(Locale.getDefault(), template,
                year, month + 1, day, lat, lng, 0, 0,
                Build.SERIAL);

        LoadTask loadTask = new LoadTask();
        loadTask.execute(url);
    }

    // LoadTask
    private class LoadTask extends AsyncTask<String, Integer, String> {
        // doInBackground
        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);

                try (InputStream input = url.openStream()) {
                    BufferedReader bufferedReader =
                            new BufferedReader(new InputStreamReader(input));
                    StringBuilder content =
                            new StringBuilder(input.available());
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        content.append(line);
                        content.append(System.getProperty("line.separator"));
                    }

                    return content.toString();
                }
            } catch (Exception ignored) {
            }

            return null;
        }

        // onProgressUpdate
        @Override
        protected void onProgressUpdate(Integer... progress) {
        }

        // onPostExecute
        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject json = new JSONObject(result);
                String dateString = json.getString(DATE);
                JSONArray points = json.getJSONArray(POINTS);

                DateFormat parseFormat =
                        new SimpleDateFormat(FORMAT, Locale.getDefault());
                Date date = parseFormat.parse(dateString);

                List<IGeoPoint> pointList = new ArrayList<>();
                List<Drawable> iconList = new ArrayList<>();

                for (int i = 0; i < points.length(); i++) {
                    JSONObject point = points.getJSONObject(i);
                    JSONArray location = point.getJSONArray(LOCATION);

                    double lat = location.getDouble(0);
                    double lng = location.getDouble(1);

                    IGeoPoint geoPoint = new GeoPoint(lat, lng);
                    pointList.add(geoPoint);

                    JSONObject metadata = point.getJSONObject(METADATA);
                    String forecast = metadata.getString(FORECAST);

                    Drawable icon = getDrawable(forecast);
                    iconList.add(icon);
                }

                icons.set(pointList, iconList);
                map.invalidate();

                SharedPreferences preferences = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putLong(DATE, date.getTime());
                editor.putString(POINTS, points.toString());
                editor.apply();

                DateFormat format =
                        DateFormat.getDateInstance(DateFormat.FULL);
                String string = format.format(date);
                String updated = getString(R.string.updated);
                String text = String.format(updated, string);
                status.setText(text);

                if (!pointList.isEmpty()) {
                    JSONObject point = points.getJSONObject(0);
                    JSONObject metadata = point.getJSONObject(METADATA);
                    dateString = metadata.getString(DATE);

                    date = parseFormat.parse(dateString);
                    text = format.format(date);
                    when.setText(text);
                } else
                    when.setText(R.string.no_data);
            } catch (Exception ignored) {
            }
        }
    }
}
