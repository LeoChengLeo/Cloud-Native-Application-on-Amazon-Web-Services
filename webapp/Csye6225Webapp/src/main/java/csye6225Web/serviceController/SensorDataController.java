package csye6225Web.serviceController;

import csye6225Web.models.SensorData;
import csye6225Web.repositories.SensorDataRepository;
import csye6225Web.services.CloudWatchService;
import csye6225Web.services.MqttClientConnector;
import csye6225Web.services.MqttClientConnect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class SensorDataController {

    @Autowired
    SensorDataRepository sensorDataRepository;

    @Autowired
    CloudWatchService cloudWatchService;

    @GetMapping("/iotService/sensorData")
    public List<SensorData> getAllSensorData()
    {

        return sensorDataRepository.findAll();

    }


    @PostMapping("/iotService/sensorData")
    public ResponseEntity<Object> createNewTransaction(@RequestBody SensorData sensorData)
    {

        cloudWatchService.putMetricData("SensorData","Temperature",Double.parseDouble(Float.toString(sensorData.getCurrValue())));

        try
        {
            sensorDataRepository.save(sensorData);
            return ResponseEntity.status(HttpStatus.CREATED).body("CREATE SUCCESS");

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

    }




    @GetMapping("/iotService/actuatorData/airConditioner")
    public ResponseEntity<Object> pubAirConActuatorData(@RequestParam (value="AirConditioner",required = true) String command)
    {

        try
        {

            MqttClientConnector notifier=new MqttClientConnector("tcp","34.238.168.201",1883);
            notifier.connect();
            notifier.publishMessage("iot/actuatorData/airConditioner",0,command.getBytes());
            notifier.disconnect();
            notifier.close();

            return ResponseEntity.status(HttpStatus.CREATED).body("Publish Success!");

        } catch (Exception e)
        {
            System.out.println("Failed to handle Request API /iotService/ActuatorData/AirConditioner.."+e.getMessage());
            return ResponseEntity.badRequest().build();
        }

    }








    @PostMapping("/iotSensordata")

    public ResponseEntity<Object> createTransaction(@RequestBody SensorData sensorData)

    {



        try

        {

            sensorDataRepository.save(sensorData);

            MqttClientConnect notifier=new MqttClientConnect("things.ubidots.com", "A1E-xpFrJY08vApdSFDjpUS9JKQ1JsmPAm","/etc/ubidots_cert.pem");

            notifier.connectUbidots();

            String a =  Float.toString(sensorData.getCurrValue());

            notifier.publishMessage("/v1.6/devices/homeiotgateway/tempsensor",2,a.getBytes());

            notifier.disconnect();

            notifier.close();

            return ResponseEntity.status(HttpStatus.CREATED).body("CREATE SUCCESS");



        } catch (Exception e) {

            System.out.println(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        }



    }





    }
