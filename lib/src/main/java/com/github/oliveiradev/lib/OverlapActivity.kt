package com.github.oliveiradev.lib

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import android.support.v4.app.BundleCompat
import com.github.oliveiradev.lib.exceptions.CancelOperationException
import com.github.oliveiradev.lib.exceptions.ExternalStorageWriteException
import com.github.oliveiradev.lib.exceptions.NotPermissionException
import com.github.oliveiradev.lib.shared.Constants
import com.github.oliveiradev.lib.shared.TypeRequest
import com.github.oliveiradev.lib.util.Utils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Genius on 03.12.2017.
 */
open class OverlapActivity: Activity() {

    companion object {
        private const val FILE_URI_EXTRA = "FILE_URI"
        private const val REQUEST_GALLERY = 1
        private const val BUNDLE = "bundle"

        fun newIntent(context: Context, typeRequest: TypeRequest, caller: Rx2Photo): Intent {
            val intent = Intent(context, OverlapActivity::class.java)
            intent.putExtra(Constants.REQUEST_TYPE_EXTRA, typeRequest)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val bundle = Bundle()
            BundleCompat.putBinder(bundle, Constants.CALLER_EXTRA, Rx2PhotoBinder(caller))
            intent.putExtra(Constants.CALLER_EXTRA, bundle)
            return intent
        }
    }

    private var fileUri: Uri? = null
    private lateinit var typeRequest: TypeRequest
    private lateinit var rx2Photo: Rx2Photo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        typeRequest = intent.extras.get(Constants.REQUEST_TYPE_EXTRA) as TypeRequest
        val bundle = intent.extras.getBundle(Constants.CALLER_EXTRA)
        if (bundle != null) {
            val iBinder = BundleCompat.getBinder(bundle, Constants.CALLER_EXTRA)
            if (iBinder is Rx2PhotoBinder) {
                rx2Photo = iBinder.rx2Photo
            }
        }

        if (hasPermission()) {
            when (typeRequest) {
                TypeRequest.GALLERY -> gallery()
                TypeRequest.CAMERA -> camera()
                TypeRequest.COMBINE -> combine(false)
                TypeRequest.COMBINE_MULTIPLE -> combine(true)
            }
        } else {
            requestPermission()
        }
    }

    private fun combine(isMultiple: Boolean) {
        if (!Utils.isExternalStorageWritable()) {
            rx2Photo.propagateThrowable(ExternalStorageWriteException())
            return
        }

        fileUri = createImageUri()
        var intentList: MutableList<Intent> = ArrayList()
        var chooserIntent: Intent? = null
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            pickIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, isMultiple)
        }
        intentList = Utils.addIntentsToList(this, intentList, pickIntent)
        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
        intentList = Utils.addIntentsToList(this, intentList, takePhotoIntent)
        if (!intentList.isEmpty()) {
            val title = if (rx2Photo.title != null) rx2Photo.title else getString(R.string.picker_header)
            chooserIntent = Intent.createChooser(intentList.removeAt(intentList.size - 1), title)
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentList.toTypedArray<Parcelable>())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && isMultiple) {
            startActivityForResult(chooserIntent, Constants.REQUEST_COMBINE_MULTIPLE)
        } else {
            startActivityForResult(chooserIntent, Constants.REQUEST_COMBINE)
        }
    }

    private fun gallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, Constants.REQUEST_ATTACH_IMAGE)
    }

    private fun camera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            if (!Utils.isExternalStorageWritable()) {
                rx2Photo.propagateThrowable(ExternalStorageWriteException())
                return
            }
            fileUri = createImageUri()
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
            startActivityForResult(takePictureIntent, Constants.REQUEST_TAKE_PICTURE)
        }
    }

    private fun hasPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_GALLERY)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            rx2Photo.propagateThrowable(NotPermissionException(NotPermissionException.RequestEnum.GALLERY))
            finish()
            return
        }

        when (typeRequest) {
            TypeRequest.CAMERA -> camera()
            TypeRequest.GALLERY -> gallery()
            TypeRequest.COMBINE -> combine(false)
            TypeRequest.COMBINE_MULTIPLE -> combine(true)
        }
    }

    /**
     * If we not choose camera, temp file is unused and must be removed
     */
    private fun removeUnusedFile() {
        if (fileUri != null)
            contentResolver.delete(fileUri, null, null)
    }

    private fun createImageUri(): Uri? {
        val contentResolver = contentResolver
        val cv = ContentValues()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        cv.put(MediaStore.Images.Media.TITLE, timeStamp)
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(FILE_URI_EXTRA, fileUri)
        outState.putParcelable(BUNDLE, intent.extras)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        fileUri = savedInstanceState.getParcelable(FILE_URI_EXTRA)

        val bundle = savedInstanceState.getParcelable(BUNDLE) as Bundle
        typeRequest = bundle.get(Constants.REQUEST_TYPE_EXTRA) as TypeRequest
        val caller = bundle.getBundle(Constants.CALLER_EXTRA)
        if (caller != null) {
            val iBinder = BundleCompat.getBinder(caller, Constants.CALLER_EXTRA)
            if (iBinder is Rx2PhotoBinder) {
                rx2Photo = iBinder.rx2Photo
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (typeRequest) {
                TypeRequest.COMBINE_MULTIPLE -> if (data != null && data.clipData != null) {
                    val mClipData = data.clipData
                    val uris = (0 until mClipData.itemCount).map { mClipData.getItemAt(it).uri }

                    rx2Photo.onActivityResult(uris)

                    removeUnusedFile()
                } else if (data != null && data.data != null) {
                    val uris = ArrayList<Uri>()
                    uris.add(data.data)
                    rx2Photo.onActivityResult(uris)

                    removeUnusedFile()
                } else if (fileUri != null) {
                    val uris = listOf(fileUri!!)

                    rx2Photo.onActivityResult(uris)
                } else {
                    rx2Photo.onActivityResult(fileUri)
                }
                TypeRequest.CAMERA -> rx2Photo.onActivityResult(fileUri)
                TypeRequest.GALLERY, TypeRequest.COMBINE -> if (data != null && data.data != null) {
                    rx2Photo.onActivityResult(data.data)
                    removeUnusedFile()
                } else {
                    rx2Photo.onActivityResult(fileUri)
                }
            }
        } else {
            rx2Photo.propagateThrowable(CancelOperationException(typeRequest))
            removeUnusedFile()
        }

        finish()
    }

    private class Rx2PhotoBinder constructor(internal val rx2Photo: Rx2Photo) : Binder()
}