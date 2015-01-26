package service;

import SRBanking.ThriftInterface.NodeID;
import model.Configuration;

import org.ini4j.Ini;
import org.ini4j.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marcin Janicki on 17.01.15.
 */
public class ConfigService {

    private static Logger log = LoggerFactory.getLogger(ConfigService.class);
    private String path;

    public ConfigService(String path)
    {
        this.path = path;
    }

    public Configuration readConfiguration()
    {
        Configuration config = new Configuration();

        Ini ini = null;

        try
        {
            ini = new Ini(new File(path));
        }
        catch(IOException e)
        {
            log.error("Can't open file at path " + path);
            System.exit(1);
        }

        Profile.Section section = ini.get("config");
        config.setKnownNodes(processNodes(section));
        try
        {
            config.setDeliveryInterval(Integer.parseInt(section.get("try_deliver_transfer_every")));
            config.setSwarmPingInterval(Integer.parseInt(section.get("ping_swarm_every")));
            config.setSwarmPingTimeout(Integer.parseInt(section.get("leader_considered_dead_after")));
            config.setSwarmSize(Integer.parseInt(section.get("swarm_size")));
        }
        catch(NumberFormatException e)
        {
            log.error("Can't parse one of the number arguments");
            System.exit(1);
        }

        return config;
    }

    private List<NodeID> processNodes(Profile.Section section)
    {
        List<NodeID> nodes = new ArrayList<NodeID>();

        String ipList = section.get("ip_list");
        String portList = section.get("port_list");

        String ips[] = ipList.trim().split(",");
        String ports[] = portList.trim().split(",");
        List<Integer> intPorts = new ArrayList<Integer>();

        for(String port: ports)
        {
            try
            {
                intPorts.add(Integer.parseInt(port));
            }
            catch(NumberFormatException e)
            {
                log.error("Can't parse port number " + port);
            }
        }

        for(String ip: ips)
        {
            for(Integer port: intPorts)
            {
                NodeID node = new NodeID();
                node.setIP(ip);
                node.setPort(port.intValue());
                nodes.add(node);
            }
        }

        return nodes;
    }

}