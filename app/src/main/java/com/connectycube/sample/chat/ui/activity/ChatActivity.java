package com.connectycube.sample.chat.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.connectycube.chat.ConnectycubeChatService;
import com.connectycube.chat.model.ConnectycubeAttachment;
import com.connectycube.chat.model.ConnectycubeChatDialog;
import com.connectycube.chat.model.ConnectycubeChatMessage;
import com.connectycube.chat.model.ConnectycubeDialogType;
import com.connectycube.core.EntityCallback;
import com.connectycube.core.exception.ResponseException;
import com.connectycube.sample.chat.R;
import com.connectycube.sample.chat.manager.DialogsManager;
import com.connectycube.sample.chat.ui.adapter.AttachmentPreviewAdapter;
import com.connectycube.sample.chat.ui.adapter.ChatAdapter;
import com.connectycube.sample.chat.ui.widget.AttachmentPreviewAdapterView;
import com.connectycube.sample.chat.utils.EditTextUtils;
import com.connectycube.sample.chat.utils.imagepicker.ImagePickHelper;
import com.connectycube.sample.chat.utils.imagepicker.OnImagePickedListener;
import com.connectycube.sample.chat.utils.chat.ChatHelper;
import com.connectycube.sample.chat.utils.callback.PaginationHistoryListener;
import com.connectycube.sample.chat.utils.callback.ChatDialogMessageListenerImp;
import com.connectycube.sample.chat.utils.holder.DialogHolder;
import com.connectycube.sample.chat.utils.chat.ChatDialogUtils;
import com.connectycube.sample.chat.utils.callback.VerboseChatConnectionListener;
import com.connectycube.ui.chatmessage.adapter.listeners.ChatAttachClickListener;
import com.connectycube.users.model.ConnectycubeUser;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ChatActivity extends BaseLoggableActivity implements OnImagePickedListener {
    private static final String TAG = ChatActivity.class.getSimpleName();
    private static final int REQUEST_CODE_ATTACHMENT = 721;
    private static final int REQUEST_CODE_SELECT_PEOPLE = 752;

    public static final String EXTRA_DIALOG = "dialog";
    public static final String EXTRA_DIALOG_ID = "dialog_id";

    private ProgressBar progressBar;
    private EditText messageEditText;

    private LinearLayout attachmentPreviewContainerLayout;
    private Snackbar snackbar;

    private ChatAdapter chatAdapter;
    private RecyclerView chatMessagesRecyclerView;
    protected List<ConnectycubeChatMessage> messagesList;
    private AttachmentPreviewAdapter attachmentPreviewAdapter;
    private ImageAttachClickListener imageAttachClickListener;

    private ConnectycubeChatDialog chatDialog;
    private ArrayList<ConnectycubeChatMessage> unShownMessages;
    private int skipPagination = 0;
    private ChatMessageListener chatMessageListener;
    private boolean checkAdapterInit;

    public static void startForResult(Activity activity, int code, ConnectycubeChatDialog dialog) {
        Intent intent = new Intent(activity, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_DIALOG, dialog);
        activity.startActivityForResult(intent, code);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate ChatActivity on Thread ID = " + Thread.currentThread().getId());

        chatDialog = (ConnectycubeChatDialog) getIntent().getSerializableExtra(EXTRA_DIALOG);

        Log.v(TAG, "deserialized dialog = " + chatDialog);
        chatDialog.initForChat(ConnectycubeChatService.getInstance());

        initViews();
        initMessagesRecyclerView();

        chatMessageListener = new ChatMessageListener();

        chatDialog.addMessageListener(chatMessageListener);

        initChat();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        if (chatDialog != null) {
            outState.putString(EXTRA_DIALOG_ID, chatDialog.getDialogId());
        }
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (chatDialog == null) {
            chatDialog = DialogHolder.getInstance().getChatDialogById(savedInstanceState.getString(EXTRA_DIALOG_ID));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        addChatMessagesAdapterListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeChatMessagesAdapterListeners();
    }

    @Override
    public void onBackPressed() {
        releaseChat();
        sendDialogId();

        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_chat, menu);

        MenuItem menuItemLeave = menu.findItem(R.id.menu_chat_action_leave);
        MenuItem menuItemAdd = menu.findItem(R.id.menu_chat_action_add);
        MenuItem menuItemDelete = menu.findItem(R.id.menu_chat_action_delete);
        if (chatDialog.getType() == ConnectycubeDialogType.PRIVATE) {
            menuItemLeave.setVisible(false);
            menuItemAdd.setVisible(false);
        } else {
            menuItemDelete.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_chat_action_info:
                ChatInfoActivity.start(this, chatDialog.getDialogId());
                return true;

            case R.id.menu_chat_action_add:
                SelectUsersActivity.startForResult(this, REQUEST_CODE_SELECT_PEOPLE, chatDialog.getDialogId());
                return true;

            case R.id.menu_chat_action_leave:
                leaveGroupChat();
                return true;

            case R.id.menu_chat_action_delete:
                deleteChat();
                return true;

            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected int getContentResId() {
        return R.layout.activity_chat;
    }

    private void sendDialogId() {
        Intent result = new Intent();
        result.putExtra(EXTRA_DIALOG_ID, chatDialog.getDialogId());
        setResult(RESULT_OK, result);
    }

    private void leaveGroupChat() {
        showProgressDialog(R.string.leave_chat);
        ChatHelper.getInstance().exitFromDialog(chatDialog, new EntityCallback<ConnectycubeChatDialog>() {
            @Override
            public void onSuccess(ConnectycubeChatDialog dialog, Bundle bundle) {
                hideProgressDialog();
                DialogsManager.getInstance().sendSystemMessageAboutUpdatingDialog(ConnectycubeChatService.getInstance().getSystemMessagesManager(), dialog);
                DialogHolder.getInstance().deleteDialog(dialog);
                finish();
            }

            @Override
            public void onError(ResponseException e) {
                hideProgressDialog();
                showErrorSnackbar(R.string.error_leave_chat, e, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        leaveGroupChat();
                    }
                });
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_SELECT_PEOPLE) {
                ArrayList<ConnectycubeUser> selectedUsers = (ArrayList<ConnectycubeUser>) data.getSerializableExtra(
                        SelectUsersActivity.EXTRA_USERS);

                updateDialog(selectedUsers);
            }
        }
    }

    @Override
    public void onImagePicked(int requestCode, File file) {
        switch (requestCode) {
            case REQUEST_CODE_ATTACHMENT:
                attachmentPreviewAdapter.add(file);
                break;
        }
    }

    @Override
    public void onImagePickError(int requestCode, Exception e) {
        showErrorSnackbar(0, e, null);
    }

    @Override
    public void onImagePickClosed(int requestCode) {
        // ignore
    }

    @Override
    protected View getSnackbarAnchorView() {
        return findViewById(R.id.list_chat_messages);
    }

    public void onSendChatClick(View view) {
        int totalAttachmentsCount = attachmentPreviewAdapter.getCount();
        Collection<ConnectycubeAttachment> uploadedAttachments = attachmentPreviewAdapter.getUploadedAttachments();
        if (!uploadedAttachments.isEmpty()) {
            if (uploadedAttachments.size() == totalAttachmentsCount) {
                for (ConnectycubeAttachment attachment : uploadedAttachments) {
                    sendChatMessage(null, attachment);
                }
            } else {
                Toast.makeText(this, R.string.chat_wait_for_attachments_to_upload, Toast.LENGTH_SHORT).show();
            }
        }

        String text = messageEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(text)) {
            sendChatMessage(text, null);
        }
    }

    public void onAttachmentsClick(View view) {
        new ImagePickHelper().pickAnImage(this, REQUEST_CODE_ATTACHMENT);
    }

    public void showMessage(ConnectycubeChatMessage message) {
        if (isAdapterConnected()) {
            chatAdapter.add(message);
            scrollMessageListDown();
        } else {
            delayShowMessage(message);
        }
    }

    private boolean isAdapterConnected() {
        return checkAdapterInit;
    }

    private void delayShowMessage(ConnectycubeChatMessage message) {
        if (unShownMessages == null) {
            unShownMessages = new ArrayList<>();
        }
        unShownMessages.add(message);
    }

    private void initViews() {
        actionBar.setDisplayHomeAsUpEnabled(true);

        messageEditText = findViewById(R.id.edit_chat_message);
        messageEditText.setFilters(new InputFilter[]{new EditTextUtils.ImageInputFilter()});
        progressBar = findViewById(R.id.progress_chat);
        attachmentPreviewContainerLayout = findViewById(R.id.layout_attachment_preview_container);

        attachmentPreviewAdapter = new AttachmentPreviewAdapter(this,
                new AttachmentPreviewAdapter.OnAttachmentCountChangedListener() {
                    @Override
                    public void onAttachmentCountChanged(int count) {
                        attachmentPreviewContainerLayout.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
                    }
                },
                new AttachmentPreviewAdapter.OnAttachmentUploadErrorListener() {
                    @Override
                    public void onAttachmentUploadError(ResponseException e) {
                        showErrorSnackbar(0, e, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                onAttachmentsClick(v);
                            }
                        });
                    }
                });
        AttachmentPreviewAdapterView previewAdapterView = findViewById(R.id.adapter_view_attachment_preview);
        previewAdapterView.setAdapter(attachmentPreviewAdapter);
    }

    private void initMessagesRecyclerView() {
        chatMessagesRecyclerView = findViewById(R.id.list_chat_messages);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatMessagesRecyclerView.setLayoutManager(layoutManager);

        messagesList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatDialog, messagesList);
        chatAdapter.setPaginationHistoryListener(new PaginationListener());
        chatMessagesRecyclerView.addItemDecoration(
                new StickyRecyclerHeadersDecoration(chatAdapter));

        chatMessagesRecyclerView.setAdapter(chatAdapter);
        imageAttachClickListener = new ImageAttachClickListener();
    }

    private void sendChatMessage(String text, ConnectycubeAttachment attachment) {
        ConnectycubeChatMessage chatMessage = new ConnectycubeChatMessage();
        if (attachment != null) {
            chatMessage.addAttachment(attachment);
            chatMessage.setBody(getString(R.string.chat_attachment));
        } else {
            chatMessage.setBody(text);
        }
        chatMessage.setSaveToHistory(true);
        chatMessage.setDateSent(System.currentTimeMillis() / 1000);
        chatMessage.setMarkable(true);

        if (!ConnectycubeDialogType.PRIVATE.equals(chatDialog.getType()) && !chatDialog.isJoined()) {
            Toast.makeText(this, "You're still joining a group chat, please wait a bit", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            chatDialog.sendMessage(chatMessage);

            if (ConnectycubeDialogType.PRIVATE.equals(chatDialog.getType())) {
                showMessage(chatMessage);
            }

            if (attachment != null) {
                attachmentPreviewAdapter.remove(attachment);
            } else {
                messageEditText.setText("");
            }
        } catch (SmackException.NotConnectedException e) {
            Log.w(TAG, e);
            Toast.makeText(this, "Can't send a message, You are not connected to chat", Toast.LENGTH_SHORT).show();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void initChat() {
        switch (chatDialog.getType()) {
            case GROUP:
            case BROADCAST:
                joinGroupChat();
                break;

            case PRIVATE:
                loadDialogUsers();
                break;

            default:
                Toast.makeText(this, String.format("%s %s", getString(R.string.chat_unsupported_type), chatDialog.getType().name()), Toast.LENGTH_SHORT).show();
                finish();
                break;
        }
    }

    private void joinGroupChat() {
        progressBar.setVisibility(View.VISIBLE);
        ChatHelper.getInstance().join(chatDialog, new EntityCallback<Void>() {
            @Override
            public void onSuccess(Void result, Bundle b) {
                if (snackbar != null) {
                    snackbar.dismiss();
                }
                loadDialogUsers();
            }

            @Override
            public void onError(ResponseException e) {
                progressBar.setVisibility(View.GONE);
                snackbar = showErrorSnackbar(R.string.connection_error, e, null);
            }
        });
    }

    private void leaveGroupDialog() {
        try {
            ChatHelper.getInstance().leaveChatDialog(chatDialog);
        } catch (XMPPException | SmackException.NotConnectedException e) {
            Log.w(TAG, e);
        }
    }

    private void releaseChat() {
        chatDialog.removeMessageListrener(chatMessageListener);
        if (!ConnectycubeDialogType.PRIVATE.equals(chatDialog.getType())) {
            leaveGroupDialog();
        }
    }

    private void updateDialog(final ArrayList<ConnectycubeUser> selectedUsers) {
        ChatHelper.getInstance().updateDialogUsers(chatDialog.getDialogId(), selectedUsers,
                new EntityCallback<ConnectycubeChatDialog>() {
                    @Override
                    public void onSuccess(ConnectycubeChatDialog dialog, Bundle args) {
                        chatDialog = dialog;
                        loadDialogUsers();
                        DialogsManager.getInstance().sendSystemMessageAboutUpdatingDialog(ConnectycubeChatService.getInstance().getSystemMessagesManager(), dialog);
                    }

                    @Override
                    public void onError(ResponseException e) {
                        showErrorSnackbar(R.string.chat_info_add_people_error, e,
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        updateDialog(selectedUsers);
                                    }
                                });
                    }
                }
        );
    }

    private void loadDialogUsers() {
        ChatHelper.getInstance().getUsersFromDialog(chatDialog, new EntityCallback<ArrayList<ConnectycubeUser>>() {
            @Override
            public void onSuccess(ArrayList<ConnectycubeUser> users, Bundle bundle) {
                setChatNameToActionBar();
                loadChatHistory();
            }

            @Override
            public void onError(ResponseException e) {
                showErrorSnackbar(R.string.chat_load_users_error, e,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                loadDialogUsers();
                            }
                        });
            }
        });
    }

    private void setChatNameToActionBar() {
        String chatName = ChatDialogUtils.getDialogName(chatDialog);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(chatName);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setHomeButtonEnabled(true);
        }
    }

    private void updateChatHistory() {
        progressBar.setVisibility(View.VISIBLE);
        loadChatHistory();
    }

    private void loadChatHistory() {
        ChatHelper.getInstance().loadChatHistory(chatDialog, skipPagination, new EntityCallback<ArrayList<ConnectycubeChatMessage>>() {
            @Override
            public void onSuccess(ArrayList<ConnectycubeChatMessage> messages, Bundle args) {
                // The newest messages should be in the end of list,
                // so we need to reverse list to show messages in the right order
                Collections.reverse(messages);
                if (!checkAdapterInit) {
                    checkAdapterInit = true;
                    chatAdapter.addList(messages);
                    addDelayedMessagesToAdapter();
                } else {
                    chatAdapter.addToList(messages);
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(ResponseException e) {
                progressBar.setVisibility(View.GONE);
                skipPagination -= ChatHelper.CHAT_HISTORY_ITEMS_PER_PAGE;
                snackbar = showErrorSnackbar(R.string.connection_error, e, null);
            }
        });
        skipPagination += ChatHelper.CHAT_HISTORY_ITEMS_PER_PAGE;
    }

    private void addDelayedMessagesToAdapter() {
        if (unShownMessages != null && !unShownMessages.isEmpty()) {
            List<ConnectycubeChatMessage> chatList = chatAdapter.getList();
            for (ConnectycubeChatMessage message : unShownMessages) {
                if (!chatList.contains(message)) {
                    chatAdapter.add(message);
                }
            }
        }
    }

    private void scrollMessageListDown() {
        chatMessagesRecyclerView.scrollToPosition(messagesList.size() - 1);
    }

    private void deleteChat() {
        ChatHelper.getInstance().deleteDialog(chatDialog, new EntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(ResponseException e) {
                showErrorSnackbar(R.string.dialogs_deletion_error, e,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                deleteChat();
                            }
                        });
            }
        });
    }

    @Override
    protected ConnectionListener initChatConnectionListener() {
        return new VerboseChatConnectionListener(getSnackbarAnchorView()) {
            @Override
            public void reconnectionSuccessful() {
                super.reconnectionSuccessful();
                skipPagination = 0;
                checkAdapterInit = false;
                switch (chatDialog.getType()) {
                    case GROUP:
                        // Join active room if we're in Group Chat
                        joinGroupChat();
                        break;
                    case PRIVATE:
                        updateChatHistory();
                        break;
                }
            }
        };
    }

    private void addChatMessagesAdapterListeners() {
        chatAdapter.setAttachImageClickListener(imageAttachClickListener);
    }

    private void removeChatMessagesAdapterListeners() {
        chatAdapter.removeAttachImageClickListener(imageAttachClickListener);
    }

    private class ChatMessageListener extends ChatDialogMessageListenerImp {
        @Override
        public void processMessage(String s, ConnectycubeChatMessage chatMessage, Integer integer) {
            showMessage(chatMessage);
        }
    }

    private class ImageAttachClickListener implements ChatAttachClickListener {

        @Override
        public void onItemClicked(ConnectycubeAttachment attachment, int position) {
            AttachmentImageActivity.start(ChatActivity.this, attachment.getUrl());
        }
    }

    private class PaginationListener implements PaginationHistoryListener {

        @Override
        public void downloadMore() {
            Log.w(TAG, "downloadMore");
            loadChatHistory();
        }
    }
}
