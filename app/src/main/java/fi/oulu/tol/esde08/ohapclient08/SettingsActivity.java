package fi.oulu.tol.esde08.ohapclient08;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

/**
 * Created by Jonna on 24.5.2015.
 */
public class SettingsActivity extends ActionBarActivity  {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}

