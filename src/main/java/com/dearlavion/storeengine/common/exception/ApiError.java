package com.dearlavion.storeengine.common.exception;

/** Matches NestJS's default HttpException JSON body shape, so nothing downstream (the frontend's
 * generic httpErrorInterceptor) needs to change when reading an error response. */
public record ApiError(int statusCode, Object message, String error) {
}
