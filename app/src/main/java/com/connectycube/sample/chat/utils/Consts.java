package com.connectycube.sample.chat.utils;

import com.connectycube.sample.chat.R;

public class Consts {
    public final static int MIN_OPPONENTS_COUNT = 2;
    public final static int MAX_OPPONENTS_COUNT = 10;

    public final static String SAMPLE_USER_CONFIG = "user_config.json";
    public final static int PREFERRED_IMAGE_SIZE_PREVIEW = ResourceUtils.getDimen(R.dimen.chat_attachment_preview_size);
    public final static int PREFERRED_IMAGE_SIZE_FULL = ResourceUtils.dpToPx(320);

    public static final int NOTIFICATION_ID = 1;

    public final static String EXTRA_FCM_MESSAGE = "message";
    public final static String EXTRA_FCM_DIALOG_ID = "dialog_id";
    public final static String ACTION_NEW_FCM_EVENT = "new-push-event";
}
