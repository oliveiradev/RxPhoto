package com.github.oliveiradev.lib

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.support.annotation.StringRes
import android.util.Pair
import com.github.oliveiradev.lib.shared.Constants
import com.github.oliveiradev.lib.shared.ResponseType
import com.github.oliveiradev.lib.shared.ResponseType.*
import com.github.oliveiradev.lib.shared.TypeRequest
import com.github.oliveiradev.lib.util.FileUtils
import com.github.oliveiradev.lib.util.Utils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * Created by Genius on 03.12.2017.
 */
open class Rx2Photo(private val context: Context) {

    private var contextWeakReference: WeakReference<Context> = WeakReference(context)
    private var sizes: Array<out Pair<Int, Int>>? = null
    private var bitmapSizes: Pair<Int, Int>? = null
    private var bitmapPublishSubject: PublishSubject<Bitmap>? = null
    private var uriPublishSubject: PublishSubject<Uri>? = null
    private var pathPublishSubject: PublishSubject<String>? = null
    private var bitmapMultiPublishSubject: PublishSubject<List<Bitmap>>? = null
    private var uriMultiPublishSubject: PublishSubject<List<Uri>>? = null
    private var pathMultiPublishSubject: PublishSubject<List<String>>? = null
    private lateinit var response: ResponseType

    var title: String? = null
        private set

    companion object {

        /**
         * Basic call method for the library
         * @param context - context place of call
         */
        @JvmStatic
        fun with(context: Context): Rx2Photo {
            return Rx2Photo(context)
        }

        /**
         * Changes the stream with bitmaps to the stream with changed bitmaps
         * @param sizes - array with new size of bitmaps
         * @return - observable that emits rescaled bitmaps
         */
        @JvmStatic
        @SafeVarargs
        fun transformToThumbnail(vararg sizes: Pair<Int, Int>): Function<Bitmap, Observable<Bitmap>> {
            return Function { bitmap ->
                Observable.fromArray(*sizes).flatMap { size -> Observable.just(getThumbnail(bitmap, size)) }
            }
        }

        /**
         * Get thumbnails bitmap for selected scale from source
         * @param bitmap - source bitmap for scale
         * @param resizeValues - pair values with requested size for bitmap
         * @return - scaled bitmap
         */
        private fun getThumbnail(bitmap: Bitmap, resizeValues: Pair<Int, Int>): Bitmap {
            return ThumbnailUtils.extractThumbnail(bitmap, resizeValues.first, resizeValues.second)
        }
    }

    /**
     * Generic request for
     * @param typeRequest - selected source for bitmap
     * @return - observable that emits bitmaps
     */
    fun requestBitmap(typeRequest: TypeRequest): Observable<Bitmap> {
        return requestBitmap(typeRequest, Pair(Constants.IMAGE_SIZE, Constants.IMAGE_SIZE))
    }

    /**
     * Request for single bitmap with explicitly set of size
     * @param typeRequest - selected source for bitmap
     * @param width - width of resized bitmap
     * @param height - height of resized bitmap
     * @return - observable that emits single bitmap
     */
    fun requestBitmap(typeRequest: TypeRequest, width: Int, height: Int): Observable<Bitmap> {
        return requestBitmap(typeRequest, Pair(width, height))
    }

    /**
     * Request for list of bitmaps with default (1024) size
     * @return - observable tah emits list of scaled bitmaps
     */
    fun requestMultiBitmap(): Observable<List<Bitmap>> {
        return requestMultiBitmap(Pair(Constants.IMAGE_SIZE, Constants.IMAGE_SIZE))
    }

    /**
     * Request for list of bitmaps with explicitly set of size
     * @param width - width of resized bitmaps
     * @param height - height of resized bitmaps
     * @return - observable that emits list of bitmaps
     */
    fun requestMultiBitmap(width: Int, height: Int): Observable<List<Bitmap>> {
        return requestMultiBitmap(Pair(width, height))
    }

    /**
     * Generic request for getting bitmap observable
     * @param typeRequest - selected source for emitter
     * @param bitmapSize - requested bitmap scale size
     * @return - explicitly scaled or not (1024 by default) bitmap
     */
    fun requestBitmap(typeRequest: TypeRequest, bitmapSize: Pair<Int, Int>): Observable<Bitmap> {
        response = BITMAP
        startOverlapActivity(typeRequest)
        this.bitmapSizes = bitmapSize
        bitmapPublishSubject = PublishSubject.create()
        return bitmapPublishSubject as PublishSubject<Bitmap>
    }

    /**
     * Request for single uri
     * @param typeRequest - selected source for emitter
     * @return - observable that emits a single uri
     */
    fun requestUri(typeRequest: TypeRequest): Observable<Uri> {
        response = URI
        startOverlapActivity(typeRequest)
        uriPublishSubject = PublishSubject.create()
        return uriPublishSubject as PublishSubject<Uri>
    }

