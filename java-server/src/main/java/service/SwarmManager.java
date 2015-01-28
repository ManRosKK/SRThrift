/**
 * Created by Marcin Janicki on 27.01.15.
 */

package service;

import SRBanking.ThriftInterface.NodeID;
import SRBanking.ThriftInterface.Swarm;
import SRBanking.ThriftInterface.TransferData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SwarmManager {
    private static Logger log = LoggerFactory.getLogger(SwarmManager.class);

    private Map<String, TransferData> pendingTransfers;
    private Map<String, Swarm> swarms;
    private Map<String, Timer> timers;


    public SwarmManager()
    {
        pendingTransfers = new HashMap<String, TransferData>();
        swarms = new HashMap<String, Swarm>();
        timers = new HashMap<String, Timer>();
    }

    public synchronized List<Swarm> getSwarms()
    {
        return new ArrayList<Swarm>(swarms.values());
    }

    public synchronized void updatePendingTransfers(String key, TransferData transferData)
    {
        pendingTransfers.put(key, transferData);
    }

    public synchronized void updateSwarm(String key, Swarm swarm)
    {
        swarms.put(key, swarm);
    }

    public synchronized void updateTimer(String key, Timer timer)
    {
        timers.put(key, timer);
    }

    public synchronized void addMemberToSwarm(String key, NodeID newMember)
    {
        Swarm swarm = swarms.get(key);
        if(swarm != null)
        {
            log.info("Added " + newMember.getIP() + ":" + newMember.getPort() + " to swarm");
            swarm.getMembers().add(newMember);
        }
    }

    public synchronized void removeMemberFromSwarm(String key, NodeID removedMember)
    {
        Swarm swarm = swarms.get(key);
        if(swarm != null)
        {
            log.info("Removed " + removedMember.getIP() + ":" + removedMember.getPort() + " from swarm");
            swarm.getMembers().remove(removedMember);
        }
    }

    public synchronized Swarm getSwarm(String key)
    {
        return swarms.get(key);
    }

    public synchronized void killSwarm(String key)
    {
        Swarm swarm = swarms.get(key);
        if(swarm != null)
        {
            log.info("Killing swarm from transfer " + key);
            swarms.remove(key);
        }
    }

    public synchronized void stopAndKillTimer(String key)
    {
        Timer timer = timers.get(key);
        if(timer != null)
        {
            log.info("Killing a timer for transfer " + key);
            timer.cancel();
            timer.purge();
            timers.remove(key);
        }
    }

    public synchronized TransferData getPendingTransfer(String key)
    {
        return pendingTransfers.get(key);
    }

    public synchronized void deliverTransfer(String key)
    {
        TransferData transferData = pendingTransfers.get(key);
        if(transferData != null)
        {
            log.info("Transfer " + key + " deliverd. Removing it from pending transfers.");
            pendingTransfers.remove(key);
        }
    }
}
