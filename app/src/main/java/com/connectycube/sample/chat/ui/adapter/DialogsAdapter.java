package com.connectycube.sample.chat.ui.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.connectycube.chat.model.ConnectycubeChatDialog;
import com.connectycube.chat.model.ConnectycubeDialogType;
import com.connectycube.sample.chat.R;
import com.connectycube.sample.chat.utils.ResourceUtils;
import com.connectycube.sample.chat.utils.chat.ChatDialogUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DialogsAdapter extends BaseListAdapter<ConnectycubeChatDialog> {

    protected List<ConnectycubeChatDialog> selectedItems;

    public DialogsAdapter(Context context, List<ConnectycubeChatDialog> dialogs) {
        super(context, dialogs);
        selectedItems = new ArrayList<>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_dialog, parent, false);

            holder = new ViewHolder();
            holder.rootLayout = convertView.findViewById(R.id.root);
            holder.nameTextView = convertView.findViewById(R.id.text_dialog_name);
            holder.lastMessageTextView = convertView.findViewById(R.id.text_dialog_last_message);
            holder.dialogImageView = convertView.findViewById(R.id.image_dialog_icon);
            holder.unreadCounterTextView = convertView.findViewById(R.id.text_dialog_unread_count);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ConnectycubeChatDialog dialog = getItem(position);
        if (dialog.getType().equals(ConnectycubeDialogType.GROUP)) {
            holder.dialogImageView.setBackgroundDrawable(ResourceUtils.getGreyCircleDrawable());
            holder.dialogImageView.setImageResource(R.drawable.ic_chat_group);
        } else {
            holder.dialogImageView.setBackgroundDrawable(ResourceUtils.getColorCircleDrawable(position));
            holder.dialogImageView.setImageDrawable(null);
        }

        holder.nameTextView.setText(ChatDialogUtils.getDialogName(dialog));
        holder.lastMessageTextView.setText(dialog.getLastMessage());

        int unreadMessagesCount = getUnreadMsgCount(dialog);
        if (unreadMessagesCount == 0) {
            holder.unreadCounterTextView.setVisibility(View.GONE);
        } else {
            holder.unreadCounterTextView.setVisibility(View.VISIBLE);
            holder.unreadCounterTextView.setText(String.valueOf(unreadMessagesCount > 99 ? "99+" : unreadMessagesCount));
        }

        holder.rootLayout.setBackgroundColor(isItemSelected(position) ? ResourceUtils.getColor(R.color.selected_list_item_color) :
                ResourceUtils.getColor(android.R.color.transparent));

        return convertView;
    }

    private int getUnreadMsgCount(ConnectycubeChatDialog chatDialog) {
        Integer unreadMessageCount = chatDialog.getUnreadMessageCount();
        if (unreadMessageCount == null) {
            return 0;
        } else {
            return unreadMessageCount;
        }
    }

    public void toggleSelection(int position) {
        ConnectycubeChatDialog item = getItem(position);
        toggleSelection(item);
    }

    public void toggleSelection(ConnectycubeChatDialog item) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.add(item);
        }
        notifyDataSetChanged();
    }

    public void selectItem(int position) {
        ConnectycubeChatDialog item = getItem(position);
        selectItem(item);
    }

    public void selectItem(ConnectycubeChatDialog item) {
        if (selectedItems.contains(item)) {
            return;
        }
        selectedItems.add(item);
        notifyDataSetChanged();
    }

    public Collection<ConnectycubeChatDialog> getSelectedItems() {
        return selectedItems;
    }

    protected boolean isItemSelected(int position) {
        return !selectedItems.isEmpty() && selectedItems.contains(getItem(position));
    }

    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        ViewGroup rootLayout;
        ImageView dialogImageView;
        TextView nameTextView;
        TextView lastMessageTextView;
        TextView unreadCounterTextView;
    }
}
