package com.sgl.exception;

public class StockConflictException extends BusinessRuleException {

	public StockConflictException(String message) {
		super(message);
	}
}