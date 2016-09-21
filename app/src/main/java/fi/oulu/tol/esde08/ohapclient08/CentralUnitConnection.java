package fi.oulu.tol.esde08.ohapclient08;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.henrikhedberg.hbdp.client.HbdpConnection;
import com.opimobi.ohap.CentralUnit;
import com.opimobi.ohap.Container;
import com.opimobi.ohap.Device;
import com.opimobi.ohap.Item;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;


/**
 * Created by Jonna on 13.5.2015.
 */
public class CentralUnitConnection extends CentralUnit {
    private InputStream inputStream;
    private OutputStream outputStream;

    HbdpConnection hbdpConnection;

    public CentralUnitConnection(URL url) {

        super(url);
    }

    @Override
    protected void changeBinaryValue(Device device, boolean value) throws IOException {

        long i = device.getId();
        OutgoingMessage listeningStartMessage = new OutgoingMessage();
        listeningStartMessage.integer8(0x0a) //message-type-binary-changed
                .integer32((int) i)
                .binary8(value)
                .writeTo(outputStream);
    }

    @Override
    protected void changeDecimalValue(Device device, double value) throws IOException {

        long i = device.getId();
        OutgoingMessage listeningStartMessage = new OutgoingMessage();
        listeningStartMessage.integer8(0x0a) //message-type-decimal-changed
                .integer32((int) i)
                .decimal64(value)
                .writeTo(outputStream);
    }

    @Override
    protected void listeningStateChanged(Container container, boolean listening) throws IOException {

        if(listening){
            nListeners++;
            if (nListeners == 1) {
                startNetworking();
            }
            sendListeningStart(container);
        }

        if(!listening){
            nListeners--;
            if (nListeners == 0){
                stopNetworking();
            }
            sendListeningStop(container);
        }
    }

    private int nListeners;

