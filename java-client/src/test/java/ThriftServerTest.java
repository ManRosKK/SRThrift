import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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

    @BeforeMethod
    public void setUp() throws Exception {

    }

    @AfterMethod
    public void tearDown() throws Exception {

    }

    @Test
    public void testMain() throws Exception {
       //runjar
        Process proc = Runtime.getRuntime().exec("java -jar " +
            "C:\\currentProjects\\SR\\SRThrift\\java-server\\target\\server-1.0-SNAPSHOT-jar-with-dependencies.jar");

        Thread.sleep(100);

        //ping server
        ThriftTestClient.pingserver(9090);

        //kill server - should fail with transport
        try{
            //ping again - should throw TTransport
            ThriftTestClient.killserver(9090);
            fail();
        }
        catch (TTransportException e)
        {
            return;
        }

        try{
            //ping again - should fail
            ThriftTestClient.pingserver(9090);
            fail();
        }
        catch (TTransportException e)
        {
            return;
        }
    }
}
