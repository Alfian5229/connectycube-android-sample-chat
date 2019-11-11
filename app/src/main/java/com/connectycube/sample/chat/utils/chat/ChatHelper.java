package com.connectycube.sample.chat.utils.chat;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.connectycube.auth.session.ConnectycubeSettings;
import com.connectycube.chat.ConnectycubeChatService;
import com.connectycube.chat.ConnectycubeRestChatService;
import com.connectycube.chat.connections.tcp.TcpChatConnectionFabric;
import com.connectycube.chat.connections.tcp.TcpConfigurationBuilder;
import com.connectycube.chat.model.ConnectycubeAttachment;
import com.connectycube.chat.model.ConnectycubeChatDialog;
import com.connectycube.chat.model.ConnectycubeChatMessage;
import com.connectycube.chat.model.ConnectycubeDialogType;
import com.connectycube.chat.request.DialogRequestBuilder;
import com.connectycube.chat.request.MessageGetBuilder;
import com.connectycube.chat.utils.DialogUtils;
import com.connectycube.core.ConnectycubeProgressCallback;
import com.connectycube.core.EntityCallback;
import com.connectycube.core.LogLevel;
import com.connectycube.core.exception.ResponseException;
import com.connectycube.core.helper.StringifyArrayList;
import com.connectycube.core.request.PagedRequestBuilder;
import com.connectycube.core.request.RequestGetBuilder;
import com.connectycube.sample.chat.App;
import com.connectycube.sample.chat.R;
import com.connectycube.sample.chat.utils.holder.DialogHolder;
import com.connectycube.sample.chat.utils.holder.UsersHolder;
import com.connectycube.sample.chat.utils.callback.EntityCallbackTwoTypeWrapper;
import com.connectycube.sample.chat.utils.callback.EntityCallbackWrapper;
import com.connectycube.storage.ConnectycubeStorage;
import com.connectycube.storage.model.ConnectycubeFile;
import com.connectycube.users.ConnectycubeUsers;
import com.connectycube.users.model.ConnectycubeUser;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ChatHelper {
    private static final String TAG = ChatHelper.class.getSimpleName();

    public static final int DIALOG_ITEMS_PER_PAGE = 100;
    public static final int CHAT_HISTORY_ITEMS_PER_PAGE = 50;
    private static final String CHAT_HISTORY_ITEMS_SORT_FIELD = "date_sent";
    private static final int SOCKET_TIMEOUT = 0;
    private static ChatHelper instance;

    private ConnectycubeChatService chatService;

    public static synchronized ChatHelper getInstance() {
        if (instance == null) {
            ConnectycubeSettings.getInstance().setLogLevel(LogLevel.DEBUG);
            ConnectycubeChatService.setDebugEnabled(true);
            ConnectycubeChatService.setConnectionFabric(new TcpChatConnectionFabric(buildChatConfigs()));
            instance = new ChatHelper();
        }
        return instance;
    }

    public boolean isLogged() {
        return ConnectycubeChatService.getInstance().isLoggedIn();
    }

    public static ConnectycubeUser getCurrentUser() {
        return ConnectycubeChatService.getInstance().getUser();
    }

    private ChatHelper() {
        chatService = ConnectycubeChatService.getInstance();
        chatService.setUseStreamManagement(true);
    }

    private static TcpConfigurationBuilder buildChatConfigs() {
        TcpConfigurationBuilder configurationBuilder = new TcpConfigurationBuilder();
        configurationBuilder.setSocketTimeout(SOCKET_TIMEOUT);
        return configurationBuilder;
    }

    public void addConnectionListener(ConnectionListener listener) {
        chatService.addConnectionListener(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        chatService.removeConnectionListener(listener);
    }

    public void login(final ConnectycubeUser user, final EntityCallback<Void> callback) {

        ConnectycubeUsers.signIn(user).performAsync(new EntityCallbackTwoTypeWrapper<ConnectycubeUser, Void>(callback) {
            @Override
            public void onSuccess(ConnectycubeUser userResult, Bundle args) {
                user.setId(userResult.getId());
                loginToChat(user, new EntityCallbackWrapper<>(callback));
            }
        });
    }

    public void loginToChat(final ConnectycubeUser user, final EntityCallback<Void> callback) {
        if (chatService.isLoggedIn()) {
            callback.onSuccess(null, null);
            return;
        }

        chatService.login(user, callback);
    }

    public void join(ConnectycubeChatDialog chatDialog, final EntityCallback<Void> callback) {
        chatDialog.join(callback);
    }

    public void leaveChatDialog(ConnectycubeChatDialog chatDialog) throws XMPPException, SmackException.NotConnectedException {
        try {
            chatDialog.leave();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        chatService.destroy();
    }

    public void createDialogWithSelectedUsers(final List<ConnectycubeUser> users,
                                              final EntityCallback<ConnectycubeChatDialog> callback) {

        ConnectycubeRestChatService.createChatDialog(ChatDialogUtils.createDialog(users)).performAsync(
                new EntityCallbackWrapper<ConnectycubeChatDialog>(callback) {
                    @Override
                    public void onSuccess(ConnectycubeChatDialog dialog, Bundle args) {
                        DialogHolder.getInstance().addDialog(dialog);
                        UsersHolder.getInstance().putUsers(users);
                        super.onSuccess(dialog, args);
                    }
                });
    }

    public void deleteDialogs(Collection<ConnectycubeChatDialog> dialogs, final EntityCallback<ArrayList<String>> callback) {
        StringifyArrayList<String> dialogsIds = new StringifyArrayList<>();
        for (ConnectycubeChatDialog dialog : dialogs) {
            dialogsIds.add(dialog.getDialogId());
        }

        ConnectycubeRestChatService.deleteDialogs(dialogsIds, false, null).performAsync(callback);
    }

    public void deleteDialog(ConnectycubeChatDialog dialog, EntityCallback<Void> callback) {
        if (dialog.getType() == ConnectycubeDialogType.BROADCAST) {
            Toast.makeText(App.getInstance(), R.string.broadcast_chat_cannot_be_deleted, Toast.LENGTH_SHORT).show();
        } else {
            ConnectycubeRestChatService.deleteDialog(dialog.getDialogId(), false)
                    .performAsync(new EntityCallbackWrapper<Void>(callback));
        }
    }

    public void exitFromDialog(ConnectycubeChatDialog dialog, EntityCallback<ConnectycubeChatDialog> callback) {
        try {
            leaveChatDialog(dialog);
        } catch (XMPPException | SmackException.NotConnectedException e) {
            callback.onError(new ResponseException(e.getMessage()));
        }

        ConnectycubeUser currentUser = UsersHolder.getInstance().getUserById(ConnectycubeChatService.getInstance().getUser().getId());
        DialogRequestBuilder requestBuilder = new DialogRequestBuilder();
        requestBuilder.removeUsers(currentUser.getId());

        dialog.setName(buildDialogNameWithoutUser(dialog.getName(), currentUser.getFullName() == null ? currentUser.getId().toString() : currentUser.getFullName()));

        ConnectycubeRestChatService.updateChatDialog(dialog, requestBuilder).performAsync(callback);
    }

    private static String buildDialogNameWithoutUser(String dialogName, String userName) {
        String regex = ", " + userName + "|" + userName + ", ";
        return dialogName.replaceAll(regex, "");
    }

    public void updateDialogUsers(String dialogId,
                                  final List<ConnectycubeUser> newDialogUsersList,
                                  EntityCallback<ConnectycubeChatDialog> callback) {
        ConnectycubeChatDialog dialog = DialogHolder.getInstance().getChatDialogById(dialogId);
        List<ConnectycubeUser> addedUsers = ChatDialogUtils.getAddedUsers(dialog, newDialogUsersList);
        List<ConnectycubeUser> removedUsers = ChatDialogUtils.getRemovedUsers(dialog, newDialogUsersList);

        ChatDialogUtils.logDialogUsers(dialog);
        ChatDialogUtils.logUsers(addedUsers);
        Log.w(TAG, "=======================");
        ChatDialogUtils.logUsers(removedUsers);

        DialogRequestBuilder requestBuilder = new DialogRequestBuilder();
        if (!addedUsers.isEmpty()) {
            requestBuilder.addUsers(addedUsers.toArray(new ConnectycubeUser[addedUsers.size()]));
        }
        if (!removedUsers.isEmpty()) {
            requestBuilder.removeUsers(removedUsers.toArray(new ConnectycubeUser[removedUsers.size()]));
        }
        dialog.setName(DialogUtils.createChatNameFromUserList(
                newDialogUsersList.toArray(new ConnectycubeUser[newDialogUsersList.size()])));

        ConnectycubeRestChatService.updateChatDialog(dialog, requestBuilder).performAsync(
                new EntityCallbackWrapper<ConnectycubeChatDialog>(callback) {
                    @Override
                    public void onSuccess(ConnectycubeChatDialog dialog, Bundle bundle) {
                        UsersHolder.getInstance().putUsers(newDialogUsersList);
                        ChatDialogUtils.logDialogUsers(dialog);
                        DialogHolder.getInstance().updateDialog(dialog);
                        super.onSuccess(dialog, bundle);
                    }
                });
    }

    public void loadChatHistory(ConnectycubeChatDialog dialog, int skipPagination,
                                final EntityCallback<ArrayList<ConnectycubeChatMessage>> callback) {
        MessageGetBuilder messageGetBuilder = new MessageGetBuilder();
        messageGetBuilder.setSkip(skipPagination);
        messageGetBuilder.setLimit(CHAT_HISTORY_ITEMS_PER_PAGE);
        messageGetBuilder.sortDesc(CHAT_HISTORY_ITEMS_SORT_FIELD);
        messageGetBuilder.markAsRead(false);

        ConnectycubeRestChatService.getDialogMessages(dialog, messageGetBuilder).performAsync(
                new EntityCallbackWrapper<ArrayList<ConnectycubeChatMessage>>(callback) {
                    @Override
                    public void onSuccess(ArrayList<ConnectycubeChatMessage> chatMessages, Bundle bundle) {

                        Set<Integer> userIds = new HashSet<>();
                        for (ConnectycubeChatMessage message : chatMessages) {
                            userIds.add(message.getSenderId());
                        }

                        if (!userIds.isEmpty()) {
                            getUsersFromMessages(chatMessages, userIds, callback);
                        } else {
                            callback.onSuccess(chatMessages, bundle);
                        }
                        // Not calling super.onSuccess() because
                        // we're want to load chat users before triggering the callback
                    }
                });
    }

    public void getDialogs(RequestGetBuilder customObjectRequestBuilder, final EntityCallback<ArrayList<ConnectycubeChatDialog>> callback) {
        customObjectRequestBuilder.setLimit(DIALOG_ITEMS_PER_PAGE);

        ConnectycubeRestChatService.getChatDialogs(null, customObjectRequestBuilder).performAsync(
                new EntityCallbackWrapper<ArrayList<ConnectycubeChatDialog>>(callback) {
                    @Override
                    public void onSuccess(ArrayList<ConnectycubeChatDialog> dialogs, Bundle args) {
                        Iterator<ConnectycubeChatDialog> dialogIterator = dialogs.iterator();
                        while (dialogIterator.hasNext()) {
                            ConnectycubeChatDialog dialog = dialogIterator.next();
                            if (dialog.getType() == ConnectycubeDialogType.BROADCAST) {
                                dialogIterator.remove();
                            }
                        }

                        getUsersFromDialogs(dialogs, callback);
                        // Not calling super.onSuccess() because
                        // we want to load chat users before triggering callback
                    }
                });
    }

    public void getDialogById(String dialogId, final EntityCallback<ConnectycubeChatDialog> callback) {
        ConnectycubeRestChatService.getChatDialogById(dialogId).performAsync(callback);
    }

    public void getUsersFromDialog(ConnectycubeChatDialog dialog,
                                   final EntityCallback<ArrayList<ConnectycubeUser>> callback) {
        List<Integer> userIds = dialog.getOccupants();

        final ArrayList<ConnectycubeUser> users = new ArrayList<>(userIds.size());
        for (Integer id : userIds) {
            ConnectycubeUser user = UsersHolder.getInstance().getUserById(id);
            if (user != null) {
                users.add(user);
            }
        }
        // If we already have all users in memory
        // there is no need to make REST requests
        if (userIds.size() == users.size()) {
            callback.onSuccess(users, null);
            return;
        }

        PagedRequestBuilder requestBuilder = new PagedRequestBuilder(userIds.size(), 1);
        ConnectycubeUsers.getUsersByIDs(userIds, requestBuilder).performAsync(
                new EntityCallbackWrapper<ArrayList<ConnectycubeUser>>(callback) {
                    @Override
                    public void onSuccess(ArrayList<ConnectycubeUser> users, Bundle bundle) {
                        UsersHolder.getInstance().putUsers(users);
                        callback.onSuccess(users, bundle);
                    }
                });
    }

    public void loadFileAsAttachment(File file, EntityCallback<ConnectycubeAttachment> callback) {
        loadFileAsAttachment(file, callback, null);
    }

    public void loadFileAsAttachment(File file, EntityCallback<ConnectycubeAttachment> callback,
                                     ConnectycubeProgressCallback progressCallback) {
        ConnectycubeStorage.uploadFileTask(file, true, progressCallback).performAsync(
                new EntityCallbackTwoTypeWrapper<ConnectycubeFile, ConnectycubeAttachment>(callback) {
                    @Override
                    public void onSuccess(ConnectycubeFile file, Bundle bundle) {
                        ConnectycubeAttachment attachment = new ConnectycubeAttachment(ConnectycubeAttachment.IMAGE_TYPE);
                        attachment.setId(file.getId().toString());
                        attachment.setUrl(file.getPublicUrl());
                        callback.onSuccess(attachment, bundle);
                    }
                });
    }

    private void getUsersFromDialogs(final ArrayList<ConnectycubeChatDialog> dialogs,
                                     final EntityCallback<ArrayList<ConnectycubeChatDialog>> callback) {
        List<Integer> userIds = new ArrayList<>();
        for (ConnectycubeChatDialog dialog : dialogs) {
            userIds.addAll(dialog.getOccupants());
            userIds.add(dialog.getLastMessageUserId());
        }

        PagedRequestBuilder requestBuilder = new PagedRequestBuilder(userIds.size(), 1);
        ConnectycubeUsers.getUsersByIDs(userIds, requestBuilder).performAsync(
                new EntityCallbackTwoTypeWrapper<ArrayList<ConnectycubeUser>, ArrayList<ConnectycubeChatDialog>>(callback) {
                    @Override
                    public void onSuccess(ArrayList<ConnectycubeUser> users, Bundle params) {
                        UsersHolder.getInstance().putUsers(users);
                        callback.onSuccess(dialogs, params);
                    }
                });
    }

    private void getUsersFromMessages(final ArrayList<ConnectycubeChatMessage> messages,
                                      final Set<Integer> userIds,
                                      final EntityCallback<ArrayList<ConnectycubeChatMessage>> callback) {

        PagedRequestBuilder requestBuilder = new PagedRequestBuilder(userIds.size(), 1);
        ConnectycubeUsers.getUsersByIDs(userIds, requestBuilder).performAsync(
                new EntityCallbackTwoTypeWrapper<ArrayList<ConnectycubeUser>, ArrayList<ConnectycubeChatMessage>>(callback) {
                    @Override
                    public void onSuccess(ArrayList<ConnectycubeUser> users, Bundle params) {
                        UsersHolder.getInstance().putUsers(users);
                        callback.onSuccess(messages, params);
                    }
                });
    }
}