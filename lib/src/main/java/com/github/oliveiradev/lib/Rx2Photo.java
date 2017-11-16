package com.github.oliveiradev.lib;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.util.Pair;

import com.github.oliveiradev.lib.shared.Constants;
import com.github.oliveiradev.lib.shared.ResponseType;
import com.github.oliveiradev.lib.shared.TypeRequest;
import com.github.oliveiradev.lib.util.Utils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import static com.github.oliveiradev.lib.shared.ResponseType.BITMAP;
import static com.github.oliveiradev.lib.shared.ResponseType.THUMB;
import static com.github.oliveiradev.lib.shared.ResponseType.URI;

public final class Rx2Photo {

    private WeakReference<Context> contextWeakReference = null;

    private final PublishSubject<Bitmap> bitmapPublishSubject = PublishSubject.create();
    private final PublishSubject<Uri> uriPublishSubject = PublishSubject.create();
    private Pair<Integer, Integer>[] sizes;
    private Pair<Integer, Integer> bitmapSizes;
    private ResponseType response;

    private Rx2Photo(final Context context) {
        contextWeakReference = new WeakReference<>(context);
    }

    public static Rx2Photo with(final Context context) {
        return new Rx2Photo(context);
    }

    public Observable<Bitmap> requestBitmap(TypeRequest typeRequest) {
        return requestBitmap(typeRequest,new Pair<>(Constants.IMAGE_SIZE,Constants.IMAGE_SIZE));
    }

    public Observable<Bitmap> requestBitmap(TypeRequest typeRequest,Integer width, Integer height) {
        return requestBitmap(typeRequest,new Pair<>(width,height));
    }

    public Observable<Bitmap> requestBitmap(TypeRequest typeRequest,Pair<Integer,Integer> bitmapSize) {
        response = BITMAP;
        startOverlapActivity(typeRequest);
        this.bitmapSizes = bitmapSize;
        return bitmapPublishSubject;
    }

    public Observable<Uri> requestUri(TypeRequest typeRequest) {
        response = URI;
        startOverlapActivity(typeRequest);
        return uriPublishSubject;
    }

    @SafeVarargs
    public final Observable<Bitmap> requestThumbnails(TypeRequest typeRequest, Pair<Integer, Integer>... sizes) {
        startOverlapActivity(typeRequest);
        response = THUMB;
        this.sizes = sizes;
        return bitmapPublishSubject;
    }

    @SafeVarargs
    public static final Function<Bitmap, Observable<Bitmap>> transformToThumbnail(final Pair<Integer, Integer>... sizes) {
        return new Function<Bitmap, Observable<Bitmap>>() {
            @Override
            public Observable<Bitmap> apply(final Bitmap bitmap) throws Exception {
                return Observable.fromArray(sizes)
                        .flatMap(new Function<Pair<Integer, Integer>, Observable<Bitmap>>() {
                            @Override
                            public Observable<Bitmap> apply(Pair<Integer, Integer> size) throws Exception {
                                return Observable.just(getThumbnail(bitmap, size));
                            }

                        });
            }
        };
    }

    private void startOverlapActivity(TypeRequest typeRequest) {
        final Context context = contextWeakReference.get();
        if (context == null) return;
        context.startActivity(OverlapActivity.newIntent(context, typeRequest, this));
    }

    @Nullable
    private Bitmap getBitmapFromStream(Uri url) throws IOException {
        final Context context = contextWeakReference.get();
        if (context == null) return null;
        return Utils.getBitmap(context, url, bitmapSizes.first, bitmapSizes.second);
    }

    private static Bitmap getThumbnail(Bitmap bitmap, Pair<Integer, Integer> resizeValues) {
        return ThumbnailUtils.extractThumbnail(bitmap,
                resizeValues.first, resizeValues.second);
    }

    void onActivityResult(Uri uri) {
        if (uri != null) {
            propagateResult(uri);
        }
    }

    private void propagateResult(Uri uri) {
        try {
            switch (response) {
                case BITMAP:
                    propagateBitmap(uri);
                    break;
                case URI:
                    propagateUri(uri);
                    break;
                case THUMB:
                    propagateThumbs(uri);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            uriPublishSubject.onError(e);
        }
    }

    private void propagateUri(Uri uri) {
        uriPublishSubject.onNext(uri);
    }

    private void propagateBitmap(final Uri uri) {
        getBitmapObservable(uri)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bitmapPublishSubject);
    }

    private Observable<Bitmap> getBitmapObservable(final Uri uri) {
        return Observable.fromCallable(new Callable<Bitmap>() {
            @Override
            public Bitmap call() throws Exception {
                try {
                    Bitmap b = getBitmapFromStream(uri);
                    if (b == null)
                        throw new RuntimeException("bitmap is null");
                    return b;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).retryWhen(getRetryHandler());
    }

    private void propagateThumbs(Uri uri) throws IOException {
        Observable
                .combineLatest(getBitmapObservable(uri), Observable.fromArray(sizes), new BiFunction<Bitmap, Pair<Integer, Integer>, Bitmap>() {
                    @Override
                    public Bitmap apply(Bitmap bitmap, Pair<Integer, Integer> size) throws Exception {
                        return getThumbnail(bitmap, size);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bitmapPublishSubject);
    }

    private Function<? super Observable<? extends Throwable>, ? extends Observable<?>> getRetryHandler() {
        return new Function<Observable<? extends Throwable>, Observable<?>>() {
            @Override public Observable<?> apply(@NonNull Observable<? extends Throwable> observable) throws Exception {
                return observable.zipWith(Observable.range(1, 5), new BiFunction<Throwable, Integer, Integer>() {

                    @Override public Integer apply(@NonNull Throwable throwable, @NonNull Integer integer) throws Exception {
                        return integer;
                    }
                }).flatMap(new Function<Integer, ObservableSource<?>>() {
                    @Override public ObservableSource<?> apply(@NonNull Integer integer) throws Exception {
                        return Observable.timer(500, TimeUnit.MILLISECONDS);
                    }
                });
            }
        };
    }
}