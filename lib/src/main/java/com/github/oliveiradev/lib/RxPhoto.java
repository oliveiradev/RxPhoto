package com.github.oliveiradev.lib;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;

import com.github.oliveiradev.lib.shared.Constants;
import com.github.oliveiradev.lib.shared.ResponseType;
import com.github.oliveiradev.lib.shared.TypeRequest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import static com.github.oliveiradev.lib.shared.ResponseType.BITMAP;
import static com.github.oliveiradev.lib.shared.ResponseType.THUMB;
import static com.github.oliveiradev.lib.shared.ResponseType.URI;

public final class RxPhoto {

    private static Context context;
    private static PublishSubject<Bitmap> bitmapPublishSubject = PublishSubject.create();
    private static PublishSubject<Uri> uriPublishSubject = PublishSubject.create();
    private static Pair<Integer, Integer>[] sizes;
    private static ResponseType response;

    private RxPhoto() {
    }

    public static Observable<Bitmap> requestBitmap(Context context, TypeRequest typeRequest) {
        RxPhoto.context = context;
        response = BITMAP;
        startOverlapActivity(typeRequest);
        return bitmapPublishSubject;
    }

    public static Observable<Uri> requestUri(Context context, TypeRequest typeRequest) {
        RxPhoto.context = context;
        response = URI;
        startOverlapActivity(typeRequest);
        return uriPublishSubject;
    }


    public static Observable<Bitmap> requestThumbnails(Context context, TypeRequest typeRequest, Pair<Integer, Integer>... sizes) {
        RxPhoto.context = context;
        startOverlapActivity(typeRequest);
        response = THUMB;
        RxPhoto.sizes = sizes;
        return bitmapPublishSubject;
    }

    public static Func1<Bitmap, Observable<Bitmap>> transformToThumbnail(final Pair<Integer, Integer>... sizes) {
        return new Func1<Bitmap, Observable<Bitmap>>() {
            @Override
            public Observable<Bitmap> call(final Bitmap bitmap) {
                return Observable.from(sizes)
                        .flatMap(new Func1<Pair<Integer, Integer>, Observable<Bitmap>>() {
                            @Override
                            public Observable<Bitmap> call(Pair<Integer, Integer> size) {
                                return Observable.just(getThumbnail(bitmap, size));
                            }
                        });
            }
        };
    }

    private static void startOverlapActivity(TypeRequest typeRequest) {
        Intent intent = new Intent(context, OverlapActivity.class);
        intent.putExtra(Constants.REQUEST_TYPE_EXTRA, typeRequest);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static Bitmap getBitmapFromStream(Uri url) throws IOException {
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), url);
    }

    private static Bitmap getThumbnail(Bitmap bitmap, Pair<Integer, Integer> resizeValues) {
        return ThumbnailUtils.extractThumbnail(bitmap,
                resizeValues.first, resizeValues.second);
    }

    protected static void onActivityResult(Uri uri) {
        if (uri != null) {
            propagateResult(uri);
        }
    }

    private static void propagateResult(Uri uri) {
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

    private static void propagateUri(Uri uri) {
        uriPublishSubject.onNext(uri);
    }

    private static void propagateBitmap(final Uri uri) {
        getBitmapObservable(uri)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bitmapPublishSubject);
    }

    private static Observable getBitmapObservable(final Uri uri) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                try {
                    Bitmap b = getBitmapFromStream(uri);
                    if (b == null)
                        throw new RuntimeException("bitmap is null");
                    subscriber.onNext(b);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).retryWhen(getRetryHandler());
    }

    private static void propagateThumbs(Uri uri) throws IOException {
        Observable
                .combineLatest(getBitmapObservable(uri), Observable.from(sizes), new Func2<Bitmap, Pair<Integer, Integer>, Bitmap>() {
                    @Override
                    public Bitmap call(Bitmap bitmap, Pair<Integer, Integer> size) {
                        return getThumbnail(bitmap, size);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bitmapPublishSubject);
    }

    public static Func1<? super Observable<? extends Throwable>, ? extends Observable<?>> getRetryHandler() {
        return new Func1<Observable<? extends Throwable>, Observable<?>>() {
            @Override
            public Observable<?> call(Observable<? extends Throwable> errors) {
                return errors
                        .zipWith(Observable.range(1, 5), new Func2<Throwable, Integer, Integer>() {
                            @Override
                            public Integer call(Throwable throwable, Integer integer) {
                                return integer;
                            }
                        })
                        .flatMap(new Func1<Integer, Observable<?>>() {
                            @Override
                            public Observable<?> call(Integer integer) {
                                return Observable.timer(500, TimeUnit.MILLISECONDS);
                            }
                        });
            }
        };
    }
}