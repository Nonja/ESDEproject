package fi.oulu.tol.esde08.ohapclient08;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.opimobi.ohap.Device;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class DeviceActivity extends BaseActivity {
    public static final String EXTRA_DEVICE_ID = "fi.oulu.tol.esde08.DEVICE_ID";
    private int backgroundColor = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        ConnectionManager connectionManager = ConnectionManager.getInstance();

        Bundle extras = getIntent().getExtras();


        try {
            String url = extras.getString(EXTRA_CENTRAL_UNIT_URL);
            final Device device = (Device) connectionManager.getCentralUnit(new URL(url)).getItemById(extras.getLong(EXTRA_DEVICE_ID));

            if (device.isInternal()){
                return;
            }

            TextView deviceName = (TextView)findViewById(R.id.textView_name);
            deviceName.setText(device.getName());

            TextView deviceDescription = (TextView)findViewById(R.id.textView_description);
            deviceDescription.setText(device.getDescription());

            TextView devicePath = (TextView)findViewById(R.id.textView_path);
            devicePath.setText(String.valueOf(device.getParent().getName()));


            //Binary device
            if (device.getValueType() == Device.ValueType.BINARY) {
                final Switch deviceSwitch = (Switch) findViewById(R.id.switch_binary);
                deviceSwitch.setVisibility(View.VISIBLE);
                deviceSwitch.setChecked(device.getBinaryValue());

                //Disable changing of value for Type.SENSOR
                if (device.getType() == Device.Type.SENSOR) {
                    setTitle("SENSOR");
                    deviceSwitch.setKeyListener(null);

                }

                setTitle("ACTUATOR");

                deviceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            try {
                                device.changeBinaryValue(true);
                                Toast.makeText(DeviceActivity.this, device.getName() + " ON", Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                device.changeBinaryValue(false);
                                Toast.makeText(DeviceActivity.this, device.getName() + " OFF", Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

            }

            //Decimal Device widgets
            if (device.getValueType() == Device.ValueType.DECIMAL) {

                final TextView decimalValue = (TextView) findViewById(R.id.textView_decimal);
                Toast.makeText(DeviceActivity.this, device.getName() + " in " + device.getUnitAbbreviation(), Toast.LENGTH_SHORT).show();
                decimalValue.setVisibility(View.VISIBLE);
                decimalValue.setText(String.format("%.0f", device.getDecimalValue()) + " " + device.getUnit());
                final SeekBar deviceSeekBar = (SeekBar) findViewById(R.id.seekBar_decimal);

                deviceSeekBar.setMax((int) (device.getMaxValue() + Math.abs(device.getMinValue())));
                deviceSeekBar.setProgress((int) (device.getDecimalValue() + device.getMaxValue()));
                deviceSeekBar.setVisibility(View.VISIBLE);

                //Disable changing of value for Type.SENSOR
                if (device.getType() == Device.Type.SENSOR) {

                    setTitle("SENSOR");

                    deviceSeekBar.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            return true;
                        }
                    });
                }

                //Decimal Actuator
                if (device.getType() == Device.Type.ACTUATOR) {
                    setTitle("ACTUATOR");
                    deviceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            try {
                                device.changeDecimalValue(progress + device.getMinValue());
                                decimalValue.setText(String.format("%.0f", progress + device.getMinValue()) + device.getUnit());
                                String valueChangedToast = String.format(device.getName() + " changed to %.0f", device.getDecimalValue());
                                Toast.makeText(DeviceActivity.this, valueChangedToast, Toast.LENGTH_SHORT).show();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    protected void onDeviceShaken(){

        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout_device);

        switch (backgroundColor) {
            case 0:
                linearLayout.setBackgroundColor(Color.CYAN);
                backgroundColor = 1;
                break;
            case 1:
                linearLayout.setBackgroundColor(Color.MAGENTA);
                backgroundColor = 2;
                break;
            case 2:
                linearLayout.setBackgroundColor(Color.YELLOW);
                backgroundColor = 3;
                break;
            case 3:
                linearLayout.setBackgroundColor(Color.GRAY);
                backgroundColor = 4;
                break;
            case 4:
                linearLayout.setBackgroundColor(Color.WHITE);
                backgroundColor = 0;
                break;
        }

    }

}
