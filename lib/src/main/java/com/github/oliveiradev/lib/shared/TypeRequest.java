package com.github.oliveiradev.lib.shared;

public enum TypeRequest {
    CAMERA(1),GALLERY(2);

    private final int value;
    TypeRequest(int val){
        value = val;
    }
}