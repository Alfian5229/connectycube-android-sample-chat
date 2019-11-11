package com.connectycube.sample.chat.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.connectycube.chat.model.ConnectycubeAttachment;
import com.connectycube.chat.model.ConnectycubeChatDialog;
import com.connectycube.chat.model.ConnectycubeChatMessage;
import com.connectycube.core.helper.CollectionsUtil;
import com.connectycube.sample.chat.R;
import com.connectycube.sample.chat.utils.ResourceUtils;
import com.connectycube.sample.chat.utils.TimeUtils;
import com.connectycube.sample.chat.utils.callback.PaginationHistoryListener;
import com.connectycube.sample.chat.utils.chat.ChatHelper;
import com.connectycube.sample.chat.utils.holder.UsersHolder;
import com.connectycube.ui.chatmessage.adapter.ConnectycubeChatAdapter;
import com.connectycube.users.model.ConnectycubeUser;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.util.List;


public class ChatAdapter extends ConnectycubeChatAdapter<ConnectycubeChatMessage> implements StickyRecyclerHeadersAdapter<RecyclerView.ViewHolder> {
    private static final String TAG = ChatAdapter.class.getSimpleName();
    private final ConnectycubeChatDialog chatDialog;
    private PaginationHistoryListener paginationListener;
    private int previousGetCount = 0;

    public ChatAdapter(Context context, ConnectycubeChatDialog chatDialog, List<ConnectycubeChatMessage> chatMessages) {
        super(context, chatMessages);
        this.chatDialog = chatDialog;
    }

    public void addToList(List<ConnectycubeChatMessage> items) {
        chatMessages.addAll(0, items);
        notifyItemRangeInserted(0, items.size());
    }

    @Override
    public void add(ConnectycubeChatMessage item) {
        this.chatMessages.add(item);
        this.notifyItemInserted(chatMessages.size() - 1);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        downloadMore(position);
        ConnectycubeChatMessage chatMessage = getItem(position);
        if (isIncoming(chatMessage) && !isRead(chatMessage)) {
            readMessage(chatMessage);
        }
        super.onBindViewHolder(holder, position);
    }

    @Override
    public String getImageUrl(int position) {
        ConnectycubeAttachment attachment = getAttach(position);
        return attachment.getUrl();
    }

    @Override
    protected void onBindViewMsgLeftHolder(TextMessageHolder holder, ConnectycubeChatMessage chatMessage, int position) {
        holder.timeTextMessageTextView.setVisibility(View.GONE);

        TextView opponentNameTextView = holder.itemView.findViewById(R.id.opponent_name_text_view);
        opponentNameTextView.setTextColor(ResourceUtils.getRandomTextColorById(chatMessage.getSenderId()));
        opponentNameTextView.setText(getSenderName(chatMessage));

        TextView customMessageTimeTextView = holder.itemView.findViewById(R.id.custom_msg_text_time_message);
        customMessageTimeTextView.setText(getDate(chatMessage.getDateSent()));

        super.onBindViewMsgLeftHolder(holder, chatMessage, position);
    }

    @Override
    protected void onBindViewAttachLeftHolder(ImageAttachHolder holder, ConnectycubeChatMessage chatMessage, int position) {
        TextView opponentNameTextView = holder.itemView.findViewById(R.id.opponent_name_attach_view);
        opponentNameTextView.setTextColor(ResourceUtils.getRandomTextColorById(chatMessage.getSenderId()));
        opponentNameTextView.setText(getSenderName(chatMessage));

        super.onBindViewAttachLeftHolder(holder, chatMessage, position);
    }

    private String getSenderName(ConnectycubeChatMessage chatMessage) {
        ConnectycubeUser sender = UsersHolder.getInstance().getUserById(chatMessage.getSenderId());
        return sender.getLogin();
    }

    private void readMessage(ConnectycubeChatMessage chatMessage) {
        try {
            chatDialog.readMessage(chatMessage);
        } catch (XMPPException | SmackException.NotConnectedException e) {
            Log.w(TAG, e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean isRead(ConnectycubeChatMessage chatMessage) {
        Integer currentUserId = ChatHelper.getCurrentUser().getId();
        return !CollectionsUtil.isEmpty(chatMessage.getReadIds()) && chatMessage.getReadIds().contains(currentUserId);
    }

    public void setPaginationHistoryListener(PaginationHistoryListener paginationListener) {
        this.paginationListener = paginationListener;
    }

    private void downloadMore(int position) {
        if (position == 0) {
            if (getItemCount() != previousGetCount) {
                paginationListener.downloadMore();
                previousGetCount = getItemCount();
            }
        }
    }

    @Override
    public long getHeaderId(int position) {
        ConnectycubeChatMessage chatMessage = getItem(position);
        return TimeUtils.getDateAsHeaderId(chatMessage.getDateSent() * 1000);
    }

    @Override
    public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
        View view = inflater.inflate(R.layout.view_chat_message_header, parent, false);
        return new RecyclerView.ViewHolder(view) {
        };
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder, int position) {
        View view = holder.itemView;
        TextView dateTextView = view.findViewById(R.id.header_date_textview);

        ConnectycubeChatMessage chatMessage = getItem(position);
        dateTextView.setText(TimeUtils.getDate(chatMessage.getDateSent() * 1000));
    }

    protected RequestListener getRequestListener(MessageViewHolder holder, int position) {
        return new ImageLoadListener((ImageAttachHolder) holder, position);
    }

    protected class ImageLoadListener extends ConnectycubeChatAdapter.ImageLoadListener<String, GlideDrawable> {
        ImageButton refreshButton;

        ImageLoadListener(final ImageAttachHolder holder, final int position) {
            super(holder);
            refreshButton = holder.itemView.findViewById(R.id.refresh_attach_view);
            refreshButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    refreshButton.setVisibility(View.GONE);
                    displayAttachment(holder, position);
                }
            });
        }

        @Override
        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
            super.onException(e, model, target, isFirstResource);
            refreshButton.setVisibility(View.VISIBLE);
            return false;
        }

        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
            super.onResourceReady(resource, model, target, isFromMemoryCache, isFirstResource);
            refreshButton.setVisibility(View.GONE);
            return false;
        }
    }
}