package com.github.oliveiradev.lib;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import com.github.oliveiradev.lib.shared.Constants;
import com.github.oliveiradev.lib.shared.TypeRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by felipe on 03/05/16.
 */
public class OverlapActivity extends Activity {

    private final static String FILE_URI_EXTRA = "FILE_URI";

    private Uri fileUri;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        TypeRequest typeRequest = (TypeRequest) intent.getExtras().get(Constants.REQUEST_TYPE_EXTRA);
        if (typeRequest == TypeRequest.GALLERY)
            gallery();
        else
            camera();
    }

    private void gallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, Constants.REQUEST_CODE_ATTACH_IMAGE);
    }

    private void camera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            fileUri = createImageUri();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            startActivityForResult(takePictureIntent, Constants.REQUEST_CODE_TAKE_PICURE);
        }

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

    private Uri getUri(int requestCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE_ATTACH_IMAGE) return data.getData();
        else if (requestCode == Constants.REQUEST_CODE_TAKE_PICURE) return fileUri;
        else return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && (requestCode == Constants.REQUEST_CODE_ATTACH_IMAGE || requestCode == Constants.REQUEST_CODE_TAKE_PICURE))
            RxPhoto.onActivityResult(getUri(requestCode, data));
        finish();
    }
}
