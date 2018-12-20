package csye6225Web.services;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;




public class MqttClientConnector implements MqttCallback {
	
	
	String _protocol;
	String _host;
	String _port;
	
	protected String _clientID;
	protected String _brokerAddr;
	protected MqttClient _mqttClient;
    protected boolean isSecure;
	protected String _pemFileName;
	
	
	
	//use this constructor to connect the remote broker with clean session 
	public MqttClientConnector(String protocol,String host, int port)
	{				
		isSecure=false;
		_protocol=protocol;
		_host=host;
		_port=Integer.toString(port);
		_clientID=MqttClient.generateClientId();
		_brokerAddr=protocol+"://"+host+":"+port;
	}
	
	
	//use this constructor to connect the broker using a SSL connection
	public MqttClientConnector(String host, String caFilePath)
	{
		isSecure=true;
		_protocol="ssl";
		_host=host;
		_port="8883";
		_clientID=MqttClient.generateClientId();
		_brokerAddr=_protocol+"://"+host+":8883";
		
	}
	
	
	public void connect() 
	{
		if(_mqttClient==null)
		{
			
			try 
			{
				_mqttClient= new MqttClient(_brokerAddr, _clientID,new MemoryPersistence());
				MqttConnectOptions option= new MqttConnectOptions();
				option.setConnectionTimeout(20);
				
				
				if(isSecure)
				{
					initSecureConnection(option);
				}
				else
				{
					option.setCleanSession(true);
				}
				
				
				_mqttClient.setCallback(this);
				_mqttClient.connect(option);
				System.out.println("Connect to Broker:"+_brokerAddr);
			 
			}
			catch(MqttException e)
			{
				System.out.println(e.toString());
				System.out.println(e.getMessage());
				
			}
			catch(Exception e)
			{
				System.out.println(e.getMessage()+" "+e.toString());
			}
			
		}
		else
		{
			System.out.println("MqttClient has not been intialized");
		}
		
		
	}



	public void close()
	{

		try
		{
			_mqttClient.close();
		}
		catch (Exception e) {
			System.out.println("Could not close _mqttClient thread....");
		}

	}

	
	
	private void initSecureConnection(MqttConnectOptions options)
	 {
	 try {
		 
		 	SSLContext sslContext = SSLContext.getInstance("SSL");
		 	KeyStore keyStore = readCertificate();
		    TrustManagerFactory trustManagerFactory =
		    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		    trustManagerFactory.init(keyStore);
		    sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
		    options.setSocketFactory(sslContext.getSocketFactory());
		    
	 } catch (Exception e) 
	 {
	 
		 System.out.println("Failed to initialize secure MQTT connection "+e.getMessage());
	 }
	 
	 }
	
	
	
	
	 private KeyStore readCertificate() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
	 {
		 
		 KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		 FileInputStream fis = new FileInputStream(_pemFileName);
		 BufferedInputStream bis = new BufferedInputStream(fis);
		 CertificateFactory cf = CertificateFactory.getInstance("X.509");
		 ks.load(null);
		 while (bis.available() > 0) 
		 {
			 Certificate cert = cf.generateCertificate(bis);
			 ks.setCertificateEntry("adk_store" + bis.available(), cert);
	     }
	     return ks;
	 }

	
	
	
	public void disconnect()
	{
	   try
	   {
		   _mqttClient.disconnect();
		   System.out.println("MqttClient disconnect from:"+_brokerAddr);
	   }
	   catch(MqttException e)
	   {
		   System.out.println(e.toString());
		   System.out.println(e.getMessage());
	   }
	   catch(Exception e)
	   {
			System.out.println(e.getMessage()+" "+e.toString());
	   }
			
	}
	
	
	
	
	
	public boolean publishMessage(String topic, int qos, byte[] payload)
	{
		
		try
		{ 
			MqttMessage message= new MqttMessage();
			message.setPayload(payload);
			message.setQos(qos);
			System.out.println("Publishing message to topic:"+topic+"...");
			_mqttClient.publish(topic, message);
			System.out.println("Successfully published message to topic:"+topic+" MqttMessageID:"+message.getId());
			return true;
			
		}
		catch (MqttException e)
		{
			System.out.println(e.toString());
			System.out.println(e.getMessage());
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage()+" "+e.toString());
		}
		
		return false;
		
	}
	
	
	
	public boolean subscribeAll()
	{
		try
		{
			_mqttClient.subscribe("$SYS/#");
			return true;
		}
		catch(MqttException e)
		{
			System.out.println(e.toString());
			System.out.println(e.getMessage());
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage()+" "+e.toString());
		}
		
		return false;
		
	}


		
	public boolean subscribeTopic(String topic, int qos)
	{
		
        try
        {
		_mqttClient.subscribe(topic,qos);
		System.out.println("Subscribed to Topic:"+topic);
		return true;
        }
        catch(MqttException e)
        {
			System.out.println(e.toString());
			System.out.println(e.getMessage());
        }
		
		return false;
		
	}
	
	

	public void connectionLost(Throwable cause) {
		
		System.out.println("Connection lost!");
		
	}
	
	public void deliveryComplete(IMqttDeliveryToken token) {
		
		System.out.println("Delievey Complete!");
		
	}
	

	public void messageArrived(String topic, MqttMessage message) throws Exception {
		
		System.out.println("Message Arrived MessageID:"+message.getId());
		System.out.println(message.toString());
		
	}

}
