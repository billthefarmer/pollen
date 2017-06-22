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
import android.app.ActionBar;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.osmdroid.api.IMapController;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.events.MapAdapter;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;

public class Map extends Activity
{
    private final static String TAG = "Map";

    private final static String TEMPLATE =
        "https://socialpollencount.co.uk/api/points/%04d/%02d/%02d?" +
        "location=[%f,%f]&distance=%d&platform=mobile&hotspots=%d";

    public final static int DELAY = 5000;
    public final static int DISTANCE = 50000;

    private boolean wifi = true;
    private boolean roaming = false;

    private MapView map;

    private Location last = null;
    private LocationManager locationManager;

    // onCreate
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        // Enable back navigation on action bar
        ActionBar actionBar = getActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

	// Set the user agent
	org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
	    .setUserAgentValue(BuildConfig.APPLICATION_ID);

	// Get the map
        map = (MapView)findViewById(R.id.map);
	if (map != null)
	{
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
	    scale.setAlignBottom(true);
	    scale.setAlignRight(true);
	    overlayList.add(scale);
        }

	// Acquire a reference to the system Location Manager
        locationManager = (LocationManager)
	    getSystemService(LOCATION_SERVICE);

	Location location =
	    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location != null)
            last = location;
    }

    // On resume
    @Override
    protected void onResume()
    {
        super.onResume();

        // Get preferences
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        wifi = preferences.getBoolean(Pollen.PREF_WIFI, true);
        roaming = preferences.getBoolean(Pollen.PREF_ROAMING, false);

	Location location =
	    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

	if (location != null)
	    loadData(location);

        locationManager
            .requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                    DELAY, 0, new LocationListener()
            {
                // onLocationChanged
                @Override
                public void onLocationChanged(Location location)
                {
                    if (last != null && location.distanceTo(last) < DISTANCE)
                        locationManager.removeUpdates(this);

                    last = location;
                    loadData(location);

                    IMapController mapController = map.getController();

                    // Zoom map
                    mapController.setZoom(10);

                    // Get point
                    GeoPoint point = new GeoPoint(location);

                    // Centre map
                    mapController.setCenter(point);
                }
 
                @Override
                public void onStatusChanged(String provider,
                                            int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
           });
    }

    // onOptionsItemSelected
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Get id
        int id = item.getItemId();
        switch (id)
        {
        // Home
        case android.R.id.home:
            finish();
            break;

        default:
            return false;
        }

        return true;
    }

    // loadData
    private void loadData(Location location)
    {
        Calendar today = Calendar.getInstance();
        int year = today.get(Calendar.YEAR);
        int month = today.get(Calendar.MONTH);
        int day = today.get(Calendar.DATE);

        double lat = location.getLatitude();
        double lng = location.getLongitude();

        String url = String.format(Locale.getDefault(), TEMPLATE,
                                   year, month, day, lat, lng, 200, 0);

        LoadTask loadTask = new LoadTask();
        loadTask.execute(url);
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
            //
        }
    }
}
