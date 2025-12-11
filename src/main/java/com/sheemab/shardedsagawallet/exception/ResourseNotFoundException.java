package com.sheemab.shardedsagawallet.exception;

public class ResourseNotFoundException extends RuntimeException {

    public ResourseNotFoundException() {
    }

    public ResourseNotFoundException(String message) {
        super(message);
    }
}
