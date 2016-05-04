package com.github.oliveiradev.lib;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

public class RxPhoto {

    private static final Subject<Bitmap, Bitmap> subject = new SerializedSubject<Bitmap, Bitmap>(PublishSubject.<Bitmap>create());
    private static Context mContext;

    public static Observable<Bitmap> request(Context context, TypeRequest typeRequest) {
        mContext = context;
        startShadowActivity(typeRequest);
        return subject.compose(Transformers.<Bitmap>applySchedeulers());
    }

    private static void startShadowActivity(TypeRequest typeRequest) {
        Intent intent = new Intent(mContext, OverlapActivity.class);
        intent.putExtra("enum", typeRequest);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private static Bitmap getBitmapFromStream(Uri url) throws IOException {
        InputStream stream = mContext.getContentResolver().openInputStream(url);
        Bitmap bitmap = BitmapFactory.decodeStream(stream);
        if (stream != null) stream.close();
        return bitmap;
    }

    protected static void onActivityResult(Uri uri) {
            if (uri != null) {
                try {
                    subject.onNext(getBitmapFromStream(uri));
                    subject.onCompleted();
                } catch (IOException e) {
                    subject.onError(e);
                    e.printStackTrace();
                }
            }
    }
}