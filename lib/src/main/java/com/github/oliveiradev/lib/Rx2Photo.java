package com.github.oliveiradev.lib;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.github.oliveiradev.lib.shared.Constants;
import com.github.oliveiradev.lib.shared.ResponseType;
import com.github.oliveiradev.lib.shared.TypeRequest;
import com.github.oliveiradev.lib.util.Utils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
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
    private String title;

    private Rx2Photo(final Context context) {
        contextWeakReference = new WeakReference<>(context);
    }

    @NonNull
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

    public Rx2Photo titleBitmapCombine(String title) {
        this.title = title;
        return this;
    }

    public Rx2Photo titleUriCombine(String title) {
        this.title = title;
        return this;
    }

    @SafeVarargs
    public final Observable<Bitmap> requestThumbnails(TypeRequest typeRequest, Pair<Integer, Integer>... sizes) {
        startOverlapActivity(typeRequest);
        response = THUMB;
        this.sizes = sizes;
        return bitmapPublishSubject;
    }

    @SafeVarargs
    public static Function<Bitmap, Observable<Bitmap>> transformToThumbnail(final Pair<Integer, Integer>... sizes) {
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

    public String getTitle() {
        return title;
    }

    void onActivityResult(Uri uri) {
        if (uri != null) {
            propagateResult(uri);
        }
    }

    void onActivityResult(List<Uri> uri) {
        propagateMultipleResult(uri);
    }

    void propagateThrowable(Throwable error) {
        switch (response) {
            case BITMAP:
            case THUMB:
                bitmapPublishSubject.onError(error);
                break;
            case URI:
                uriPublishSubject.onError(error);
                break;
            default:
                break;
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

    private void propagateMultipleResult(List<Uri> uri) {
        try {
            switch (response) {
                case BITMAP:
                    propagateMultipleBitmap(uri);
                    break;
                case URI:
                    propagateMultipleUri(uri);
                    break;
                case THUMB:
                    propagateMultipleThumbs(uri);
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

    private void propagateMultipleUri(List<Uri> uri) {
        for (Uri item : uri) {
            uriPublishSubject.onNext(item);
        }
    }

    private void propagateBitmap(final Uri uri) {
        getBitmapObservable(uri)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bitmapPublishSubject);
    }

    private void propagateMultipleBitmap(final List<Uri> uri) {
        getBitmapMultipleObservable(uri)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<List<Bitmap>, Boolean>() {
                    @Override
                    public Boolean apply(List<Bitmap> bitmaps) throws Exception {
                        for (Bitmap item : bitmaps) {
                            bitmapPublishSubject.onNext(item);
                        }

                        return true;
                    }
                })
                .subscribe();
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

    private Observable<List<Bitmap>> getBitmapMultipleObservable(final List<Uri> uri) {
        return Observable.fromCallable(new Callable<List<Bitmap>>() {
            @Override
            public List<Bitmap> call() throws Exception {
                try {
                    List<Bitmap> list = new ArrayList<>();

                    for (Uri item : uri) {
                        list.add(getBitmapFromStream(item));
                    }

                    return list;
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

    private void propagateMultipleThumbs(List<Uri> uri) throws IOException {
        Observable
                .combineLatest(getBitmapMultipleObservable(uri), Observable.fromArray(sizes), new BiFunction<List<Bitmap>, Pair<Integer, Integer>, List<Bitmap>>() {
                    @Override
                    public List<Bitmap> apply(List<Bitmap> bitmaps, Pair<Integer, Integer> size) throws Exception {
                        List<Bitmap> list = new ArrayList<>();

                        for (Bitmap item : bitmaps) {
                            list.add(getThumbnail(item, size));
                        }

                        return list;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<List<Bitmap>, Boolean>() {
                    @Override
                    public Boolean apply(List<Bitmap> bitmaps) throws Exception {
                        for (Bitmap item : bitmaps) {
                            bitmapPublishSubject.onNext(item);
                        }

                        return true;
                    }
                })
                .subscribe();
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