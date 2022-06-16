package pl.charmas.android.reactivelocation.observables.activity;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;

import pl.charmas.android.reactivelocation.observables.ObservableContext;
import rx.Observable;
import rx.Observer;

public class ActivityUpdatesObservable extends BaseActivityObservable<ActivityRecognitionResult> {
    private static final String ACTION_ACTIVITY_DETECTED = "pl.charmas.android.reactivelocation.ACTION_ACTIVITY_UPDATE_DETECTED";

    private final Context context;
    private final int detectionIntervalMilliseconds;
    private ActivityUpdatesBroadcastReceiver receiver;

    public static Observable<ActivityRecognitionResult> createObservable(ObservableContext ctx, int detectionIntervalMiliseconds) {
        return Observable.create(new ActivityUpdatesObservable(ctx, detectionIntervalMiliseconds));
    }

    private ActivityUpdatesObservable(ObservableContext context, int detectionIntervalMilliseconds) {
        super(context);
        this.context = context.getContext();
        this.detectionIntervalMilliseconds = detectionIntervalMilliseconds;
    }

    @Override
    protected void onGoogleApiClientReady(GoogleApiClient apiClient, Observer<? super ActivityRecognitionResult> observer) {
        receiver = new ActivityUpdatesBroadcastReceiver(observer);
        context.registerReceiver(receiver, new IntentFilter(ACTION_ACTIVITY_DETECTED));
        PendingIntent receiverIntent = getReceiverPendingIntent();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(apiClient, detectionIntervalMilliseconds, receiverIntent);
    }

    private PendingIntent getReceiverPendingIntent() {
        return PendingIntent.getBroadcast(context, 0, new Intent(ACTION_ACTIVITY_DETECTED), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    protected void onUnsubscribed(GoogleApiClient apiClient) {
        if (apiClient.isConnected()) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(apiClient, getReceiverPendingIntent());
        }
        if (receiver != null) {
            context.unregisterReceiver(receiver);
            receiver = null;
        }
    }

    private static class ActivityUpdatesBroadcastReceiver extends BroadcastReceiver {
        private final Observer<? super ActivityRecognitionResult> observer;

        public ActivityUpdatesBroadcastReceiver(Observer<? super ActivityRecognitionResult> observer) {
            this.observer = observer;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityRecognitionResult.hasResult(intent)) {
                ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
                observer.onNext(result);
            }
        }
    }
}
