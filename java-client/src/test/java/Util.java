import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import org.ini4j.Profile;
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
    public static Map<Integer,StringBuilder> stdoutMap = new HashMap<Integer,StringBuilder>();
    public static Map<Integer,StringBuilder> stderrMap = new HashMap<Integer,StringBuilder>();
    static {
        Ini prefs = null;
        try {
            prefs = new Ini(new File(filename));
        } catch (IOException e) {
            System.out.println("Preferences load failure");
            System.exit(1);
        }
        Profile.Section shellStringsSection = prefs.get("shellStrings");
        for (String option: shellStringsSection.keySet()) {
            if("default".equals(option))
            {
                defaultLanguage = shellStringsSection.get(option);
            }
            else
            {
                shellStrings.put(option, shellStringsSection.get(option));
            }
        }
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
        try {
            EasyClient.pingserver(IP,port);
        } catch (TException e) {
            e.printStackTrace();
        }

        stdoutMap.put(port, new StringBuilder());
        stderrMap.put(port, new StringBuilder());

        final BufferedReader readerStdout = new BufferedReader (new InputStreamReader(proc.getInputStream()));
        final BufferedReader readerStderr = new BufferedReader (new InputStreamReader(proc.getErrorStream()));

        Thread T=new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    String line;
                    while ((line = readerStdout.readLine()) != null) {
                        //stdoutMap.get(port).append("Stdout" + port + ": " + line + "\n");
                        System.out.println("Stdout" + port + ": " + line);
                        System.out.flush();
                }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } );
        T.start();

        Thread T2=new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    String line;
                    while ((line = readerStderr.readLine()) != null) {
                        //stderrMap.get(port).append("StdErr" + port + ": " + line + "\n");
                        System.out.println("StdErr" + port + ": " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } );
        T2.start();

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

    public static void runNServers(String IP, int portlow, long balance,String config, int count) throws IOException {
        for (int i=0;i<count;++i)
        {
            runServer(IP,portlow + i,balance,config);
        }
    }

    public static void runNServers(String IP, int portlow, long balance,String config, String language, int count) throws IOException {
        for (int i=0;i<count;++i)
        {
            runServer(IP,portlow + i,balance,config,language);
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
                Process process = processMap.get(port);
                if(process != null)
                {
                    //System.out.println("Error" + port + ":");
                    //System.out.println(convertStreamToString(process.getErrorStream()));
                    //Reporter.log(stdoutMap.get(port).toString(),true);
                    Reporter.log(stderrMap.get(port).toString(),true);
                    //System.out.println("StdOut" + port + ": ");
                    //System.out.println(convertStreamToString(process.getInputStream()));
                    //System.out.flush();
                    processMap.remove(port);
                }
                else
                {
                    System.out.println("Process not available " + port);
                    System.out.flush();
                }

            }
            catch (TTransportException e){

            }
            catch (TException e) {

            }
        ;
    }

}

