package model;

import SRBanking.ThriftInterface.NodeService;
import org.apache.thrift.transport.TTransport;

/**
 * Created by Marcin Janicki on 27.01.15.
 */
public class Connection {
    private NodeService.Client client;
    private TTransport transport;

    public Connection(NodeService.Client client, TTransport transport) {
        this.setClient(client);
        this.setTransport(transport);
    }


    public NodeService.Client getClient() {
        return client;
    }

    public TTransport getTransport() {
        return transport;
    }

    public void setClient(NodeService.Client client) {
        this.client = client;
    }

    public void setTransport(TTransport transport) {
        this.transport = transport;
    }
}
