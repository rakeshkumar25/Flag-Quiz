package com.projects.safariapps.flagquiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final String CHOICES = "pref_numberOfChoices";
    public static final String REGIONS = "pref_regionsToInclude";

    private boolean phoneDevice = true;
    private boolean preferencesChanged = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set Default Values in the apps Shared Preferences

        PreferenceManager.setDefaultValues(this,R.xml.preferences,false);

        // register listeners for Shared Preferences
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(preferencesChangeListener);

        // Determine Screen Size
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        //If device is a tablet, then set PhoneDevice to false
        if(screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE || screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE)
            phoneDevice = false; // Not a phone sized device

        // If running on a phone device, allow only portrait orientation
        if(phoneDevice){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Get the device current orientation and Inflate Menu

        int orientation = getResources().getConfiguration().orientation;

        if(orientation == Configuration.ORIENTATION_PORTRAIT){
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        }
        else {
            return false;
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Display the settings activity when running on a phone

        Intent preferencesIntent = new Intent(MainActivity.this,SettingsActivity.class);
        startActivity(preferencesIntent);
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(preferencesChanged){

            // Now that Default preferences have been set, Initialize MainActivityFragment and start the Quiz

            MainActivityFragment quizFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.quizFragment);
            quizFragment.updateGuessRows(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.updateRegions(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.resetQuiz();
            preferencesChanged = false;
        }

    }

    private OnSharedPreferenceChangeListener preferencesChangeListener = new OnSharedPreferenceChangeListener() {
        //called when user changed the app's preferences

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            preferencesChanged = true;
            MainActivityFragment quizFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.quizFragment);
            if(key.equals(CHOICES)){ // # of choices to display changed

                quizFragment.updateGuessRows(sharedPreferences);
                quizFragment.resetQuiz();
            }
            else if (key.equals(REGIONS)){ // Regions to be included after any changes

                Set<String> regions = sharedPreferences.getStringSet(REGIONS,null);
                if (regions != null && regions.size()>0){

                    quizFragment.updateRegions(sharedPreferences);
                    quizFragment.resetQuiz();

                }else {
                    // Must select one region otherwise set North America as default

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    regions.add(getString(R.string.default_region));
                    editor.putString(REGIONS,null);
                    editor.apply();

                    Toast.makeText(MainActivity.this,R.string.default_region_message, Toast.LENGTH_SHORT).show();

                }

            }

            Toast.makeText(MainActivity.this,R.string.restarting_quiz,Toast.LENGTH_SHORT).show();
        }
    };
}
