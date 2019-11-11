package com.connectycube.sample.chat.manager;

import android.os.Bundle;

import com.connectycube.chat.ConnectycubeChatService;
import com.connectycube.chat.SystemMessagesManager;
import com.connectycube.chat.model.ConnectycubeChatDialog;
import com.connectycube.chat.model.ConnectycubeChatMessage;
import com.connectycube.chat.model.ConnectycubeDialogType;
import com.connectycube.sample.chat.utils.chat.ChatHelper;
import com.connectycube.sample.chat.utils.holder.DialogHolder;
import com.connectycube.sample.chat.utils.chat.ChatDialogUtils;
import com.connectycube.sample.chat.utils.callback.EntityCallbackImpl;
import com.connectycube.users.model.ConnectycubeUser;

import org.jivesoftware.smack.SmackException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class DialogsManager {

    private static final String PROPERTY_OCCUPANTS_IDS = "occupants_ids";
    private static final String PROPERTY_DIALOG_TYPE = "dialog_type";
    private static final String PROPERTY_DIALOG_NAME = "dialog_name";
    private static final String PROPERTY_NOTIFICATION_TYPE = "notification_type";
    private static final String CREATING_DIALOG = "creating_dialog";
    private static final String UPDATING_DIALOG = "updating_dialog";

    private static DialogsManager instance;

    private DialogsManager() {
    }

    public static synchronized DialogsManager getInstance() {
        if (instance == null) {
            instance = new DialogsManager();
        }
        return instance;
    }

    private Set<ManagingDialogsCallbacks> managingDialogsCallbackListener = new CopyOnWriteArraySet<>();

    private boolean isMessageCreatingDialog(ConnectycubeChatMessage systemMessage) {
        return CREATING_DIALOG.equals(systemMessage.getProperty(PROPERTY_NOTIFICATION_TYPE));
    }

    private boolean isMessageUpdatingDialog(ConnectycubeChatMessage systemMessage) {
        return UPDATING_DIALOG.equals(systemMessage.getProperty(PROPERTY_NOTIFICATION_TYPE));
    }

    private ConnectycubeChatMessage buildSystemMessageAboutCreatingGroupDialog(ConnectycubeChatDialog dialog) {
        ConnectycubeChatMessage chatMessage = buildSystemMessageProperties(dialog);
        chatMessage.setProperty(PROPERTY_NOTIFICATION_TYPE, CREATING_DIALOG);
        return chatMessage;
    }

    private ConnectycubeChatMessage buildSystemMessageAboutUpdatingGroupDialog(ConnectycubeChatDialog dialog) {
        ConnectycubeChatMessage chatMessage = buildSystemMessageProperties(dialog);
        chatMessage.setProperty(PROPERTY_NOTIFICATION_TYPE, UPDATING_DIALOG);
        return chatMessage;
    }

    private ConnectycubeChatMessage buildSystemMessageProperties(ConnectycubeChatDialog dialog) {
        ConnectycubeChatMessage chatMessage = new ConnectycubeChatMessage();
        chatMessage.setDialogId(dialog.getDialogId());
        chatMessage.setProperty(PROPERTY_OCCUPANTS_IDS, ChatDialogUtils.getOccupantsIdsStringFromList(dialog.getOccupants()));
        chatMessage.setProperty(PROPERTY_DIALOG_TYPE, String.valueOf(dialog.getType().getCode()));
        chatMessage.setProperty(PROPERTY_DIALOG_NAME, String.valueOf(dialog.getName()));
        return chatMessage;
    }

    private ConnectycubeChatDialog buildChatDialogFromSystemMessage(ConnectycubeChatMessage chatMessage) {
        ConnectycubeChatDialog chatDialog = new ConnectycubeChatDialog();
        chatDialog.setDialogId(chatMessage.getDialogId());
        chatDialog.setOccupantsIds(ChatDialogUtils.getOccupantsIdsListFromString((String) chatMessage.getProperty(PROPERTY_OCCUPANTS_IDS)));
        chatDialog.setType(ConnectycubeDialogType.parseByCode(Integer.parseInt(chatMessage.getProperty(PROPERTY_DIALOG_TYPE).toString())));
        chatDialog.setName(chatMessage.getProperty(PROPERTY_DIALOG_NAME).toString());
        chatDialog.setUnreadMessageCount(0);

        return chatDialog;
    }

    public void sendSystemMessageAboutCreatingDialog(SystemMessagesManager systemMessagesManager, ConnectycubeChatDialog dialog) {
        ConnectycubeChatMessage systemMessageCreatingDialog = buildSystemMessageAboutCreatingGroupDialog(dialog);
        sendSystemMessage(systemMessagesManager, dialog, systemMessageCreatingDialog);
    }

    public void sendSystemMessageAboutUpdatingDialog(SystemMessagesManager systemMessagesManager, ArrayList<String> dialogsIds) {
        for (String id : dialogsIds) {
            ConnectycubeChatDialog dialog = DialogHolder.getInstance().getChatDialogById(id);
            if (dialog != null && !dialog.isPrivate()) {
                dialog.getOccupants().remove(ConnectycubeChatService.getInstance().getUser().getId());
                sendSystemMessageAboutUpdatingDialog(systemMessagesManager, dialog);
            }
        }
    }

    public void sendSystemMessageAboutUpdatingDialog(SystemMessagesManager systemMessagesManager, ConnectycubeChatDialog dialog) {
        ConnectycubeChatMessage systemMessageUpdatingDialog = buildSystemMessageAboutUpdatingGroupDialog(dialog);
        sendSystemMessage(systemMessagesManager, dialog, systemMessageUpdatingDialog);
    }

    private void sendSystemMessage(SystemMessagesManager systemMessagesManager, ConnectycubeChatDialog dialog, ConnectycubeChatMessage systemMessage) {
        try {
            for (Integer recipientId : dialog.getOccupants()) {
                if (!recipientId.equals(ConnectycubeChatService.getInstance().getUser().getId())) {
                    systemMessage.setRecipientId(recipientId);
                    systemMessagesManager.sendSystemMessage(systemMessage);
                }
            }
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void loadUsersFromDialog(ConnectycubeChatDialog chatDialog) {
        ChatHelper.getInstance().getUsersFromDialog(chatDialog, new EntityCallbackImpl<ArrayList<ConnectycubeUser>>());
    }

    public void onGlobalMessageReceived(String dialogId, ConnectycubeChatMessage chatMessage) {
        if (DialogHolder.getInstance().hasDialogWithId(dialogId)) {
            DialogHolder.getInstance().updateDialog(dialogId, chatMessage);
            notifyListenersDialogUpdated(dialogId);
        } else {
            loadNewDialog(dialogId);
        }
    }

    public void onSystemMessageReceived(ConnectycubeChatMessage systemMessage) {
        ConnectycubeChatDialog chatDialog = buildChatDialogFromSystemMessage(systemMessage);
        if (isMessageCreatingDialog(systemMessage)) {
            chatDialog.initForChat(ConnectycubeChatService.getInstance());
            DialogHolder.getInstance().addDialog(chatDialog);
            notifyListenersDialogCreated(chatDialog);
        } else if (isMessageUpdatingDialog(systemMessage)) {
            if (DialogHolder.getInstance().hasDialogWithId(chatDialog.getDialogId())) {
                DialogHolder.getInstance().updateDialog(chatDialog);
                notifyListenersDialogUpdated(chatDialog.getDialogId());
            } else {
                loadNewDialog(chatDialog.getDialogId());
            }
        }
    }

    private void loadNewDialog(String dialogId) {
        ChatHelper.getInstance().getDialogById(dialogId, new EntityCallbackImpl<ConnectycubeChatDialog>() {
            @Override
            public void onSuccess(ConnectycubeChatDialog chatDialog, Bundle bundle) {
                loadUsersFromDialog(chatDialog);
                DialogHolder.getInstance().addDialog(chatDialog);
                notifyListenersNewDialogLoaded(chatDialog);
            }
        });
    }

    private void notifyListenersDialogCreated(final ConnectycubeChatDialog chatDialog) {
        for (ManagingDialogsCallbacks listener : getManagingDialogsCallbackListeners()) {
            listener.onDialogCreated(chatDialog);
        }
    }

    private void notifyListenersDialogUpdated(final String dialogId) {
        for (ManagingDialogsCallbacks listener : getManagingDialogsCallbackListeners()) {
            listener.onDialogUpdated(dialogId);
        }
    }

    private void notifyListenersNewDialogLoaded(final ConnectycubeChatDialog chatDialog) {
        for (ManagingDialogsCallbacks listener : getManagingDialogsCallbackListeners()) {
            listener.onNewDialogLoaded(chatDialog);
        }
    }

    public void addManagingDialogsCallbackListener(ManagingDialogsCallbacks listener) {
        if (listener != null) {
            managingDialogsCallbackListener.add(listener);
        }
    }

    public void removeManagingDialogsCallbackListener(ManagingDialogsCallbacks listener) {
        managingDialogsCallbackListener.remove(listener);
    }

    public Collection<ManagingDialogsCallbacks> getManagingDialogsCallbackListeners() {
        return Collections.unmodifiableCollection(managingDialogsCallbackListener);
    }

    public interface ManagingDialogsCallbacks {

        void onDialogCreated(ConnectycubeChatDialog chatDialog);

        void onDialogUpdated(String chatDialog);

        void onNewDialogLoaded(ConnectycubeChatDialog chatDialog);
    }
}
