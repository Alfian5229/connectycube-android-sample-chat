package com.connectycube.sample.chat.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.connectycube.chat.ConnectycubeChatService;
import com.connectycube.sample.chat.R;
import com.connectycube.sample.chat.utils.ResourceUtils;
import com.connectycube.users.model.ConnectycubeUser;

import java.util.List;

public class UsersAdapter extends BaseListAdapter<ConnectycubeUser> {

    protected ConnectycubeUser currentUser;

    public UsersAdapter(Context context, List<ConnectycubeUser> users) {
        super(context, users);
        currentUser = ConnectycubeChatService.getInstance().getUser();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ConnectycubeUser user = getItem(position);

        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_user, parent, false);
            holder = new ViewHolder();
            holder.userImageView = convertView.findViewById(R.id.image_user);
            holder.loginTextView = convertView.findViewById(R.id.text_user_login);
            holder.userCheckBox = convertView.findViewById(R.id.checkbox_user);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (isUserMe(user)) {
            holder.loginTextView.setText(context.getString(R.string.placeholder_username_you, user.getLogin()));
        } else {
            holder.loginTextView.setText(user.getLogin());
        }

        if (isAvailableForSelection(user)) {
            holder.loginTextView.setTextColor(ResourceUtils.getColor(R.color.text_color_black));
        } else {
            holder.loginTextView.setTextColor(ResourceUtils.getColor(R.color.text_color_medium_grey));
        }

        holder.userImageView.setBackgroundDrawable(ResourceUtils.getColorCircleDrawable(position));
        holder.userCheckBox.setVisibility(View.GONE);

        return convertView;
    }

    protected boolean isUserMe(ConnectycubeUser user) {
        return currentUser != null && currentUser.getId().equals(user.getId());
    }

    protected boolean isAvailableForSelection(ConnectycubeUser user) {
        return currentUser == null || !currentUser.getId().equals(user.getId());
    }

    protected static class ViewHolder {
        ImageView userImageView;
        TextView loginTextView;
        CheckBox userCheckBox;
    }
}
