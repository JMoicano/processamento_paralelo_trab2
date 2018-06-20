package br.inf.ufes.ppd;

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;

import com.sun.messaging.ConnectionConfiguration;
import com.sun.messaging.Queue;

public class ProducerConsumer {
	protected Queue subAttackQueue;
	protected Queue guessQueue;
	protected JMSContext context;
	protected JMSProducer producer;
	protected JMSConsumer consumer;

	public ProducerConsumer(String host) {
		try {
			System.out.println("obtaining connection factory...");
			com.sun.messaging.ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
			
			connectionFactory.setProperty(ConnectionConfiguration.imqAddressList,host+":7676");
			connectionFactory.setProperty(ConnectionConfiguration.imqConsumerFlowLimitPrefetch,"false");
			
			System.out.println("obtained connection factory.");
			
			System.out.println("obtaining queues...");
			subAttackQueue = new com.sun.messaging.Queue("SubAttackQueue");
			guessQueue = new com.sun.messaging.Queue("GuessQueue");
			System.out.println("obtained queues.");
	
			context = connectionFactory.createContext();
			producer = context.createProducer();
			
		} catch (JMSException e) {
			e.printStackTrace();
		}	
	}
}
