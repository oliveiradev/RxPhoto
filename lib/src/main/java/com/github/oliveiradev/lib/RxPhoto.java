package com.github.oliveiradev.lib;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Pair;

import com.github.oliveiradev.lib.shared.Constants;
import com.github.oliveiradev.lib.shared.ResponseType;
import com.github.oliveiradev.lib.shared.TypeRequest;

import java.io.IOException;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
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

    private static void propagateBitmap(Uri uri) throws IOException {
        bitmapPublishSubject.onNext(getBitmapFromStream(uri));
    }

    private static void propagateThumbs(Uri uri) throws IOException {
        final Bitmap bitmap = getBitmapFromStream(uri);
        Observable.from(sizes)
                .doOnNext(new Action1<Pair<Integer, Integer>>() {
                    @Override
                    public void call(Pair<Integer, Integer> size) {
                        bitmapPublishSubject.onNext(getThumbnail(bitmap, size));
                    }
                })
                .subscribe();
    }
}