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

public final class RxPhoto {

    private static Subject<Bitmap, Bitmap> subject = new SerializedSubject(PublishSubject.create());
    private static Context mContext;

    public static Observable<Bitmap> request(Context context, TypeRequest typeRequest) {
        mContext = context;
        startOverlapActivity(typeRequest);
        subject = Factory.create();
        return subject;
    }

    private static void startOverlapActivity(TypeRequest typeRequest) {
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

    private static class Factory{
        public static Subject create(){
            return new SerializedSubject(PublishSubject.create());
        }
    }
}