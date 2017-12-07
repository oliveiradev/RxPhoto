package com.github.oliveiradev.lib.exceptions

import com.github.oliveiradev.lib.shared.TypeRequest

/**
 * Created by Genius on 03.12.2017.
 */
class CancelOperationException(var type: TypeRequest): Exception()