package csye6225Web.services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttClientConnect implements MqttCallback {
    private String _protocol;
    private String _host;
    private int _port;

    private String _clientID;
    private String _brokerAddr;
    private MqttClient _mqttClient;
    private String _userName;
    private String _pemFileName;
    private boolean _isSecureConn;
    private static final Logger _Logger = Logger.getLogger(MqttClientConnect.class.getName());

    // Constructor to set the token host and pemFileName file
    public MqttClientConnect(String host, String userName, String pemFileName) {
        super();
        if (host != null && host.trim().length() > 0) {
            _host = host;
        }
        if (userName != null && userName.trim().length() > 0) {
            _userName = userName;
        }
        if (pemFileName != null) {
            File file = new File(pemFileName);
            if (file.exists()) {
                _protocol = "ssl";
                _port = 8883;
                _pemFileName = pemFileName;
                _isSecureConn = true;
                _Logger.info("PEM file valid. Using secure connection: " + _pemFileName);
            } else {
                _Logger.warning("PEM file invalid. Using insecure connection: " + pemFileName);
            }
        }
        _clientID = MqttClient.generateClientId();
        _brokerAddr = _protocol + "://" + _host + ":" + _port;
        _Logger.info("Using URL for broker conn: " + _brokerAddr);
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
    // create the connection with Ubidots
    public void connectUbidots() {
        if (_mqttClient == null) {
            MemoryPersistence persistence = new MemoryPersistence();
            try {
                _mqttClient = new MqttClient(_brokerAddr, _clientID, persistence);

                MqttConnectOptions connOpts = new MqttConnectOptions();
                // TODO: do we always want a clean session?
                connOpts.setCleanSession(true);
                if (_userName != null) {
                    connOpts.setUserName(_userName);
                }
                // if we are using a secure connection, there's a bunch of stuff we need to
                // do...
                if (_isSecureConn) {
                    initSecureConnection(connOpts);
                }
                _mqttClient.setCallback(this);
                _mqttClient.connect(connOpts);
                _Logger.info("Connected to broker: " + _brokerAddr);
            } catch (MqttException e) {
                _Logger.log(Level.SEVERE, "Failed to connect to broker: " + _brokerAddr, e);
            }
        }
    }

    // Configuring the TLS, which is using secure MQTT connection
    private void initSecureConnection(MqttConnectOptions connOpts) {
        try {
            _Logger.info("Configuring TLS...");
            SSLContext sslContext = SSLContext.getInstance("SSL");
            KeyStore keyStore = readCertificate();
            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());

            trustManagerFactory.init(keyStore);
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

            connOpts.setSocketFactory(sslContext.getSocketFactory());
            _Logger.info("Configuring TLS done...");

        } catch (Exception e) {
            _Logger.log(Level.SEVERE, "Failed to initialize secure MQTT connection.", e);
        }
    }

    // read the PEMfile
    private KeyStore readCertificate()
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream fis = new FileInputStream(_pemFileName);
        BufferedInputStream bis = new BufferedInputStream(fis);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        ks.load(null);
        while (bis.available() > 0) {
            Certificate cert = cf.generateCertificate(bis);
            ks.setCertificateEntry("jy_store" + bis.available(), cert);
        }
        return ks;
    }

    // publish message function, send data to broker
    public boolean publishMessage(String topic, int qos, byte[] payload) {
        // set tag to check if success
        boolean success = false;
        try {
            _Logger.info("Publishing message to topic: " + topic);
            MqttMessage message = new MqttMessage();
            // payload data add in message
            message.setPayload(payload);
            // Qos level set
            message.setQos(qos);

            _mqttClient.publish(topic, message);
            _Logger.info("Published Success! MqttMessageID:" + message.getId());
            success = true;
        } catch (MqttException e) {
            _Logger.log(Level.SEVERE, "Failed to publish MQTT message: " + e.getMessage());
        }
        return success;
    }

    // subscribe the topic only, filter other unnecessary topic, and set the Qos
    // level
    public boolean subscribeTopic(String topic, int qos) {
        try {
            _mqttClient.subscribe(topic, qos);
            _Logger.info("Subscribed to Topic:  " + topic);
            return true;
        } catch (MqttException e) {
            _Logger.log(Level.WARNING, "Failed to subscribe  topics.", e);
        }
        return false;
    }

    // subscribeToAll get all msg
    public boolean subscribeToAll() {
        try {
            _mqttClient.subscribe("$SYS/#");
            return true;
        } catch (MqttException e) {
            _Logger.log(Level.WARNING, "Failed to subscribe to all topics.", e);
        }
        return false;
    }

    @Override
    public void connectionLost(Throwable cause) {
        _Logger.log(Level.WARNING, "Connection to broker lost. Will retry soon.", cause);

    }

    @Override
//		 Called when a message arrives from the server that matches any
//		 subscription made by the client
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        _Logger.info("Message arrived: " + topic + ", " + new String(message.getPayload()));

    }

    // check if publish API send to broker
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        try {
            _Logger.info("Delivery complete: " + token.getMessageId() + " - " + token.getResponse() + " - "
                    + token.getMessage());
        } catch (Exception e) {
            _Logger.log(Level.SEVERE, "Failed to retrieve message from token.", e);
        }

    }

}
