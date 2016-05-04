package com.github.oliveiradev.lib;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by felipe on 03/05/16.
 */
public class OverlapActivity extends Activity {

    private Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        TypeRequest typeRequest = (TypeRequest) intent.getExtras().get("enum");
        if (typeRequest == TypeRequest.GALLERY)
            gallery();
        else
            camera();
    }

    public void gallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, Constants.REQUEST_CODE_ATTACH_IMAGE);
    }

    private void camera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageTempFile();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            fileUri = Uri.fromFile(photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            startActivityForResult(takePictureIntent, Constants.REQUEST_CODE_TAKE_PICURE);
        }
    }


    @SuppressLint("SimpleDateFormat")
    private File createImageTempFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
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
