import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import org.testng.Reporter;

import java.io.*;
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
    public static String defaultConfigFile = "config//default.ini";
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
        System.out.println(new File(defaultConfigFile).getAbsolutePath());
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }


    public static void runServer(String IP, final int port, long balance, String configFile, String language) throws IOException {
        String execString = shellStrings.get(language) + " " + IP + " " + port + " " +
                balance + " " + (new File(configFile).getAbsolutePath());
        System.out.println(execString);
        System.out.flush();
        Process proc = Runtime.getRuntime ().exec(execString);

        processMap.put(port,proc);
    }

    public static void runServer(String IP, int port, long balance, String configFile) throws IOException {
        runServer(IP,port,balance,configFile,defaultLanguage);
    }

    public static void runServer(String IP, int port, long balance) throws IOException {
        runServer(IP,port,balance,defaultConfigFile);
    }

    public static void runServer(String IP, int port) throws IOException {
        runServer(IP,port,defaultBalance);
    }

    public static void runNServers(String IP, int portlow, int count) throws IOException {
        for (int i=0;i<count;++i)
        {
            runServer(IP,portlow + i);
        }
    }

    public static void pingNServers(String IP,int portlow, int count) throws TException {
        for (int i=0;i<count;++i)
        {
            EasyClient.pingserver(IP,portlow + i);
        }
    }

    public static void pingNServersExpectFail(String IP,int portlow, int count) throws TException {
        int countFail = 0;
        for (int i=0;i<count;++i)
        {
            try{
                //ping again - ping should throw an exception
                EasyClient.pingserver(IP, portlow + i);
            }
            catch (TTransportException e)
            {
                countFail++;
            }
        }
        if (countFail!=count)
        {
            throw new TException("Some ports are not shut down! " + (count-countFail) + "/" + count);
        }
    }

    public static void killNServers(String IP, int portlow, int count) throws TException {
        for (int i=0;i<count;++i)
        {
            killServerNoException(IP,portlow + i);
        }
    }

    public static void killServerNoException(String IP, int port)  {
            try{
                EasyClient.killserver(IP,port);
                System.out.println("Error" + port + ":");
                System.out.println(convertStreamToString(processMap.get(port).getErrorStream()));
                System.out.println("StdOut" + port + ": ");
                System.out.println(convertStreamToString(processMap.get(port).getInputStream()));
                System.out.flush();
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
            Util.killNServers("localhost",9080,11);
        } catch (TException e) {
            e.printStackTrace();
        }
    }
}

