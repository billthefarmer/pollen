
package org.billthefarmer.pollen;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Forecast extends ListFragment
{
    public final static String TAG = "Forecast";
    public final static String TEMPLATE =
        "https://socialpollencount.co.uk/api/forecast?" +
        "location=[%f,%f]&platform=mobile";

    // @Override
    // public View onCreateView(LayoutInflater inflater, ViewGroup container,
    //                          Bundle savedInstanceState)
    // {
    //     // Inflate the layout for this fragment
    //     return inflater.inflate(R.layout.main, container, false);
    // }
}
