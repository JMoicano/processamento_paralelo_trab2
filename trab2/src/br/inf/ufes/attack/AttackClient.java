package br.inf.ufes.attack;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import br.inf.ufes.ppd.Attacker;
import br.inf.ufes.ppd.Guess;
public class AttackClient {
	public static void main(String args[]) {
		String host = args[0]; //adress to master host
		String dicPath = args[1]; //path to dctionary
		String inFilePath = args[2]; //path to encrypted file
		String knownWord = args[3]; //know word in the file
		Boolean optimize = false;
		ArrayList<String> _dict;
		String cryptKey = null;
		int size = 0;
		if(args.length == 5) {
			size = Integer.parseInt(args[4]); //size in bytes of the encrypted file
		} else if (args.length == 6) {
			optimize = (args[5] == "true")?true:false;
		}
			
		File inFile = new File(inFilePath);
		byte[] bytes;

		File f = new File(dicPath);
		
		//Load dictionary
		try(FileReader fileReader = new FileReader(f);
			BufferedReader b = new BufferedReader(fileReader)) {
			_dict = new ArrayList<String>();
			String readLine = "";
			while ((readLine = b.readLine()) != null) {
				_dict.add(readLine);
			}
			cryptKey = _dict.get(new Random().nextInt(_dict.size()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		byte[] criptedFile = null;
		
		//if encrypted file doesn't exists, generate a random one 
		if(!inFile.exists()) {
			if(size == 0) {
				Random r = new Random();
				//random size between 50000 and 200000
				size = r.nextInt(150000) + 50000 - knownWord.length();
			}
			
			bytes = new byte[size];
			try (FileOutputStream fos = new FileOutputStream(inFile)){
				new Random().nextBytes(bytes);
				
				//Concatenate generated file with known word
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
				outputStream.write(knownWord.getBytes());
				outputStream.write(bytes);

				bytes = outputStream.toByteArray();
				if (cryptKey == null) cryptKey = "madman";
				saveFile("./" + cryptKey + ".ori", bytes);
				criptedFile = encryptMsg(cryptKey, bytes);
				fos.write(criptedFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();	
			}
		}
		try {
			criptedFile = Files.readAllBytes(inFile.toPath());
			
		} catch (IOException e1) {
			
			e1.printStackTrace();
		}
		Registry registry;
		
		try {
			registry = LocateRegistry.getRegistry(host);
			Attacker mestre = (Attacker) registry.lookup("mestre");
			long startTime;// = System.nanoTime();
			Guess[] guesses;

			int newAttackTimeSize = -1;
			if (!optimize) {
				for (int i = 100; i <= 40000; i+=300) {
					startTime = System.nanoTime();
					guesses = mestre.attack(criptedFile, knownWord.getBytes(), i);
					double time_diff = (System.nanoTime() - startTime)/1000000;
					System.out.println(i + ";" + time_diff + ";" + 	size);
					for(Guess g : guesses) {
						saveFile(g.getKey() + ".msg", g.getMessage());
					}	
				}
			} else {
				// first run to min attack size
				int minAttackSize = 100;
				int maxAttackSize = 40000;
				startTime = System.nanoTime();
				guesses = mestre.attack(criptedFile, knownWord.getBytes(), minAttackSize);
				double shortAttackTime = (System.nanoTime() - startTime)/1000000;
				startTime = System.nanoTime();
				guesses = mestre.attack(criptedFile, knownWord.getBytes(), maxAttackSize);
				double longAttackTime = (System.nanoTime() - startTime)/1000000;
				double optimumTime = 9999999.0;
				
				double minAttackTime;
				double maxAttackTime;
				int minAttackTimeSize;
				int maxAttackTimeSize;
				
				if (longAttackTime >= shortAttackTime) {
					maxAttackTime = longAttackTime;
					maxAttackTimeSize = maxAttackSize;
					minAttackTime = shortAttackTime;
					minAttackTimeSize = minAttackSize;
				} else {
					minAttackTime = longAttackTime;
					minAttackTimeSize = maxAttackSize;
					maxAttackTime = shortAttackTime;
					maxAttackTimeSize = minAttackSize;
				}
				
				//int actualAttackSize = (minAttackSize + maxAttackSize)/2;
				double attackTime = minAttackTime;
				int count = 0;
				int repeat = 0;
				while (count < 10000 || repeat < 3) {
					System.out.println("Attack #" + (count+1));
					newAttackTimeSize = ((minAttackTimeSize + maxAttackTimeSize)/2);
					startTime = System.nanoTime();
					guesses = mestre.attack(criptedFile, knownWord.getBytes(), newAttackTimeSize);
					double newAttackTime = (System.nanoTime() - startTime)/1000000;
					// novo atack esta no intervalo
					if (newAttackTime >= minAttackTime && newAttackTime <= maxAttackTime) {
						maxAttackTimeSize = newAttackTimeSize;
						maxAttackTime = newAttackTime;
					} else if (newAttackTime <= minAttackTime) {
						maxAttackTimeSize = minAttackTimeSize;
						maxAttackTime = minAttackTime;
						minAttackTimeSize = newAttackTimeSize;
						minAttackTime = newAttackTime;
					}
					
					if ((newAttackTimeSize == ((minAttackTimeSize + maxAttackTimeSize)/2)) || (Math.abs(newAttackTime - attackTime) < 0.003)) repeat++;
					count++;
				}
				System.out.println("count: " + count + " repeat: " + repeat + " bestAttackSize: " + newAttackTimeSize);
			}
			//int attackSize = 4000; //TODO: ajustar isso para colocar num loop quando for testar
			
		} catch (NotBoundException | IOException e) {
			e.printStackTrace();
		}

		
	}
	
	//Save a byte array into a file
	private static void saveFile(String filename, byte[] data) throws IOException {

		FileOutputStream out = new FileOutputStream(filename);
		out.write(data);
		out.close();

	}
	
	//encrypt a message (to use when generating a random file
	private static byte[] encryptMsg(String k, byte[] message) {
		try {
			byte[] key = k.getBytes();
			SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");

			Cipher cipher = Cipher.getInstance("Blowfish");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec);

			//System.out.println("message size (bytes) = "+message.length);

			byte[] encrypted = cipher.doFinal(message);

			return encrypted;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return message;
	}

}
