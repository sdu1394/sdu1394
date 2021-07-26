

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.jms.*;
import javax.naming.*;
import javax.transaction.xa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmsSender extends tibjmsPerfCommon implements Serializable
{
	
	private static Logger logger = LoggerFactory.getLogger("skHynixCustomLogger");
	    
    private ConnectionFactory   factory	   = null;
    private Session session     									= null;
    private TextMessage  msg				   						= null;
    private Map<String, Destination>		destinationMap     		= new ConcurrentHashMap<String, Destination>();
    private Map<String, MessageProducer>	messageProducerMap  	= new ConcurrentHashMap<String, MessageProducer>();

    // get the connection
    private Connection connection = null;
       
    public EmsSender(String emsServerUrl) {
    	String[] options = {"-server", emsServerUrl};
    	
    	logger.info("emsServerUrl: {}", emsServerUrl);
    	
    	if (connection == null) {
//            parseArgs(options);
            serverUrl = emsServerUrl;
            prepareConnection();
    	}

    }

    /** 
     * The producer thread's run method.
     */
    private void prepareConnection()
    {      
        try
        {
            
            factory = new com.tibco.tibjms.TibjmsConnectionFactory(serverUrl);
            connection = factory.createConnection(username,password);           
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            
            logger.info("EmsSender resource created.");

        }
        catch (JMSException e)
        {
        	logger.error("JMSException", e);
        }

    }

    public void sendJmsMessage(String queueName, String sendMessage, String properties) throws JMSException {
    	long sTime = System.currentTimeMillis();    
    	Destination destination = null;
    	if (destinationMap.containsKey(queueName) == false) {
    		destination = session.createQueue(queueName);
    		destinationMap.put(queueName, destination);
    	} else {
    		destination = destinationMap.get(queueName);
    	}    	
    	MessageProducer msgProducer = null; 
    	if (messageProducerMap.containsKey(queueName) == false) {
        	msgProducer = session.createProducer(destination);
        	// set the delivery mode
            msgProducer.setDeliveryMode(javax.jms.DeliveryMode.PERSISTENT);
            messageProducerMap.put(queueName, msgProducer);    		
    	} else {
    		msgProducer = messageProducerMap.get(queueName);
    	}
    	
    	msgProducer.setDisableMessageID(true);
        msgProducer.setDisableMessageTimestamp(true);
	    	
    	// create the message
        if (msg == null) {
        	msg = session.createTextMessage();
        } else {
        	msg.clearBody();
        }

        msg.setText(sendMessage);

        if (properties != null && properties.trim().length() > 0) {
        	
        	logger.info(" [sendJmsMessage] properties from BW : [{}] " , properties);
        	String[] propertyList = properties.split("\\|\\|");
        	
        	for (int i=0; i<propertyList.length; i++) {
        		String[] propertyItem = propertyList[i].split("\\:\\:");
        		
        		if(propertyItem != null && propertyItem.length == 2) {
        			msg.setStringProperty(propertyItem[0], propertyItem[1]);
        			
        			logger.info(" [sendJmsMessage] propertyItem [key:{}, value:{}] " , propertyItem[0], propertyItem[1]);
        		}
        	}        	 
        }
        msgProducer.send(msg);
        logger.info(" [sendJmsMessage] Send performance {} " , System.currentTimeMillis() - sTime);
    }

	private void usage()
	{
		logger.error("\nUsage: You did not use correct option.");
	    logger.error("\n");
	
	    logger.error("");
	    logger.error("   -server       <server URL>  - EMS server URL, default is local server");
	    logger.error("   -user         <user name>   - user name, default is null");
	    logger.error("   -password     <password>    - password, default is null");
	    logger.error("   -queue        <queue-name>  - queue name, no default");
	    
	    System.exit(0);
	}

	/**
	 * Parse the command line arguments.
	 */
	private void parseArgs(String[] args)
	{
		if (connection != null ) return;
	    int i=0;
	    while (i < args.length)
	    {
	        if (args[i].compareTo("-server")==0)
	        {
	            if ((i+1) >= args.length) usage();
	            serverUrl = args[i+1];
	            i += 2;
	        }
	        else if (args[i].compareTo("-queue")==0)
	        {
	            if ((i+1) >= args.length) usage();
	            destName = args[i+1];
	            i += 2;
	            useTopic = false;
	        }
	        else if (args[i].compareTo("-topic")==0)
	        {
	            if ((i+1) >= args.length) usage();
	            destName = args[i+1];
	            i += 2;
	            useTopic = true;
	        }
	        else if (args[i].compareTo("-user")==0)
	        {
	            if ((i+1) >= args.length) usage();
	            username = args[i+1];
	            i += 2;
	        }
	        else if (args[i].compareTo("-password")==0)
	        {
	            if ((i+1) >= args.length) usage();
	            password = args[i+1];
	            i += 2;
	        }
	        else
	        {
	            logger.error("Error: invalid option: " + args[i]);
	        }
	    }
	}
	/**
	 * main
	 * @throws JMSException 
	 */
	public static void main(String[] args) throws JMSException
	{    	
		EmsSender sender = new EmsSender("tcp://localhost:7222");
		sender.sendJmsMessage("INHO.4TMN0012", "Hello world", null);
	}
}