/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mountwilson.common;


/**
 *
 * @author dsmagadX
 */
public class TAException extends Exception {
    

	ErrorCode errorCode = null;

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
    
    private TAException(){
        
    }
    
    public TAException(ErrorCode errorCode, String message){
        super(message);
        this.errorCode = errorCode;
    }

	public TAException(ErrorCode errorCode, String message, Exception e) {
        super(message,e);
        this.errorCode = errorCode;
		
	}
    
}
