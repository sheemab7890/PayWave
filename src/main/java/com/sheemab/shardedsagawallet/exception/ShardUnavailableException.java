package com.sheemab.shardedsagawallet.exception;

public class ShardUnavailableException extends RuntimeException{

    public ShardUnavailableException(){}

    public ShardUnavailableException(String message){
        super(message);
    }
}
