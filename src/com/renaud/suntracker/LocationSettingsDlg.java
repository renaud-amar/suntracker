package com.renaud.suntracker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.RadioButton;

public class LocationSettingsDlg extends DialogFragment {
	
	private EditText m_editTextLongitude;
	private EditText m_editTextLatitude;
	private RadioButton m_radioLocationGPS;
	private RadioButton m_radioLocationManual;
	
	public LocationSettingsDlg() {
	}
	
	/*@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.location_settings, container);
        //mEditText = (EditText) view.findViewById(R.id.txt_your_name);
        getDialog().setTitle("Location Settings");

        return view;
    }*/
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction:
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // Inflate and set the custom layout for the dialog:
        View view = inflater.inflate(R.layout.location_settings, null);
        // now that we have the view, get the edit text fields:
        m_editTextLongitude = (EditText) view.findViewById(R.id.editTextLongitude);
        m_editTextLatitude = (EditText) view.findViewById(R.id.editTextLatitude);
        // set the listener for the radio buttons:
        m_radioLocationGPS = (RadioButton) view.findViewById(R.id.radioLocationGPS);
        m_radioLocationManual = (RadioButton) view.findViewById(R.id.radioLocationManual);
        OnClickListener radioLocationListener = new OnClickListener() {
            				public void onClick(View v) {
            					onRadioLocationClick(v);
            				}	
        				};
        m_radioLocationGPS.setOnClickListener(radioLocationListener);
        m_radioLocationManual.setOnClickListener(radioLocationListener);
        
        builder.setView(view);
        //builder.setMessage("Location settings");
        //builder.setTitle("Location settings");
        // Add the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   // upon clicking OK, read the longitude and latitude values and pass it to the activity:
                	   double longitude = Double.parseDouble(m_editTextLongitude.getText().toString());
                	   double latitude  = Double.parseDouble(m_editTextLatitude.getText().toString());
                       SunTrackerActivity callingActivity = (SunTrackerActivity) getActivity();
                       callingActivity.onUpdateLocationSettings(longitude, latitude);
                       //dialog.dismiss();
                   }
               });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // User cancelled the dialog
                   }
               });
        
        // Create the AlertDialog object and return it
        return builder.create();
	}
	
	// enable/disable the manual location edit text fields
	// depending on whether the user selected GPS or manual:
	public void onRadioLocationClick(View view) {
	    // Is the button now checked?
	    boolean checked = ((RadioButton) view).isChecked();
	    
	    // Check which radio button was clicked
	    switch(view.getId()) {
	        case R.id.radioLocationGPS:
	            if (checked) {
	            	m_editTextLongitude.setEnabled(false);
	            	m_editTextLatitude.setEnabled(false);
	            }
	            break;
	        case R.id.radioLocationManual:
	            if (checked) {
	            	m_editTextLongitude.setEnabled(true);
	            	m_editTextLatitude.setEnabled(true);
	            }
	            break;
	    }
	}

}
