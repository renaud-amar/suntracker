package com.renaud.suntracker;

import java.util.Date;
import java.util.TimeZone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.ImageView;

public class SunTrackerImageView extends ImageView {

	float m_azimuth = 0;
	float m_inclination = 0;
	float[] m_sunAzimuth;
	float[] m_sunInclination;
	float m_upOrDown = 0;
	
	double m_longitude = -122.32; // default longitude for san mateo
	double m_latitude = 37.56; // default latitude for san mateo
	
	int m_pixelsPerDegree = 20; // approximation of the number of pixels per degree of rotation as seen from the phone's camera
	
	int m_numHoursBefore = 6; // number of hours to display sun positions in the past
	int m_numHoursAfter = 6; // number of hours to display sun positions in the future
	int m_totalNumPositions;
	
	int m_screenWidth;
	int m_screenHeight;
	Display m_display;
	
	public SunTrackerImageView(Context context) {
        super(context);
        Init(context);
    }
	
	public SunTrackerImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Init(context);
    }

    public SunTrackerImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Init(context);
    }
    
    public void Init(Context context)
    {
    	WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    	m_display = wm.getDefaultDisplay();
    	Point size = new Point();
    	m_display.getSize(size);
    	m_screenWidth = size.x;
    	m_screenHeight = size.y;
    	calculateSunPositions();
    }
    
    protected void calculateSunPositions()
	{
    	m_totalNumPositions = m_numHoursBefore + m_numHoursAfter + 1;
		m_sunAzimuth = new float[m_totalNumPositions];
		m_sunInclination = new float[m_totalNumPositions];
		
		Date nowDate = new Date();
		long hour = 3600 * 1000; // in ms
		
		// get GMT offset in ms:
		TimeZone tz = TimeZone.getDefault();
		int offsetGMT = tz.getOffset(nowDate.getTime());
		
		SunPositionCalculator sunPos = new SunPositionCalculator();
		for (int i = 0; i < m_totalNumPositions; i++)
		{
			// calculate the current GMT date with the hour offset:
			Date date = new Date(nowDate.getTime() + (i - m_numHoursBefore)*hour - offsetGMT);
			
			sunPos.calculateSunPos(date.getYear(), date.getMonth(), date.getDay(),
								   date.getHours(), date.getMinutes(), date.getSeconds(),
								   m_longitude, m_latitude);
			
			m_sunAzimuth[i] = (float) sunPos.GetAzimuth();
			m_sunInclination[i] = 90 - (float) sunPos.GetZenith();			
		}
	}
    
    public void UpdatePhoneOrientation(float azimuth, float inclination, float upOrDown)
    {
    	m_azimuth = azimuth;
    	m_inclination = inclination;
    	m_upOrDown = upOrDown;
    	if(upOrDown > 0)
    		m_inclination = m_inclination * -1;
    	// invalidate the view after we've updated the azimuth:
    	super.invalidate();
    }
    
    public void UpdateLocation(double longitude, double latitude)
    {
    	m_longitude = longitude;
    	m_latitude = latitude;
    	// recalculate sun positions:
    	calculateSunPositions();
    	super.invalidate();
    }
    
    public void UpdateTimeRangeDisplay(int numHoursBefore, int numHoursAfter)
    {
    	m_numHoursBefore = numHoursBefore;
    	m_numHoursAfter = numHoursAfter;
    	m_totalNumPositions = m_numHoursBefore + m_numHoursAfter + 1;  
    	// recalculate sun positions:
    	calculateSunPositions();
    	super.invalidate();
    }
    
    public int getNumHoursBefore()
    {
    	return m_numHoursBefore;
    }
    
    public int getNumHoursAfter()
    {
    	return m_numHoursAfter;
    }
    
    
    public void SetSunPosition(float azimuth, float inclination)
    {
    	//m_sunAzimuth = azimuth;
    	//m_sunInclination = inclination;
    }

    @Override
    protected void onDraw(Canvas canvas) {
    	// TODO:
    	// - change colors of circles, maybe gradient going to and from current sun position.
    	//   maybe color depending on where sun is in sky
        Paint paintSun = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        Paint paintLines = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLines.setStyle(Paint.Style.STROKE);
        paintLines.setColor(Color.rgb(255, 0, 0));
        paintLines.setStrokeWidth(4);
        paintLines.setAlpha(150);
        
        Paint paintLabels = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLabels.setColor(Color.BLUE);
        paintLabels.setTextSize(30);
                
        float oldSunPosX = 0;
        float oldSunPosY = 0;
        
        int iSunRadius = 60; 
        boolean bOldSunOnScreen = false;
        
        for (int i=0; i < m_totalNumPositions; i++)
        {
        	// make sure we handle the passage of the azimuth from 0 to 360 by adding an offset: 
        	int offset = 0;
        	if (m_sunAzimuth[i] - m_azimuth < -180)
        		offset = 360;
        	else if (m_sunAzimuth[i] - m_azimuth > 180)
        		offset = -360;
        	
        	float sunPosX = m_screenWidth/2 + (m_sunAzimuth[i] - m_azimuth + offset)*m_pixelsPerDegree;
        	float sunPosY = m_screenHeight/2 - (m_sunInclination[i] - m_inclination)*m_pixelsPerDegree;
        	
        	boolean bOnScreen = true;
        	// don't draw stuff that's outside of the screen, so check if the current sun is out of bounds:
        	if (sunPosX + iSunRadius < 0 || sunPosX - iSunRadius > m_screenWidth ||
        		sunPosY + iSunRadius < 0 || sunPosY - iSunRadius > m_screenHeight)
        		bOnScreen = false;
        	
        	if (bOnScreen)
        	{
        		// gradient of colors from red to yellow back to red:
        		//paintSun.setColor(Color.rgb(255, i <=6 ? (255/6)*i : 255 - (255/6)*(i-6), 0));
        		
        		if (i == m_numHoursBefore)
        			paintSun.setColor(Color.YELLOW);
        		else
        			paintSun.setColor(Color.RED);
        			
        		paintSun.setAlpha(150);
        		canvas.drawCircle(sunPosX, sunPosY, iSunRadius, paintSun);
        	}
        	
        	// draw a line between the current sun position and the previous one
        	// if this isn't the first sun and if at least one of them is on screen:
        	if (i>0 && (bOnScreen || bOldSunOnScreen))
        	{
        		// first determine the angle of the line formed by old pos and the current pos:
        		double theta = Math.atan(Math.abs(sunPosY-oldSunPosY)/Math.abs(sunPosX-oldSunPosX));
        		float xOffset = (float)(iSunRadius*Math.cos(theta));
        		float yOffset = (float)(iSunRadius*Math.sin(theta));
        		
        		// draw the line from the edge of each circle: 
        		canvas.drawLine(oldSunPosX + (oldSunPosX < sunPosX ? 1 : -1)*xOffset,
        				oldSunPosY + (oldSunPosY < sunPosY ? 1 : -1)*yOffset,
        				sunPosX + (oldSunPosX < sunPosX ? -1 : 1)*xOffset,
        				sunPosY + (oldSunPosY < sunPosY ? -1 : 1)*yOffset,
        				paintLines);
        	}
        	
        	if (bOnScreen)
        	{
        		String strHour = String.format("%s%d", i - m_numHoursBefore > 0 ? "+" : "", i - m_numHoursBefore);
        		Rect bounds = new Rect();
        		// get the text's bounding rectangle so that we can center the label:
        		paintLabels.getTextBounds(strHour, 0, strHour.length(), bounds);
        		canvas.drawText(strHour, sunPosX-(int)(bounds.width()/2), sunPosY+(int)(bounds.height()/2), paintLabels);
        	}
        	
        	// update the old sun pos:
        	oldSunPosX = sunPosX;
        	oldSunPosY = sunPosY;
        	bOldSunOnScreen = bOnScreen;
        }
        
        Paint paintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBackground.setColor(Color.BLACK);
        paintBackground.setAlpha(100);
        canvas.drawRect(20, 20, 200, 255, paintBackground);
        
        Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(15);
        
        canvas.drawText(String.format("Azimuth: %.2f", m_azimuth), 30, 40, paintText);
        canvas.drawText(String.format("Inclination: %.2f", m_inclination), 30, 80, paintText);
        canvas.drawText(String.format("Up or Down: %.2f", m_upOrDown), 30, 120, paintText);
        
        int degrees = 0;
	    switch (m_display.getRotation())
	    {
	    	case Surface.ROTATION_0: degrees = 0; break;
	        case Surface.ROTATION_90: degrees = 90; break;
	        case Surface.ROTATION_180: degrees = 180; break;
	        case Surface.ROTATION_270: degrees = 270; break;
	    }
        
        canvas.drawText(String.format("Orientation: %d", degrees), 30, 160, paintText);
        
        canvas.drawText(String.format("Longitude: %.4f", m_longitude), 30, 200, paintText);
        canvas.drawText(String.format("Latitude: %.4f", m_latitude), 30, 240, paintText);
        
        super.onDraw(canvas);
    }

}

