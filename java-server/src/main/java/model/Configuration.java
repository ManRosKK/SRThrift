package model;

import SRBanking.ThriftInterface.NodeID;

import java.util.List;

/**
 * Created by Marcin Janicki on 17.01.15.
 */
public class Configuration {

    private List<NodeID> knownNodes;
    private Integer deliveryInterval;
    private Integer swarmPingInterval;
    private Integer swarmPingTimeout;
    private Integer swarmSize;


    public List<NodeID> getKnownNodes() {
        return knownNodes;
    }

    public void setKnownNodes(List<NodeID> knownNodes) {
        this.knownNodes = knownNodes;
    }

    public Integer getDeliveryInterval() {
        return deliveryInterval;
    }

    public void setDeliveryInterval(Integer deliveryInterval) {
        this.deliveryInterval = deliveryInterval;
    }

    public Integer getSwarmPingInterval() {
        return swarmPingInterval;
    }

    public void setSwarmPingInterval(Integer swarmPingInterval) {
        this.swarmPingInterval = swarmPingInterval;
    }

    public Integer getSwarmPingTimeout() {
        return swarmPingTimeout;
    }

    public void setSwarmPingTimeout(Integer swarmPingTimeout) {
        this.swarmPingTimeout = swarmPingTimeout;
    }

    public Integer getSwarmSize() {
        return swarmSize;
    }

    public void setSwarmSize(Integer swarmSize) {
        this.swarmSize = swarmSize;
    }
}
