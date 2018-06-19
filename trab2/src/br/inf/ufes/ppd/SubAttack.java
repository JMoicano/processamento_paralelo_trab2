package br.inf.ufes.ppd;



/**
 * SubAttack.java
 */


import java.io.Serializable;

public class SubAttack implements Serializable {
	private byte[] ciphertext;
	private byte[] knowntext;
	private long initialwordindex;
	private long finalwordindex;
	private int attackNumber;

	
	
	public SubAttack(byte[] ciphertext, byte[] knowntext, long initialwordindex, long finalwordindex,
			int attackNumber) {
		super();
		this.ciphertext = ciphertext;
		this.knowntext = knowntext;
		this.initialwordindex = initialwordindex;
		this.finalwordindex = finalwordindex;
		this.attackNumber = attackNumber;
	}
	public byte[] getCiphertext() {
		return ciphertext;
	}
	public byte[] getKnowntext() {
		return knowntext;
	}
	public long getInitialwordindex() {
		return initialwordindex;
	}
	public long getFinalwordindex() {
		return finalwordindex;
	}
	public int getAttackNumber() {
		return attackNumber;
	}
	
	
}