    /**
     * Request for single path of file
     * @param typeRequest - selected source for emitter
     * @return - observable that emits a single path
     */
    fun requestPath(typeRequest: TypeRequest): Observable<String> {
        response = PATH
        startOverlapActivity(typeRequest)
        pathPublishSubject = PublishSubject.create()
        return pathPublishSubject as PublishSubject<String>
    }

    /**
     * Request for list of bitmaps with explicitly set of size
     * @param bitmapSize - requested bitmap scale size
     * @return - explicitly scaled or not (1024 by default) bitmap
     */
    fun requestMultiBitmap(bitmapSize: Pair<Int, Int>): Observable<List<Bitmap>> {
        response = BITMAP
        startOverlapActivity(TypeRequest.COMBINE_MULTIPLE)
        this.bitmapSizes = bitmapSize
        bitmapMultiPublishSubject = PublishSubject.create()
        return bitmapMultiPublishSubject as PublishSubject<List<Bitmap>>
    }

    /**
     * Request for list of uris
     * @return - observable that emits a list of uris
     */
    fun requestMultiUri(): Observable<List<Uri>> {
        response = URI
        startOverlapActivity(TypeRequest.COMBINE_MULTIPLE)
        uriMultiPublishSubject = PublishSubject.create()
        return uriMultiPublishSubject as PublishSubject<List<Uri>>
    }

    /**
     * Request for list of paths
     * @return - observable that emits a list of paths
     */
    fun requestMultiPath(): Observable<List<String>> {
        response = PATH
        startOverlapActivity(TypeRequest.COMBINE_MULTIPLE)
        pathMultiPublishSubject = PublishSubject.create()
        return pathMultiPublishSubject as PublishSubject<List<String>>
    }

    /**
     * Request to receive a picture in the form of thumbnail from the place of call
     * @param typeRequest -
     * @param sizes
     * @return observable that emits single bitmap image
     */
    @SafeVarargs
    fun requestThumbnails(typeRequest: TypeRequest, vararg sizes: Pair<Int, Int>): Observable<Bitmap> {
        startOverlapActivity(typeRequest)
        response = THUMB
        this.sizes = sizes
        bitmapPublishSubject = PublishSubject.create()
        return bitmapPublishSubject as PublishSubject<Bitmap>
    }

    /**
     * Adding title to intent chooser on string
     * @param title - title in string
     * @return - parent class
     */
    fun titleCombine(title: String): Rx2Photo {
        this.title = title
        return this
    }

    /**
     * Adding title to intent chooser on resource id
     * @param titleId - title in resources id
     * @return - parent class
     */
    fun titleCombine(@StringRes titleId: Int): Rx2Photo {
        this.title = contextWeakReference.get()?.getString(titleId)
        return this
    }

    /**
     * Start activity for action
     * Calling {@link OverlapActivity#newIntent} with selected type request and Rx2Photo instance
     * @param typeRequest - selected request
     */
    private fun startOverlapActivity(typeRequest: TypeRequest) {
        val context = contextWeakReference.get() ?: return
        context.startActivity(OverlapActivity.newIntent(context, typeRequest, this))
    }

    /**
     * Get the bitmap from the source by URI
     * @param uri - uri source
     * @return image in bitmap
     */
    @Throws(IOException::class)
    private fun getBitmapFromStream(uri: Uri): Bitmap? {
        val context = contextWeakReference.get() ?: return null
        return Utils.getBitmap(context, uri, bitmapSizes?.first, bitmapSizes?.second)
    }

    /**
     * Processing the result of selecting images by the user
     * @param uri - single uri of selected image
     */
    internal fun onActivityResult(uri: Uri?) {
        uri?.let {
            propagateResult(it)
        }
    }

    /**
     *Processing the results of selecting images by the user
     * @param uri - list of uris of selected images
     */
    internal fun onActivityResult(uri: List<Uri>) {
        propagateMultipleResult(uri)
    }

    /**
     * Handle throwable from activity
     * @param error - throwable
     */
    internal fun propagateThrowable(error: Throwable) {
        when (response) {
            BITMAP, THUMB -> {
                bitmapMultiPublishSubject?.onError(error)
                bitmapPublishSubject?.onError(error)
            }
            URI -> {
                uriMultiPublishSubject?.onError(error)
                uriPublishSubject?.onError(error)
            }
            PATH -> {
                pathMultiPublishSubject?.onError(error)
                pathPublishSubject?.onError(error)
            }
        }
    }

    /**
     * Handle result from activity
     * @param uri - uri-result
     */
    private fun propagateResult(uri: Uri) {
        try {
            when (response) {
                BITMAP -> propagateBitmap(uri)
                URI -> propagateUri(uri)
                THUMB -> propagateThumbs(uri)
                PATH -> propagatePath(uri)
            }
        } catch (e: Exception) {
            uriPublishSubject?.onError(e)
            bitmapPublishSubject?.onError(e)
        }
    }

