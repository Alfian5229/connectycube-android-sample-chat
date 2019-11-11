package com.connectycube.sample.chat.utils.imagepicker;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.connectycube.sample.chat.App;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class GetFilepathFromUriTask extends AsyncTask<Intent, Void, File> {
    private static final String SCHEME_CONTENT = "content";
    private static final String SCHEME_CONTENT_GOOGLE = "content://com.google.android";
    private static final String SCHEME_FILE = "file";

    private WeakReference<Context> ctxWeakReference;
    private OnImagePickedListener listener;
    private int requestCode;
    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private boolean isExceptionOccurred;
    private ProgressDialog progressDialog;

    GetFilepathFromUriTask(Context context, OnImagePickedListener listener, int requestCode) {
        this.ctxWeakReference = new WeakReference<>(context);
        this.listener = listener;
        this.requestCode = requestCode;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        showProgressDialog();
    }

    @Override
    protected final File doInBackground(Intent... params) {
        try {
            return performInBackground(params);
        } catch (final Exception e) {
            isExceptionOccurred = true;
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    onException(e);
                }
            });
            return null;
        }
    }

    public File performInBackground(Intent... params) throws Exception {
        Intent data = params[0];

        String imageFilePath = null;
        Uri uri = data.getData();
        String uriScheme = uri.getScheme();

        boolean isFromGoogleApp = uri.toString().startsWith(SCHEME_CONTENT_GOOGLE);
        boolean isKitKatAndUpper = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        if (SCHEME_CONTENT.equalsIgnoreCase(uriScheme) && !isFromGoogleApp && !isKitKatAndUpper) {
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = App.getInstance().getContentResolver().query(uri, filePathColumn, null, null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    imageFilePath = cursor.getString(columnIndex);
                }
                cursor.close();
            }
        } else if (SCHEME_FILE.equalsIgnoreCase(uriScheme)) {
            imageFilePath = uri.getPath();
        } else {
            imageFilePath = ImageUtils.saveUriToFile(uri);
        }

        if (TextUtils.isEmpty(imageFilePath)) {
            throw new IOException("Can't find a filepath for URI " + uri.toString());
        }

        return new File(imageFilePath);
    }

    @Override
    protected final void onPostExecute(File result) {
        if (!isExceptionOccurred) {
            onResult(result);
        }
    }

    public void onResult(File file) {
        hideProgressDialog();
        Log.w(GetFilepathFromUriTask.class.getSimpleName(), "onResult listener = " + listener);
        if (listener != null) {
            listener.onImagePicked(requestCode, file);
        }
    }

    public void onException(Exception e) {
        hideProgressDialog();
        Log.w(GetFilepathFromUriTask.class.getSimpleName(), "onException listener = " + listener);
        if (listener != null) {
            listener.onImagePickError(requestCode, e);
        }
    }

    protected void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    protected void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(ctxWeakReference.get());
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);

            // Disable the back button
            DialogInterface.OnKeyListener keyListener = new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    return keyCode == KeyEvent.KEYCODE_BACK;
                }
            };
            progressDialog.setOnKeyListener(keyListener);
        }

        progressDialog.show();

    }
}