package com.sheemab.shardedsagawallet.exception;

public class DuplicateRequestException extends RuntimeException{

    public DuplicateRequestException(){}

    public DuplicateRequestException(String message){
        super(message);
    }
}
