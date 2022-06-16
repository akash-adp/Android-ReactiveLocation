package pl.charmas.android.reactivelocation.observables.location;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import pl.charmas.android.reactivelocation.observables.BaseLocationObservable;
import pl.charmas.android.reactivelocation.observables.ObservableContext;
import rx.Observable;
import rx.Observer;

public class LastKnownLocationObservable extends BaseLocationObservable<Location> {

    public static Observable<Location> createObservable(ObservableContext ctx) {
        return Observable.create(new LastKnownLocationObservable(ctx));
    }

    private LastKnownLocationObservable(ObservableContext ctx) {
        super(ctx);
    }

    @Override
    protected void onGoogleApiClientReady(GoogleApiClient apiClient, Observer<? super Location> observer) {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            observer.onError(new Exception("Permission:ACCESS_FINE_LOCATION "));
            return;
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(apiClient);
        if (location != null) {
            observer.onNext(location);
        }
        observer.onCompleted();
    }
}
