package fi.oulu.tol.esde08.ohapclient08;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.opimobi.ohap.Device;

/**
 * Created by Jonna on 27.6.2015.
 */
public class BaseActivity extends ActionBarActivity implements SensorEventListener {
    public static final String EXTRA_CENTRAL_UNIT_URL = "fi.oulu.tol.esde.esde08.CENTRAL_UNIT_URL";
    protected String url;

    private SensorManager sensorManager;
    private float sensorCurrent = SensorManager.GRAVITY_EARTH;
    private float sensorLast = SensorManager.GRAVITY_EARTH;
    private float shakeSensor = 0;
    private int counter = 0;
    private long lastTime = 0;


    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

        if (url == null || url.isEmpty())
            return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String prefString = prefs.getString("editText_preference", "");

        if (!prefString.equals(url)) {
            Intent i = new Intent(this, ContainerActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        url = prefs.getString("editText_preference", "http://ohap.opimobi.com:18000/");

        System.out.println("BaseActivity.onCreate called and url is " + url);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

        Toast toast = Toast.makeText(getApplicationContext(), "Tip: Shake the device to toggle colors", Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        long currentTime = System.currentTimeMillis();

        if ((currentTime - lastTime) > 500){
            
            lastTime = currentTime;

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            sensorCurrent = (float) Math.sqrt((double) (x*x + y*y + z*z));
            float delta = sensorCurrent - sensorLast;
            shakeSensor = shakeSensor * 0.9f + delta;

            sensorLast = sensorCurrent;

            if (shakeSensor > 10) {
                counter++;
                //Toast toast = Toast.makeText(getApplicationContext(), "Device has shaken " + counter + " times.", Toast.LENGTH_LONG);
                //toast.show();
                onDeviceShaken();

            }
        }

    }

    protected void onDeviceShaken() {}

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}