    private void startNetworking() throws IOException {

        hbdpConnection = new HbdpConnection(this.getURL());

        inputStream = hbdpConnection.getInputStream();
        outputStream = hbdpConnection.getOutputStream();

        final OutgoingMessage outgoingMessage = new OutgoingMessage();
        outgoingMessage.integer8(0x00)      // message-type-login
                .integer8(0x01)             // protocol-version
                .text("Nonja")              // login-name
                .text("U2FltH86")           // login-password
                .writeTo(outputStream);

        Thread t = new Thread() {
            class IncomingMessageHandler implements Runnable {
                private IncomingMessage incomingMessage;
                public IncomingMessageHandler(IncomingMessage incomingMessage) {
                    this.incomingMessage = incomingMessage;
                }

                @Override
                public void run() {
                    int messageType = incomingMessage.integer8();
                    Log.d("messagetype", String.valueOf(messageType));
                    switch (messageType) {
                        case 0x01: //message-type-logout
                            String logoutText = incomingMessage.text();
                            Log.d(TAG, logoutText);
                            break;
                        case 0x02: //message-type-ping
                            long pingIdentifier = incomingMessage.integer32();
                            OutgoingMessage logoutMessage = new OutgoingMessage();
                            try {
                                logoutMessage.integer8(0x03) //message-type-pong
                                    .integer32((int) pingIdentifier)
                                    .writeTo(outputStream);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case 0x04: //message-type-decimal-sensor
                            long decimalSensorIdentifier = incomingMessage.integer32();
                            double decimalSensorValue = incomingMessage.decimal64();
                            long decimalSensorDataParentIdentifier = incomingMessage.integer32();

                            Container DSparent = (Container) getCentralUnit().getItemById(decimalSensorDataParentIdentifier);
                            Device decimalSensor = new Device(DSparent, decimalSensorIdentifier, Device.Type.SENSOR, Device.ValueType.DECIMAL);
                            decimalSensor.setDecimalValue(decimalSensorValue);
                            setItemData(decimalSensor);

                            double decimalSensorMin = incomingMessage.decimal64();
                            double decimalSensorMax = incomingMessage.decimal64();
                            String decimalSensorUnit = incomingMessage.text();
                            String decimalSensorAbbreviaton = incomingMessage.text();

                            decimalSensor.setMinMaxValues(decimalSensorMin, decimalSensorMax);
                            decimalSensor.setUnit(decimalSensorUnit, decimalSensorAbbreviaton);
                            break;
                        case 0x05: //message-type-decimal-actuator
                            long decimalActuatorIdentifier = incomingMessage.integer32();
                            double decimalActuatorValue = incomingMessage.decimal64();
                            long decimalActuatorDataParentIdentifier = incomingMessage.integer32();

                            Container DAparent = (Container) getCentralUnit().getItemById(decimalActuatorDataParentIdentifier);
                            Device decimalActuator = new Device(DAparent, decimalActuatorIdentifier, Device.Type.ACTUATOR, Device.ValueType.DECIMAL);
                            decimalActuator.setDecimalValue(decimalActuatorValue);
                            setItemData(decimalActuator);

                            double decimalActuatorMin = incomingMessage.decimal64();
                            double decimalActuatorMax = incomingMessage.decimal64();
                            String decimalActuatorUnit = incomingMessage.text();
                            String decimalActuatorAbbreviaton = incomingMessage.text();

                            decimalActuator.setMinMaxValues(decimalActuatorMin, decimalActuatorMax);
                            decimalActuator.setUnit(decimalActuatorUnit, decimalActuatorAbbreviaton);
                            break;
                        case 0x06: //message-type-binary-sensor
                            long binarySensorIdentifier = incomingMessage.integer32();
                            boolean binarySensorValue = incomingMessage.binary8();
                            long binarySensorDataParentIdentifier = incomingMessage.integer32();

                            Container BSparent = (Container) getCentralUnit().getItemById(binarySensorDataParentIdentifier);
                            Device binarySensor = new Device(BSparent, binarySensorIdentifier, Device.Type.SENSOR, Device.ValueType.BINARY);
                            binarySensor.setBinaryValue(binarySensorValue);
                            setItemData(binarySensor);
                            break;
                        case 0x07: //message-type-binary-actuator
                            long binaryActuatorIdentifier = incomingMessage.integer32();
                            boolean binaryActuatorValue = incomingMessage.binary8();
                            long binaryActuatorDataParentIdentifier = incomingMessage.integer32();

                            Container BAparent = (Container) getCentralUnit().getItemById(binaryActuatorDataParentIdentifier);
                            Device binaryActuator = new Device(BAparent, binaryActuatorIdentifier, Device.Type.ACTUATOR, Device.ValueType.BINARY);
                            binaryActuator.setBinaryValue(binaryActuatorValue);
                            setItemData(binaryActuator);
                            break;
                        case 0x08: //message-type-container
                            long itemIdentifier = incomingMessage.integer32();
                            long itemDataParentIdentifier = incomingMessage.integer32();

                            CentralUnit centralUnit = getCentralUnit();

                            //message-root-container
                            if (itemIdentifier == 0) {
                                Item item = centralUnit.getItemById(0);
                                setItemData(item);
                            }
                            if (itemIdentifier != 0){
                                Container container = new Container(centralUnit, itemIdentifier);
                                setItemData(container);
                            }
                            break;
                        case 0x09: //message-type-decimal-changed
                            long decimalChangedIdentifier = incomingMessage.integer32();
                            double decimalValue = incomingMessage.decimal64();

                            Device decimalChanged = (Device) getItemById(decimalChangedIdentifier);
                            decimalChanged.setDecimalValue(decimalValue);
                            break;
                        case 0x0a: //message-type-binary-changed
                            long binaryChangedIdentifier = incomingMessage.integer32();
                            boolean binaryValue = incomingMessage.binary8();

                            Device binaryChanged = (Device) getItemById(binaryChangedIdentifier);
                            binaryChanged.setBinaryValue(binaryValue);
                            break;
                        case 0x0b: //message-type-item-removed
                            long itemRemovedIdentifier = incomingMessage.integer32();
                            Item item = getItemById(itemRemovedIdentifier);
                            item.destroy();
                            break;
                        default:
                            Log.d(TAG, "Unrecognised message type");
                            break;
                    }
                }

                public void setItemData(Item item){
                    //Set item data
                    String itemDataName = incomingMessage.text();
                    String itemDataDescription = incomingMessage.text();
                    boolean itemDataInternal = incomingMessage.binary8();
                    double itemDataX = incomingMessage.decimal64();
                    double itemDataY = incomingMessage.decimal64();
                    double itemDataZ = incomingMessage.decimal64();

                    item.setName(itemDataName);
                    item.setDescription(itemDataDescription);
                    item.setInternal(itemDataInternal);
                    item.setLocation((int)itemDataX, (int)itemDataY, (int)itemDataZ);
                }
            }

            public final String TAG = Thread.currentThread().getName();

            public void run() {
                try {
                    Handler handler = new Handler(Looper.getMainLooper());
                    while (true) {
                        IncomingMessage incomingMessage = new IncomingMessage();
                        incomingMessage.readFrom(inputStream);
                        handler.post(new IncomingMessageHandler(incomingMessage));
                    }
                } catch (EOFException e) {
                    Log.d(TAG, "Networking stopped.");
                } catch (IOException e) {
                    Log.e(TAG, "Exception in startNetworking(): " + e.toString());
                }
            }
        };

        t.start();
    }

    private void stopNetworking() throws IOException {

        OutgoingMessage logoutMessage = new OutgoingMessage();
        logoutMessage.integer8(0x01) //message-type-logout
            .text("");
        outputStream.close();
    }

    private void sendListeningStart(Container container) throws IOException {

        long i = container.getId();
        OutgoingMessage listeningStartMessage = new OutgoingMessage();
        listeningStartMessage.integer8(0x0c) //message-type-listening-start
            .integer32((int) i)
            .writeTo(outputStream);
    }

    private void sendListeningStop(Container container) throws IOException {

        long i = container.getId();
        OutgoingMessage listeningStopMessage = new OutgoingMessage();
        listeningStopMessage.integer8(0x0d) //message-type-listening-stop
            .integer32((int) i)
            .writeTo(outputStream);
    }
}
