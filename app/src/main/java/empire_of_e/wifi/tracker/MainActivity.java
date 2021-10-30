package empire_of_e.wifi.tracker;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;

public class MainActivity extends Activity {

    private ListView wifiList;
    private WifiManager wifiManager;
		static Activity me;
    private final int MY_PERMISSION_REQUEST= 1;
    WifiReceiver receiverWifi;
		Button buttonScan ;
		Boolean running = false;
    ProgressBar pb;
		LocationManager locationManager;
    LocationListener locationListener;
		Button plot;
		TextView gps;
		Intent noti;

		public static double lat;
		public static double lon;
		public static String loc;
		public static float accu;
		public static int time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
				time = (int) System.currentTimeMillis();
				me = this;
				pb = findViewById(R.id.pb);
				showProgress(false);
        wifiList = findViewById(R.id.wifiList);
				noti = new Intent(this, Notify.class);
				gps = findViewById(R.id.gps);
				plot = findViewById(R.id.plot);
				checkPerms(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION});
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
				createNotificationChannel();
    }

		public void plotMAP(View view) {
				File myFile = new File(Environment.getExternalStorageDirectory(), "wardrive" + time + ".csv");
				try {
						BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(myFile)));
						String line;
						String results = "";

						while ((line = in.readLine()) != null) {
								String[] parts = line.split(",");
								String SSID = parts[0];
								String MAC = parts[1];
								Integer STRE = Integer.parseInt(parts[2].trim());
								String ENC = parts[3];
								String FREQ = parts[4];
								String LOC = parts[5];
								String lati = LOC.split(":")[0];
								String longi = LOC.split(":")[1];
								Integer ACCU = Integer.parseInt(parts[6].trim());

								String result=lati + "," + longi + "," + ACCU + "," + URLEncoder.encode(SSID) + "<br>MAC : " + MAC + "<br>ENCRYPTION : " + ENC + "<br>LAT : " + lati + "<br>LON : " + lon + "<br>STRENGTH : " + STRE + "|";
								results += result;
						}		

						results = results.substring(0, results.length() - 1);

						Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://gps-plotter.herokuapp.com/?showLine=true&zoom=17&locations=" + results));
						startActivity(browserIntent);
				}
				catch (Exception e) {
						toast(e.getMessage());
				}




		}


		private void createNotificationChannel() {
				// Create the NotificationChannel, but only on API 26+ because
				// the NotificationChannel class is new and not in the support library
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						CharSequence name = "Video Playback";
						String description = "Background Video Playback";
						int importance = NotificationManager.IMPORTANCE_LOW;
						NotificationChannel channel = new NotificationChannel("zoopy", name, importance);
						channel.setDescription(description);
						// Register the channel with the system; you can't change the importance
						// or other notification behaviors after this
						NotificationManager notificationManager = getSystemService(NotificationManager.class);
						notificationManager.createNotificationChannel(channel);
				}
		}


		public void initWifi() {
				wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            toast("PLEASE ENABLE Wi-Fi");
						//   wifiManager.setWifiEnabled(true);
        }

				wifiList.setOnItemClickListener(new ListView.OnItemClickListener(){

								@Override
								public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
										String[] data = p1.getItemAtPosition(p3).toString().split("\n");
										String SSID = data[0];
										Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com/search?q=" + URLEncoder.encode(SSID)));
										startActivity(browserIntent);
										//Toast.makeText(MainActivity.this,data.getText().toString(),Toast.LENGTH_LONG).show();
								}
						});

				if (!running) {
						running = true;

						new Thread(new Runnable(){
										@Override
										public void run() {
												wifiManager.disconnect();

												while (running) {
														if (wifiManager.startScan() == true) {
																SystemClock.sleep(6000);
																locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
														}
														else {
																showProgress(true);
																wifiManager.disconnect();
																wifiManager.setWifiEnabled(false);
																wifiManager.setWifiEnabled(true);
																while (wifiManager.startScan() != true) {
																}
																showProgress(false);
																SystemClock.sleep(6000);
														}
												} 
										}
								}).start();
				}
		}
		//https://www.google.com/maps/place/41%C2%B010'12.0%22S+147%C2%B044'38.7%22E/@-41.1702777,147.7441316,18z/data=!3m1!1e3

		public void initLoc() {
				locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
				locationListener = new LocationListener() {
						@Override
						public void onLocationChanged(Location location) {
								lat = location.getLatitude();
								lon = location.getLongitude();
								loc = location.toString();
								accu = location.getAccuracy();
								String accuStr = String.valueOf(accu);
								String  accuracy = accuStr.substring(0, accuStr.indexOf(".")) + " m";
								String newAccu;
								if (accu <= 15) {
										newAccu = "GPS LOCKED  -  " + accuracy;
										if (accu >= 4.0 & accu <= 10.0) {
												newAccu = "GPS LOCKED - INDOORS " + accuracy;
										}
										if (accu < 4) {
												newAccu = "GPS LOCKED  - HIGHLY ACCURATE " + accuracy;
										}

								}
								else {
										newAccu = "GPS WARMING UP";
								}
								updateGPS(newAccu);
						}
						@Override
						public void onStatusChanged(String provider, int status, Bundle extras) {
								// TODO: Implement this method
						}
				};
				if (!locationManager.isLocationEnabled()) {
						toast("PLEASE ENABLE GPS");
				}
				else {
						locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
				}
		}
		public void showProgress(final boolean shouldShow) {
				me.runOnUiThread(new Runnable(){
								@Override
								public void run() {
										int vis = 0;
										if (shouldShow) {
												vis = pb.VISIBLE;
										}
										else {
												vis = pb.GONE;
										}
										pb.setVisibility(vis);
								}
						});

		}

		public void updateGPS(final String val) {
				me.runOnUiThread(new Runnable(){

								@Override
								public void run() {
										gps.setText(val);
								}
						});
		}


    @Override
    protected void onResume() {
				stopService(noti);
        super.onResume();
    }





    @Override
    protected void onPostResume() {
        super.onPostResume();
        receiverWifi = new WifiReceiver(wifiManager, wifiList);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(receiverWifi, intentFilter);
        wifiManager.startScan();
    }

		public static void toast(final String mess) {
				me.runOnUiThread(new Runnable(){

								@Override
								public void run() {
										Toast.makeText(me, mess, Toast.LENGTH_LONG).show();
								}
						});
		}

    @Override
    protected void onPause() {
				Notify.title = "Wi-Fi Tracker";
        Notify.descr = "Scanning Active";
				startService(noti);
     //   unregisterReceiver(receiverWifi);
			 super.onPause();
    }
	



		private void checkPerms(String[] perms) {     
				ActivityCompat.requestPermissions(MainActivity.this, perms, MY_PERMISSION_REQUEST);
		}

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
										//     Toast.makeText(MainActivity.this, "permission granted", Toast.LENGTH_SHORT).show();

										if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
												initLoc();
										}
										if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
												initWifi();
										}
                }
								else {
										Toast.makeText(MainActivity.this, "permission not granted", 200).show();
                    return;
                }
                break;
        }
    }
}
