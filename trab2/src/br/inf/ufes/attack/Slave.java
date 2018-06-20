package br.inf.ufes.attack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.ProducerConsumer;
import br.inf.ufes.ppd.SubAttack;

public class Slave extends ProducerConsumer implements MessageListener {
	private ArrayList<String> _dict; //dictionary words
	
	public Slave (String host, String dicPath) {
		super(host);
		consumer = context.createConsumer(subAttackQueue);
		File f = new File(dicPath);
		 
		//Initialize dictionary
		try(FileReader fileReader = new FileReader(f);
			BufferedReader b = new BufferedReader(fileReader)) {
			this._dict = new ArrayList<String>();
			String readLine = "";
			while ((readLine = b.readLine()) != null) {
				this._dict.add(readLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		consumer.setMessageListener(this);

	}
	
	private void attack (byte[] ciphertext, byte[] knowntext, long initialindex, long finalindex, int attackNumber) {
		System.out.println("SubAttack for Attack #" + attackNumber + " started!");
		for (long aux = initialindex; aux <= finalindex; ++aux) {
		
			String known_text = new String(knowntext);
			byte[] key = _dict.get((int)aux).getBytes();
			//Try to decrypt at every dictionary word in set indexes 
			try {
				SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");
	
				Cipher cipher = Cipher.getInstance("Blowfish");
				cipher.init(Cipher.DECRYPT_MODE, keySpec);
	
				byte[] message = ciphertext;
	
				byte[] decrypted = cipher.doFinal(message);
				String decryptedStr = new String(decrypted);
				if (decryptedStr.contains(known_text)) {
					//if a candidate word was found send a message
					ObjectMessage m = context.createObjectMessage();
					m.setObject(new Guess(new String(key), decrypted));
					m.setIntProperty("attackNumber", attackNumber);
					producer.send(guessQueue, m);
				}
			} catch (javax.crypto.BadPaddingException | InvalidKeyException | IllegalBlockSizeException | NoSuchAlgorithmException | NoSuchPaddingException | JMSException e) { }
			
			
		}
		try {
			//Send the final message
			Message m = context.createMessage();
			m.setIntProperty("attackNumber", attackNumber);
			System.out.println("SubAttack for Attack #" + attackNumber + " finished!");
			producer.send(guessQueue, m);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onMessage(Message msg) {

		try {
			if(msg instanceof ObjectMessage) {
				Object obj = ((ObjectMessage) msg).getObject();
				if(obj instanceof SubAttack) {
					SubAttack sa = (SubAttack) obj;
					attack(sa.getCiphertext(), sa.getKnowntext(), sa.getInitialwordindex(), sa.getFinalwordindex(), sa.getAttackNumber());
				}
			
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
		String host = (args.length < 2) ? "127.0.0.1" : args[0];
		String dicPath = (args.length < 2) ? args[0] : args[1];
		Slave slave = new Slave(host, dicPath);
	}
}
