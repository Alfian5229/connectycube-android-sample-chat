package com.connectycube.sample.chat.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.view.ActionMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.connectycube.chat.ConnectycubeChatService;
import com.connectycube.chat.IncomingMessagesManager;
import com.connectycube.chat.SystemMessagesManager;
import com.connectycube.chat.exception.ChatException;
import com.connectycube.chat.listeners.ChatDialogMessageListener;
import com.connectycube.chat.listeners.SystemMessageListener;
import com.connectycube.chat.model.ConnectycubeChatDialog;
import com.connectycube.chat.model.ConnectycubeChatMessage;
import com.connectycube.core.EntityCallback;
import com.connectycube.core.exception.ResponseException;
import com.connectycube.core.request.RequestGetBuilder;
import com.connectycube.pushnotifications.services.ConnectycubePushManager;
import com.connectycube.pushnotifications.services.SubscribeService;
import com.connectycube.sample.chat.R;
import com.connectycube.sample.chat.manager.DialogsManager;
import com.connectycube.sample.chat.ui.adapter.DialogsAdapter;
import com.connectycube.sample.chat.utils.NotificationUtils;
import com.connectycube.sample.chat.utils.SharedPrefsHelper;
import com.connectycube.sample.chat.utils.callback.ChatDialogMessageListenerImp;
import com.connectycube.sample.chat.utils.callback.EntityCallbackImpl;
import com.connectycube.sample.chat.utils.callback.PushSubscribeListenerImpl;
import com.connectycube.sample.chat.utils.chat.ChatHelper;
import com.connectycube.sample.chat.utils.holder.DialogHolder;
import com.connectycube.sample.chat.utils.holder.UsersHolder;
import com.connectycube.users.ConnectycubeUsers;
import com.connectycube.users.model.ConnectycubeUser;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import java.util.ArrayList;
import java.util.Collection;

import static com.connectycube.sample.chat.ui.activity.ChatActivity.EXTRA_DIALOG_ID;
import static com.connectycube.sample.chat.utils.Consts.ACTION_NEW_FCM_EVENT;
import static com.connectycube.sample.chat.utils.Consts.EXTRA_FCM_DIALOG_ID;
import static com.connectycube.sample.chat.utils.Consts.EXTRA_FCM_MESSAGE;
import static com.connectycube.sample.chat.utils.Consts.NOTIFICATION_ID;

public class DialogsActivity extends BaseLoggableActivity implements DialogsManager.ManagingDialogsCallbacks {
    private static final String TAG = DialogsActivity.class.getSimpleName();
    private static final int REQUEST_SELECT_PEOPLE = 155;
    private static final int REQUEST_DIALOG_ID_FOR_UPDATE = 156;

    private FloatingActionButton fabCreateChat;
    private ActionMode currentActionMode;
    private SwipyRefreshLayout setOnRefreshListener;
    private RequestGetBuilder requestBuilder;
    private int skipRecords = 0;
    private boolean isProcessingResultInProgress;

    private BroadcastReceiver pushBroadcastReceiver;
    private DialogsAdapter dialogsAdapter;
    private ChatDialogMessageListener allDialogsMessagesListener;
    private SystemMessagesListener systemMessagesListener;
    private SystemMessagesManager systemMessagesManager;
    private IncomingMessagesManager incomingMessagesManager;
    private DialogsManager dialogsManager;
    private String notificationDlgId;
    private String currentDialogId;

