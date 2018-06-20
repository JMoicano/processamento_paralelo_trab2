package br.inf.ufes.attack;

import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.TextMessage;

import com.sun.messaging.ConnectionConfiguration;

import br.inf.ufes.ppd.Attacker;
import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.ProducerConsumer;
import br.inf.ufes.ppd.SubAttack;

public class Master extends ProducerConsumer implements Attacker, MessageListener {
	private Map<Integer, EncapInt> pendingSubAttacks;
	private Map<Integer, List<Guess>> guesses;
	private int currentAttack;
	
	private static final int dict_size = 80368;
	
	public Master(String host) {
		super(host);
		currentAttack = 0;
		pendingSubAttacks = new HashMap<>();
		guesses = new HashMap<>();
		
		consumer = context.createConsumer(guessQueue);
		consumer.setMessageListener(this);
	}
	@Override
	public Guess[] attack(byte[] ciphertext, byte[] knowntext, int subAttackSize) {
		int attackNumber = currentAttack++;
		EncapInt pendingSubAttack;
		synchronized (pendingSubAttacks) {
			pendingSubAttack = new EncapInt();
			pendingSubAttacks.put(attackNumber, pendingSubAttack);
		}
		synchronized (guesses) {
			guesses.put(attackNumber, new ArrayList<>());
		}
		for (int i = 0; i < dict_size; i+=subAttackSize) {
			int finalwordindex = i + subAttackSize < dict_size ? i + subAttackSize : dict_size; 
			SubAttack sa = new SubAttack(ciphertext, knowntext, i, finalwordindex - 1, attackNumber);
			ObjectMessage m = context.createObjectMessage();
			try {
				m.setObject(sa);
				producer.send(subAttackQueue, m);
				pendingSubAttack.v++;
				System.out.println("SubAttack #" + pendingSubAttack.v + " for Attack #" + attackNumber + " launched!");
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
		
		try {
			synchronized (pendingSubAttack) {
				pendingSubAttack.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Guess[] retorno;
		synchronized (guesses) {
			retorno = guesses.remove(attackNumber).toArray(new Guess[0]);
		}
		
		return retorno;
		
	}
	@Override
	public void onMessage(Message msg) {
		try {
			int attackNumber = msg.getIntProperty("attackNumber");
			if(msg instanceof ObjectMessage) {
				Object obj = ((ObjectMessage) msg).getObject();
				if(obj instanceof Guess) {
					Guess g = (Guess) obj;
					synchronized (guesses) {
						guesses.get(attackNumber).add(g);
					}
				}
			
			} else {
				
				EncapInt pendingSubAttack;
				synchronized (pendingSubAttacks) {
					pendingSubAttack = pendingSubAttacks.get(attackNumber);
				}
				synchronized (pendingSubAttack) {
					pendingSubAttack.v--;
					System.out.println(pendingSubAttack.v + " pending subAttacks for Attack #" + attackNumber);
					if(pendingSubAttack.v == 0) {
						synchronized (pendingSubAttacks) {
							pendingSubAttacks.remove(attackNumber);
						}
						pendingSubAttack.notify();
					}
				}
				
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	private class EncapInt {
		public int v = 0;
	}
	public static void main(String[] args) {
		String host = (args.length < 1) ? "127.0.0.1" : args[0];
		try {
			//Create an instance and register a reference to master in registry with name "master"
			Master mestre = new Master(host);
			Attacker mestreref = (Attacker) UnicastRemoteObject.exportObject(mestre, 0);
			Registry registry = LocateRegistry.getRegistry(); // opcional: host
			registry.rebind("mestre", mestreref);
		    System.err.println("Master online");
		} catch (Exception e) {
			System.err.println("Server exception: " + e.toString()); 
			e.printStackTrace();
		}

	}
	
}
