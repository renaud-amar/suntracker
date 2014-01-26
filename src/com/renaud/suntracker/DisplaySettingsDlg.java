package com.renaud.suntracker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class DisplaySettingsDlg extends DialogFragment {
	
	private EditText m_editTextNumHoursBefore;
	private EditText m_editTextNumHoursAfter;
	int m_numHoursBefore;
	int m_numHoursAfter;
	
	public DisplaySettingsDlg(int numHoursBefore, int numHoursAfter) {
		m_numHoursBefore = numHoursBefore;
		m_numHoursAfter = numHoursAfter;
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
        View view = inflater.inflate(R.layout.display_settings, null);
        // now that we have the view, get the edit text fields:
        m_editTextNumHoursBefore = (EditText) view.findViewById(R.id.editTextNumHoursBefore);
        m_editTextNumHoursAfter = (EditText) view.findViewById(R.id.editTextNumHoursAfter);
        
        // set the default values:
        m_editTextNumHoursBefore.setText(String.valueOf(m_numHoursBefore));
        m_editTextNumHoursAfter.setText(String.valueOf(m_numHoursAfter));
        
        builder.setView(view);
        //builder.setMessage("Display settings");
        //builder.setTitle("Display settings");
        // Add the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   // upon clicking OK, read the before and after hours values and pass it to the activity:
                	   m_numHoursBefore = Integer.parseInt(m_editTextNumHoursBefore.getText().toString());
                	   m_numHoursAfter = Integer.parseInt(m_editTextNumHoursAfter.getText().toString());
                       SunTrackerActivity callingActivity = (SunTrackerActivity) getActivity();
                       callingActivity.onUpdateDisplaySettings(m_numHoursBefore, m_numHoursAfter);
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

}
