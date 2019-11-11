package com.connectycube.sample.chat.utils.callback;

import android.os.Bundle;

import com.connectycube.core.EntityCallback;
import com.connectycube.core.exception.ResponseException;

public class EntityCallbackImpl<T> implements EntityCallback<T> {

    public EntityCallbackImpl() {
    }

    @Override
    public void onSuccess(T result, Bundle bundle) {

    }

    @Override
    public void onError(ResponseException e) {

    }
}
