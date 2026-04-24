package cl.coders.faketraveler;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MockedLocationService extends Service {

    @NonNull
    private static final String TAG = MockedLocationService.class.getSimpleName();

    @NonNull
    private static final String CHANNEL_ID = "faketraveler_channel";

    @NonNull
    protected final MutableLiveData<MockState> mockState = new MutableLiveData<>();
    @NonNull
    protected final MutableLiveData<Location> mockedLocation = new MutableLiveData<>();

    @NonNull
    private final List<MockedLocationProvider> providers = new ArrayList<>();

    @NonNull
    private final Timer timer = new Timer();
    @NonNull
    private final Set<TimerTask> tasks = Collections.synchronizedSet(new HashSet<>());

    private static final int NOTIFICATION_ID = 1;
    @NonNull
    private final Handler notificationHandler = new Handler(Looper.getMainLooper());
    private String destinationName = "destination";
    private int currentProgress = 0;

    @NonNull
    @Override
    public IBinder onBind(Intent intent) {
        indicateBinding();
        return new MockedBinder(this);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Mock is finished");
        for (TimerTask t : tasks)
            t.cancel();
        tasks.clear();
        for (MockedLocationProvider prov : providers)
            prov.shutdown();
        providers.clear();
        mockState.postValue(MockState.NOT_MOCKED);
        return super.onUnbind(intent);
    }

    private void indicateBinding() {
        mockState.postValue(MockState.SERVICE_BOUND);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "FakeTraveler Location",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows mock location status");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void startForegroundWithNotification() {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("FakeTraveler")
                .setContentText("Starting mock location...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        startForeground(NOTIFICATION_ID, builder.build());
    }

    private void updateNotification(int progress) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                Intent notificationIntent = new Intent(this, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("FakeTraveler")
                        .setContentText("Moving to " + destinationName + " — " + progress + "% complete")
                        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                        .setContentIntent(pendingIntent)
                        .setOngoing(true)
                        .setProgress(100, progress, false);

                manager.notify(NOTIFICATION_ID, builder.build());
            }
        }
    }

    protected void startRoute(ArrayList<double[]> points, double totalMeters, float speedKmh, boolean loop, String destName) {
        this.destinationName = destName != null ? destName : "destination";

        try {
            providers.clear();
            providers.add(new MockedLocationProvider(LocationManager.GPS_PROVIDER, this));
            providers.add(new MockedLocationProvider(LocationManager.NETWORK_PROVIDER, this));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                providers.add(new MockedLocationProvider(LocationManager.FUSED_PROVIDER, this));
            }

            startForegroundWithNotification();

            RouteTask routeTask = new RouteTask(points, totalMeters, speedKmh, loop);
            timer.schedule(routeTask, 0L, 1000L);
            tasks.add(routeTask);
            mockState.postValue(MockState.MOCKED);
        } catch (SecurityException e) {
            Log.e(TAG, "Could not construct mock location providers!", e);
            mockState.postValue(MockState.MOCK_ERROR);
        }
    }

    class RouteTask extends TimerTask {
        private final ArrayList<double[]> routePoints;
        private final double totalMeters;
        private final float speedKmh;
        private final boolean loop;
        private double coveredMeters = 0;
        private double prevLat = 0;
        private double prevLon = 0;
        private boolean isFirstUpdate = true;

        public RouteTask(ArrayList<double[]> points, double totalMeters, float speedKmh, boolean loop) {
            this.routePoints = points;
            this.totalMeters = totalMeters;
            this.speedKmh = speedKmh;
            this.loop = loop;
            if (points != null && points.size() > 0) {
                this.prevLat = points.get(0)[0];
                this.prevLon = points.get(0)[1];
            }
        }

        @Override
        public void run() {
            coveredMeters += speedKmh / 3.6;

            if (coveredMeters >= totalMeters) {
                if (loop) {
                    coveredMeters = 0;
                } else {
                    this.cancel();
                    for (TimerTask t : tasks) {
                        t.cancel();
                    }
                    tasks.clear();
                    for (MockedLocationProvider prov : providers) {
                        prov.shutdown();
                    }
                    providers.clear();
                    mockState.postValue(MockState.NOT_MOCKED);
                    return;
                }
            }

            if (routePoints == null || routePoints.isEmpty()) {
                return;
            }

            double distanceSoFar = 0;
            double currentLat = 0;
            double currentLon = 0;
            int segmentIndex = 0;

            for (int i = 1; i < routePoints.size(); i++) {
                double[] p1 = routePoints.get(i - 1);
                double[] p2 = routePoints.get(i);
                double segmentDist = GeoUtils.haversineMeters(p1[0], p1[1], p2[0], p2[1]);

                if (distanceSoFar + segmentDist >= coveredMeters) {
                    double remaining = coveredMeters - distanceSoFar;
                    double ratio = remaining / segmentDist;
                    currentLat = p1[0] + (p2[0] - p1[0]) * ratio;
                    currentLon = p1[1] + (p2[1] - p1[1]) * ratio;
                    segmentIndex = i - 1;
                    break;
                }
                distanceSoFar += segmentDist;
            }

            double bearing = 0;
            if (!isFirstUpdate && prevLat != 0 && prevLon != 0) {
                bearing = GeoUtils.bearingDegrees(prevLat, prevLon, currentLat, currentLon);
            }
            prevLat = currentLat;
            prevLon = currentLon;
            isFirstUpdate = false;

            Location value = new Location(LocationManager.GPS_PROVIDER);
            value.setLatitude(currentLat);
            value.setLongitude(currentLon);
            value.setSpeed(speedKmh / 3.6f);
            value.setBearing((float) bearing);
            value.setAccuracy(3f);
            value.setTime(System.currentTimeMillis());
            value.setElapsedRealtimeNanos(android.os.SystemClock.elapsedRealtimeNanos());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                value.setBearingAccuracyDegrees(5f);
                value.setVerticalAccuracyMeters(5f);
                value.setSpeedAccuracyMetersPerSecond(0.5f);
            }

            mockedLocation.postValue(value);
            for (MockedLocationProvider prov : providers) {
                prov.pushLocation(currentLat, currentLon);
            }

            int progress = (int) ((coveredMeters / totalMeters) * 100);
            if (progress > 100) progress = 100;
            currentProgress = progress;
            updateNotification(progress);
        }
    }

    protected void startMockedService(double longitude, double latitude, double longitudeDistance, double latitudeDistance, long mockMilli, int maxTime, float mockSpeed) {
        try {
            providers.clear();
            providers.add(new MockedLocationProvider(LocationManager.GPS_PROVIDER, this));
            providers.add(new MockedLocationProvider(LocationManager.NETWORK_PROVIDER, this));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                providers.add(new MockedLocationProvider(LocationManager.FUSED_PROVIDER, this));
            }

            MockedTask mockedTask = new MockedTask(longitude, latitude, longitudeDistance, latitudeDistance, maxTime, mockSpeed);
            timer.schedule(mockedTask, 0L, mockMilli);
            tasks.add(mockedTask);
            mockState.postValue(MockState.MOCKED);
        } catch (SecurityException e) {
            Log.e(TAG, "Could not construct mock location providers!", e);
            mockState.postValue(MockState.MOCK_ERROR);
        }
    }

    class MockedTask extends TimerTask {
        private final float speed;
        private double longitude;
        private double latitude;
        private final double longitudeMockedDistance;
        private final double latitudeMockedDistance;
        private final int maxLocationTimes;
        private int currentTimes = 0;

        public MockedTask(double longitude, double latitude, double longitudeMockedDistance, double latitudeMockedDistance, int maxTimes, float mockSpeed) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.longitudeMockedDistance = longitudeMockedDistance;
            this.latitudeMockedDistance = latitudeMockedDistance;
            this.maxLocationTimes = maxTimes;
            this.speed = mockSpeed;
        }

        @Override
        public void run() {
            Location value = new Location(LocationManager.GPS_PROVIDER);
            value.setLongitude(longitude);
            value.setLatitude(latitude);
            if (speed > 0) {
                value.setSpeed(speed);
                value.setAccuracy(0.1f);
                value.setTime(System.currentTimeMillis());

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    value.setSpeedAccuracyMetersPerSecond(0.01f);
                }
            }

            mockedLocation.postValue(value);
            for (MockedLocationProvider prov : providers)
                prov.pushLocation(latitude, longitude);
            ++currentTimes;
            if (maxLocationTimes != 0 && maxLocationTimes == currentTimes) {
                this.cancel();
                stopSelf();
                mockState.postValue(MockState.NOT_MOCKED);
            }
            latitude += latitudeMockedDistance;
            longitude += longitudeMockedDistance;
        }
    }

    public static class MockedBinder extends Binder {
        @NonNull
        private final MockedLocationService service;
        @NonNull
        public final LiveData<MockState> mockState;
        @NonNull
        public final LiveData<Location> mockedLocation;

        public MockedBinder(@NonNull MockedLocationService service) {
            this.service = service;
            this.mockState = service.mockState;
            this.mockedLocation = service.mockedLocation;
        }

        public void continueMock() {
            service.indicateBinding();
        }

        public void startMock(double longitude, double latitude, double longitudeDistance, double latitudeDistance, long mockMilli, int maxTimes, float mockSpeed) {
            service.startMockedService(longitude, latitude, longitudeDistance, latitudeDistance, mockMilli, maxTimes, mockSpeed);
        }

        public void startRoute(ArrayList<double[]> points, double totalMeters, float speedKmh, boolean loop, String destName) {
            service.startRoute(points, totalMeters, speedKmh, loop, destName);
        }
    }

}
