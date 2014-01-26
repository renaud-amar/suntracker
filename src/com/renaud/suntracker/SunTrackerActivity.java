package com.renaud.suntracker;

import android.app.Activity;
import android.app.DialogFragment;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

public class SunTrackerActivity extends Activity {
    
    private static final String TAG = "SunTrackerActivity";
    private static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_FASTEST; //SensorManager.SENSOR_DELAY_NORMAL;
	private SensorManager m_sensorManager;
	private LocationManager m_locationManager;
	private SurfaceView m_cameraPreview = null;
	private SurfaceHolder m_previewHolder = null;
	private Camera m_camera = null;
	private boolean m_inPreview = false;
	private boolean m_cameraConfigured = false;

	SunTrackerImageView m_sunTrackerImageView;
	
	public static volatile float m_azimuth = (float) 0;
	public static volatile float m_inclination = (float) 0;
	public static volatile float kFilteringFactor = (float)0.05;
	public static float m_upOrDown = (float)0;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{		
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.main);
		
		m_cameraPreview = (SurfaceView)findViewById(R.id.cameraPreview);
	    m_previewHolder = m_cameraPreview.getHolder();
	    m_previewHolder.addCallback(surfaceCallback);
	    m_previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
	    m_sunTrackerImageView = (SunTrackerImageView)findViewById(R.id.sunTrackerImageView);

	    // set up sensor manager for Orientation and Accelerometer:
		m_sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		m_sensorManager.registerListener(mySensorEventListener, m_sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SENSOR_DELAY);
		m_sensorManager.registerListener(mySensorEventListener, m_sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SENSOR_DELAY);
		
