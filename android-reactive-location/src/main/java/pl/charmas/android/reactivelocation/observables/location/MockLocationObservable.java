package pl.charmas.android.reactivelocation.observables.location;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;

import pl.charmas.android.reactivelocation.observables.BaseLocationObservable;
import pl.charmas.android.reactivelocation.observables.ObservableContext;
import pl.charmas.android.reactivelocation.observables.StatusException;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;

public class MockLocationObservable extends BaseLocationObservable<Status> {
    private Observable<Location> locationObservable;
    private Subscription mockLocationSubscription;

    public static Observable<Status> createObservable(ObservableContext context, Observable<Location> locationObservable) {
        return Observable.create(new MockLocationObservable(context, locationObservable));
    }

    protected MockLocationObservable(ObservableContext ctx, Observable<Location> locationObservable) {
        super(ctx);
        this.locationObservable = locationObservable;
    }

    @Override
    protected void onGoogleApiClientReady(final GoogleApiClient apiClient, final Observer<? super Status> observer) {
        // this throws SecurityException if permissions are bad or mock locations are not enabled,
        // which is passed to observer's onError by BaseObservable
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            observer.onError(new Exception("Permission: ACCESS_FINE_LOCATION"));
            return;
        }
        LocationServices.FusedLocationApi.setMockMode(apiClient, true)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            startLocationMocking(apiClient, observer);
                        } else {
                            observer.onError(new StatusException(status));
                        }
                    }
                });
    }

    private void startLocationMocking(final GoogleApiClient apiClient, final Observer<? super Status> observer) {
        mockLocationSubscription = locationObservable.subscribe(new Action1<Location>() {
            @Override
            public void call(Location location) {
                if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    observer.onError(new Exception("Permission: ACCESS_FINE_LOCATION"));
                    return;
                }
                LocationServices.FusedLocationApi.setMockLocation(apiClient, location)
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                                if (status.isSuccess()) {
                                    observer.onNext(status);
                                } else {
                                    observer.onError(new StatusException(status));
                                }
                            }
                        });
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                observer.onError(throwable);
            }
        }, new Action0() {
            @Override
            public void call() {
                observer.onCompleted();
            }
        });
    }

    @Override
    protected void onUnsubscribed(GoogleApiClient locationClient) {
        if (locationClient.isConnected()) {
            try {
                LocationServices.FusedLocationApi.setMockMode(locationClient, false);
            } catch (SecurityException e) {
                // if this happens then we couldn't have switched mock mode on in the first place,
                // and the observer's onError will already have been called
            }
        }
        if (mockLocationSubscription != null && !mockLocationSubscription.isUnsubscribed()) {
            mockLocationSubscription.unsubscribe();
        }
    }
}
