package com.github.oliveiradev.lib.exceptions;

import com.github.oliveiradev.lib.shared.TypeRequest;

/**
 * Created by Genius on 27.11.2017.
 */

public class CancelOperationException extends Exception {

    private TypeRequest type;

    public CancelOperationException(TypeRequest type) {
        this.type = type;
    }

    public TypeRequest getType() {
        return type;
    }
}
