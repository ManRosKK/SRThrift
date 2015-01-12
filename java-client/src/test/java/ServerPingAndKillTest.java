import org.apache.thrift.transport.TTransportException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Created by sven on 2015-01-09.
 */
public class ServerPingAndKillTest {

   @Test
    public void test10Servers() throws Exception
    {
        int lowserver = 9080;
        int count = 10;
        String IP = "localhost";

        Util.runNServers(IP,lowserver, count);
        Util.pingNServers(IP,lowserver, count);
        Util.killNServers(IP,lowserver, count);
        Util.pingNServersExpectFail(IP,lowserver, count);
    }

    @Test
    public void testPingAndKillTwoServers() throws Exception {
        //run server
        int port1 = 9080;
        int port2 = 9090;
        String IP = "localhost";
        Util.runServer(IP,port1);
        Util.runServer(IP,port2);

        //ping server
        EasyClient.pingserver(IP,port1);
        EasyClient.pingserver(IP,port2);

        Util.killServerNoException(IP,port1);
        Util.killServerNoException(IP,port2);


        try{
            //ping again - ping should throw an exception
            EasyClient.pingserver(IP,port1);
            fail();
        }
        catch (TTransportException e)
        {
            //ok
        }

        try{
            //ping again - ping should throw an exception
            EasyClient.pingserver(IP,port2);
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
        String IP = "localhost";
        Util.runServer(IP,port);

        //ping server
        EasyClient.pingserver(IP,port);

        //kill server - should fail with transport
        Util.killServerNoException(IP,port);

        try{
            //ping again - ping should throw an exception
            EasyClient.pingserver(IP,port);
            assertEquals("Port should be down", false);
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
        String IP = "localhost";
        Util.runServer(IP,port);

        //ping server
        EasyClient.pingserver(IP,port);

        //kill server - should fail with transport
        Util.killServerNoException(IP,port);

        try{
            //ping again - should fail
            EasyClient.pingserver(IP,port);
            assertEquals("Port should be down", false);
        }
        catch (TTransportException e)
        {
            return;
        }
    }
}
