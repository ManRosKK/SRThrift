import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Created by sven on 2015-01-09.
 */
public class BasicTransfers {

    String IP = "localhost";
    int port = 9080;
    long balance = 501;
    String IP2 = "localhost";
    int port2 = 9081;
    long balance2 = 502;

    @BeforeMethod
    public void setUp() throws Exception {
        //arrange
        Util.runServer(IP, port, balance);
        Util.runServer(IP2, port2, balance2);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Util.killServerNoException(IP,port);
        Util.killServerNoException(IP2,port2);
    }

    @Test
    public void BasicAccountBalance() throws Exception {
        long balanceRead = EasyClient.getBalance(IP,port);
        long balance2Read = EasyClient.getBalance(IP2,port2);
        //assert
        assertEquals(balanceRead, balance);
        assertEquals(balance2Read, balance2);
    }

    @Test
    public void BasicTransfer() throws Exception {
        long value = 30;
        EasyClient.makeTransfer(IP, port, IP2, port2, value);

        long balanceRead = EasyClient.getBalance(IP,port);
        long balance2Read = EasyClient.getBalance(IP2,port2);
        //assert
        assertEquals(balanceRead, balance-value);
        assertEquals(balance2Read, balance2+value);
    }

    @Test
    public void BasicTransferAndReverseTransfer() throws Exception {
        long value = 35;
        EasyClient.makeTransfer(IP, port, IP2, port2, value);
        EasyClient.makeTransfer(IP2, port2, IP, port, value);

        long balanceRead = EasyClient.getBalance(IP,port);
        long balance2Read = EasyClient.getBalance(IP2,port2);
        //assert
        assertEquals(balanceRead, balance);
        assertEquals(balance2Read, balance2);
    }

    @Test
    public void BasicSelfTransfer() throws Exception {
        long value = 35;
        EasyClient.makeTransfer(IP, port, IP, port, value);

        long balanceRead = EasyClient.getBalance(IP,port);
        //assert
        assertEquals(balanceRead, balance);
    }
}
