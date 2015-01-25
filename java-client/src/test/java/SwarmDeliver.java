import SRBanking.ThriftInterface.NotEnoughMembersToMakeTransfer;
import SRBanking.ThriftInterface.Swarm;
import SRBanking.ThriftInterface.TransferData;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Created by sven on 2015-01-09.
 */
public class SwarmDeliver {

    String IP = "127.0.0.1";
    int port = 9080;
    long balance = 501;
    String IP2 = "127.0.0.1";
    int port2 = 9081;
    long balance2 = 502;
    String IPReceiver = "127.0.0.1";
    int portReceiver = 9082;
    long balanceReceiver = 503;
    String configFile = "config\\testSwarmBasics.ini";
    String configFileOne = "config\\testSwarmBasicsOne.ini";

    @BeforeMethod
    public void setUp() throws Exception {
        //multiple config files so
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Util.killServerNoException(IP,port);
        Util.killServerNoException(IP2, port2);
        Util.killServerNoException(IPReceiver, portReceiver);
    }

    @Test
    public void SwarmDeliver() throws Exception {
        Util.runServer(IP, port, balance, configFile);
        Util.runServer(IP2, port2, balance2, configFile);

        long value = 5;
        EasyClient.makeTransfer(IP, port, IPReceiver, portReceiver, value);

        Util.runServer(IPReceiver, portReceiver, balanceReceiver, configFile);

        Thread.sleep(1000);

        List<Swarm> swarmList = EasyClient.getSwarmList(IP, port);
        List<Swarm> swarmList2 = EasyClient.getSwarmList(IP2, port2);
        List<Swarm> swarmListR = EasyClient.getSwarmList(IPReceiver, portReceiver);
        assertEquals(swarmList.size(),0);
        assertEquals(swarmList2.size(),0);
        assertEquals(swarmListR.size(),0);

        List<TransferData> history = EasyClient.getHistory(IP, port);
        List<TransferData> history2 = EasyClient.getHistory(IP2, port2);
        List<TransferData> historyR = EasyClient.getHistory(IPReceiver, portReceiver);
        assertEquals(history.size(), 0);
        assertEquals(history2.size(), 0);
        assertEquals(historyR.size(), 1);
        assertEquals(historyR.get(0).getValue(), value);
        assertEquals(historyR.get(0).getTransferID().getSender().getIP(), IP);
        assertEquals(historyR.get(0).getTransferID().getSender().getPort(), port);
        assertEquals(historyR.get(0).getReceiver().getIP(), IPReceiver);
        assertEquals(historyR.get(0).getReceiver().getPort(), portReceiver);
    }

    @Test
    public void SwarmDeliverOne() throws Exception {
        //arrange
        Util.runServer(IP, port, balance, configFileOne);

        long value = 5;
        EasyClient.makeTransfer(IP, port, IPReceiver, portReceiver, value);

        Util.runServer(IPReceiver, portReceiver, balanceReceiver, configFileOne);

        Thread.sleep(1000);

        //assert
        List<Swarm> swarmList = EasyClient.getSwarmList(IP, port);
        List<Swarm> swarmListR = EasyClient.getSwarmList(IPReceiver, portReceiver);
        assertEquals(swarmList.size(),0);
        assertEquals(swarmListR.size(),0);

        List<TransferData> history = EasyClient.getHistory(IP, port);
        List<TransferData> historyR = EasyClient.getHistory(IPReceiver, portReceiver);
        assertEquals(history.size(), 0);
        assertEquals(historyR.size(), 1);
        assertEquals(historyR.get(0).getValue(), value);
        assertEquals(historyR.get(0).getTransferID().getSender().getIP(), IP);
        assertEquals(historyR.get(0).getTransferID().getSender().getPort(), port);
        assertEquals(historyR.get(0).getReceiver().getIP(), IPReceiver);
        assertEquals(historyR.get(0).getReceiver().getPort(), portReceiver);
    }

    @Test
    public void SwarmDoubleDeliver() throws Exception {
        Util.runServer(IP, port, balance, configFile);
        Util.runServer(IP2, port2, balance2, configFile);

        long value1 = 10;
        long value2 = 20;
        EasyClient.makeTransfer(IP, port, IPReceiver, portReceiver, value1);
        EasyClient.makeTransfer(IP2, port2, IPReceiver, portReceiver, value2);

        Util.runServer(IPReceiver, portReceiver, balanceReceiver, configFile);

        Thread.sleep(2000);
        System.out.println("Test");
        System.out.flush();

        List<Swarm> swarmList = EasyClient.getSwarmList(IP, port);
        List<Swarm> swarmList2 = EasyClient.getSwarmList(IP2, port2);
        List<Swarm> swarmListR = EasyClient.getSwarmList(IPReceiver, portReceiver);
        assertEquals(swarmList.size(),0);
        assertEquals(swarmList2.size(),0);
        assertEquals(swarmListR.size(),0);

        long balanceRRead = EasyClient.getBalance(IPReceiver, portReceiver);
        assertEquals(balanceRRead,balanceReceiver+value1+value2);

        List<TransferData> history = EasyClient.getHistory(IP, port);
        List<TransferData> history2 = EasyClient.getHistory(IP2, port2);
        List<TransferData> historyR = EasyClient.getHistory(IPReceiver, portReceiver);
        assertEquals(history.size(), 0);
        assertEquals(history2.size(), 0);
        assertEquals(historyR.size(), 2);
    }
}
