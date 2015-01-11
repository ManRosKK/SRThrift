import SRBanking.ThriftInterface.*;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;

import static org.testng.Assert.assertEquals;

/**
 * Created by sven on 2015-01-09.
 */
public class EasyClient {
    private static class AutoClosingClient implements AutoCloseable
    {
        private TTransport transport;

        public NodeService.Client getClient() {
            return client;
        }

        private NodeService.Client client;
        public AutoClosingClient(String IP, int port) throws TTransportException {
            this.transport = new TSocket("localhost", port);
            this.transport.open();
            TProtocol protocol = new TBinaryProtocol(this.transport);
            client = new NodeService.Client(protocol);
        }

        @Override
        public void close(){
            if(transport!=null)
            {
                transport.close();
            }
        }
    }
    private static Logger log = LoggerFactory.getLogger(EasyClient.class);

    public static void main(String[] args)
    {
        String IP = "localhost";
        int port = 9080;
        long balance = 501;
        String IP2 = "localhost";
        int port2 = 9081;
        long balance2 = 502;
        String IPReceiver = "localhost";
        int portReceiver = 13467;
        String configFile = "config\\testSwarmBasics.ini";
        try {
            int value = 5;
            EasyClient.makeTransfer(IP, port, IP2, portReceiver, value);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            List<Swarm> swarmList = EasyClient.getSwarmList(IP, port);
            assertEquals(1,swarmList.size());
            assertEquals(2,swarmList.get(0).getMembersSize());
            assertEquals(port,swarmList.get(0).getLeader().getPort());
            assert((port == swarmList.get(0).getMembers().get(0).getPort())
                    || port == swarmList.get(0).getMembers().get(1).getPort());
            assert((port2 == swarmList.get(0).getMembers().get(0).getPort())
                    || port2 == swarmList.get(0).getMembers().get(1).getPort());
        } catch (TException e) {
            e.printStackTrace();
        }
    }


    public static AutoClosingClient getClient(String IP, int port) throws TTransportException {

        return new AutoClosingClient(IP,port);
    }

    public static void stopClient(NodeService.Client client)
    {
        client.getInputProtocol().getTransport().close();
        client.getOutputProtocol().getTransport().close();
    }

    public static void pingserver(String IP, int port) throws TException {
        try(AutoClosingClient acclient = getClient(IP, port)){
            NodeService.Client client = acclient.getClient();

            log.info("About to ping server");

            try {
                client.ping();
            } catch (TException e) {
                e.printStackTrace();
                log.error("Connected but unable to ping??");
                throw e;
            }

            log.info("Server pigned");
        } catch (TException e) {
            throw e;
        } catch (Exception e) {
            //don't throw - closing exception - will not happen
        }

    }


    public static void killserver(String IP, int port) throws TTransportException {
        try(AutoClosingClient acclient = getClient(IP, port)) {
            NodeService.Client client = acclient.getClient();
            try {
                client.stop();
            } catch (TException e) {
                //if stoped it can throw the exception
            }
        }
    }

    public static List<Swarm> getSwarmList(String IP,int port) throws TException {
        try(AutoClosingClient acclient = getClient(IP, port)) {
            NodeService.Client client = acclient.getClient();
            List<Swarm> swarmList = client.getSwarmList();
            return swarmList;
        }
    }

    public static void delSwarm(String IP,int port, TransferID swarmID ) throws TException {
        try(AutoClosingClient acclient = getClient(IP, port)) {
            NodeService.Client client = acclient.getClient();
            client.delSwarm(swarmID);
        }
    }

    public static void makeTransfer(String IPS, int portS,String IPR, int portR, long value) throws TException {
        try(AutoClosingClient acclient = getClient(IPS, portS)) {
            NodeService.Client client = acclient.getClient();

            NodeID receiver = new NodeID();
            receiver.setPort(portR);
            receiver.setIP(IPR);

            client.makeTransfer(receiver, value);
        }
    }

    public static long getBalance(String IP,int port) throws TException {
        try(AutoClosingClient acclient = getClient(IP, port)) {
            NodeService.Client client = acclient.getClient();
            long balance = client.getAccountBalance();
            return balance;
        }
    }
}
