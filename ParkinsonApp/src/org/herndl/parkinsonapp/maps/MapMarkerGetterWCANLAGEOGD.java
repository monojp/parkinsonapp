package org.herndl.parkinsonapp.maps;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.herndl.parkinsonapp.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapMarkerGetterWCANLAGEOGD implements CallbackString,
		CallbackMapMarkerGetterWCANLAGEOGD {

	private Context context;
	private static final String dataURL = "http://data.wien.gv.at/daten/geo?service=WFS&request=GetFeature&version=1.1.0&typeName=ogdwien:WCANLAGEOGD&srsName=EPSG:4326&outputFormat=json";
	private static final int cacheSeconds = 60 * 60 * 24 * 7; // 1 week

	private CallbackMapMarkerGetterWCANLAGEOGD callbackMapMarkerGetterWCANLAGEOGD;

	MapMarkerGetterWCANLAGEOGD(
			Context context,
			CallbackMapMarkerGetterWCANLAGEOGD callbackMapMarkerGetterWCANLAGEOGD) {
		this.callbackMapMarkerGetterWCANLAGEOGD = callbackMapMarkerGetterWCANLAGEOGD;
		this.context = context;

		Log.v("MapMarkerGetterWCANLAGEOGD", "construct");

		// check if the user has a network connection
		if (!hasNetworkAccess()) {
			Toast.makeText(context, R.string.network_down, Toast.LENGTH_SHORT)
					.show();

			// try to get (also old) data from cache
			String data = new DownloadToStringTask(context, this,
					Integer.MAX_VALUE).getFromCache(dataURL);
			callbackString(data);
		}
		// start download task, which calls "callbackString" if ready
		else {
			try {
				new DownloadToStringTask(context, this, cacheSeconds)
						.execute(new URL(dataURL));
			} catch (MalformedURLException e) {
				Log.e("MapMarkerGetterWCANLAGEOGD", e.toString());
				return;
			}
		}
	}

	// check if the user has an active network connection
	private boolean hasNetworkAccess() {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}

	// callback from download task, parses json, builds the list
	// of marker options and calls the methode which calls the origin callback
	// methode
	@Override
	public void callbackString(String data) {
		Log.v("MapMarkerGetterWCANLAGEOGD", "callbackString");
		List<MarkerOptions> markerOptions = new ArrayList<MarkerOptions>();

		// got no data from downloader, return the empty list
		if (data == null) {
			callbackMapMarkerGetterWCANLAGEOGD(markerOptions);
			return;
		}

		try {
			JSONArray features = (JSONArray) new JSONObject(data)
					.get("features");
			for (int i = 0; i < features.length(); i++) {
				JSONObject row = features.getJSONObject(i);
				String type = row.getString("type");
				// String id = row.getString("id");
				String geometry_type = row.getJSONObject("geometry").getString(
						"type");
				JSONArray coordinates = row.getJSONObject("geometry")
						.getJSONArray("coordinates");
				double lng = coordinates.getDouble(0);
				double lat = coordinates.getDouble(1);

				JSONObject properties = row.getJSONObject("properties");
				String kategorie = properties.getString("KATEGORIE");
				String strasse = properties.getString("STRASSE");
				String oeffnungszeit = properties.getString("OEFFNUNGSZEIT");

				// set icon according to category
				BitmapDescriptor icon = null;
				if (kategorie.indexOf("Euro-Key") != -1)
					icon = BitmapDescriptorFactory
							.fromResource(R.drawable.wceurokeyogd);
				else if (kategorie.indexOf("Behindertenkabine") != -1)
					icon = BitmapDescriptorFactory
							.fromResource(R.drawable.oeffwcbehindertenkabine);
				else if (kategorie.indexOf("Pissoir") != -1)
					icon = BitmapDescriptorFactory
							.fromResource(R.drawable.oeffwcpissoir);
				else
					icon = BitmapDescriptorFactory
							.fromResource(R.drawable.oeffwcwartepersonal);

				// unknown marker object
				if (!type.equals("Feature") || !geometry_type.equals("Point")) {
					Log.w("CallbackMapMarkerGetterWCANLAGEOGD",
							"invalid feature type " + type + " geometry_type "
									+ geometry_type + " found");
				}

				// create marker
				markerOptions
						.add(new MarkerOptions().position(new LatLng(lat, lng))
								.icon(icon).draggable(false)
								.snippet(oeffnungszeit).title(strasse));
			}
			// execute callback
			callbackMapMarkerGetterWCANLAGEOGD(markerOptions);
		} catch (JSONException e) {
			Log.e("MapMarkerGetterWCANLAGEOGD:callbackString", e.toString());
		}
	}

	@Override
	public void callbackMapMarkerGetterWCANLAGEOGD(
			List<MarkerOptions> markerOptions) {
		callbackMapMarkerGetterWCANLAGEOGD
				.callbackMapMarkerGetterWCANLAGEOGD(markerOptions);
	}

}
