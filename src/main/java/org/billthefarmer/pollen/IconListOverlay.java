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

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.util.List;

// IconListOverlay
public class IconListOverlay extends Overlay
{
    // Usual values in the (U,V) coordinates system of the icon image
    public static final float ANCHOR_CENTER = 0.5f, ANCHOR_LEFT = 0.0f,
                              ANCHOR_TOP = 0.0f, ANCHOR_RIGHT = 1.0f, ANCHOR_BOTTOM = 1.0f;

    protected List<Drawable> iconList;
    protected List<IGeoPoint> positionList;

    protected float bearing = 0.0f;
    protected float anchorU = ANCHOR_CENTER, anchorV = ANCHOR_CENTER;
    protected float alpha = 1.0f; // opaque

    protected boolean flat = false; // billboard;

    protected Point point = new Point();

    // IconListOverlay
    public IconListOverlay()
    {
    }

    // IconListOverlay
    public IconListOverlay(List<IGeoPoint> positionList,
                           List<Drawable> iconList)
    {
        set(positionList, iconList);
    }

    // Draw the icons.
    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow)
    {
        if (shadow)
            return;
        if (iconList == null)
            return;
        if (positionList == null)
            return;

        final Projection pj = mapView.getProjection();

        int i = 0;
        for (IGeoPoint pos : positionList)
        {
            Drawable icon = iconList.get(i++);

            pj.toPixels(pos, point);
            int width = icon.getIntrinsicWidth();
            int height = icon.getIntrinsicHeight();
            Rect rect = new Rect(0, 0, width, height);
            rect.offset(-(int) (anchorU * width), -(int) (anchorV * height));
            icon.setBounds(rect);

            icon.setAlpha((int) (alpha * 255));

            float rotationOnScreen = (flat ? -bearing :
                                      mapView.getMapOrientation() - bearing);
            drawAt(canvas, icon, point.x, point.y, false, rotationOnScreen);
        }
    }

    // getPositionList
    public List<IGeoPoint> getPositionList()
    {
        return positionList;
    }

    // set
    public IconListOverlay set(List<IGeoPoint> positionList,
                               List<Drawable> iconList)
    {
        this.positionList = positionList;
        this.iconList = iconList;

        return this;
    }
}
