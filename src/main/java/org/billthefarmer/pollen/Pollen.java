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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

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

    public final static String TEMPLATE =
        "https://socialpollencount.co.uk/api/forecast?" +
        "location=[%f,%f]&platform=mobile";
    public final static String FORMAT =
        "yyyy-MM-dd'T'HH:mm:ss";

    public final static int DELAY = 5000;
    public final static int DISTANCE = 50000;

    private Location last = null;
    private Location location = null;
    private LocationManager locationManager;

    private TextView status;
    private ListView list;

    // onCreate
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        status = (TextView) findViewById(R.id.status);
        list = (ListView) findViewById(R.id.list);

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

        if (location == null)
        {
            location =
                locationManager
                .getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (location != null)
                last = location;
        }
    }

    // On resume
    @Override
    protected void onResume()
    {
        super.onResume();

        status.setText(R.string.locating);

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
                                            int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
            }, null);
    }

    // onSaveInstanceState
    @Override
    protected void onSaveInstanceState (Bundle outState)
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

    // onCreateOptionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
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
                List<JSONObject> forecastList = new ArrayList<JSONObject>();
                for (int i = 0; i < forecast.length(); i++)
                    forecastList.add(forecast.getJSONObject(i));

                PollenAdapter adapter = new PollenAdapter(forecastList);

                if (list != null)
                {
                    list.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }

                DateFormat format =
                    DateFormat.getDateInstance(DateFormat.FULL);
                Date date = new Date(millis);
                String string = format.format(date);
                String updated = getString(R.string.updated);
                String text = String.format(updated, string);
                status.setText(text);
            }

            catch (Exception e) {}

            return;
        }

        double lat = location.getLatitude();
        double lng = location.getLongitude();

        String url = String.format(Locale.getDefault(), TEMPLATE, lat, lng);

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

        public int getCount ()
        {
            return objectList.size();
        }

        // getItem
        @Override
        public JSONObject getItem (int position)
        {
            return objectList.get(position);
        }

        // getItemId
        @Override
        public long getItemId (int position)
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

            TextView dateView = (TextView)
                convertView.findViewById(R.id.date);
            TextView pollenView = (TextView)
                convertView.findViewById(R.id.pollen);
            TextView weatherView = (TextView)
                convertView.findViewById(R.id.weather);

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

            catch (Exception e) {}

            if (dateString != null)
            {
                try
                {
                    Date date = parseFormat.parse(dateString);
                    dateString = dateFormat.format(date);
                }

                catch (Exception e) {}

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

    private class LoadTask extends AsyncTask<String, Integer, String>
    {
        // doInBackground
        @Override
        protected String doInBackground(String... urls)
        {
            try
            {
                URL url = new URL(urls[0]);
                InputStream input = url.openStream();

                try
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

                finally
                {
                    input.close();
                }
            }

            catch (Exception e) {}

            return null;
        }

        // onProgressUpdate
        @Override
        protected void onProgressUpdate(Integer... progress) {}

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

                List<JSONObject> forecastList = new ArrayList<JSONObject>();

                for (int i = 0; i < forecast.length(); i++)
                    forecastList.add(forecast.getJSONObject(i));

                PollenAdapter adapter = new PollenAdapter(forecastList);

                if (list != null)
                {
                    list.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }

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

            catch (Exception e) {}
        }
    }
}
