package com.github.oliveiradev.lib;

import android.Manifest;
import android.app.Activity;
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
import android.provider.MediaStore;
import android.support.v4.app.BundleCompat;
import android.widget.Toast;

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
    private static final int REQUEST_CAMERA = 2;
    private static final int REQUEST_GALLERY = 1;

    private Uri fileUri;

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
        TypeRequest typeRequest = (TypeRequest) intent.getExtras().get(Constants.REQUEST_TYPE_EXTRA);
        final Bundle bundle = intent.getExtras().getBundle(Constants.CALLER_EXTRA);
        if (bundle != null) {
            IBinder iBinder = BundleCompat.getBinder(bundle, Constants.CALLER_EXTRA);
            if (iBinder instanceof Rx2PhotoBinder) {
                Rx2PhotoBinder binder = (Rx2PhotoBinder) iBinder;
                rx2Photo = binder.rx2Photo;
            }
        }

        if(hasPermission(typeRequest)) {
            if (typeRequest == TypeRequest.GALLERY)
                gallery();
            else
                camera();
        }else {
            requestPermission(typeRequest);
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
            fileUri = createImageUri();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            startActivityForResult(takePictureIntent, Constants.REQUEST_CODE_TAKE_PICURE);
        }

    }

    private boolean hasPermission(TypeRequest typeRequest) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(typeRequest == TypeRequest.GALLERY){

                return checkSelfPermission( Manifest.permission.WRITE_EXTERNAL_STORAGE )
                        == PackageManager.PERMISSION_GRANTED;
            }else {
                return checkSelfPermission( Manifest.permission.WRITE_EXTERNAL_STORAGE )
                        == PackageManager.PERMISSION_GRANTED
                        &&  checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
            }
        }else {
            return true;
        }
    }

    private void requestPermission(TypeRequest typeRequest){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{typeRequest == TypeRequest.GALLERY ? Manifest.permission.WRITE_EXTERNAL_STORAGE : Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    typeRequest == TypeRequest.GALLERY ? REQUEST_GALLERY : REQUEST_CAMERA );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA:
                if(grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                    camera();

                }else {
                    Toast.makeText(this,R.string.error_camera_permission,Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case REQUEST_GALLERY:
                if(grantResults.length >= 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    gallery();

                }else {
                    Toast.makeText(this,R.string.error_gallery_permission,Toast.LENGTH_LONG).show();
                    finish();
                }
                break;


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
            rx2Photo.onActivityResult(getUri(requestCode, data));
        finish();
    }
}
