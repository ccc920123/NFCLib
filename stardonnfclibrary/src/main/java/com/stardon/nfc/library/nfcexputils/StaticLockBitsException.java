package com.stardon.nfc.library.nfcexputils;

@SuppressWarnings("serial")

public class StaticLockBitsException extends Exception {
	// Parameterless Constructor
	public StaticLockBitsException() {
	}

	// Constructor that accepts a message
	public StaticLockBitsException(String message) {
		super(message);
	}

}