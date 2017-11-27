package com.github.oliveiradev.lib;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.BundleCompat;

import com.github.oliveiradev.lib.exceptions.CancelOperationException;
import com.github.oliveiradev.lib.exceptions.ExternalStorageWriteException;
import com.github.oliveiradev.lib.exceptions.NotPermissionException;
import com.github.oliveiradev.lib.shared.Constants;
import com.github.oliveiradev.lib.shared.TypeRequest;
import com.github.oliveiradev.lib.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by felipe on 03/05/16.
 */
public class OverlapActivity extends Activity {

    private final static String FILE_URI_EXTRA = "FILE_URI";
    private static final int REQUEST_GALLERY = 1;

    private Uri fileUri;
    private TypeRequest typeRequest;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }
    }

    private Rx2Photo rx2Photo = null;

    private static class Rx2PhotoBinder extends Binder {

        final Rx2Photo rx2Photo;

        private Rx2PhotoBinder(Rx2Photo rx2Photo) {
            this.rx2Photo = rx2Photo;
        }
    }

    public static Intent newIntent(final Context context,  final TypeRequest typeRequest, final Rx2Photo caller) {
        Intent intent = new Intent(context, OverlapActivity.class);
        intent.putExtra(Constants.REQUEST_TYPE_EXTRA, typeRequest);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final Bundle bundle = new Bundle();
        BundleCompat.putBinder(bundle, Constants.CALLER_EXTRA, new Rx2PhotoBinder(caller));
        intent.putExtra(Constants.CALLER_EXTRA, bundle);
        return intent;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        typeRequest = (TypeRequest) intent.getExtras().get(Constants.REQUEST_TYPE_EXTRA);
        final Bundle bundle = intent.getExtras().getBundle(Constants.CALLER_EXTRA);
        if (bundle != null) {
            IBinder iBinder = BundleCompat.getBinder(bundle, Constants.CALLER_EXTRA);
            if (iBinder instanceof Rx2PhotoBinder) {
                Rx2PhotoBinder binder = (Rx2PhotoBinder) iBinder;
                rx2Photo = binder.rx2Photo;
            }
        }

        if (hasPermission()) {
            switch (typeRequest) {
                case GALLERY:
                    gallery();
                    break;
                case CAMERA:
                    camera();
                    break;
                case COMBINE:
                    combine(false);
                    break;
                case COMBINE_MULTIPLE:
                    combine(true);
                    break;
            }
        } else {
            requestPermission();
        }
    }

    private void combine(boolean isMultiple) {
        if (!Utils.isExternalStorageWritable()) {
            rx2Photo.propagateThrowable(new ExternalStorageWriteException());
            return;
        }

        fileUri = createImageUri();
        List<Intent> intentList = new ArrayList<>();
        Intent chooserIntent = null;
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            pickIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, isMultiple);
        }
        intentList = Utils.addIntentsToList(this, intentList, pickIntent);
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        intentList = Utils.addIntentsToList(this, intentList, takePhotoIntent);
        if (!intentList.isEmpty()) {
            String title = rx2Photo.getTitle() != null ? rx2Photo.getTitle() : getString(R.string.picker_header);
            chooserIntent = Intent.createChooser(intentList.remove(intentList.size() - 1), title);
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentList.toArray(new Parcelable[]{}));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && isMultiple) {
            startActivityForResult(chooserIntent, Constants.REQUEST_CODE_COMBINE_MULPITPLE);
        } else {
            startActivityForResult(chooserIntent, Constants.REQUEST_CODE_COMBINE);
        }
    }

    private void gallery() {
        /*Intent intent = new Intent();
        intent.setType("image*//*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);*/
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, Constants.REQUEST_CODE_ATTACH_IMAGE);
    }

    private void camera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            if (!Utils.isExternalStorageWritable()) {
                rx2Photo.propagateThrowable(new ExternalStorageWriteException());
                return;
            }
            fileUri = createImageUri();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            startActivityForResult(takePictureIntent, Constants.REQUEST_CODE_TAKE_PICURE);
        }
    }

    private boolean hasPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_GALLERY);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            rx2Photo.propagateThrowable(new NotPermissionException(NotPermissionException.RequestEnum.GALLERY));
            finish();
            return;
        }

        switch (typeRequest) {
            case CAMERA:
                camera();
                break;
            case GALLERY:
                gallery();
                break;
            case COMBINE:
                combine(false);
                break;
            case COMBINE_MULTIPLE:
                combine(true);
                break;
        }
    }

    /**
     * If we not choose camera, temp file is unused and must be removed
     */
    private void removeUnusedFile() {
        if (fileUri != null)
            getContentResolver().delete(fileUri, null, null);
    }

    private Uri createImageUri() {
        ContentResolver contentResolver = getContentResolver();
        ContentValues cv = new ContentValues();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        cv.put(MediaStore.Images.Media.TITLE, timeStamp);
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(FILE_URI_EXTRA, fileUri);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        fileUri = savedInstanceState.getParcelable(FILE_URI_EXTRA);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (typeRequest) {
                case COMBINE_MULTIPLE:
                    if (data != null && data.getClipData() != null) {
                        ClipData mClipData = data.getClipData();
                        List<Uri> uris = new ArrayList<>();

                        for (int i = 0; i < mClipData.getItemCount(); i++) {
                            uris.add(mClipData.getItemAt(i).getUri());
                        }

                        rx2Photo.onActivityResult(uris);

                        removeUnusedFile();
                    } else if (data != null && data.getData() != null) {
                        List<Uri> uris = new ArrayList<>();
                        uris.add(data.getData());
                        rx2Photo.onActivityResult(uris);

                        removeUnusedFile();
                    } else {
                        rx2Photo.onActivityResult(fileUri);
                    }
                    break;
                case CAMERA:
                    rx2Photo.onActivityResult(fileUri);
                    break;
                case GALLERY:
                case COMBINE:
                    if (data != null && data.getData() != null) {
                        rx2Photo.onActivityResult(data.getData());
                        removeUnusedFile();
                    } else {
                        rx2Photo.onActivityResult(fileUri);
                    }
                    break;
            }
        } else {
            rx2Photo.propagateThrowable(new CancelOperationException(typeRequest));
        }

        finish();
    }
}