    /**
     * Handle multiple result from activity
     * @param uris - uris items from activity
     */
    private fun propagateMultipleResult(uris: List<Uri>) {
        try {
            when (response) {
                BITMAP -> propagateMultipleBitmap(uris)
                URI -> propagateMultipleUri(uris)
                THUMB -> propagateMultipleThumbs(uris)
                PATH -> propagateMultiplePaths(uris)
            }
        } catch (e: Exception) {
            uriPublishSubject?.onError(e)
            bitmapPublishSubject?.onError(e)
        }
    }

    /**
     * Handle single result from activity
     * @param uri - uri item from activity
     */
    private fun propagateUri(uri: Uri) {
        uriPublishSubject?.onNext(uri)
    }

    /**
     * Handle single result from activity
     * @param uri - uri item from activity
     */
    private fun propagatePath(uri: Uri) {
        pathPublishSubject?.onNext(FileUtils.getPath(context, uri))
    }

    /**
     * Handle multiple result from activity
     * @param uris - uris items from activity
     */
    private fun propagateMultipleUri(uris: List<Uri>) {
        uriMultiPublishSubject?.onNext(uris)
    }

    /**
     * Handle result list of paths from activity
     * @param uris - uris of path image activity
     */
    private fun propagateMultiplePaths(uris: List<Uri>) {
        pathMultiPublishSubject?.let {
            Observable.just(uris)
                .map { it.map { FileUtils.getPath(context, it) } }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(it)
        }
    }

    /**
     * Handle single result bitmap from activity
     * @param uriBitmap - uri for bitmap image activity
     */
    private fun propagateBitmap(uriBitmap: Uri) {
        bitmapPublishSubject?.let {
            getBitmapObservable(uriBitmap)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(it)
        }
    }

    /**
     * Handle result list of bitmaps from activity
     * @param uris - uris of bitmap image activity
     */
    private fun propagateMultipleBitmap(uris: List<Uri>) {
        bitmapMultiPublishSubject?.let {
            getBitmapMultipleObservable(uris)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(it)
        }
    }

    /**
     * Handle single result bitmap from activity
     * @param uri - uri for bitmap image activity
     * @return - observable that emits single bitmap
     */
    private fun getBitmapObservable(uri: Uri): Observable<Bitmap> {
        return Observable.fromCallable(Callable {
            try {
                return@Callable getBitmapFromStream(uri) ?: throw RuntimeException("Bitmap is null")
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }).retryWhen(getRetryHandler())
    }

    /**
     * Handle list for result bitmaps from activity
     * @param uris - list of uris for bitmap image activity
     * @return - observable that emits list of bitmaps
     */
    private fun getBitmapMultipleObservable(uris: List<Uri>): Observable<List<Bitmap>> {
        return Observable.fromCallable(Callable<List<Bitmap>> {
            try {
                val list = ArrayList<Bitmap>()

                for (item in uris) {
                    val tmp = getBitmapFromStream(item)
                    if (tmp != null) {
                        list.add(tmp)
                    }
                }

                return@Callable list
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }).retryWhen(getRetryHandler())
    }

    /**
     * Handle single result bitmap from activity for create a thumbnails
     * @param uri - uri for thumbnail bitmap image activity
     */
    @Throws(IOException::class)
    private fun propagateThumbs(uri: Uri) {
        bitmapPublishSubject?.let {
            sizes?.let {
                Observable
                        .combineLatest(getBitmapObservable(uri), Observable.fromArray(*sizes as Array<out Pair<Int, Int>>), BiFunction<Bitmap, Pair<Int, Int>, Bitmap> { bitmap, size -> getThumbnail(bitmap, size) })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(bitmapPublishSubject as PublishSubject<Bitmap>)
            }
        }
    }

    /**
     * Handle list of result bitmap from activity for create a thumbnails
     * @param uris - list of uris for thumbnail bitmap image activity
     */
    @Throws(IOException::class)
    private fun propagateMultipleThumbs(uris: List<Uri>) {
        sizes?.let {
            Observable
                    .combineLatest(getBitmapMultipleObservable(uris), Observable.fromArray(*it), BiFunction<List<Bitmap>, Pair<Int, Int>, List<Bitmap>> { bitmaps, size ->
                        bitmaps.mapTo(ArrayList()) { getThumbnail(it, size) }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .map { bitmaps ->
                        bitmapMultiPublishSubject?.onNext(bitmaps)
                        true
                    }
                    .subscribe()
        }
    }

    /**
     * The error handler. Does five retry attempts
     * @return function that emits 5 attempts, where 500 milliseconds for each
     */
    private fun getRetryHandler(): Function<in Observable<out Throwable>, out Observable<Long>> {
        return Function { observable -> observable.zipWith(Observable.range(1, 5), BiFunction<Throwable, Int, Int> { _, integer -> integer })
                .flatMap { Observable.timer(500, TimeUnit.MILLISECONDS) } }
    }
}