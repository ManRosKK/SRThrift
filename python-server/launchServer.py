import sys, glob, os, signal
import logging
from multiprocessing import freeze_support
from random import shuffle
import threading
from threading import Thread
from SRBanking.ThriftInterface import NodeService
from SRBanking.ThriftInterface.ttypes import TransferData, NodeID, TransferID,\
    Swarm, NotEnoughMembersToMakeTransfer, NotEnoughMoney, NotSwarmMemeber,\
    WrongSwarmLeader
from SRBanking.ThriftInterface import NodeService
from ConfigParser import ConfigParser
import time
from thrift.transport.TTransport import TTransportException
import SRBanking
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



def overrides(interface_class):
    def overrider(method):
        assert(method.__name__ in dir(interface_class))
        return method
    return overrider

class ServerHandler(NodeService.Iface):
    """
    List of fields:
    - nodeID
    - accountBalance
    - counter
    - config
    - mySwarms
    - pendingTransfers
    - transferHistory
    """

    def run_leader_thread(self):
        while not self.endThread:
            #for every swarm
            for swarm in self.mySwarms:
                if (swarm.leader == self.nodeID):
                    #leader mode on

                    #try to deliver
                    transferData = self.getTransferByID(swarm.transfer)
                    try:
                        with AutoClient(transferData.receiver.IP,transferData.receiver.port) as client:
                            client.deliverTransfer(self.nodeID,transferData)
                        print("Delivered")
                        #remove myself and others from swarm
                        self.unmakeSwarm(swarm)
                        continue
                    except:
                        print("Not delivered",sys.exc_info()[0])

                    #check whether others are alive
                    for node in swarm.members:
                        if(node != self.nodeID):
                            try:
                                with AutoClient(node.IP,node.port) as client:
                                    client.ping(self.nodeID)
                            except TTransportException:
                                print("Unable to ping, gotta find someone else")
                                deathCounter = self.deathCounter.get(node)
                                if(deathCounter is None):
                                    print("Death counter on!")
                                    deathCounter = 1
                                    self.deathCounter[node]=deathCounter
                                else:
                                    print("Death!")
                                    self.deathCounter[node]= deathCounter + 1
                                    if self.deathCounter[node] > 1:
                                        self.funeral(swarm,node)

                    print("Checking fellows")
                    #check whether there is enough members in swarm
                    if self.getSwarmSize() > len(swarm.members):
                        print("recruit em all!")
                        how_much = self.getSwarmSize() - len(swarm.members)
                        neighbours = self.getNeighbours(how_much,swarm.members)
                        print("enough neighbours!")
                        swarm.members += neighbours
                        #inform them
                        for x in neighbours:
                            with AutoClient(x.IP,x.port) as client:
                                client.addToSwarm(self.nodeID,swarm,self.getTransferByID(swarm.transfer))
                        #inform the rest
                        for x in swarm.members[0:-how_much]:
                            with AutoClient(x.IP,x.port) as client:
                                client.updateSwarmMembers(self.nodeID,swarm)
                else:
                    #slave mode on
                    #check if pinged recently
                    if time.time() - self.hb[swarm.transfer] > config.getint("config", "leader_considered_dead_after"):
                        self.localElectNewLeader(swarm)
            time.sleep(config.getint("config", "try_deliver_transfer_every")/1000.0)

    def __init__(self, ip, port, accountBalance,config):
        self.nodeID = NodeID(IP=ip,port=port)
        self.accountBalance = accountBalance
        self.counter = 0;
        self.config = config
        self.mySwarms = []
        self.pendingTransfers = []
        self.transferHistory = []
        self.endThread = False
        self.deathCounter = {}
        self.hb = {}
        t1 = threading.Thread(target=self.run_leader_thread)
        t1.start()

    @overrides(NodeService.Iface)
    def makeTransfer(self, receiver, value):

        #prepare transfer data
        transferID = TransferID(sender=self.nodeID, counter=self.counter)
        self.counter = self.counter + 1
        transferData = TransferData(transferID=transferID, receiver=receiver,value=value)

        #open connection to server
        address = receiver.IP
        port = receiver.port
        if(self.accountBalance >= value):
            self.accountBalance -= value
        else:
            raise NotEnoughMoney(moneyAvailable=self.accountBalance, moneyRequested=value)
        try:
            with AutoClient(address,port) as client:
                print("port open")
                client.deliverTransfer(self.nodeID,transferData)
                print("transfer send")
        except:
            print("transfer not made",sys.exc_info()[0])
            #receiver offline
            self.makeSwarm(transferData)
        print("makeTransfer: return")

    @overrides(NodeService.Iface)
    def getAccountBalance(self):
        return self.accountBalance



    @overrides(NodeService.Iface)
    def ping(self, sender):
        #this method is intentionally left blank
        pass

    @overrides(NodeService.Iface)
    def pingSwarm(self, sender, transfer):
        swarm = None
        try:
            swarm = self.getSwarmByID(transfer)
        except:
            raise NotSwarmMemeber(receiverNode=self.nodeID, transfer=transfer)
        if (swarm.leader != sender):
            raise WrongSwarmLeader(receiverNode=self.nodeID, sender=swarm.leader, transfer=transfer)
        self.hb[transfer] = time.time()

    @overrides(NodeService.Iface)
    def updateSwarmMembers(self,sender,swarm):
        swarmlocal = self.getSwarmByID(swarm.transfer)
        swarmlocal.members = swarm.members

    @overrides(NodeService.Iface)
    def addToSwarm(self,sender,swarm,transferData):
        self.mySwarms += [swarm]
        self.pendingTransfers += [transferData]
        self.hb[swarm.transfer] = time.time()
        print("added to swarm!",swarm.transfer)

    @overrides(NodeService.Iface)
    def delSwarm(self,sender,transferID):
        self.mySwarms = [x for x in self.mySwarms if x.transfer != transferID]
        self.pendingTransfers= [x for x in self.pendingTransfers if x.transferID != transferID]

    #elect swarm member

    @overrides(NodeService.Iface)
    def electSwarmLeader(self, sender, candidate, transfer):
        swarm = None
        try:
            swarm = self.getSwarmByID(transfer)
        except:
            raise NotSwarmMemeber(receiverNode=self.nodeID, transfer=transfer)

        if (self.nodeID.IP == candidate.IP):
            return self.nodeID.port > candidate.port
        return self.nodeID.IP > candidate.IP

    @overrides(NodeService.Iface)
    def electionEndedSwarm(self, sender,swarmnew):
        swarmold = self.getSwarmByID(swarmnew.transfer)
        self.mySwarms.remove(swarmold)
        self.mySwarms.append(swarmnew)

    @overrides(NodeService.Iface)
    def deliverTransfer(self, sender,transfer_data):
        print("Received transfer",transfer_data)
        self.accountBalance += transfer_data.value
        self.transferHistory += [transfer_data]

    #debug

    def getSwarmList(self):
        return self.mySwarms

    #start swarm election

    def getTransfers(self):
        return self.transferHistory

    def stop(self):
        print("Stopping server with SIGINT")
        os.kill(os.getpid(),signal.SIGINT)


    #UTILLLLLLLLLLLLLLS

    def localElectNewLeader(self,swarm):
        print("Electing new leader...")
        #get alive ppl
        alive_ppl = self.getAlivePpl(swarm.members)

        #check if leader is up?
        if swarm.leader in alive_ppl:
            return

        #start election
        #for everyone and not me
        for node in alive_ppl:
            if (node == self.nodeID):
                continue
            #check alive

    def getAlivePpl(self,list_of_nodes):
        alive = []
        for node in list_of_nodes:
            try:
                with AutoClient(ip,port) as client:
                    client.ping(self.nodeID)
                    alive += [NodeID(IP=ip, port=port)]
            except:
                #dead - don't care
                pass
        return alive

    def makeSwarm(self,transferData):
        print("making swarm")
        #scan for ppl
        how_much = self.getSwarmSize() - 1
        neighbours = self.getNeighbours(how_much,[])
        print("neighbours",neighbours)

        #choose first n-1 neighbours
        if(len(neighbours) >= how_much):
            print("enough to swarm")
            swarm = Swarm(transfer=transferData.transferID, leader=self.nodeID, members=neighbours[0:how_much]+[self.nodeID])
            self.mySwarms += [swarm]
            self.pendingTransfers += [transferData]
            for i in xrange(how_much):
                with AutoClient(neighbours[i].IP,neighbours[i].port) as client:
                    client.addToSwarm(self.nodeID,swarm,transferData)
        else:
            raise NotEnoughMembersToMakeTransfer(membersAvailable=len(neighbours),membersRequested=how_much)
        print("makeSwarm exit: mySwarm is now: ",self.mySwarms)

    def unmakeSwarm(self,swarm):
        print("unmaking swarm")

        #get members
        for node in swarm.members:
            print("got members")
            if(node != self.nodeID):
                print("got not self")
                try:
                    with AutoClient(node.IP,node.port) as client:
                        client.delSwarm(self.nodeID,swarm.transfer)
                except:
                    print("client not delted from swarm",sys.exc_info()[0],node.IP,node.port)
        print("got all but me")
        self.delSwarm(self.nodeID,swarm.transfer)
        print("got me")

    def funeral(self,swarm,nodeID):
        #remove from swarm members
        swarm.members.remove(nodeID)
        #inform everyone
        for node in swarm.members:
            if(node != self.nodeID):
                try:
                    with AutoClient(node.IP,node.port) as client:
                        client.updateSwarmMembers(self.nodeID,swarm)
                except:
                    print("client not informed about funeral...",sys.exc_info()[0],node.IP,node.port)

    def getNeighbours(self,how_much_max,blacklist):
        nlist = []
        iplist = self.getIPList()
        portlist = self.getPortList()
        shuffle(iplist)
        shuffle(portlist)
        for ip in iplist:
            for port in portlist:
                try:
                    if (len(nlist) >= how_much_max):
                        break
                    if (ip==self.nodeID.IP and port==self.nodeID.port):
                        continue
                    for node in blacklist:
                        if (ip==node.IP and port==node.port):
                            continue
                    with AutoClient(ip,port) as client:
                        client.ping(self.nodeID)
                        nlist += [NodeID(IP=ip, port=port)]
                except:
                    pass
        return nlist

    #localutils
    def getTransferByID(self,transferID):
        transfersByID = [i for i in self.pendingTransfers if i.transferID==transferID]
        return transfersByID[0]
    def getSwarmByID(self,transferID):
        transfersByID = [i for i in self.mySwarms if i.transfer==transferID]
        return transfersByID[0]
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
    server.serve()
    print('done.')
