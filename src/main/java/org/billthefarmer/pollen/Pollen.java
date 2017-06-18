package org.billthefarmer.pollen;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;

public class Pollen extends Activity
{
    // onCreate
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final ActionBar actionBar = getActionBar();

        // Specify that tabs should be displayed in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener()
            {
                public void onTabSelected(ActionBar.Tab tab,
                                          FragmentTransaction transaction)
                {
                    // show the given tab
                    int pos = tab.getPosition();
                    switch (pos)
                    {
                    case 0:
                        break;
                    case 1:
                        break;
                    }
                }

                public void onTabUnselected(ActionBar.Tab tab,
                                            FragmentTransaction transaction)
                {
                    // hide the given tab
                    int pos = tab.getPosition();
                    switch (pos)
                    {
                    case 0:
                        break;
                    case 1:
                        break;
                    }
                }

                public void onTabReselected(ActionBar.Tab tab,
                                            FragmentTransaction transaction) {}
            };

        // Add tabs, specifying the tab's text and TabListener
        actionBar.addTab(actionBar.newTab()
                         .setText(R.string.forecast)
                         .setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab()
                         .setText(R.string.map)
                         .setTabListener(tabListener));


    }
}
