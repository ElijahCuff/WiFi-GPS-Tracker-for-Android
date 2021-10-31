package empire_of_e.wifi.tracker;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import empire_of_e.wifi.tracker.R;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends Activity {

    private ListView wifiList;
    private WifiManager wifiManager;
		static Activity me;
    private final int MY_PERMISSION_REQUEST= 1;
    WifiReceiver receiverWifi;
		Button buttonScan ;
		Boolean running = false;
    static  ProgressBar pb;
		LocationManager locationManager;
    LocationListener locationListener;
		Button plot;
		TextView gps;

		WebView wv;

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
				wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
				locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

				gps = findViewById(R.id.gps);
				plot = findViewById(R.id.plot);
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
			  checkPerms(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION});



		}


		public void showMap(final String locationString) {
				new Thread(new Runnable(){

								@Override
								public void run() {
										try {
												final String response = sendPost("https://gps-plotter.herokuapp.com", "showPath=true&showArea=false&zoom=17&locations=" + locationString);
												runOnUiThread(new Runnable(){
																@Override
																public void run() {
																		AlertDialog.Builder alertDial = new AlertDialog.Builder(MainActivity.this);
																		alertDial.setCancelable(false);
																		wv = new WebView(me);
																		wv.setWebChromeClient(new WebChromeClient(){});
																		wv.setWebViewClient(new WebViewClient(){});
																		WebSettings ws = wv.getSettings();
																		ws.setJavaScriptEnabled(true);
																		alertDial.setView(wv);
																		alertDial.setPositiveButton("DONE", new DialogInterface.OnClickListener() {
																						public void onClick(DialogInterface dialog, int which) {
																						}
																				});
																		alertDial.setIcon(R.drawable.ic_launcher);
																		
																		plot.setEnabled(true);
																		plot.setText("PLOT ON MAP");
																		
																		alertDial.show();
																		ViewGroup.LayoutParams lp =	 wv.getLayoutParams();
																		lp.height = 900;
																		wv.setLayoutParams(lp);
																		wv.loadData(response, "text/html", "UTF-8");
																}
														});
										}
										catch (Exception e) {
												toast(e.getMessage());
										}
										finally {
										}
								}
						}).start();
		}

		public void plotMAP(View view) {
				File myFile = new File(Environment.getExternalStorageDirectory(), "wardrive" + time + ".csv");
			plot.setText("PLOTTING");
			plot.setEnabled(false);
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
						showMap(URLEncoder.encode(results));
				}
				catch (Exception e) {
						toast(e.getMessage());
				}
		}

		public void initWifi() {
				wifiList.setOnItemClickListener(new ListView.OnItemClickListener(){
								@Override
								public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
										String[] data = p1.getItemAtPosition(p3).toString().split("\n");
										String SSID = data[0];
										Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com/search?q=" + URLEncoder.encode(SSID)));
										startActivity(browserIntent);				}
						});

				if (!running) {
						running = true;
						new Thread(new Runnable(){
										@Override
										public void run() {
												while (running) {
														scanWifi();
														locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
														SystemClock.sleep(6000);
												}
										}
								}).start();
				}
		}


		//https://www.google.com/maps/place/41%C2%B010'12.0%22S+147%C2%B044'38.7%22E/@-41.1702777,147.7441316,18z/data=!3m1!1e3

		private	void scanWifi() {
				if (wifiManager.startScan() != true) {	
						showProgress(true);
						wifiManager.disconnect();
						wifiManager.setWifiEnabled(false);
						wifiManager.setWifiEnabled(true);
						while (wifiManager.startScan() != true) {}
						showProgress(false);
				}		
		}


		// Function to send post request and collect errors
		String currentUrl;
		String error;
  	HttpsURLConnection conn;
		private String sendPost(String url, String data) throws Exception {

				URL obj = new URL(url);
				conn = (HttpsURLConnection) obj.openConnection();
				// Act like a real browser
				conn.setUseCaches(false);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
				conn.setRequestProperty("Content-Length", Integer.toString(data.length()));
				conn.setConnectTimeout(4000);
				conn.setReadTimeout(10000);
				conn.setFollowRedirects(true);
				conn.setDoOutput(true);
				conn.setDoInput(true);

				// Send post request
				DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
				wr.writeBytes(data);
				wr.flush();
				wr.close();

				// Collect response data for error analysis
				BufferedReader in = 
						new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String inputLine;
				StringBuilder response = new StringBuilder();
				while ((inputLine = in.readLine()) != null) {
						response.append(inputLine + "\n");
				}
				int responseCode = conn.getResponseCode();
				String responseMessage = conn.getResponseMessage();
				in.close();
				return response.toString();
		}


		public void initLoc() {
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
												newAccu = "GPS LOCKED " + accuracy;
										}
										if (accu < 4) {
												newAccu = "GPS LOCKED  " + accuracy;
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

		public static void showProgress(final boolean shouldShow) {
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



		private void checkPerms(String[] perms) {    
				requestPermissions(perms, MY_PERMISSION_REQUEST);
		}

		@Override
		protected void onDestroy() {

				super.onDestroy();
		}


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

										if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
												if (locationManager.isLocationEnabled()) {
														initLoc();
												}
										if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
												if (!wifiManager.isWifiEnabled()) {
														try {
													    	wifiManager.setWifiEnabled(true);
																initWifi();
														}
														catch (Error e) {
																toast(e.getMessage());
														}
												}
												else {
														initWifi();
												}
										}
								}
								else {
										toast("permission not granted");
								}
				}
		}
}
