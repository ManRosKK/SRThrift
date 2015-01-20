import SRBanking.ThriftInterface.NotEnoughMoney;
import SRBanking.ThriftInterface.TransferData;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Created by sven on 2015-01-09.
 */
public class BasicTransfers {

    String IP = "127.0.0.1";
    int port = 9080;
    long balance = 501;
    String IP2 = "127.0.0.1";
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
        Thread.sleep(1000);
        long value = 30;
        EasyClient.makeTransfer(IP, port, IP2, port2, value);

        long balanceRead = EasyClient.getBalance(IP,port);
        long balance2Read = EasyClient.getBalance(IP2,port2);
        //assert
        assertEquals(balanceRead, balance-value);
        assertEquals(balance2Read, balance2+value);
    }

    @Test(expectedExceptions = NotEnoughMoney.class)
    public void TransferWithoutMoney() throws Exception {
        long value = 99999999;
        EasyClient.makeTransfer(IP, port, IP2, port2, value);
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

    @Test
    public void TransferHistory() throws Exception {
        long value = 30;
        EasyClient.makeTransfer(IP, port, IP2, port2, value);

        List<TransferData> history = EasyClient.getHistory(IP, port);
        List<TransferData> history2 = EasyClient.getHistory(IP2, port2);

        //assert
        assertEquals(history.size(), 0);
        assertEquals(history2.size(), 1);
        assertEquals(history2.get(0).getValue(), value);
        assertEquals(history2.get(0).getTransferID().getSender().getPort(), port);
        assertEquals(history2.get(0).getTransferID().getSender().getIP(), IP);
        assertEquals(history2.get(0).getReceiver().getPort(), port2);
        assertEquals(history2.get(0).getReceiver().getIP(), IP2);
    }
}
