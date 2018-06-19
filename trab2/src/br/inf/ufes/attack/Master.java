package br.inf.ufes.attack;

import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;

import com.sun.messaging.ConnectionConfiguration;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.SubAttack;

public class Master implements Remote {
	private Map<Integer, Integer> pendingSubAttacks;
	private int currentAttack;
	Queue subAttackQueue;
	Queue guessQueue;
	JMSContext context;
	JMSProducer producer;
	JMSConsumer consumer;
	
	private static final int dict_size = 80368;
	
	public Guess[] attack(byte[] ciphertext, byte[] knowntext, int subAttackSize) {
		int attackNumber = currentAttack++;
		pendingSubAttacks.put(attackNumber, 0);
		for (int i = 0; i < dict_size; i+=subAttackSize) {
			int finalwordindex = i + subAttackSize < dict_size ? i + subAttackSize : dict_size; 
			SubAttack sa = new SubAttack(ciphertext, knowntext, i, finalwordindex - 1, attackNumber);
			ObjectMessage m = context.createObjectMessage();
			try {
				m.setObject(sa);
				producer.send(subAttackQueue, m);
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
		
		return null;
		
	}
	
	public Master(String host) {
		currentAttack = 0;
		pendingSubAttacks = new HashMap<>();
		try {
			System.out.println("obtaining connection factory...");
			com.sun.messaging.ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
			
			connectionFactory.setProperty(ConnectionConfiguration.imqAddressList,host+":7676");
			
			System.out.println("obtained connection factory.");
			
			System.out.println("obtaining queues...");
			subAttackQueue = new com.sun.messaging.Queue("SubAttackQueue");
			guessQueue = new com.sun.messaging.Queue("GuessQueue");
			System.out.println("obtained queues.");
	
			context = connectionFactory.createContext();
			producer = context.createProducer();
			consumer = context.createConsumer(guessQueue);
			
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
	}
	public static void main(String[] args) {
		String host = (args.length < 1) ? "127.0.0.1" : args[0];
		try {
			//Create an instance and register a reference to master in registry with name "master"
			Master mestre = new Master(host);
			Master mestreref = (Master) UnicastRemoteObject.exportObject(mestre, 0);
			Registry registry = LocateRegistry.getRegistry(); // opcional: host
			registry.rebind("mestre", mestreref);
		    System.err.println("Master online");
		} catch (Exception e) {
			System.err.println("Server exception: " + e.toString()); 
			e.printStackTrace();
		}

	}
	

}
