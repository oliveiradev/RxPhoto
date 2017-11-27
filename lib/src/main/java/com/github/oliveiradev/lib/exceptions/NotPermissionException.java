package com.github.oliveiradev.lib.exceptions;

/**
 * Created by Genius on 25.11.2017.
 */

public class NotPermissionException extends Exception {

    private RequestEnum mReason;

    public NotPermissionException(RequestEnum reason) {
        mReason = reason;
    }

    public RequestEnum getReason() {
        return mReason;
    }

    public enum RequestEnum {
        CAMERA,
        GALLERY
    }
}
