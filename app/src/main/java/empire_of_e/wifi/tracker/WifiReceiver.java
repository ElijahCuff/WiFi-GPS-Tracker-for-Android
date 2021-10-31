package empire_of_e.wifi.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.v4.util.ArrayMap;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;;

class WifiReceiver extends BroadcastReceiver {

    WifiManager wifiManager;

    ListView wifiDeviceList;
		int time;
		Map<String,Integer> checkList = new ArrayMap<String,Integer>();

    public WifiReceiver(WifiManager wifiManager, ListView wifiDeviceList) {
        this.wifiManager = wifiManager;
        this.wifiDeviceList = wifiDeviceList;
				this.time = MainActivity.time;
    }

		private void logWifi(String sSID, String mAC, Integer sTREN, String eNC, String fREQ, String location, Integer accu) {
				File myFile = new File(Environment.getExternalStorageDirectory(), "wardrive" + time + ".csv");
				if (!myFile.exists()) {
						try {
								myFile.createNewFile();
								myFile.setReadable(true);
								myFile.setWritable(true);
						}
						catch (IOException e) {
								MainActivity.toast(e.getMessage());
						}
				}
				
				try {
						BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(myFile)));
						String line;
						String results = "";
						boolean newWifi = true;
						while ((line = in.readLine()) != null) {
								String[] parts = line.split(",");
								String SSID = parts[0];
								String MAC = parts[1];
								Integer STRE = Integer.parseInt(parts[2].trim());
								String ENC = parts[3];
								String FREQ = parts[4];
								String LOC = parts[5];
								Integer ACCU = Integer.parseInt(parts[6].trim());

								if ((sSID + mAC).equals((SSID + MAC))) {
										newWifi = false;
										if (sTREN > STRE) {
								      	results += SSID + "," + MAC + "," + sTREN  + "," + ENC + "," + FREQ + "," + location + "," + accu + "\n";
										//		MainActivity.toast("UPDATED : " + SSID + MAC);
										}
										else {
												results += SSID + "," + MAC + "," + STRE  + "," + ENC + "," + FREQ + "," + LOC + "," + ACCU + "\n";	
										}
								}
								else {
										results += line + "\n";
								}
						}		
						if (newWifi) {
								results += sSID + "," + mAC + "," + sTREN + "," + eNC + "," + fREQ + "," + location + "," + accu + "\n";
								//	MainActivity.toast("ADDED : "+sSID+mAC);
						}				
						writeFile(myFile, results.getBytes());
				}
				catch (Exception e) {
				}
		}

		protected static void writeFile(File file, byte [] data) throws IOException {
        try(FileOutputStream fileOutputStream = new FileOutputStream(file.getAbsolutePath(), false)) {
            fileOutputStream.write(data);
					  fileOutputStream.flush();
				    fileOutputStream.close();
        }
    }


    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            final List<ScanResult> wifiList = wifiManager.getScanResults();

						final ArrayList<String> deviceList = new ArrayList<>();
						for (ScanResult scanResult : wifiList) {
					
								try {
										String accuracy = String.valueOf(MainActivity.accu);
										Double lat = MainActivity.lat;
										Double lon = MainActivity.lon;
										String accu = accuracy.substring(0, accuracy.indexOf("."));
										String location = lat + ":" + lon;
										String SSID = scanResult.SSID;
										if (SSID.length() <= 1) {
												SSID = "SSID NOT TRANSMITTED";
										}
										String MAC = scanResult.BSSID.toUpperCase();
										String ENC = scanResult.capabilities;
										String FREQ = String.valueOf(scanResult.frequency);
										int STREN = wifiManager.calculateSignalLevel(scanResult.level, 100);
										String encStr = "NONE";
										if (ENC.contains("]")) {
												encStr = ENC.replace("[", "").replace("]", ", ").substring(0, ENC.length() - 2);
										}

										deviceList.add(SSID + "\n" + STREN + "% \n  MAC : " + MAC +  "\n  FREQUENCY : " + ((double)Integer.parseInt(FREQ) / 1000) + " GHz\n  ENCRYPTION : " + encStr);

										if (lat != 0.0) {
												logWifi(SSID, MAC, STREN, ENC, FREQ, location, Integer.parseInt(accu));
										}

								}
								catch (Error e) {
										MainActivity.toast(e.getMessage());
								}
								catch (Exception e) {
										MainActivity.toast(e.getMessage());
								}
						}
						//  Toast.makeText(context, sb, Toast.LENGTH_SHORT).show();

						if (deviceList.size() > 0) {

								MainActivity.me.runOnUiThread(new Runnable(){

												@Override
												public void run() {

														int firstVisiblePosition = wifiDeviceList. getFirstVisiblePosition();
														View view = wifiDeviceList.getChildAt(0);
														int distFromTop = (view == null) ? 0 : view.getTop();
														deviceList.sort(new Comparator<String>(){
																		@Override
																		public int compare(String s1, String s2) {
																				return s1.compareToIgnoreCase(s2);
																		}
																});
														ArrayAdapter arrayAdapter = new ArrayAdapter(context, android.R.layout.simple_list_item_1, deviceList.toArray());
														wifiDeviceList.setAdapter(arrayAdapter);
														wifiDeviceList. setSelectionFromTop(firstVisiblePosition, distFromTop);
												}
										});

						}

        }
    }
}
