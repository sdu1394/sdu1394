import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.*;
import javax.naming.*;
import javax.transaction.xa.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastSendEventToQueue extends tibjmsPerfCommon implements Serializable
{
	
	private static Logger logger = LoggerFactory.getLogger("skHynixCustomLogger");
    
    private Map<Long, EmsSender> emsSenderPool 						= null;
    private String emsUrl = null;
       
    /**
     * Constructor. JavaGlobalInstance에서 사용 해야 함
     * @param emsServerUrl
     */
    public FastSendEventToQueue(String emsServerUrl) {
    	this.emsUrl = emsServerUrl;
    	this.emsSenderPool = new ConcurrentHashMap<Long, EmsSender>();
    }


    
    private EmsSender getEmsSender(long threadId) {
    	EmsSender emsSender = this.emsSenderPool.get(threadId);
    	
    	if(emsSender == null) {
    		emsSender = new EmsSender(this.emsUrl);		
    		this.emsSenderPool.put(threadId, emsSender);
			logger.info("======> New Thread ID. {}", threadId);
			
			return emsSender;
    	}else {
   		
    		logger.info("======> Get the emsSender from emsSender pool.. {}, emsSender Pool Size: {}", threadId, this.emsSenderPool.size());
    		return emsSender;
    	}	
    }
    
    public void sendJmsMessage(String queueName, String sendMessage, String properties) throws JMSException {
    	EmsSender sender = this.getEmsSender(Thread.currentThread().getId());
    	sender.sendJmsMessage(queueName, sendMessage, properties);
    }
    
    @Deprecated
    public void sendJmsMessage(String queueName, String sendMessage, int count) throws JMSException {
    	for(int i=0; i<count; i++) {
    		this.sendJmsMessage(queueName, sendMessage, null);
    	}
    }
}
    
    