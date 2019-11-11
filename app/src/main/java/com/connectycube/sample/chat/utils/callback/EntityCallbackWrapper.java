package com.connectycube.sample.chat.utils.callback;

import android.os.Bundle;

import com.connectycube.core.EntityCallback;

public class EntityCallbackWrapper<T> extends EntityCallbackTwoTypeWrapper<T, T> {
    public EntityCallbackWrapper(EntityCallback<T> callback) {
        super(callback);
    }

    @Override
    public void onSuccess(T t, Bundle bundle) {
        onSuccessInMainThread(t, bundle);
    }
}
