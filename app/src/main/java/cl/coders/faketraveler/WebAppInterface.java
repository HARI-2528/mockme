package cl.coders.faketraveler;

import static cl.coders.faketraveler.MainActivity.SourceChange.CHANGE_FROM_MAP;

import android.util.Log;
import android.webkit.JavascriptInterface;

import androidx.annotation.NonNull;


public class WebAppInterface {

    @NonNull
    private final MainActivity mainActivity;

    public WebAppInterface(@NonNull MainActivity mA) {
        mainActivity = mA;
    }

    @JavascriptInterface
    public void setPosition(final String str) {
        mainActivity.runOnUiThread(() -> {
            String lat = str.substring(str.indexOf('(') + 1, str.indexOf(','));
            String lng = str.substring(str.indexOf(',') + 2, str.indexOf(')'));

            try {
                mainActivity.setLatLng(Double.parseDouble(lat), Double.parseDouble(lng), CHANGE_FROM_MAP);
            } catch (Throwable t) {
                Log.e(WebAppInterface.class.toString(), "Could not set new position from map!", t);
            }
        });
    }

    @JavascriptInterface
    public void setZoom(final String str) {
        mainActivity.runOnUiThread(() -> {
            try {
                mainActivity.setZoom(Double.parseDouble(str));
            } catch (Throwable t) {
                Log.e(WebAppInterface.class.toString(), "Could not save zoom!", t);
            }
        });
    }

    @JavascriptInterface
    public void setPointA(final String lat, final String lon) {
        mainActivity.runOnUiThread(() -> {
            try {
                mainActivity.setPointA(Double.parseDouble(lat), Double.parseDouble(lon));
            } catch (Throwable t) {
                Log.e(WebAppInterface.class.toString(), "Could not set point A!", t);
            }
        });
    }

    @JavascriptInterface
    public void setPointB(final String lat, final String lon) {
        mainActivity.runOnUiThread(() -> {
            try {
                mainActivity.setPointB(Double.parseDouble(lat), Double.parseDouble(lon));
            } catch (Throwable t) {
                Log.e(WebAppInterface.class.toString(), "Could not set point B!", t);
            }
        });
    }

    @JavascriptInterface
    public void previewRoute() {
        mainActivity.runOnUiThread(() -> {
            mainActivity.previewRoute();
        });
    }

    @JavascriptInterface
    public void startJourney() {
        mainActivity.runOnUiThread(() -> {
            mainActivity.startJourney();
        });
    }

}