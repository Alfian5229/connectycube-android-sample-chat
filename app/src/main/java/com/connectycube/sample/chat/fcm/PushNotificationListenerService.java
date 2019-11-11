package com.connectycube.sample.chat.fcm;

import com.connectycube.pushnotifications.services.fcm.FcmPushListenerService;
import com.connectycube.sample.chat.R;
import com.connectycube.sample.chat.ui.activity.LoginActivity;
import com.connectycube.sample.chat.utils.NotificationUtils;
import com.connectycube.sample.chat.utils.ResourceUtils;

import java.util.Map;

import static com.connectycube.sample.chat.utils.Consts.NOTIFICATION_ID;

public class PushNotificationListenerService extends FcmPushListenerService {

    @Override
    public void sendPushMessage(Map data, String message, String messageId, String dialogId, String userId) {
        super.sendPushMessage(data, message, messageId, dialogId, userId);
        NotificationUtils.showNotification(this, LoginActivity.class,
                ResourceUtils.getString(R.string.notification_title), message, dialogId,
                R.drawable.ic_info, NOTIFICATION_ID);
    }
}