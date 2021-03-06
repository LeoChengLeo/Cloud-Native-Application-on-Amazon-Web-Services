package csye6225Web.serviceController;


import csye6225Web.models.Receipt;
import csye6225Web.models.Transaction;
import csye6225Web.repositories.ReceiptRepository;
import csye6225Web.repositories.TransactionRepository;
import csye6225Web.services.CloudWatchService;
import csye6225Web.services.S3Service;
import csye6225Web.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
public class ReceiptController {

    @Autowired
    private ReceiptRepository receiptRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private CloudWatchService cloudWatchService;
    @Autowired
    private UserService userService;
    @Autowired
     private S3Service s3Service;



    Double get_attachments=0.0;
    Double post_attachment=0.0;
    Double put_attachment=0.0;
    Double delete_attachment=0.0;


    @GetMapping("/transaction/{id}/attachments")
    public ResponseEntity<Object> getAttachments(@RequestHeader(value="username",required = true) String username,
                                                 @RequestHeader(value="password",required = true) String password,
                                                 @PathVariable(value = "id") long id)
    {


        cloudWatchService.putMetricData("GetRequest","/transaction/{id}/attachments",++get_attachments);
        if(!userService.userIsValid(username,password)){return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username and password");}



        for(Transaction tran: userService.findUser(username).getTransactions())
        {
            if(tran.getId()==id)
            {
                return ResponseEntity.ok().body(tran.getAttachments());
            }

        }


        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ID NOT FOUND");


    }


    @PostMapping("/transaction/{id}/attachment")
    public ResponseEntity<Object> postNewAttachment(@RequestHeader(value="username",required = true) String username,
                                                    @RequestHeader(value="password",required = true) String password,
                                                    @RequestParam(value = "receipt", required = true) MultipartFile receipt,
                                                    @PathVariable(value = "id") long id)
    {


        cloudWatchService.putMetricData("PostRequest","/transaction/{id}/attachment",++post_attachment);
        if(!userService.userIsValid(username,password)){return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username and password");}


        for(Transaction tran: userService.findUser(username).getTransactions())
        {
            if(tran.getId()==id)
            {
                String receiptURL=s3Service.uploadAttachment(username,receipt);
                if(receiptURL==null){return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("attachment might not valid\n");}

                    Receipt _receipt=new Receipt();
                    _receipt.setUrl(receiptURL);
                    _receipt.setTransaction(tran);
                    tran.getAttachments().add(_receipt);
                    receiptRepository.save(_receipt);

                    return ResponseEntity.ok().body(_receipt);

            }
         }


         return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ID NOT FOUND");


     
    }

    @PostMapping("transaction/{id}/attachment/{attachmentID}")
    public ResponseEntity<Object> updateAttachment(@RequestHeader(value="username",required = true) String username,
                                                   @RequestHeader(value="password",required = true) String password,
                                                   @RequestParam(value = "receipt", required = true) MultipartFile receipt,
                                                   @PathVariable(value="id") long id ,
                                                   @PathVariable(value="attachmentID") long attachID)
    {

        cloudWatchService.putMetricData("PutRequest","/transaction/{id}/attachment/{attachmentID}",++put_attachment);
        if(!userService.userIsValid(username,password)){return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username and password");}

           for(Transaction tran: userService.findUser(username).getTransactions())
           {
               if(tran.getId()==id)
               {

                   for(Receipt rece:tran.getAttachments())
                   {

                       if(rece.getId()==attachID)
                       {

                           try
                           {
                               s3Service.updateAttachment(username,rece.getUrl(),receipt);
                               return ResponseEntity.ok().body(rece);

                           } catch (Exception e)
                           {
                               return ResponseEntity.badRequest().body(e);
                           }

                       }


                   }

                   return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Attachment ID NOT FOUND\n");

               }
            }


            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Transaction ID NOT FOUND\n");
           
    }





    @DeleteMapping("transaction/{id}/attachment/{attachmentID}")
    public ResponseEntity<Object> deleteAttachment(@RequestHeader(value="username",required = true) String username,
                                                   @RequestHeader(value="password",required = true) String password,
                                                   @PathVariable(value = "id") long id,
                                                   @PathVariable(value="attachmentID") long attachID)
    {

        cloudWatchService.putMetricData("DeleteRequest","/transaction/{id}/attachment/{attachmentID}",++delete_attachment);
        if(!userService.userIsValid(username,password)){return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username and password\n");}



        for(Transaction tran: userService.findUser(username).getTransactions())
        {

            if(tran.getId()==id)
            {
                try
                {


                    for(Receipt r:tran.getAttachments())
                    {
                        if (r.getId() == attachID)
                        {

                            s3Service.deleteAttachment(username,r.getUrl());
                            tran.getAttachments().remove(r);
                            receiptRepository.deleteById(attachID);
                            return ResponseEntity.noContent().build();

                        }
                    }

                   return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Attachment ID NOT FOUND\n");

                } catch (Exception e)
                {
                    return ResponseEntity.badRequest().body(e);
                }


            }
         }


         return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Transaction ID NOT FOUND\n");

    }


}
