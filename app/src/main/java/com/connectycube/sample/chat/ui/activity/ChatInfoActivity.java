package com.connectycube.sample.chat.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.connectycube.chat.model.ConnectycubeChatDialog;
import com.connectycube.sample.chat.R;
import com.connectycube.sample.chat.ui.adapter.UsersAdapter;
import com.connectycube.sample.chat.utils.holder.DialogHolder;
import com.connectycube.sample.chat.utils.holder.UsersHolder;
import com.connectycube.users.model.ConnectycubeUser;

import java.util.List;

import static com.connectycube.sample.chat.ui.activity.ChatActivity.EXTRA_DIALOG_ID;

public class ChatInfoActivity extends BaseActivity {

    private ListView usersListView;
    private ConnectycubeChatDialog dialog;

    public static void start(Context context, String dialogId) {
        Intent intent = new Intent(context, ChatInfoActivity.class);
        intent.putExtra(EXTRA_DIALOG_ID, dialogId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        String dialogId = getIntent().getStringExtra(EXTRA_DIALOG_ID);
        dialog = DialogHolder.getInstance().getChatDialogById(dialogId);
        usersListView = findViewById(R.id.list_login_users);

        actionBar.setDisplayHomeAsUpEnabled(true);

        buildUserList();
    }

    @Override
    protected View getSnackbarAnchorView() {
        return usersListView;
    }

    private void buildUserList() {
        List<Integer> userIds = dialog.getOccupants();
        List<ConnectycubeUser> users = UsersHolder.getInstance().getUsersByIds(userIds);

        UsersAdapter adapter = new UsersAdapter(this, users);
        usersListView.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
