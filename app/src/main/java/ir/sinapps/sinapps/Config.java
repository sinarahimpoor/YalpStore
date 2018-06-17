package ir.sinapps.sinapps;

public class Config {

    public static String baseURL = "http://192.168.1.200/sinapps/api/";
    public static final String URL_REQUEST_SMS = baseURL + "request_sms.php";
    public static final String URL_VERIFY_OTP = baseURL + "verify_otp.php";

    public static final int TAPSELL_REQUEST_CODE = 12;


    // SMS provider identification
    // It should match with your SMS gateway origin
    // You can use  MSGIND, TESTER and ALERTS as sender ID
    // If you want custom sender Id, approve MSG91 to get one


    public static void Config(){

    }




}
