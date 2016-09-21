package fi.oulu.tol.esde08.ohapclient08;

import com.opimobi.ohap.CentralUnit;

import java.net.URL;
import java.util.HashMap;

/**
 * Created by Jonna on 13.5.2015.
 */
public class ConnectionManager {
    private static ConnectionManager ourInstance = new ConnectionManager();

    public static ConnectionManager getInstance() {
        return ourInstance;
    }

    private ConnectionManager() {
        items = new HashMap<>();
    }

    private HashMap<URL, CentralUnitConnection> items;

    public CentralUnit getCentralUnit(URL url){
        if (items.containsKey(url)) {
            return items.get(url);
        } else {
            CentralUnitConnection centralUnitConnection = new CentralUnitConnection(url);
            items.put(url, centralUnitConnection);
            return centralUnitConnection;
        }
    }
}
