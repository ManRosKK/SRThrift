import sys, glob, os, signal
import logging
from multiprocessing import freeze_support
import threading
from threading import Thread
from SRBanking.ThriftInterface import NodeService
from SRBanking.ThriftInterface.ttypes import TransferData, NodeID, TransferID,\
    Swarm
from SRBanking.ThriftInterface import NodeService
from ConfigParser import ConfigParser
import time
logging.basicConfig()
sys.path.append('../thrift/gen-py/')

#sys.path.insert(0, glob.glob('../thrift/thrift_binary/lib/py/build/lib.*')[0])

from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol
from thrift.server import TServer

class AutoClient:
    def __init__(self, address,port):
        self.address = address
        self.port = port
        #print("init ",self.address," ",port)

    def __enter__(self):
        #print("entering ",self.address,self.port)
        transport = TSocket.TSocket(self.address, self.port)
        self.transport = TTransport.TBufferedTransport(transport)
        transport.open()
        protocol = TBinaryProtocol.TBinaryProtocol(transport)
        self.client = NodeService.Client(protocol)
        return self.client

    def __exit__(self, type, value, tb):
        #print("exit ",self.address,self.port)
        if (self.transport != None):
            self.transport.close()

class ServerHandler:
    """
    List of fields:
    - nodeID
    - accountBalance
    - counter
    - config
    - mySwarms
    - pendingTransfers
    """
    def __init__(self, ip, port, accountBalance,config):
        self.nodeID = NodeID(IP=ip,port=port)
        self.accountBalance = accountBalance
        self.counter = 0;
        self.config = config
        self.mySwarms = []
        self.pendingTransfers = []
    def ping(self):
        pass
    def stop(self):
        print("Stopping")
        os.kill(os.getpid(),signal.SIGINT)
        print("Stopped")
    def getAccountBalance(self):
        return self.accountBalance
    def deliverTransfer(self,transfer_data):
        self.accountBalance += transfer_data.value
    def makeTransfer(self, receiver, value):

        #prepare transfer data
        transferID = TransferID(sender=self.nodeID, counter=self.counter)
        self.counter = self.counter + 1
        transferData = TransferData(transferID=transferID, receiver=receiver,value=value)

        #open connection to server
        address = receiver.IP
        port = receiver.port
        self.accountBalance -= value
        try:
            with AutoClient(address,port) as client:
                client.deliverTransfer(transferData);
        except:
            print(sys.exc_info()[0])
            #receiver offline
            self.makeSwarm(transferData)
        print("makeTransfer: return")

    def makeSwarm(self,transferData):
        print("making swarm")
        #scan for ppl
        neighbours = self.getNeighbours()
        print("neighbours",neighbours)

        #choose first n-1 neighbours
        how_much = self.getSwarmSize() - 1
        if(len(neighbours) >= how_much):
            print("enough to swarm")
            swarm = Swarm(transfer=transferData.transferID, leader=self.nodeID, members=neighbours[0:how_much]+[self.nodeID])
            self.mySwarms += [swarm]
            for i in xrange(how_much):
                with AutoClient(neighbours[i].IP,neighbours[i].port) as client:
                    client.addToSwarm(swarm,transferData)
        else:
            print("not enough to swarm")
        print("makeSwarm exit: mySwarm is now: ",self.mySwarms)

    def getNeighbours(self):
        nlist = []
        for ip in self.getIPList():
            for port in self.getPortList():
                try:
                    if (ip==self.nodeID.IP and port==self.nodeID.port):
                        continue
                    with AutoClient(ip,port) as client:
                        client.ping()
                        nlist += [NodeID(IP=ip, port=port)]
                except:
                    pass
        return nlist

    def addToSwarm(self,swarm,transferData):
        self.mySwarms += [swarm]
        self.pendingTransfers += [transferData]

    def getSwarmList(self):
        return self.mySwarms

    def getIPList(self):
        get = config.get("config", "ip_list")
        return get.split(',')
    def getPortList(self):
        get = config.get("config", "port_list")
        return [int(x) for x in get.split(',')]
    def getSwarmSize(self):
        return config.getint("config", "swarm_size")

if __name__ == "__main__":
    how_much_args = 4
    if len(sys.argv) == how_much_args + 1: #script name is first arg
        ip = sys.argv[1]
        port = int(sys.argv[2])
        balance =  int(sys.argv[3])
        config_file =  sys.argv[4]
    else:
        print("usage ./app ip port balance configFile")
        sys.exit(1)

    #run server
    print(ip,port,balance)
    config = ConfigParser()
    config.read(config_file)

    handler = ServerHandler(ip,port,balance,config)
    processor = NodeService.Processor(handler)
    transport = TSocket.TServerSocket(host=ip,port=port)
    tfactory = TTransport.TBufferedTransportFactory()
    pfactory = TBinaryProtocol.TBinaryProtocolFactory()
    server = TServer.TThreadPoolServer(processor, transport, tfactory, pfactory)
    print('Starting the server...')
    #sys.stdout.flush()
    time.sleep(0.1);
    server.serve()
    print('done.')



#parse config

