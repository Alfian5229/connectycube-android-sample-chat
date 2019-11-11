package com.connectycube.sample.chat.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.connectycube.chat.ConnectycubeChatService;
import com.connectycube.chat.model.ConnectycubeChatDialog;
import com.connectycube.core.EntityCallback;
import com.connectycube.core.exception.ResponseException;
import com.connectycube.sample.chat.R;
import com.connectycube.sample.chat.ui.adapter.CheckboxUsersAdapter;
import com.connectycube.sample.chat.utils.holder.DialogHolder;
import com.connectycube.users.ConnectycubeUsers;
import com.connectycube.users.model.ConnectycubeUser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.connectycube.sample.chat.ui.activity.ChatActivity.EXTRA_DIALOG_ID;
import static com.connectycube.sample.chat.utils.Consts.SAMPLE_USER_CONFIG;
import static com.connectycube.sample.chat.utils.configs.UserConfig.getAllUsersFromFile;

public class SelectUsersActivity extends BaseActivity {
    public static final String EXTRA_USERS = "users";
    public static final int MINIMUM_CHAT_OCCUPANTS_SIZE = 2;
    private static final long CLICK_DELAY = TimeUnit.SECONDS.toMillis(2);

    private ListView usersListView;

    private CheckboxUsersAdapter usersAdapter;
    private long lastClickTime = 0L;

    public static void start(Context context) {
        Intent intent = new Intent(context, SelectUsersActivity.class);
        context.startActivity(intent);
    }

    /**
     * Start activity for picking users
     *
     * @param activity activity to return result
     * @param code     request code for onActivityResult() method
     *                 <p>
     *                 in onActivityResult there will be 'ArrayList<ConnectycubeUser>' in the intent extras
     *                 which can be obtained with SelectPeopleActivity.EXTRA_USERS key
     */
    public static void startForResult(Activity activity, int code) {
        startForResult(activity, code, null);
    }

    public static void startForResult(Activity activity, int code, String dialogId) {
        Intent intent = new Intent(activity, SelectUsersActivity.class);
        intent.putExtra(EXTRA_DIALOG_ID, dialogId);
        activity.startActivityForResult(intent, code);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_users);

        usersListView = findViewById(R.id.list_select_users);

        TextView listHeader = (TextView) LayoutInflater.from(this)
                .inflate(R.layout.include_list_hint_header, usersListView, false);
        listHeader.setText(R.string.select_users_list_hint);
        usersListView.addHeaderView(listHeader, null, false);

        if (isEditingChat()) {
            setActionBarTitle(R.string.select_users_edit_chat);
        } else {
            setActionBarTitle(R.string.select_users_create_chat);
        }
        actionBar.setDisplayHomeAsUpEnabled(true);
        loadUsers();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_select_users, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if ((SystemClock.uptimeMillis() - lastClickTime) < CLICK_DELAY) {
            return super.onOptionsItemSelected(item);
        }
        lastClickTime = SystemClock.uptimeMillis();

        switch (item.getItemId()) {
            case R.id.menu_select_people_action_done:
                if (usersAdapter != null) {
                    List<ConnectycubeUser> users = new ArrayList<>(usersAdapter.getSelectedUsers());
                    if (users.size() >= MINIMUM_CHAT_OCCUPANTS_SIZE) {
                        passResultToCallerActivity();
                    } else {
                        Toast.makeText(this, R.string.select_users_choose_users, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected View getSnackbarAnchorView() {
        return findViewById(R.id.layout_root);
    }

    private void passResultToCallerActivity() {
        Intent result = new Intent();
        ArrayList<ConnectycubeUser> selectedUsers = new ArrayList<>(usersAdapter.getSelectedUsers());
        result.putExtra(EXTRA_USERS, selectedUsers);
        setResult(RESULT_OK, result);
        finish();
    }

    private void loadUsers() {
        showProgressDialog(R.string.dlg_loading_opponents);
        ArrayList<String> usersLogins = new ArrayList<>();
        List<ConnectycubeUser> users = getAllUsersFromFile(SAMPLE_USER_CONFIG, this);
        for (ConnectycubeUser user : users) {
            usersLogins.add(user.getLogin());
        }

        ConnectycubeUsers.getUsersByLogins(usersLogins, null).performAsync(new EntityCallback<ArrayList<ConnectycubeUser>>() {
            @Override
            public void onSuccess(ArrayList<ConnectycubeUser> users, Bundle bundle) {
                hideProgressDialog();
                initCurrentUserFullNameIfExist(users);
                initAdapter(users);
            }

            @Override
            public void onError(ResponseException e) {
                hideProgressDialog();
                Toast.makeText(getApplicationContext(), getString(R.string.loading_users_error, e.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initCurrentUserFullNameIfExist(ArrayList<ConnectycubeUser> users) {
        ConnectycubeUser currentUser = ConnectycubeChatService.getInstance().getUser();
        for (ConnectycubeUser user : users) {
            if (currentUser.getId().equals(user.getId())) {
                currentUser.setFullName(user.getFullName());
                break;
            }
        }
    }

    private void initAdapter(ArrayList<ConnectycubeUser> users) {
        String dialogId = getIntent().getStringExtra(EXTRA_DIALOG_ID);

        usersAdapter = new CheckboxUsersAdapter(SelectUsersActivity.this, users);
        if (dialogId != null) {
            ConnectycubeChatDialog dialog = DialogHolder.getInstance().getChatDialogById(dialogId);
            if (dialog != null) {
                usersAdapter.addSelectedUsers(dialog.getOccupants());
            }
        }
        usersListView.setAdapter(usersAdapter);
    }

    private boolean isEditingChat() {
        return getIntent().getSerializableExtra(EXTRA_DIALOG_ID) != null;
    }
}
