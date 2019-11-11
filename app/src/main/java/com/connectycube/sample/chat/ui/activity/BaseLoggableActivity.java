package com.connectycube.sample.chat.ui.activity;

import android.os.Bundle;

import com.connectycube.sample.chat.utils.callback.VerboseChatConnectionListener;
import com.connectycube.sample.chat.utils.chat.ChatHelper;

import org.jivesoftware.smack.ConnectionListener;

public abstract class BaseLoggableActivity extends BaseActivity {
    protected ConnectionListener chatConnectionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentResId());
        chatConnectionListener = initChatConnectionListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ChatHelper.getInstance().addConnectionListener(chatConnectionListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ChatHelper.getInstance().removeConnectionListener(chatConnectionListener);
    }

    protected abstract int getContentResId();

    protected ConnectionListener initChatConnectionListener() {
        return new VerboseChatConnectionListener(getSnackbarAnchorView());
    }
}
