package pl.charmas.android.reactivelocation.observables.location;

import android.Manifest;
import android.app.PendingIntent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import pl.charmas.android.reactivelocation.observables.BaseLocationObservable;
import pl.charmas.android.reactivelocation.observables.ObservableContext;
import pl.charmas.android.reactivelocation.observables.StatusException;
import rx.Observable;
import rx.Observer;

public class AddLocationIntentUpdatesObservable extends BaseLocationObservable<Status> {
    private final LocationRequest locationRequest;
    private final PendingIntent intent;

    public static Observable<Status> createObservable(ObservableContext ctx, LocationRequest locationRequest, PendingIntent intent) {
        return Observable.create(new AddLocationIntentUpdatesObservable(ctx, locationRequest, intent));
    }

    private AddLocationIntentUpdatesObservable(ObservableContext ctx, LocationRequest locationRequest, PendingIntent intent) {
        super(ctx);
        this.locationRequest = locationRequest;
        this.intent = intent;
    }

    @Override
    protected void onGoogleApiClientReady(GoogleApiClient apiClient, final Observer<? super Status> observer) {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            observer.onError(new Exception("Permission: ACCESS_FINE_LOCATION"));
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, intent)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (!status.isSuccess()) {
                            observer.onError(new StatusException(status));
                        } else {
                            observer.onNext(status);
                            observer.onCompleted();
                        }
                    }
                });

    }
}
