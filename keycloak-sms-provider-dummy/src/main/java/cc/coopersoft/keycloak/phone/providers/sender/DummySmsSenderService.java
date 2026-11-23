package cc.coopersoft.keycloak.phone.providers.sender;

import cc.coopersoft.keycloak.phone.providers.exception.MessageSendException;
import cc.coopersoft.keycloak.phone.providers.spi.FullSmsSenderAbstractService;
import org.jboss.logging.Logger;

import java.util.Properties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class DummySmsSenderService extends FullSmsSenderAbstractService {
    //public static void main(String args[]){
//    DummySmsSenderService dms = new DummySmsSenderService("");
//    try {
//        dms.sendMessage("+919518798331","org number");
//    }
//    catch (
//            Exception e
//
//    ){e.printStackTrace();}
//}
    private static final Logger logger = Logger.getLogger(DummySmsSenderService.class);
    private static final Properties config = new Properties();

    public DummySmsSenderService(String realmDisplay) {
        super(realmDisplay);
    }

//this is retailer
//    @Override
//    public void sendMessage(String phoneNumber, String message) throws MessageSendException {
//
//        // -------------------- DB1 (Fetch OrgNumber)  this for staging --------------------
////        String DB1_URL = "jdbc:postgresql://c5aqesf78f9ot5.cluster-czrs8kj4isg7.us-east-1.rds.amazonaws.com:5432/d6pe2t1g6qvcmu";
////        String DB1_USER = "u475ldfrmm7k3c";
////        String DB1_PASSWORD = "pd75500e394d1610b3befdefdf7843f72380afaa13200096909eaeeb62a5353a4";
//        // -------------------- DB1 (Fetch OrgNumber)  this for production --------------------
//        String DB1_URL = "jdbc:postgresql://ccj2ci9okh6spb.cluster-czrs8kj4isg7.us-east-1.rds.amazonaws.com:5432/ddqvt27sompbv2";
//        String DB1_USER = "u9u3301niq9j6p";
//        String DB1_PASSWORD = "pa0697fc3d74df41ec31618e63d9b15936b09630315476a6eb3878cd3ffb7a720";
//
//        // -------------------- DB2 (Insert SMS Record) this for staging --------------------
////        String DB2_URL = "jdbc:postgresql://cd7f19r8oktbkp.cluster-czrs8kj4isg7.us-east-1.rds.amazonaws.com:5432/d6dii8ddd1c5sp";
////        String DB2_USER = "u6co2nie5oq323";
////        String DB2_PASSWORD = "pc89e641c85b925dd2c05b1505bae4d661630284ed8b8e528e68b457315405434";
//
//        // -------------------- DB2 (Insert SMS Record) this for production --------------------
//        String DB2_URL = "jdbc:postgresql://ec2-18-232-236-24.compute-1.amazonaws.com:5432/dfge6esau3e9l8";
//        String DB2_USER = "ueisl9vjsbc3fn";
//        String DB2_PASSWORD = "p293ba7ae475f575d921f7ad4d4e377b19749b84c83866e1d802ea547b271b520";
//
//
//        // Query from DB1
//        String queryOrgNumber = "SELECT value FROM salesforce.picklist " +
//                "WHERE object = 'SMS' AND fieldname = 'OrgNumber' LIMIT 1";
//
//        // Insert into DB2 after getting OrgNumber
//        String insertQuery = "INSERT INTO salesforce.tdc_tsw__Scheduled_Sms__c (" +
//                "name, tdc_tsw__channel__c, tdc_tsw__message_text__c, " +
//                "tdc_tsw__phone_api__c, tdc_tsw__related_object_id__c, tdc_tsw__sender_number__c" +
//                ") VALUES (?, ?, ?, ?, ?, ?) RETURNING Heroku_external_id__c";
//
////        String newMessage = message.replace("Connect+", "Connexion");
////        System.out.println("New message ===> " + newMessage);
//        String name = "005GA00000B303BYAR";
//        String channel = "SMS";
//        String messageText = message;
//        String phoneApi = phoneNumber;
//        String relatedObjectId = "005GA00000B303BYAR";
//        String senderNumber = "";
//
//
//        try {
//            try (
//                    Connection conn1 = DriverManager.getConnection(DB1_URL, DB1_USER, DB1_PASSWORD);
//                    PreparedStatement stmt1 = conn1.prepareStatement(queryOrgNumber)
//            ) {
//
//                ResultSet rs1 = stmt1.executeQuery();
//                if (rs1.next()) {
//                    senderNumber = rs1.getString("value");
//                    logger.info("Fetched Sender Name from DB1: " + senderNumber);
//                } else {
//                    logger.warn("No sender name found!");
//                }
//            }
//
//
//            try (
//                    Connection conn2 = DriverManager.getConnection(DB2_URL, DB2_USER, DB2_PASSWORD);
//                    PreparedStatement stmt2 = conn2.prepareStatement(insertQuery)
//            ) {
//
//                stmt2.setString(1, name);
//                stmt2.setString(2, channel);
//                stmt2.setString(3, messageText);
//                stmt2.setString(4, phoneApi);
//                stmt2.setString(5, relatedObjectId);
//                stmt2.setString(6, senderNumber);
//
//
//                ResultSet rs2 = stmt2.executeQuery();
//                if (rs2.next()) {
//                    String herokuId = rs2.getString(1);
//                    logger.info("Inserted into DB2 - Heroku ID: " + herokuId);
//                }
//            }
//
//        } catch (SQLException e) {
//            logger.error("Database Error: " + e.getMessage(), e);
//        }
//    }
    @Override
    public void sendMessage(String phoneNumber, String message) throws MessageSendException {

        // -------------------- DB1 (Fetch OrgNumber)  this for staging --------------------
        String DB1_URL = "jdbc:postgresql://c5aqesf78f9ot5.cluster-czrs8kj4isg7.us-east-1.rds.amazonaws.com:5432/d6pe2t1g6qvcmu";
        String DB1_USER = "u475ldfrmm7k3c";
        String DB1_PASSWORD = "pd75500e394d1610b3befdefdf7843f72380afaa13200096909eaeeb62a5353a4";
        // -------------------- DB1 (Fetch OrgNumber)  this for production --------------------
//        String DB1_URL = "jdbc:postgresql://c8qf22e50d0hk5.cluster-czrs8kj4isg7.us-east-1.rds.amazonaws.com:5432/d609kdrkotk2d3";
//        String DB1_USER = "u1p03k6bd1gfpl";
//        String DB1_PASSWORD = "p3e3e67124bfe603d0ce9afbe37c12a1ebb1b8d7ac90284d1f5941e01322eaf73";

        // -------------------- DB2 (Insert SMS Record) this for staging --------------------
        String DB2_URL = "jdbc:postgresql://cd7f19r8oktbkp.cluster-czrs8kj4isg7.us-east-1.rds.amazonaws.com:5432/d6dii8ddd1c5sp";
        String DB2_USER = "u6co2nie5oq323";
        String DB2_PASSWORD = "pc89e641c85b925dd2c05b1505bae4d661630284ed8b8e528e68b457315405434";

        // -------------------- DB2 (Insert SMS Record) this for production --------------------
//        String DB2_URL = "jdbc:postgresql://ec2-18-232-236-24.compute-1.amazonaws.com:5432/dfge6esau3e9l8";
//        String DB2_USER = "ueisl9vjsbc3fn";
//        String DB2_PASSWORD = "p293ba7ae475f575d921f7ad4d4e377b19749b84c83866e1d802ea547b271b520";


        // Query from DB1
        String queryOrgNumber = "SELECT value FROM salesforce.picklist " +
                "WHERE object = 'SMS' AND fieldname = 'OrgNumber' LIMIT 1";

        // Insert into DB2 after getting OrgNumber
        String insertQuery = "INSERT INTO salesforce.tdc_tsw__Scheduled_Sms__c (" +
                "name, tdc_tsw__channel__c, tdc_tsw__message_text__c, " +
                "tdc_tsw__phone_api__c, tdc_tsw__related_object_id__c, tdc_tsw__sender_number__c" +
                ") VALUES (?, ?, ?, ?, ?, ?) RETURNING Heroku_external_id__c";

//        String newMessage = message.replace("Connect+", "Connexion");
//        System.out.println("New message ===> " + newMessage);
        String name = "005GA00000B303BYAR";
        String channel = "SMS";
        String messageText = message;
        String phoneApi = phoneNumber;
        String relatedObjectId = "005GA00000B303BYAR";
        String senderNumber = "";


        try {
            try (
                    Connection conn1 = DriverManager.getConnection(DB1_URL, DB1_USER, DB1_PASSWORD);
                    PreparedStatement stmt1 = conn1.prepareStatement(queryOrgNumber)
            ) {

                ResultSet rs1 = stmt1.executeQuery();
                if (rs1.next()) {
                    senderNumber = rs1.getString("value");
                    logger.info("Fetched Sender Name from DB1: " + senderNumber);
                } else {
                    logger.warn("No sender name found!");
                }
            }


            try (
                    Connection conn2 = DriverManager.getConnection(DB2_URL, DB2_USER, DB2_PASSWORD);
                    PreparedStatement stmt2 = conn2.prepareStatement(insertQuery)
            ) {

                stmt2.setString(1, name);
                stmt2.setString(2, channel);
                stmt2.setString(3, messageText);
                stmt2.setString(4, phoneApi);
                stmt2.setString(5, relatedObjectId);
                stmt2.setString(6, senderNumber);


                ResultSet rs2 = stmt2.executeQuery();
                if (rs2.next()) {
                    String herokuId = rs2.getString(1);
                    logger.info("Inserted into DB2 - Heroku ID: " + herokuId);
                }
            }

        } catch (SQLException e) {
            logger.error("Database Error: " + e.getMessage(), e);
        }
    }
    @Override
    public void close() {
    }
}