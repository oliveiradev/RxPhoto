package com.github.oliveiradev.lib.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.github.oliveiradev.lib.shared.Constants
import java.io.IOException

/**
 * Created by Genius on 03.12.2017.
 */
class Utils {

    companion object {

        @Throws(IOException::class)
        private fun modifyOrientation(bitmap: Bitmap, image_absolute_path: String?): Bitmap {
            val ei = ExifInterface(image_absolute_path)
            val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotate(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotate(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotate(bitmap, 270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flip(bitmap, true, false)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> flip(bitmap, false, true)
                else -> bitmap
            }
        }

        /**
         * Adding multiple intents of camera and gallery
         * @param context - current context
         * @param list - List<Intent> for receiving incoming Intents
         * @param intent - Intent for receive
        </Intent> */
        fun addIntentsToList(context: Context, list: MutableList<Intent>, intent: Intent): MutableList<Intent> {
            val resInfo = context.packageManager.queryIntentActivities(intent, 0)
            for (resolveInfo in resInfo) {
                val packageName = resolveInfo.activityInfo.packageName
                val targetedIntent = Intent(intent)
                targetedIntent.`package` = packageName
                list.add(targetedIntent)
            }
            return list
        }

        /**
         * Is external storage available for write
         * @return - is available
         */
        fun isExternalStorageWritable(): Boolean {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

        private fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(degrees)
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        private fun flip(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
            val matrix = Matrix()
            matrix.preScale((if (horizontal) -1 else 1).toFloat(), (if (vertical) -1 else 1).toFloat())
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        @Throws(IOException::class)
        fun getBitmap(context: Context, uri: Uri, width: Int?, height: Int?): Bitmap {
            var path: String?
            path = try {
                getRealPathFromURI(uri, context) //from Gallery
            } catch (e: Exception) {
                null
            }

            if (path == null)
                path = uri.path

            val iOptions = BitmapFactory.Options()
            iOptions.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, iOptions)

            iOptions.inSampleSize = calculateInSampleSize(iOptions, width ?: Constants.IMAGE_SIZE, height ?: Constants.IMAGE_SIZE)
            iOptions.inJustDecodeBounds = false

            val original = BitmapFactory.decodeFile(path, iOptions)

            return modifyOrientation(original, path)
        }

        private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {

            // Raw height and width of image
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {

                val halfHeight = height / 2
                val halfWidth = width / 2

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while (halfHeight / inSampleSize > reqHeight && halfWidth / inSampleSize > reqWidth) {

                    inSampleSize *= 2
                }
            }

            return inSampleSize
        }

        private fun getRealPathFromURI(contentUri: Uri, context: Context): String? {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = context.contentResolver.query(contentUri, proj, null, null, null) ?: return null
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            val s = cursor.getString(columnIndex)
            cursor.close()
            return s
        }
    }
}