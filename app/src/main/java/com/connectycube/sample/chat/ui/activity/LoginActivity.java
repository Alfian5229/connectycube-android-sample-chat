package com.connectycube.sample.chat.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.connectycube.auth.session.ConnectycubeSessionManager;
import com.connectycube.core.EntityCallback;
import com.connectycube.core.exception.ResponseException;
import com.connectycube.sample.chat.R;
import com.connectycube.sample.chat.ui.adapter.UsersAdapter;
import com.connectycube.sample.chat.utils.SharedPrefsHelper;
import com.connectycube.sample.chat.utils.chat.ChatHelper;
import com.connectycube.users.model.ConnectycubeUser;

import java.util.List;

import static com.connectycube.sample.chat.utils.Consts.EXTRA_FCM_DIALOG_ID;
import static com.connectycube.sample.chat.utils.Consts.SAMPLE_USER_CONFIG;
import static com.connectycube.sample.chat.utils.configs.UserConfig.getAllUsersFromFile;

public class LoginActivity extends BaseActivity {

    private List<ConnectycubeUser> users;
    private String dialogIdFromPush;

    public static void start(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        dialogIdFromPush = getIntent().getStringExtra(EXTRA_FCM_DIALOG_ID);
        performLoginAction();
    }

    private void initUserAdapter() {
        ListView userListView = findViewById(R.id.list_login_users);

        TextView listHeader = (TextView) LayoutInflater.from(this)
                .inflate(R.layout.include_list_hint_header, userListView, false);
        listHeader.setText(R.string.login_select_user_for_login);

        userListView.addHeaderView(listHeader, null, false);
        userListView.setOnItemClickListener(new OnUserLoginItemClickListener());
        UsersAdapter adapter = new UsersAdapter(LoginActivity.this, users);
        userListView.setAdapter(adapter);
    }

    protected void performLoginAction() {
        if (checkSignIn()) {
            restoreChatSession();
        } else {
            proceedLogin();
        }
    }

    protected void proceedLogin() {
        initUsers();
        initUserAdapter();
    }

    protected boolean checkSignIn() {
        return SharedPrefsHelper.getInstance().hasUser();
    }

    private void restoreChatSession() {
        if (ChatHelper.getInstance().isLogged()) {
            DialogsActivity.start(this, dialogIdFromPush);
            finish();
        } else {
            ConnectycubeUser currentUser = getUserFromSession();
            login(currentUser);
        }
    }

    private ConnectycubeUser getUserFromSession() {
        ConnectycubeUser user = SharedPrefsHelper.getInstance().getUser();
        user.setId(ConnectycubeSessionManager.getInstance().getSessionParameters().getUserId());
        return user;
    }

    @Override
    protected View getSnackbarAnchorView() {
        return findViewById(R.id.list_login_users);
    }

    private void initUsers() {
        users = getAllUsersFromFile(SAMPLE_USER_CONFIG, this);
    }

    private void login(final ConnectycubeUser user) {
        showProgressDialog(R.string.dlg_login);
        ChatHelper.getInstance().login(user, new EntityCallback<Void>() {
            @Override
            public void onSuccess(Void result, Bundle bundle) {
                SharedPrefsHelper.getInstance().saveUser(user);
                DialogsActivity.start(LoginActivity.this, dialogIdFromPush);
                finish();

                hideProgressDialog();
            }

            @Override
            public void onError(ResponseException e) {
                hideProgressDialog();
                showErrorSnackbar(R.string.login_chat_login_error, e, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        login(user);
                    }
                });
            }
        });
    }

    private class OnUserLoginItemClickListener implements AdapterView.OnItemClickListener {

        public static final int LIST_HEADER_POSITION = 0;

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (position == LIST_HEADER_POSITION) {
                return;
            }

            final ConnectycubeUser user = (ConnectycubeUser) parent.getItemAtPosition(position);
            login(user);
        }
    }
}