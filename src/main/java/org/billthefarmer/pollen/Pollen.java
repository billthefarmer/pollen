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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Pollen extends Activity
{
    private final static String TAG = "Pollen";

    private final static String DATE = "date";
    private final static String POLLEN_COUNT = "pollen_count";
    private final static String TEMPERATURE = "temperature";
    private final static String WEATHER = "weather";
    private final static String FORECAST = "forecast";
    private final static String LATITUDE = "latitude";
    private final static String LONGITUDE = "longitude";

    public final static String DARK = "dark";

    public final static String POLLEN =
        "aHR0cHM6Ly9zb2NpYWxwb2xsZW5jb3VudC5jby51ay9hcGkvZm9yZWNhc3Q/bG9j" +
        "YXRpb249WyVmLCVmXSZwbGF0Zm9ybT1tb2JpbGUK";

    public final static String FORMAT =
        "yyyy-MM-dd'T'HH:mm:ss";

    public final static int DELAY = 5000;
    public final static int DISTANCE = 50000;

    private final static int REQUEST_CREATE = 1;
    private final static int REQUEST_RESUME = 2;

    private Location last = null;
    private Location location = null;
    private LocationManager locationManager;

    private TextView status;
    private TextView empty;
    private ListView list;

    private boolean dark;

    // onCreate
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        dark = preferences.getBoolean(DARK, true);

        if (dark)
            setTheme(R.style.AppDarkTheme);

        setContentView(R.layout.main);

        status = findViewById(R.id.status);
        empty = findViewById(R.id.empty);
        list = findViewById(R.id.list);

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager)
                          getSystemService(LOCATION_SERVICE);

        if (savedInstanceState != null)
        {
            double lat = savedInstanceState.getDouble(LATITUDE);
            double lng = savedInstanceState.getDouble(LONGITUDE);

            location = new Location(TAG);
            location.setLatitude(lat);
            location.setLongitude(lng);

            last = location;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION},
                                   REQUEST_CREATE);
                return;
            }
        }

        if (location == null)
        {
            location = locationManager
                .getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (location != null)
                last = location;
        }
    }

    // onRequestPermissionsResult
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults)
    {
        switch (requestCode)
        {
        case REQUEST_CREATE:
            for (int i = 0; i < grantResults.length; i++)
                if (permissions[i].equals(Manifest.permission
                                          .ACCESS_FINE_LOCATION) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    // Granted
                    if (location == null)
                    {
                        location = locationManager
                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);

                        if (location != null)
                            last = location;
                    }
            break;

        case REQUEST_RESUME:
            for (int i = 0; i < grantResults.length; i++)
                if (permissions[i].equals(Manifest.permission
                                          .ACCESS_FINE_LOCATION) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    // Granted
                    doResume();
        }
    }

    // On resume
    @Override
    protected void onResume()
    {
        super.onResume();

        status.setText(R.string.locating);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)
            {

                requestPermissions(new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION},
                                   REQUEST_RESUME);
                return;
            }
        }

        doResume();
    }

    // doResume
    private void doResume()
    {
        location =
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location != null)
        {
            loadData(location);
            last = location;
        }

        locationManager
            .requestSingleUpdate(LocationManager.GPS_PROVIDER,
                                 new LocationListener()
        {
            // onLocationChanged
            @Override
            public void onLocationChanged(Location location)
            {
                if (last == null || location.distanceTo(last) > DISTANCE)
                {
                    loadData(location);
                    last = location;
                }
            }

            @Override
            public void onStatusChanged(String provider,
                                        int status, Bundle extras)
            {
            }

            @Override
            public void onProviderEnabled(String provider)
            {
            }

            @Override
            public void onProviderDisabled(String provider)
            {
            }
        }, null);
    }

    // onSaveInstanceState
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (location != null)
        {
            double lat = location.getLatitude();
            double lng = location.getLongitude();

            outState.putDouble(LATITUDE, lat);
            outState.putDouble(LONGITUDE, lng);
        }
    }

    // onPause
    @Override
    public void onPause()
    {
        super.onPause();

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putBoolean(DARK, dark);
        editor.apply();
    }

    // onCreateOptionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    // onPrepareOptionsMenu
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        menu.findItem(R.id.action_dark).setChecked(dark);
        return true;
    }

    // onOptionsItemSelected
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Get id
        switch (item.getItemId())
        {
        // Map
        case R.id.action_map:
            onMapClick();
            break;

        // Dark
        case R.id.action_dark:
            onDarkClick(item);
            break;

        default:
            return false;
        }

        return true;
    }

    // onMapClick
    private void onMapClick()
    {
        Intent intent = new Intent(this, Map.class);
        startActivity(intent);
    }

    // onDarkClick
    private void onDarkClick(MenuItem item)
    {
        dark = !dark;
        item.setChecked(dark);

        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.M)
            recreate();
    }

    // loadData
    private void loadData(Location location)
    {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        // Check connectivity before update
        ConnectivityManager manager = (ConnectivityManager)
                                      getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();

        // Check connected
        if (info == null || !info.isConnected())
        {
            if (status != null)
                status.setText(R.string.no_connection);

            long millis = preferences.getLong(DATE, -1);
            String forecastString = preferences.getString(FORECAST, null);

            if (millis < 0 || forecastString == null)
                return;

            try
            {
                JSONArray forecast = new JSONArray(forecastString);
                List<JSONObject> forecastList = new ArrayList<>();
                for (int i = 0; i < forecast.length(); i++)
                    forecastList.add(forecast.getJSONObject(i));

                PollenAdapter adapter = new PollenAdapter(forecastList);

                if (list != null)
                {
                    list.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }

                if ((empty != null) && (forecastList.isEmpty()))
                    empty.setText(R.string.no_data);

                DateFormat format =
                    DateFormat.getDateInstance(DateFormat.FULL);
                Date date = new Date(millis);
                String string = format.format(date);
                String updated = getString(R.string.updated);
                String text = String.format(updated, string);
                status.setText(text);
            }
            catch (Exception e)
            {
            }

            return;
        }

        double lat = location.getLatitude();
        double lng = location.getLongitude();

        String template = new String(Base64.decode(POLLEN, Base64.DEFAULT));
        String url = String.format(Locale.getDefault(), template, lat, lng);

        LoadTask loadTask = new LoadTask();
        loadTask.execute(url);
    }

    // PollenAdapter
    private class PollenAdapter extends BaseAdapter
    {
        private List<JSONObject> objectList;
        private DateFormat parseFormat;
        private DateFormat dateFormat;

        // PollenAdapter
        private PollenAdapter(List<JSONObject> objectList)
        {
            super();

            this.objectList = objectList;

            parseFormat = new SimpleDateFormat(FORMAT, Locale.getDefault());
            dateFormat = DateFormat.getDateInstance(DateFormat.FULL);
        }

        public int getCount()
        {
            return objectList.size();
        }

        // getItem
        @Override
        public JSONObject getItem(int position)
        {
            return objectList.get(position);
        }

        // getItemId
        @Override
        public long getItemId(int position)
        {
            return 0;
        }

        // getView
        @Override
        public View getView(int position, View convertView, ViewGroup container)
        {
            if (convertView == null)
                convertView = getLayoutInflater().inflate(R.layout.item,
                              container, false);

            TextView dateView = convertView.findViewById(R.id.date);
            TextView pollenView = convertView.findViewById(R.id.pollen);
            TextView weatherView = convertView.findViewById(R.id.weather);

            JSONObject item = getItem(position);

            String dateString = null;
            String pollenString = null;
            String tempString = null;
            String weatherString = null;

            try
            {
                dateString = item.getString(DATE);
                pollenString = item.getString(POLLEN_COUNT);
                tempString = item.getString(TEMPERATURE);
                weatherString = item.getString(WEATHER);
            }
            catch (Exception e)
            {
            }

            if (dateString != null)
            {
                try
                {
                    Date date = parseFormat.parse(dateString);
                    dateString = dateFormat.format(date);
                }
                catch (Exception e)
                {
                }

                dateView.setText(dateString);
            }

            if (pollenString != null)
                pollenView.setText(pollenString);

            if (tempString != null && weatherString != null)
            {
                String string = String.format(Locale.getDefault(),
                                              "%s - %s°C", weatherString,
                                              tempString);

                weatherView.setText(string);
                weatherView.setVisibility(View.VISIBLE);
            }
            else
                weatherView.setVisibility(View.GONE);

            return convertView;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class LoadTask extends AsyncTask<String, Integer, String>
    {
        // doInBackground
        @Override
        protected String doInBackground(String... urls)
        {
            try
            {
                URL url = new URL(urls[0]);

                try (InputStream input = url.openStream())
                {
                    BufferedReader bufferedReader =
                        new BufferedReader(new InputStreamReader(input));
                    StringBuilder content =
                        new StringBuilder(input.available());
                    String line;
                    while ((line = bufferedReader.readLine()) != null)
                    {
                        content.append(line);
                        content.append(System.getProperty("line.separator"));
                    }

                    return content.toString();
                }
            }
            catch (Exception e)
            {
            }

            return null;
        }

        // onProgressUpdate
        @Override
        protected void onProgressUpdate(Integer... progress)
        {
        }

        // onPostExecute
        @Override
        protected void onPostExecute(String result)
        {
            try
            {
                JSONObject json = new JSONObject(result);
                String dateString = json.getString(DATE);
                JSONArray forecast = json.getJSONArray(FORECAST);

                DateFormat parseFormat =
                    new SimpleDateFormat(FORMAT, Locale.getDefault());
                Date date = parseFormat.parse(dateString);

                List<JSONObject> forecastList = new ArrayList<>();

                for (int i = 0; i < forecast.length(); i++)
                    forecastList.add(forecast.getJSONObject(i));

                PollenAdapter adapter = new PollenAdapter(forecastList);

                if (list != null)
                {
                    list.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }

                if ((empty != null) && (forecastList.isEmpty()))
                    empty.setText(R.string.no_data);

                SharedPreferences preferences = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putLong(DATE, date.getTime());
                editor.putString(FORECAST, forecast.toString());
                editor.apply();

                DateFormat format =
                    DateFormat.getDateInstance(DateFormat.FULL);
                String string = format.format(date);
                String updated = getString(R.string.updated);
                String text = String.format(updated, string);
                status.setText(text);
            }
            catch (Exception e)
            {
            }
        }
    }
}
