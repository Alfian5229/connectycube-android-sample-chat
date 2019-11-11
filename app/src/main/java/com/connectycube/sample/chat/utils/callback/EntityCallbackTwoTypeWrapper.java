package com.connectycube.sample.chat.utils.callback;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.connectycube.core.EntityCallback;
import com.connectycube.core.exception.ResponseException;

public class EntityCallbackTwoTypeWrapper<T, R> implements EntityCallback<T> {
    protected static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    protected EntityCallback<R> callback;

    public EntityCallbackTwoTypeWrapper(EntityCallback<R> callback) {
        this.callback = callback;
    }

    @Override
    public void onSuccess(T t, Bundle bundle) {
        // Do nothing, we want to trigger callback with another data type
    }

    @Override
    public void onError(ResponseException error) {
        onErrorInMainThread(error);
    }

    protected void onSuccessInMainThread(final R result, final Bundle bundle) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(result, bundle);
            }
        });
    }

    protected void onErrorInMainThread(final ResponseException error) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(error);
            }
        });
    }
}
