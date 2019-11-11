package com.connectycube.sample.chat.ui.adapter;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.connectycube.chat.model.ConnectycubeAttachment;
import com.connectycube.core.ConnectycubeProgressCallback;
import com.connectycube.core.EntityCallback;
import com.connectycube.core.exception.ResponseException;
import com.connectycube.sample.chat.App;
import com.connectycube.sample.chat.R;
import com.connectycube.sample.chat.utils.ResourceUtils;
import com.connectycube.sample.chat.utils.chat.ChatHelper;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class AttachmentPreviewAdapter extends BaseListAdapter<File> {

    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private Map<File, ConnectycubeAttachment> fileConnectycubeAttachmentMap;
    private Map<File, Integer> fileUploadProgressMap;

    private OnAttachmentCountChangedListener onAttachmentCountChangedListener;
    private OnAttachmentUploadErrorListener onAttachmentUploadErrorListener;

    public AttachmentPreviewAdapter(Context context,
                                    OnAttachmentCountChangedListener countChangedListener,
                                    OnAttachmentUploadErrorListener errorListener) {
        super(context);
        fileConnectycubeAttachmentMap = Collections.synchronizedMap(new HashMap<File, ConnectycubeAttachment>());
        fileUploadProgressMap = Collections.synchronizedMap(new HashMap<File, Integer>());
        onAttachmentCountChangedListener = countChangedListener;
        onAttachmentUploadErrorListener = errorListener;
    }

    @Override
    public void add(final File item) {
        fileUploadProgressMap.put(item, 1);
        ChatHelper.getInstance().loadFileAsAttachment(item, new EntityCallback<ConnectycubeAttachment>() {
            @Override
            public void onSuccess(ConnectycubeAttachment result, Bundle params) {
                fileUploadProgressMap.remove(item);
                fileConnectycubeAttachmentMap.put(item, result);
                notifyDataSetChanged();
            }

            @Override
            public void onError(ResponseException e) {
                onAttachmentUploadErrorListener.onAttachmentUploadError(e);
                remove(item);
            }
        }, new ConnectycubeProgressCallback() {
            @Override
            public void onProgressUpdate(final int progress) {
                fileUploadProgressMap.put(item, progress);
                mainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        });

        super.add(item);
        onAttachmentCountChangedListener.onAttachmentCountChanged(getCount());
    }

    @Override
    public void remove(File item) {
        fileUploadProgressMap.remove(item);
        fileConnectycubeAttachmentMap.remove(item);

        super.remove(item);
        onAttachmentCountChangedListener.onAttachmentCountChanged(getCount());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.item_attachment_preview, parent, false);
            holder.attachmentImageView = (ImageView) convertView.findViewById(R.id.image_attachment_preview);
            holder.progressBar = (ProgressBar) convertView.findViewById(R.id.progress_attachment_preview);
            holder.deleteButton = (ImageButton) convertView.findViewById(R.id.button_attachment_preview_delete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final File attachmentFile = getItem(position);
        Glide.with(App.getInstance())
                .load(attachmentFile)
                .override(ResourceUtils.getDimen(R.dimen.chat_attachment_preview_size),
                        ResourceUtils.getDimen(R.dimen.chat_attachment_preview_size))
                .into(holder.attachmentImageView);

        if (isFileUploading(attachmentFile)) {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.deleteButton.setVisibility(View.GONE);
            holder.deleteButton.setOnClickListener(null);

            int progress = fileUploadProgressMap.get(attachmentFile);
            holder.progressBar.setProgress(progress);
        } else {
            holder.progressBar.setVisibility(View.GONE);
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    remove(attachmentFile);
                }
            });
        }

        return convertView;
    }

    public void remove(ConnectycubeAttachment attachment) {
        if (fileConnectycubeAttachmentMap.containsValue(attachment)) {
            for (File file : fileConnectycubeAttachmentMap.keySet()) {
                ConnectycubeAttachment attachmentFromMap = fileConnectycubeAttachmentMap.get(file);
                if (attachmentFromMap.equals(attachment)) {
                    remove(file);
                    break;
                }
            }
        }
    }

    public Collection<ConnectycubeAttachment> getUploadedAttachments() {
        return new HashSet<>(fileConnectycubeAttachmentMap.values());
    }

    private boolean isFileUploading(File attachmentFile) {
        return fileUploadProgressMap.containsKey(attachmentFile) && !fileConnectycubeAttachmentMap.containsKey(attachmentFile);
    }

    private static class ViewHolder {
        ImageView attachmentImageView;
        ProgressBar progressBar;
        ImageButton deleteButton;
    }

    public interface OnAttachmentCountChangedListener {
        void onAttachmentCountChanged(int count);
    }

    public interface OnAttachmentUploadErrorListener {
        void onAttachmentUploadError(ResponseException e);
    }
}
