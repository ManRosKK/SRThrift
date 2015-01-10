import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;

import java.io.IOException;

import static org.testng.Assert.assertEquals;

/**
 * Created by sven on 2015-01-09.
 */
public class Util {

    public static int defaultBalance = 500;

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static void runserver(String IP, int port, long balance) throws IOException {
        Process proc = Runtime.getRuntime().exec("java -jar " +
                "C:\\currentProjects\\SR\\SRThrift\\java-server\\target\\server-1.0-SNAPSHOT-jar-with-dependencies.jar " +
                IP + " " + port + " " + balance);
    }

    public static void runserver(int port) throws IOException {
        runserver("127.0.0.1",port,defaultBalance);
    }

    public static void runNServers(int portlow, int count) throws IOException {
        for (int i=0;i<count;++i)
        {
            runserver(portlow+i);
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
            Util.killNServers(9080,10);
        } catch (TException e) {
            e.printStackTrace();
        }
    }
}
