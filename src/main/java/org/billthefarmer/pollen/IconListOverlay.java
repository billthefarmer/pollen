
package org.billthefarmer.pollen;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.Paint;
import android.util.DisplayMetrics;

import java.util.List;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.Projection;

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

    protected Point point = new Point();;

    // IconListOverlay
    public IconListOverlay() {}

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
        for (IGeoPoint pos: positionList)
        {
            Drawable icon = iconList.get(i++);

            pj.toPixels(pos, point);
            int width = icon.getIntrinsicWidth();
            int height = icon.getIntrinsicHeight();
            Rect rect = new Rect(0, 0, width, height);
            rect.offset(-(int)(anchorU * width), -(int)(anchorV * height));
            icon.setBounds(rect);

            icon.setAlpha((int) (alpha * 255));

            float rotationOnScreen = (flat ? -bearing :
                                      mapView.getMapOrientation() -bearing);
            drawAt(canvas, icon, point.x, point.y, false, rotationOnScreen);
        }
    }

    public List<IGeoPoint> getPositionList()
    {
        return positionList;
    }

    public IconListOverlay set(List<IGeoPoint> positionList,
                               List<Drawable> iconList)
    {
        this.positionList = positionList;
        this.iconList = iconList;

        return this;
    }
}