    public static void start(Context context, String dialogId) {
        Intent intent = new Intent(context, DialogsActivity.class);
        intent.putExtra(EXTRA_DIALOG_ID, dialogId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pushBroadcastReceiver = new PushBroadcastReceiver();

        allDialogsMessagesListener = new AllDialogsMessageListener();
        systemMessagesListener = new SystemMessagesListener();

        dialogsManager = DialogsManager.getInstance();

        initUi();
        initCurrentDialogId(getIntent());

        setActionBarTitle(getString(R.string.dialogs_logged_in_as, ChatHelper.getCurrentUser().getLogin()));

        registerChatListeners();
        if (DialogHolder.getInstance().getDialogs().size() > 0) {
            loadDialogsREST(true, true);
        } else {
            loadDialogsREST(false, true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterChatListeners();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        initCurrentDialogId(intent);
        Log.d(TAG, "onNewIntent currentDialogId= " + currentDialogId);
        if (currentDialogId != null) {
            ConnectycubeChatDialog dialog = DialogHolder.getInstance().getChatDialogById(currentDialogId);
            ChatActivity.startForResult(DialogsActivity.this, REQUEST_DIALOG_ID_FOR_UPDATE, dialog);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_dialogs, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (isProcessingResultInProgress) {
            return super.onOptionsItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.menu_dialogs_action_logout:
                userLogout();
                item.setEnabled(false);
                invalidateOptionsMenu();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            isProcessingResultInProgress = true;
            if (requestCode == REQUEST_SELECT_PEOPLE) {
                ArrayList<ConnectycubeUser> selectedUsers = (ArrayList<ConnectycubeUser>) data
                        .getSerializableExtra(SelectUsersActivity.EXTRA_USERS);

                if (isPrivateDialogExist(selectedUsers)) {
                    selectedUsers.remove(ChatHelper.getCurrentUser());
                    ConnectycubeChatDialog existingPrivateDialog = DialogHolder.getInstance().getPrivateDialogWithUser(selectedUsers.get(0));
                    isProcessingResultInProgress = false;
                    currentDialogId = existingPrivateDialog.getDialogId();
                    ChatActivity.startForResult(DialogsActivity.this, REQUEST_DIALOG_ID_FOR_UPDATE, existingPrivateDialog);
                } else {
                    showProgressDialog(R.string.create_chat);
                    createDialog(selectedUsers);
                }
            } else if (requestCode == REQUEST_DIALOG_ID_FOR_UPDATE) {
                if (data != null) {
                    String dialogId = data.getStringExtra(ChatActivity.EXTRA_DIALOG_ID);
                    loadUpdatedDialog(dialogId);
                } else {
                    isProcessingResultInProgress = false;
                    updateDialogsList();
                }
            }
        } else {
            updateDialogsAdapter();
        }
        currentDialogId = null;
        hideProgressDialog();
    }

    @Override
    protected int getContentResId() {
        return R.layout.activity_dialogs;
    }

    private void initCurrentDialogId(Intent intent) {
        currentDialogId = intent.getStringExtra(EXTRA_FCM_DIALOG_ID);
    }

    private void proceedToChat() {
        ConnectycubeChatDialog dialog = DialogHolder.getInstance().getChatDialogById(currentDialogId);
        ChatActivity.startForResult(DialogsActivity.this, REQUEST_DIALOG_ID_FOR_UPDATE, dialog);
    }

    private boolean shouldProceedToChat(boolean proceed) {
        return !TextUtils.isEmpty(currentDialogId) && proceed;
    }

    private boolean isPrivateDialogExist(ArrayList<ConnectycubeUser> allSelectedUsers) {
        ArrayList<ConnectycubeUser> selectedUsers = new ArrayList<>(allSelectedUsers);
        selectedUsers.remove(ChatHelper.getCurrentUser());
        return selectedUsers.size() == 1 && DialogHolder.getInstance().hasPrivateDialogWithUser(selectedUsers.get(0));
    }

    private void loadUpdatedDialog(String dialogId) {
        ChatHelper.getInstance().getDialogById(dialogId, new EntityCallbackImpl<ConnectycubeChatDialog>() {
            @Override
            public void onSuccess(ConnectycubeChatDialog result, Bundle bundle) {
                isProcessingResultInProgress = false;
                DialogHolder.getInstance().addDialog(result);
                updateDialogsAdapter();
            }

            @Override
            public void onError(ResponseException e) {
                isProcessingResultInProgress = false;
            }
        });
    }

    @Override
    protected View getSnackbarAnchorView() {
        return findViewById(R.id.layout_root);
    }

    @Override
    public ActionMode startSupportActionMode(@NonNull ActionMode.Callback callback) {
        currentActionMode = super.startSupportActionMode(callback);
        return currentActionMode;
    }

    private void userLogout() {
        showProgressDialog(R.string.dlg_logout);
        ChatHelper.getInstance().destroy();
        logout();
    }

    private void proceedLogout() {
        SharedPrefsHelper.getInstance().removeUser();
        NotificationUtils.cancelNotification();
        LoginActivity.start(DialogsActivity.this);
        DialogHolder.getInstance().clear();
        hideProgressDialog();
        finish();
    }

    private void logout() {
        if (ConnectycubePushManager.getInstance().isSubscribedToPushes()) {
            ConnectycubePushManager.getInstance().addListener(new PushSubscribeListenerImpl() {
                @Override
                public void onSubscriptionDeleted(boolean success) {
                    logoutREST();
                    ConnectycubePushManager.getInstance().removeListener(this);
                }
            });
            SubscribeService.unSubscribeFromPushes(DialogsActivity.this);
        } else {
            logoutREST();
        }
    }

    private void logoutREST() {
        ConnectycubeUsers.signOut().performAsync(new EntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {
                proceedLogout();
            }

            @Override
            public void onError(ResponseException e) {
                Toast.makeText(DialogsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                proceedLogout();
            }
        });
    }

    private void updateDialogsList() {
        requestBuilder.setSkip(skipRecords = 0);
        loadDialogsREST(true, true);
    }

    public void onStartNewChatClick(View view) {
        SelectUsersActivity.startForResult(this, REQUEST_SELECT_PEOPLE);
    }

    private void initUi() {
        LinearLayout emptyHintLayout = findViewById(R.id.layout_chat_empty);
        ListView dialogsListView = findViewById(R.id.list_dialogs_chats);
        fabCreateChat = findViewById(R.id.fab_dialogs_new_chat);
        setOnRefreshListener = findViewById(R.id.swipy_refresh_layout);

        dialogsAdapter = new DialogsAdapter(this, new ArrayList<>(DialogHolder.getInstance().getDialogs().values()));

        TextView listHeader = (TextView) LayoutInflater.from(this)
                .inflate(R.layout.include_list_hint_header, dialogsListView, false);
        listHeader.setText(R.string.dialogs_list_hint);
        dialogsListView.setEmptyView(emptyHintLayout);
        dialogsListView.addHeaderView(listHeader, null, false);

        dialogsListView.setAdapter(dialogsAdapter);

        dialogsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ConnectycubeChatDialog selectedDialog = (ConnectycubeChatDialog) parent.getItemAtPosition(position);
                if (currentActionMode == null) {
                    currentDialogId = selectedDialog.getDialogId();
                    dismissNotificationIfNeed(selectedDialog);
                    ChatActivity.startForResult(DialogsActivity.this, REQUEST_DIALOG_ID_FOR_UPDATE, selectedDialog);
                } else {
                    dialogsAdapter.toggleSelection(selectedDialog);
                }
            }
        });
        dialogsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ConnectycubeChatDialog selectedDialog = (ConnectycubeChatDialog) parent.getItemAtPosition(position);
                startSupportActionMode(new DeleteActionModeCallback());
                dialogsAdapter.selectItem(selectedDialog);
                return true;
            }
        });
        requestBuilder = new RequestGetBuilder();

        setOnRefreshListener.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh(SwipyRefreshLayoutDirection direction) {
                requestBuilder.setSkip(skipRecords += ChatHelper.DIALOG_ITEMS_PER_PAGE);
                loadDialogsREST(true, false);
            }
        });
    }

    private void dismissNotificationIfNeed(ConnectycubeChatDialog... dialogs) {
        for (ConnectycubeChatDialog dialog : dialogs) {
            if (TextUtils.equals(notificationDlgId, dialog.getDialogId())) {
                NotificationUtils.cancelNotification();
                break;
            }
        }
    }

    private void registerChatListeners() {
        incomingMessagesManager = ConnectycubeChatService.getInstance().getIncomingMessagesManager();
        systemMessagesManager = ConnectycubeChatService.getInstance().getSystemMessagesManager();

        if (incomingMessagesManager != null) {
            incomingMessagesManager.addDialogMessageListener(allDialogsMessagesListener);
        }

        if (systemMessagesManager != null) {
            systemMessagesManager.addSystemMessageListener(systemMessagesListener);
        }

        dialogsManager.addManagingDialogsCallbackListener(this);

        LocalBroadcastManager.getInstance(this).registerReceiver(pushBroadcastReceiver, new IntentFilter(ACTION_NEW_FCM_EVENT));
    }

    private void unregisterChatListeners() {
        if (incomingMessagesManager != null) {
            incomingMessagesManager.removeDialogMessageListrener(allDialogsMessagesListener);
        }

        if (systemMessagesManager != null) {
            systemMessagesManager.removeSystemMessageListener(systemMessagesListener);
        }

        dialogsManager.removeManagingDialogsCallbackListener(this);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(pushBroadcastReceiver);
    }

    private void createDialog(final ArrayList<ConnectycubeUser> selectedUsers) {
        ChatHelper.getInstance().createDialogWithSelectedUsers(selectedUsers,
                new EntityCallback<ConnectycubeChatDialog>() {
                    @Override
                    public void onSuccess(ConnectycubeChatDialog dialog, Bundle args) {
                        isProcessingResultInProgress = false;
                        dialogsManager.sendSystemMessageAboutCreatingDialog(systemMessagesManager, dialog);
                        currentDialogId = dialog.getDialogId();
                        ChatActivity.startForResult(DialogsActivity.this, REQUEST_DIALOG_ID_FOR_UPDATE, dialog);
                        hideProgressDialog();
                    }

                    @Override
                    public void onError(ResponseException e) {
                        isProcessingResultInProgress = false;
                        hideProgressDialog();
                        showErrorSnackbar(R.string.dialogs_creation_error, e, null);
                    }
                }
        );
    }

    private void loadDialogsREST(final boolean silentUpdate, final boolean clearDialogHolder) {
        isProcessingResultInProgress = true;
        if (!silentUpdate) {
            showProgressDialog(R.string.dlg_loading);
        }

        ChatHelper.getInstance().getDialogs(requestBuilder, new EntityCallback<ArrayList<ConnectycubeChatDialog>>() {
            @Override
            public void onSuccess(ArrayList<ConnectycubeChatDialog> dialogs, Bundle bundle) {
                isProcessingResultInProgress = false;
                setOnRefreshListener.setRefreshing(false);

                if (clearDialogHolder) {
                    DialogHolder.getInstance().clear();
                }
                DialogHolder.getInstance().addDialogs(dialogs);
                updateDialogsAdapter();

                if (shouldProceedToChat(!silentUpdate)) {
                    proceedToChat();
                } else {
                    hideProgressDialog();
                }
            }

            @Override
            public void onError(ResponseException e) {
                isProcessingResultInProgress = false;
                hideProgressDialog();
                setOnRefreshListener.setRefreshing(false);
                Toast.makeText(DialogsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDialogsAdapter() {
        dialogsAdapter.updateList(new ArrayList<>(DialogHolder.getInstance().getDialogs().values()));
    }

    @Override
    public void onDialogCreated(ConnectycubeChatDialog chatDialog) {
        updateDialogsAdapter();
    }

    @Override
    public void onDialogUpdated(String chatDialog) {
        updateDialogsAdapter();
    }

    @Override
    public void onNewDialogLoaded(ConnectycubeChatDialog chatDialog) {
        updateDialogsAdapter();
    }

    private class DeleteActionModeCallback implements ActionMode.Callback {

        public DeleteActionModeCallback() {
            fabCreateChat.hide();
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.action_mode_dialogs, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_dialogs_action_delete:
                    deleteSelectedDialogs();
                    if (currentActionMode != null) {
                        currentActionMode.finish();
                    }
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            currentActionMode = null;
            dialogsAdapter.clearSelection();
            fabCreateChat.show();
        }

        private void deleteSelectedDialogs() {
            final Collection<ConnectycubeChatDialog> selectedDialogs = dialogsAdapter.getSelectedItems();
            dismissNotificationIfNeed(selectedDialogs.toArray(new ConnectycubeChatDialog[selectedDialogs.size()]));

            ChatHelper.getInstance().deleteDialogs(selectedDialogs, new EntityCallback<ArrayList<String>>() {
                @Override
                public void onSuccess(ArrayList<String> dialogsIds, Bundle bundle) {
                    DialogsManager.getInstance().sendSystemMessageAboutUpdatingDialog(ConnectycubeChatService.getInstance().getSystemMessagesManager(), dialogsIds);
                    DialogHolder.getInstance().deleteDialogs(dialogsIds);
                    updateDialogsAdapter();
                }

                @Override
                public void onError(ResponseException e) {
                    showErrorSnackbar(R.string.dialogs_deletion_error, e,
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    deleteSelectedDialogs();
                                }
                            });
                }
            });
        }
    }

    private class PushBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra(EXTRA_FCM_MESSAGE);
            String dialogId = intent.getStringExtra(EXTRA_FCM_DIALOG_ID);
            Log.v(TAG, "Received broadcast " + intent.getAction() + " with data: " + message + ", dialogId= " + dialogId);
            notificationDlgId = dialogId;
            requestBuilder.setSkip(skipRecords = 0);
            loadDialogsREST(true, true);
        }
    }

    private class SystemMessagesListener implements SystemMessageListener {
        @Override
        public void processMessage(final ConnectycubeChatMessage chatMessage) {
            dialogsManager.onSystemMessageReceived(chatMessage);
        }

        @Override
        public void processError(ChatException e, ConnectycubeChatMessage chatMessage) {

        }
    }

    private class AllDialogsMessageListener extends ChatDialogMessageListenerImp {
        @Override
        public void processMessage(final String dialogId, final ConnectycubeChatMessage chatMessage, Integer senderId) {
            if (!senderId.equals(ChatHelper.getCurrentUser().getId())) {
                dialogsManager.onGlobalMessageReceived(dialogId, chatMessage);

                showMessageNotificationIfNeed(dialogId, chatMessage);
            }
        }
    }

    private void showMessageNotificationIfNeed(String dialogId, ConnectycubeChatMessage chatMessage) {
        Log.d(TAG, "currentDialogId= " + currentDialogId + ", dialogId= " + dialogId);
        if (!TextUtils.equals(currentDialogId, dialogId)) {
            notificationDlgId = dialogId;
            ConnectycubeChatDialog dialog = DialogHolder.getInstance().getChatDialogById(dialogId);
            ConnectycubeUser senderUser = UsersHolder.getInstance().getUserById(chatMessage.getSenderId());
            String sender = dialog != null ? dialog.getName() : senderUser != null ?
                    senderUser.getLogin() : chatMessage.getSenderId().toString();
            String notificationMessage = getString(R.string.dialogs_incoming_msg, sender, chatMessage.getBody());

            NotificationUtils.showNotification(DialogsActivity.this, LoginActivity.class,
                    getString(R.string.notification_title), notificationMessage, dialogId,
                    R.drawable.ic_info, NOTIFICATION_ID);
        }
    }
}