		// set up location manager for GPS:
		m_locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		m_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, gpsListener); 
	}
	
	// listener for sensor events:
	private SensorEventListener mySensorEventListener = new SensorEventListener() {
		   
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			
			// handle orientation sensor events:
			if(event.sensor.getType() == Sensor.TYPE_ORIENTATION)
		    {		        
				/* Add 90 (modulo 360) to the azimuth value when we're locked in landscape mode:
				   azimuth = (event.values[0] + 90) % 360;
				   Ideally we'd want to re-orient the display depending on the screen's orientation
				   in landscape mode we need to use the ROLL as the inclination angle, but in portrait mode
				   I'm guessing we need to use the PITCH (event.values[1]) instead of the ROLL. Not totally sure about that though!
				   inclination = 90 - event.values[2];
				*/
				
				// TODO: figure out how to handle the azimuth passage from 360 to 0 in regards to the rolling filter
				// apply a rolling filter to the raw azimuth and inclination values from the sensors:
				m_azimuth =(float) (( ((event.values[0] + 90) % 360)* kFilteringFactor) + (m_azimuth * (1.0 - kFilteringFactor)));
		        m_inclination = (float) (((90 - event.values[2]) * kFilteringFactor) + (m_inclination * (1.0 - kFilteringFactor)));
				
		        // report the new phone orientation:
		        m_sunTrackerImageView.UpdatePhoneOrientation(m_azimuth, m_inclination, m_upOrDown);
		    }
			else
			// handle accelerometer sensor events:
		    if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		    {
		    	// read the accelerometer sensor to determine whether the phone is pointing up or down:
		    	m_upOrDown =	(float) ((event.values[2] * kFilteringFactor) + (m_upOrDown * (1.0 - kFilteringFactor)));
		    }
		}
	};
	
	// listener for the GPS events:
	LocationListener gpsListener = new LocationListener()
	{ 
		Location curLocation;
	    boolean locationChanged = false;
	    
	    public void onLocationChanged(Location location)
	    {
	    	if(curLocation == null)
	    		locationChanged = true;
	    	else
	    	{
	    		// don't take into account changes in location that are below the 3rd decimal:
	    		if(roundDecimals(curLocation.getLatitude(), 3) == roundDecimals(location.getLatitude(), 3) &&
	    		   roundDecimals(curLocation.getLongitude(), 3) == roundDecimals(location.getLongitude(), 3) )
	    			locationChanged = false;
	    		else
	    			locationChanged = true;
	    	}
	         
	    	if (locationChanged)
	    	{
	    		curLocation = location;
	    		// show a toast for the updated location:
	    		Toast.makeText(getApplicationContext(), String.format("Location changed: Long = %.4f, Lat = %.4f", curLocation.getLongitude(), curLocation.getLatitude()), Toast.LENGTH_LONG).show();
	    		// update the location in the image view:
	    		m_sunTrackerImageView.UpdateLocation(curLocation.getLongitude(), curLocation.getLatitude());
	    	}
	    }
	    
	    public void onProviderDisabled(String provider){}
	    public void onProviderEnabled(String provider){}
	    public void onStatusChanged(String provider, int status, Bundle extras){}
	};

		
	@Override
	protected void onResume()
	{
		super.onResume();
		// register the listener for orientation and accelerometer sensors:
		m_sensorManager.registerListener(mySensorEventListener, m_sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SENSOR_DELAY);
		m_sensorManager.registerListener(mySensorEventListener, m_sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SENSOR_DELAY);
		// register the GPS listener:
		m_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, gpsListener); 
		
		m_camera = Camera.open();
	    startPreview();
	}

	@Override
	protected void onPause()
	{
		// unregister sensor listener:
		m_sensorManager.unregisterListener(mySensorEventListener);
		// unregister location listener:
		m_locationManager.removeUpdates(gpsListener);
		
		// stop the camera preview:
		if (m_inPreview)
			m_camera.stopPreview();
				    
		m_camera.release();
		m_camera = null;
		m_inPreview = false;
		
		super.onPause();
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		m_sensorManager.unregisterListener(mySensorEventListener);
		m_locationManager.removeUpdates(gpsListener);
	}
	
	private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters)
	{
		Camera.Size result=null;

		for (Camera.Size size : parameters.getSupportedPreviewSizes())
		{
			if (size.width<=width && size.height<=height)
			{
				if (result==null)
				{
					result=size;
				}
				else
				{
					int resultArea=result.width*result.height;
					int newArea=size.width*size.height;

					if (newArea>resultArea)
					{
						result=size;
					}
				}
			}
		}

		return(result);
	}

	private void initPreview(int width, int height)
	{
		if (m_camera!=null && m_previewHolder.getSurface()!=null)
		{
			try
			{
				m_camera.setPreviewDisplay(m_previewHolder);
			}
			catch (Throwable t)
			{
				Log.e("SunTracker-surfaceCallback", "Exception in setPreviewDisplay()", t);
				Toast.makeText(SunTrackerActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
			}

			if (!m_cameraConfigured)
			{
				Camera.Parameters parameters = m_camera.getParameters();
				Camera.Size size=getBestPreviewSize(width, height, parameters);

				if (size!=null)
				{
					/*// do the screen rotation here based on getorientation(): 
					int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
				    int degrees = 0;
				    switch (rotation)
				    {
				    	case Surface.ROTATION_0: degrees = 0; break;
				        case Surface.ROTATION_90: degrees = 90; break;
				        case Surface.ROTATION_180: degrees = 180; break;
				        case Surface.ROTATION_270: degrees = 270; break;
				    }
				    
				    android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
				    android.hardware.Camera.getCameraInfo(0, info);
				    m_camera.setDisplayOrientation((info.orientation - degrees + 360) % 360); */
					
					parameters.setPreviewSize(size.width, size.height);
					m_camera.setParameters(parameters);
					m_cameraConfigured=true;
				}
			}
		}
	}

	private void startPreview()
	{
		if (m_cameraConfigured && m_camera!=null)
		{
			m_camera.startPreview();
			m_inPreview=true;
		}
	}

	SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback()
	{
		public void surfaceCreated(SurfaceHolder holder)
		{
			// no-op -- wait until surfaceChanged()
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
		{
			initPreview(width, height);
			startPreview();
		}

		public void surfaceDestroyed(SurfaceHolder holder)
		{
			// no-op
		}
	};
	
	// create the options menu:
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }
    
    // user selected an item from the options menu:
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.menu_location:
        {
        	DialogFragment dialog = new LocationSettingsDlg();
            dialog.show(getFragmentManager(), "LocationSettingsDlg");
            return true;	
        }
        case R.id.menu_display:
        {
        	DialogFragment dialog = new DisplaySettingsDlg(m_sunTrackerImageView.getNumHoursBefore(), m_sunTrackerImageView.getNumHoursAfter());
            dialog.show(getFragmentManager(), "DisplaySettingsDlg");
            return true;
        }
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    public void onUpdateLocationSettings(double longitude, double latitude) 
    {
    	// update the location in the image view:
		m_sunTrackerImageView.UpdateLocation(longitude, latitude);
    }
    
    public void onUpdateDisplaySettings(int numHoursBefore, int numHoursAfter) 
    {
    	// update the time range display in the image view:
		m_sunTrackerImageView.UpdateTimeRangeDisplay(numHoursBefore, numHoursAfter);
    }
    
    public double roundDecimals(double d, int nDecimals)
    {
    	int dec = (int) Math.pow(10, nDecimals);
    	return (double) Math.round(d*dec)/dec;
    }
}