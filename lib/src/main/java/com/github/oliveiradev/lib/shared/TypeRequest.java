package com.github.oliveiradev.lib.shared;

public enum TypeRequest {
    CAMERA(1),GALLERY(2),COMBINE(3),COMBINE_MULTIPLE(4);

    private final int value;
    TypeRequest(int val){
        value = val;
    }
}