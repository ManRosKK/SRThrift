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
import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.prefs.Preferences;

import static org.testng.Assert.assertEquals;

/**
 * Created by sven on 2015-01-09.
 */
public class EasyClient {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface EasyCallable{
    }

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

    public static Method getMethodByName(String name) throws ClassNotFoundException, NoSuchMethodException {
        Class cls =Class.forName("EasyClient");
        Method[] methods = cls.getMethods();
        for (Method m : methods) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        throw new NoSuchMethodException();
    }

    private static void printUsage() throws ClassNotFoundException {
        Map<String,List<String>> actionMap = new LinkedHashMap<>();
        actionMap.put("makeTransfer", Arrays.asList("receiverIP","receiverPort","value"));
        actionMap.put("virtualStop", Arrays.asList("shouldStop"));

        Class cls =Class.forName("EasyClient");
        Method[] methods = cls.getMethods();
        System.out.println("Usage: ./client <IP> <port> action actionParams");
        System.out.println();
        System.out.println("Available actions:");
        for (Method m : methods) {
            EasyCallable easyCallable = m.getAnnotation(EasyCallable.class);
            if (easyCallable == null) {
                continue;
            }
            System.out.print("    ");
            System.out.print(m.getName());
            Class<?>[] parameterTypes = m.getParameterTypes();

            for (int i=0;i<parameterTypes.length;++i) {
                Class<?> paramClass = parameterTypes[i];
                if(i<2)
                    continue;
                System.out.print(" ");
                System.out.print(actionMap.get(m.getName()).get(i-2));
                System.out.print("(" + paramClass.getName() + ")");
            }
            System.out.println();
        }
    }
    public static void main(String[] args) throws ClassNotFoundException {

        //check argument count - must be 3 or more
        if (args.length < 3)
        {
            printUsage();
            return;
        }

        //invoke method

        int actionNameArgC = 2;
        int actionParamsArgC = 3;

        //get action name
        String actionName = args[2];
        //check action params
        Method m = null;
        try {
            m = getMethodByName(actionName);
        } catch (NoSuchMethodException e) {
            System.out.println("No such method exists!");
            System.out.println();
            printUsage();
            return;
        }

        Class<?>[] parameterTypes = m.getParameterTypes();
        ArrayList<Object> invokeParams = new ArrayList<>();

        //add ip and port
        invokeParams.add(args[0]);
        invokeParams.add(Integer.parseInt(args[1]));

        for (int i=0;i<parameterTypes.length-2;++i) {
            String param = args[actionParamsArgC+i];
            Class<?> parameterType = parameterTypes[i+2];
            if(parameterType.equals(String.class))
            {
                invokeParams.add(param);
            }
            else if (parameterType.equals(Integer.class))
            {
                invokeParams.add(Integer.parseInt(param));
            }
            else if (parameterType.equals(Long.class))
            {
                invokeParams.add(Long.parseLong(param));
            }
            else if (parameterType.equals(Boolean.class))
            {
                invokeParams.add(Boolean.parseBoolean(param));
            }
        }
        try {
            System.out.println("Invoking " + m.getName() + " " + invokeParams);
            Object invoke = m.invoke(null, invokeParams.toArray());
            System.out.print("Return: ");
            System.out.println(invoke);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            System.out.println("Return: " + e.getCause().getClass().getName());
        }
        catch (Exception e) {
            System.out.println("Return: " + e.getClass().getName());
        }



    }


    public static AutoClosingClient getClient(String IP, Integer port) throws TTransportException {

        return new AutoClosingClient(IP,port);
    }

    @EasyCallable
    public static void pingserver(String IP, Integer port) throws TException {
        try(AutoClosingClient acclient = getClient(IP, port)){
            NodeService.Client client = acclient.getClient();

            //log.info("About to ping server");

            try {
                NodeID receiver = new NodeID();
                receiver.setPort(port);
                receiver.setIP(IP);
                client.ping(receiver);
            } catch (TException e) {
                e.printStackTrace();
                log.error("Connected but unable to ping??");
                throw e;
            }

            log.info("Server is up " + port);
        } catch (TException e) {
            throw e;
        } catch (Exception e) {
            //don't throw - closing exception - will not happen
        }

    }

    @EasyCallable
    public static void killserver(String IP, Integer port) throws TTransportException {
        try(AutoClosingClient acclient = getClient(IP, port)) {
            NodeService.Client client = acclient.getClient();
            try {
                client.stop();
            } catch (TException e) {
                //if stoped it can throw the exception
            }
        }
    }

    @EasyCallable
    public static List<Swarm> getSwarmList(String IP,Integer port) throws TException {
        try(AutoClosingClient acclient = getClient(IP, port)) {
            NodeService.Client client = acclient.getClient();
            List<Swarm> swarmList = client.getSwarmList();
            return swarmList;
        }
    }

    @EasyCallable
    public static List<TransferData> getHistory(String IP,Integer port) throws TException {
        try(AutoClosingClient acclient = getClient(IP, port)) {
            NodeService.Client client = acclient.getClient();
            List<TransferData> swarmList = client.getTransfers();
            return swarmList;
        }
    }

    @EasyCallable
    public static void makeTransfer(String IPS, Integer portS,String IPR, Integer portR, Long value) throws TException {
        try(AutoClosingClient acclient = getClient(IPS, portS)) {
            NodeService.Client client = acclient.getClient();

            NodeID receiver = new NodeID();
            receiver.setPort(portR);
            receiver.setIP(IPR);

            client.makeTransfer(receiver, value);
        }
    }

    @EasyCallable
    public static long getBalance(String IP,Integer port) throws TException {
        try(AutoClosingClient acclient = getClient(IP, port)) {
            NodeService.Client client = acclient.getClient();
            long balance = client.getAccountBalance();
            return balance;
        }
    }


    public static void setBlacklist(String IP,Integer port, List<NodeID> blacklist) throws TException {
        try(AutoClosingClient acclient = getClient(IP, port)) {
            NodeService.Client client = acclient.getClient();
            client.setBlacklist(blacklist);
        }
    }

    @EasyCallable
    public static void virtualStop(String IP,Integer port, Boolean shouldStop) throws TException {
        try(AutoClosingClient acclient = getClient(IP, port)) {
            NodeService.Client client = acclient.getClient();
            client.virtualStop(shouldStop);
        }
    }
}
