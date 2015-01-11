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

        Util.runNServers(lowserver, count);
        Util.pingNServers(lowserver, count);
        Util.killNServers(lowserver, count);
        Util.pingNServersExpectFail(lowserver, count);
    }

    @Test
    public void testPingAndKillTwoServers() throws Exception {
        //run server
        int port1 = 9080;
        int port2 = 9090;
        Util.runServer(port1);
        Util.runServer(port2);

        //ping server
        ThriftTestClient.pingserver(port1);
        ThriftTestClient.pingserver(port2);

        Util.killServerNoException(port1);
        Util.killServerNoException(port2);


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
        Util.runServer(port);

        //ping server
        ThriftTestClient.pingserver(port);

        //kill server - should fail with transport
        Util.killServerNoException(port);

        try{
            //ping again - ping should throw an exception
            ThriftTestClient.pingserver(port);
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
        Util.runServer(port);

        //ping server
        ThriftTestClient.pingserver(port);

        //kill server - should fail with transport
        Util.killServerNoException(port);

        try{
            //ping again - should fail
            ThriftTestClient.pingserver(port);
            assertEquals("Port should be down", false);
        }
        catch (TTransportException e)
        {
            return;
        }
    }
}
