package br.inf.ufes.attack;

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Queue;

public class Slave {
	Queue subAttackQueue;
	Queue guessQueue;
	JMSContext context;
	JMSProducer producer;
	JMSConsumer consumer;
}
