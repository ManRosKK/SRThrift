import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Created by sven on 2015-01-09.
 */
public class ThriftServerTest {

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private void runserver(int port) throws IOException {
        Process proc = Runtime.getRuntime().exec("java -jar " +
                "C:\\currentProjects\\SR\\SRThrift\\java-server\\target\\server-1.0-SNAPSHOT-jar-with-dependencies.jar " +
                port);
    }

    private void runNServers(int portlow, int count) throws IOException {
        for (int i=0;i<count;++i)
        {
            runserver(portlow+i);
        }
    }

    private void pingNServers(int portlow, int count) throws TException {
        for (int i=0;i<count;++i)
        {
            ThriftTestClient.pingserver(portlow+i);
        }
    }

    private void pingNServersExpectFail(int portlow, int count) throws TException {
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


    private void killNServers(int portlow, int count) throws TException {
        for (int i=0;i<count;++i)
        {
            try{
                ThriftTestClient.killserver(portlow+i);
            }
            catch (TTransportException e){};
        }
    }

    @BeforeMethod
    public void setUp() throws Exception {

    }

    @AfterMethod
    public void tearDown() throws Exception {

    }

    @Test
    public void test10Servers() throws Exception
    {
        int lowserver = 9080;
        int count = 10;

        runNServers(lowserver,count);
        pingNServers(lowserver,count);
        killNServers(lowserver,count);
        pingNServersExpectFail(lowserver,count);
    }

    @Test
    public void testPingAndKillTwoServers() throws Exception {
        //run server
        int port1 = 9080;
        int port2 = 9090;
        runserver(port1);
        runserver(port2);

        //ping server
        ThriftTestClient.pingserver(port1);
        ThriftTestClient.pingserver(port2);

        //kill server - should fail with transport
        try{
            ThriftTestClient.killserver(port1);
        }
        catch (TTransportException e){};
        try{
            ThriftTestClient.killserver(port2);
        }
        catch (TTransportException e){};

        try{
            //ping again - ping should throw an exception
            ThriftTestClient.pingserver(port1);
            fail();
        }
        catch (TTransportException e)
        {
            //ok
        }

        try{
            //ping again - ping should throw an exception
            ThriftTestClient.pingserver(port2);
            fail();
        }
        catch (TTransportException e)
        {
            //ok
        }
    }


    @Test
    public void testPingAndKill9080() throws Exception {
        //run server
        int port = 9080;
        runserver(port);

        //ping server
        ThriftTestClient.pingserver(port);

        //kill server - should fail with transport
        try{
            ThriftTestClient.killserver(port);
        }
        catch (TTransportException e){};

        try{
            //ping again - ping should throw an exception
            ThriftTestClient.pingserver(port);
            assertEquals("Port should be down",false);
        }
        catch (TTransportException e)
        {
            return;
        }
    }

    @Test
    public void testPingAndKill9090() throws Exception {
        //run server
        int port = 9090;
        runserver(port);

        //ping server
        ThriftTestClient.pingserver(port);

        //kill server - should fail with transport
        try{
            ThriftTestClient.killserver(port);
        }
        catch (TTransportException e)
        {

        }

        try{
            //ping again - should fail
            ThriftTestClient.pingserver(port);
            assertEquals("Port should be down",false);
        }
        catch (TTransportException e)
        {
            return;
        }
    }
}
