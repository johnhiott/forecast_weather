package com.oolcay.weather;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.oolcay.weather.Models.Location;
import com.oolcay.weather.Network.Request;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends FragmentActivity {

  private ForecastApplication state;
  private DatabaseHandler mDatabaseHandler;
  private ViewPager mViewPager;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Context context = getApplicationContext();
    state = ((ForecastApplication) getApplicationContext());
    setContentView(R.layout.main);

    mViewPager = new ViewPager(this);
    mViewPager.setId(R.id.viewPager);

    FrameLayout layout = (FrameLayout) findViewById(R.id.fragmentContainer);
    layout.addView(mViewPager);

    mDatabaseHandler = new DatabaseHandler(context);
    state.setAllLocations(mDatabaseHandler.getAllLocations());

    GetWeather getWeather = new GetWeather(mDatabaseHandler.getAllLocations());
    getWeather.execute();

  }

  @Override
  public void onResume(){
    super.onResume();
    state = ((ForecastApplication) getApplicationContext());
  }

  @Override
  public void onPause(){
    super.onPause();
    state.setCurrentLocation(state.getAllLocations().get(mViewPager.getCurrentItem()).getId());
  }

  public void openLocations(View v){
    Intent intent = new Intent(this, LocationActivity.class);
    startActivity(intent);
  }

  private void handleResponse(List<Location> results){
    state.setAllLocations(results);

    ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
    progressBar.setVisibility(View.GONE);

    FragmentManager fragmentManager = getSupportFragmentManager();


    mViewPager.setAdapter(new FragmentStatePagerAdapter(fragmentManager) {
      @Override
      public Fragment getItem(int i) {
        return LocationFragment.newInstance(i);

      }
      @Override
      public int getCount() {
        return state.getAllLocations().size();
      }
    });

    ConfigHelper configHelper = new ConfigHelper(getApplicationContext());

    int locationId = state.getCurrentLocation();

    if (state.getAllLocations().size() == 0){

    }else{
      if (locationId == -1){
        state.setCurrentLocation(configHelper.getHomeLocation());
      }
    }

    for (int x=0; x< state.getAllLocations().size(); x++){
      if (state.getAllLocations().get(x).getId() == state.getCurrentLocation()){
        mViewPager.setCurrentItem(x);
        break;
      }
    }
  }

  public class GetWeather extends AsyncTask<Object, Void, List<Location>> {

    private List<Location> mLocations;

    GetWeather(List<Location> list){
      mLocations = list;
    }

    @Override
    protected List<Location> doInBackground(Object... params) {
      JSONObject weatherData = null;

      for (int x=0; x< mLocations.size(); x++ ){

        try{

          double lat = mLocations.get(x).getLat();
          double lon = mLocations.get(x).getLon();

          Request request = new Request();

          request.setUrl(Constants.FORECAST_URL + Constants.FORECAST_KEY + "/" + lat + "," + lon);

          weatherData = request.getJsonResponse();

          JSONObject hourly = weatherData.getJSONObject("currently");

          String temperature = hourly.getString("temperature");

          mLocations.get(x).setLastUpdated(System.currentTimeMillis()/1000L);

        }catch (Exception e){
          Log.e("WEATHER", e.toString());
        }
      }
      return mLocations;
    }

    @Override
    protected void onPostExecute(List<Location> list){
        handleResponse(list);
    }
  }
}