package fi.oulu.tol.esde08.ohapclient08;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.opimobi.ohap.Container;
import com.opimobi.ohap.Device;
import com.opimobi.ohap.Item;

import java.net.URL;


public class ContainerActivity extends BaseActivity {
    public static final String EXTRA_CONTAINER_ID = "fi.oulu.tol.esde08.CONTAINER_ID";
    private int backgroundColor = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        ConnectionManager connectionManager = ConnectionManager.getInstance();
        try {
            final Container container;

            Bundle extras = getIntent().getExtras();

            //Get CentralUnit URL from preferences if missing
            if (extras == null){
                container = connectionManager.getCentralUnit(new URL(url));
                System.out.println("Connecting to " + url);
            } else {
                url = extras.getString(EXTRA_CENTRAL_UNIT_URL);
                container = (Container) connectionManager.getCentralUnit(new URL(url)).getItemById(extras.getLong(EXTRA_CONTAINER_ID));
            }

            if (!container.isListening()){
                container.startListening();
            }

            if (container.isInternal()){
                return;
            }

            ListView listView = (ListView) findViewById(R.id.listView_list);
            listView.setAdapter(new ContainerListAdapter(container));

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Item item = (Item) parent.getItemAtPosition(position);
                    Toast.makeText(ContainerActivity.this, item.getName() + " selected", Toast.LENGTH_SHORT).show();

                    if (item instanceof Container)
                    {
                        Intent intent = new Intent(ContainerActivity.this, ContainerActivity.class);
                        intent.putExtra(EXTRA_CONTAINER_ID, id);
                        intent.putExtra(EXTRA_CENTRAL_UNIT_URL, url);
                        startActivity(intent);
                    }
                    if (item instanceof Device)
                    {
                        Intent intent = new Intent(ContainerActivity.this, DeviceActivity.class);
                        intent.putExtra(DeviceActivity.EXTRA_DEVICE_ID, id);
                        intent.putExtra(DeviceActivity.EXTRA_CENTRAL_UNIT_URL, url);
                        startActivity(intent);
                    }
                }
            });

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    protected void onDeviceShaken(){

        ListView listView = (ListView) findViewById(R.id.listView_list);

        switch (backgroundColor) {
            case 0:
                listView.setBackgroundColor(Color.CYAN);
                backgroundColor = 1;
                break;
            case 1:
                listView.setBackgroundColor(Color.MAGENTA);
                backgroundColor = 2;
                break;
            case 2:
                listView.setBackgroundColor(Color.YELLOW);
                backgroundColor = 3;
                break;
            case 3:
                listView.setBackgroundColor(Color.GRAY);
                backgroundColor = 4;
                break;
            case 4:
                listView.setBackgroundColor(Color.WHITE);
                backgroundColor = 0;
                break;
        }

    }

}
