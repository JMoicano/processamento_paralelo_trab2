package br.inf.ufes.ppd;



/**
 * Guess.java
 */


import java.io.Serializable;

public class Guess implements Serializable {
	private String key;
	// chave candidata

	private byte[] message;
	// mensagem decriptografada com a chave candidata

	public Guess(String key, byte[] message) {
		super();
		this.key = key;
		this.message = message;
	}
	public String getKey() {
		return key;
	}
	public byte[] getMessage() {
		return message;
	}

}
