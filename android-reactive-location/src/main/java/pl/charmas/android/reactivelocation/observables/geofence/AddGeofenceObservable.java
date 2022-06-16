package pl.charmas.android.reactivelocation.observables.geofence;

import android.Manifest;
import android.app.PendingIntent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import pl.charmas.android.reactivelocation.observables.BaseLocationObservable;
import pl.charmas.android.reactivelocation.observables.ObservableContext;
import pl.charmas.android.reactivelocation.observables.StatusException;
import rx.Observable;
import rx.Observer;

public class AddGeofenceObservable extends BaseLocationObservable<Status> {
    private final GeofencingRequest request;
    private final PendingIntent geofenceTransitionPendingIntent;

    public static Observable<Status> createObservable(ObservableContext ctx, GeofencingRequest request, PendingIntent geofenceTransitionPendingIntent) {
        return Observable.create(new AddGeofenceObservable(ctx, request, geofenceTransitionPendingIntent));
    }

    private AddGeofenceObservable(ObservableContext ctx, GeofencingRequest request, PendingIntent geofenceTransitionPendingIntent) {
        super(ctx);
        this.request = request;
        this.geofenceTransitionPendingIntent = geofenceTransitionPendingIntent;
    }

    @Override
    protected void onGoogleApiClientReady(GoogleApiClient apiClient, final Observer<? super Status> observer) {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            observer.onError(new Exception("Permission: ACCESS_FINE_LOCATION"));
            return;
        }
        LocationServices.GeofencingApi.addGeofences(apiClient, request, geofenceTransitionPendingIntent)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            observer.onNext(status);
                            observer.onCompleted();
                        } else {
                            observer.onError(new StatusException(status));
                        }
                    }
                });
    }

}
