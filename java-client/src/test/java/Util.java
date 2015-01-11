import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import static org.testng.Assert.assertEquals;

/**
 * Created by sven on 2015-01-09.
 */
public class Util {

    public static int defaultBalance = 500;

    public static String filename = "localSystem.ini";
    public static Map<String,String> shellStrings = new HashMap<String,String>();
    public static String defaultLanguage;
    public static Map<Integer,Process> processMap = new HashMap<Integer,Process>();
    static {
        Preferences prefs = null;
        try {
            prefs = new IniPreferences(new Ini(new File(filename)));
        } catch (IOException e) {
            System.out.println("Preferences load failure");
            System.exit(1);
        }
        String python = prefs.node("shellStrings").get("python", null);
        shellStrings.put("python",python);
        String java = prefs.node("shellStrings").get("java", null);
        shellStrings.put("java",java);
        defaultLanguage = prefs.node("shellStrings").get("default", null);

        if (python == null || java == null || defaultLanguage == null) {
            System.err.println("shellStrings not complete, check " + filename);
            System.exit(1);
        }
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static void runServer(String IP, int port, long balance, String language) throws IOException {

        String execString = shellStrings.get(language) + " " + IP + " " + port + " " + balance;
        Process proc = Runtime.getRuntime().exec(execString);
        System.out.println(execString);
        processMap.put(port,proc);
    }

    public static void runServer(String IP, int port, long balance) throws IOException {
        runServer(IP,port,balance,defaultLanguage);
    }

    public static void runServer(int port) throws IOException {
        runServer("localhost", port, defaultBalance);
    }

    public static void runNServers(int portlow, int count) throws IOException {
        for (int i=0;i<count;++i)
        {
            runServer(portlow + i);
        }
    }

    public static void pingNServers(int portlow, int count) throws TException {
        for (int i=0;i<count;++i)
        {
            ThriftTestClient.pingserver(portlow+i);
        }
    }

    public static void pingNServersExpectFail(int portlow, int count) throws TException {
        for (int i=0;i<count;++i)
        {
            try{
                //ping again - ping should throw an exception
                ThriftTestClient.pingserver(portlow+i);
                assertEquals("Port should be down",false);
            }
            catch (TTransportException e)
            {
                return;
            }
        }
    }

    public static void killNServers(int portlow, int count) throws TException {
        for (int i=0;i<count;++i)
        {
            killServerNoException(portlow+i);
        }
    }

    public static void killServerNoException(int port)  {
            try{
                ThriftTestClient.killserver(port);
                System.out.println(convertStreamToString( processMap.get(port).getErrorStream()));
                System.out.println(convertStreamToString( processMap.get(port).getInputStream()));
            }
            catch (TTransportException e){

            }
            catch (TException e) {

            }
        ;
    }

    public static void main(String [] args) {
        //kill
        try {
            Util.killNServers(9080,11);
        } catch (TException e) {
            e.printStackTrace();
        }
    }
}

