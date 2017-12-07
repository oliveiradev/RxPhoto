package com.github.oliveiradev.lib.exceptions

/**
 * Created by Genius on 03.12.2017.
 */
class NotPermissionException(source: NotPermissionException.RequestEnum): Exception() {

    enum class RequestEnum {
        CAMERA,
        GALLERY
    }
}