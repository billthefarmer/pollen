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
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.osmdroid.api.IMapController;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.events.MapAdapter;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.mylocation.SimpleLocationOverlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;

public class Map extends Activity
{
    private final static String TAG = "Map";

    private final static String TEMPLATE =
        "https://socialpollencount.co.uk/api/points/%04d/%02d/%02d?" +
        "location=[%f,%f]&distance=%d&platform=mobile&hotspots=%d";

    // onCreate
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
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
