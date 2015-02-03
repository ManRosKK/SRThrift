import sys, glob, os, signal
import logging
from multiprocessing import freeze_support
from random import shuffle
import threading
from threading import Thread
from threading import BoundedSemaphore
from SRBanking.ThriftInterface import NodeService
from SRBanking.ThriftInterface.ttypes import TransferData, NodeID, TransferID,\
    Swarm, NotEnoughMembersToMakeTransfer, NotEnoughMoney, NotSwarmMemeber,\
    WrongSwarmLeader, AlreadySwarmMemeber
from SRBanking.ThriftInterface import NodeService
from ConfigParser import ConfigParser
import time
from thrift.transport.TTransport import TTransportException
import SRBanking
import traceback

root = logging.getLogger()
root.setLevel(logging.DEBUG)
ch = logging.StreamHandler(sys.stdout)
ch.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s - %(name)s -  %(levelname)s - %(message)s')
ch.setFormatter(formatter)
root.addHandler(ch)

sys.path.append('../thrift/gen-py/')

#sys.path.insert(0, glob.glob('../thrift/thrift_binary/lib/py/build/lib.*')[0])

from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol
from thrift.server import TServer


globalBlacklist = []

class AutoClient:
    def __init__(self, address,port):
        self.address = address
        self.port = port
        #logging.info(("init ",self.address," ",port))

    def __enter__(self):
        if NodeID(IP=self.address, port=self.port) in globalBlacklist:
            raise TTransportException("Blacklisted!")
        #logging.info(("entering ",self.address,self.port))
        transport = TSocket.TSocket(self.address, self.port)
        self.transport = TTransport.TBufferedTransport(transport)
        transport.open()
        protocol = TBinaryProtocol.TBinaryProtocol(transport)
        self.client = NodeService.Client(protocol)
        return self.client

    def __exit__(self, type, value, tb):
        #logging.info(("exit ",self.address,self.port))
        if (self.transport != None):
            self.transport.close()

def overrides(interface_class):
    def overrider(method):
        assert(method.__name__ in dir(interface_class))
        return method
    return overrider

def smaller(node1,node2):
        return ((str(node1.IP)+str(node1.port)) <  (str(node2.IP)+str(node2.port)))

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
            #logging.info((len(self.mySwarms),"to look at"))
            for swarm in self.mySwarms:
                #logging.info((swarm.transfer,"to look at"))
                if (swarm.leader == self.nodeID):
                    #leader mode on

                    #try to deliver
                    transferData = self.getTransferByID(swarm.transfer)
                    try:
                        with AutoClient(transferData.receiver.IP,transferData.receiver.port) as client:
                            client.deliverTransfer(self.nodeID,transferData)
                        logging.info(("Delivered"))
                        #remove myself and others from swarm
                        self.unmakeSwarm(swarm)
                        continue
                    except:
                        logging.info(("Not delivered",sys.exc_info()[0]))

                    logging.info(("Pinging swarm members",swarm.members))
                    #check whether others are alive
                    for node in list(swarm.members):
                        if(node != self.nodeID):
                            try:
                                logging.info(("Pinging swarm member",node))
                                with AutoClient(node.IP,node.port) as client:
                                    client.pingSwarm(self.nodeID,swarm.transfer)
                            except TTransportException:
                                logging.info(("Unable to ping, gotta find someone else"))
                                deathCounter = self.deathCounter.get(node)
                                if(deathCounter is None):
                                    logging.info(("Death counter on!",node))
                                    deathCounter = 1
                                    self.deathCounter[node]=deathCounter
                                else:
                                    logging.info(("Death!",node))
                                    self.deathCounter[node]= deathCounter + 1
                                    if self.deathCounter[node] > 1:
                                        self.funeral(swarm,node)

                    logging.info(("Checking fellows"))
                    #check whether there is enough members in swarm
                    if self.getSwarmSize() > len(swarm.members):
                        logging.info(("Call to arms!",len(swarm.members),"of",self.getSwarmSize(),swarm))
                        how_much = self.getSwarmSize() - len(swarm.members)
                        neighbours = self.getNeighbours(how_much,swarm.members)
                        logging.info(("Got some fresh blood!", neighbours))
                        #remove those who are
                        swarm.members += neighbours
                        #inform them
                        for x in neighbours:
                            with AutoClient(x.IP,x.port) as client:
                                client.addToSwarm(self.nodeID,swarm,self.getTransferByID(swarm.transfer))
                        #inform the rest
                        for x in swarm.members[0:-how_much]:
                            with AutoClient(x.IP,x.port) as client:
                                client.updateSwarmMembers(self.nodeID,swarm)
                    logging.info(("Leader done it's hard work."))

                else:
                    #slave mode on
                    #check if pinged recently
                    #logging.info(("checking heartbeat",swarm.transfer))
                    if time.time() - self.hb[str(swarm.transfer.sender)+str(swarm.transfer.counter)] > config.getint("config", "leader_considered_dead_after")/1000.0:
                        logging.info(("The king is dead. ",swarm))
                        self.localElectNewLeader(swarm)
            time.sleep(config.getint("config", "try_deliver_transfer_every")/1000.0)
            self.virtualStopLock.acquire(blocking=1)
            self.virtualStopLock.release()

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
        self.virtualStopLock = threading.BoundedSemaphore()
        self.virtualStoped = False
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
            pass
        else:
            raise NotEnoughMoney(moneyAvailable=self.accountBalance, moneyRequested=value)
        try:
            with AutoClient(address,port) as client:
                logging.info(("port open"))
                client.deliverTransfer(self.nodeID,transferData)
                self.accountBalance -= transferData.value
                logging.info(("transfer send"))
        except:
            logging.info(("transfer not made",sys.exc_info()[0]))
            #receiver offline
            self.makeSwarm(transferData)
        logging.info(("makeTransfer: return"))

    @overrides(NodeService.Iface)
    def getAccountBalance(self):
        return self.accountBalance

    @overrides(NodeService.Iface)
    def ping(self, sender):
        global globalBlacklist
        if sender in globalBlacklist:
            raise TTransportException("Call from blacklisted member!")
        if self.virtualStoped:
            raise TTransportException("Virtual stopped")
        #this method is intentionally left blank
        pass

    @overrides(NodeService.Iface)
    def pingSwarm(self, sender, transfer):
        global globalBlacklist
        if sender in globalBlacklist:
            raise TTransportException("Call from blacklisted member!")
        if self.virtualStoped:
            raise TTransportException("Virtual stopped")
        logging.info(("received pingSwarm",sender,transfer))
        swarm = None
        try:
            swarm = self.getSwarmByID(transfer)
        except:
            raise NotSwarmMemeber(receiverNode=self.nodeID, transfer=transfer)
        if (swarm.leader != sender):
            raise WrongSwarmLeader(receiverNode=self.nodeID, leader=swarm.leader, transfer=transfer)

        self.hb[str(transfer.sender)+str(transfer.counter)] = time.time()

    @overrides(NodeService.Iface)
    def updateSwarmMembers(self,sender,swarm):
        global globalBlacklist
        if sender in globalBlacklist:
            raise TTransportException("Call from blacklisted member!")
        if self.virtualStoped:
            raise TTransportException("Virtual stopped")
        logging.info(("updateSwarmMembers",sender,swarm))
        swarmlocal = self.getSwarmByID(swarm.transfer)
        if (swarmlocal.leader != sender):
            raise WrongSwarmLeader(receiverNode=self.nodeID, leader=swarm.leader, transfer=swarm.transfer)
        swarmlocal.members = swarm.members

    @overrides(NodeService.Iface)
    def addToSwarm(self,sender,swarm,transferData):
        global globalBlacklist
        logging.info(("Got add to swarm from ",sender,swarm))
        if sender in globalBlacklist:
            raise TTransportException("Call from blacklisted member!")
        if self.virtualStoped:
            raise TTransportException("Virtual stopped")
        try:
            swarmOld = self.getSwarmByID(swarm.transfer)
            logging.info(("Already in swarm ",swarmOld))
            raise AlreadySwarmMemeber(receiverNode=self, leader=swarmOld.leader, transfer=swarm.transfer)
        except IndexError:
            logging.info(("Add to swarm: Setting hb for ",swarm.transfer))
            self.hb[str(swarm.transfer.sender)+str(swarm.transfer.counter)] = time.time()
            self.pendingTransfers += [transferData]
            self.mySwarms += [swarm]
            logging.info(("added to swarm!",swarm))

    @overrides(NodeService.Iface)
    def delSwarm(self,sender,transferID):
        global globalBlacklist
        if sender in globalBlacklist:
            raise TTransportException("Call from blacklisted member!")
        if self.virtualStoped:
            raise TTransportException("Virtual stopped")
        self.mySwarms = [x for x in self.mySwarms if x.transfer != transferID]
        self.pendingTransfers= [x for x in self.pendingTransfers if x.transferID != transferID]

    #elect swarm member

    @overrides(NodeService.Iface)
    def electSwarmLeader(self, sender, candidate, transfer):
        logging.info(("Received elect from",sender))
        global globalBlacklist
        if sender in globalBlacklist:
            raise TTransportException("Call from blacklisted member!")
        if self.virtualStoped:
            raise TTransportException("Virtual stopped")
        swarm = None
        try:
            swarm = self.getSwarmByID(transfer)
        except:
            raise NotSwarmMemeber(receiverNode=self.nodeID, transfer=transfer)

        return smaller(self.nodeID,candidate)


    @overrides(NodeService.Iface)
    def electionEndedSwarm(self, sender,swarmnew):
        global globalBlacklist
        if sender in globalBlacklist:
            raise TTransportException("Call from blacklisted member!")
        if self.virtualStoped:
            raise TTransportException("Virtual stopped")
        swarmold = self.getSwarmByID(swarmnew.transfer)
        self.mySwarms.remove(swarmold)
        self.mySwarms.append(swarmnew)

    @overrides(NodeService.Iface)
    def deliverTransfer(self, sender,transfer_data):
        global globalBlacklist
        if sender in globalBlacklist:
            raise TTransportException("Call from blacklisted member!")
        if self.virtualStoped:
            raise TTransportException("Virtual stopped")
        logging.info(("Received transfer",transfer_data))

        #look for transfer in history
        if transfer_data in self.transferHistory:
            logging.info(("Already got such transfer!",transfer_data))
            return
        else:
            logging.info(("Received new transfer!",transfer_data))
            self.accountBalance += transfer_data.value
            self.transferHistory += [transfer_data]

    #debug
    @overrides(NodeService.Iface)
    def getSwarmList(self):
        return self.mySwarms

    #start swarm election
    @overrides(NodeService.Iface)
    def getTransfers(self):
        return self.transferHistory

    @overrides(NodeService.Iface)
    def stop(self):
        logging.info(("Stopping server with SIGINT"))
        os.kill(os.getpid(),signal.SIGSEGV)

    @overrides(NodeService.Iface)
    def virtualStop(self, shouldStop):
        if(shouldStop):
            logging.info(("Virtual stopping server"))
            self.virtualStopLock.acquire(blocking=1)
            self.virtualStoped = True
        else:
            logging.info(("Virtual starting server"))
            self.virtualStopLock.release()
            self.virtualStoped = False

    def setBlacklist(self, blacklist):
        global globalBlacklist
        globalBlacklist = blacklist
        logging.info(("Blacklist set", blacklist))

    #UTILLLLLLLLLLLLLLS

    def localElectNewLeader(self,swarm):
        logging.info(("Electing new leader...",swarm))
        #get alive ppl
        alive_ppl = self.getAlivePpl(swarm.members)

        #check if leader is up?
        if swarm.leader in alive_ppl:
            logging.info(("Leader is alive! Election cancelled",swarm))
            #if leader is up, no need to elect new one
            return

        #start election
        #for everyone and not me
        for node in alive_ppl:
            if (node == self.nodeID):
                continue

            if not smaller(node,self.nodeID):
                continue
            #check alive - elect
            try:
                logging.info((self.nodeID.port," contacts leaderly ",node.port))
                with AutoClient(node.IP,node.port) as client:
                    result = client.electSwarmLeader(self.nodeID,self.nodeID,swarm.transfer)

                    #result means "he wants to be a leader badly.
                    if result == True:
                        logging.info((node.port," wants to be a leader. Badly.",swarm))
                        return
                    else:
                        logging.info((node.port," thinks I am superior",swarm))
            except:
                #dead - don't care
                pass

        swarm.leader = self.nodeID
        swarm.members = alive_ppl
        logging.info(("I AM NOW THE EMPEROR",swarm))

        # not found anyone better, so become a leader!
        for node in alive_ppl:
            #check alive - elect
            try:
                with AutoClient(node.IP,node.port) as client:
                    client.electionEndedSwarm(self.nodeID,swarm)
            except:
                #dead - don't care, will take care of it later
                pass

    def getAlivePpl(self,list_of_nodes):
        alive = []
        for node in list_of_nodes:
            try:
                with AutoClient(node.IP,node.port) as client:
                    client.ping(self.nodeID)
                    alive += [NodeID(IP=node.IP, port=node.port)]
            except:
                #dead - don't care
                pass
        return alive

    def makeSwarm(self,transferData):
        logging.info(("making swarm"))
        #scan for ppl
        how_much = self.getSwarmSize() - 1
        neighbours = self.getNeighbours(how_much,[])
        logging.info(("neighbours",neighbours))

        #choose first n-1 neighbours
        if(len(neighbours) >= how_much):
            logging.info(("enough to swarm"))
            swarm = Swarm(transfer=transferData.transferID, leader=self.nodeID, members=neighbours[0:how_much]+[self.nodeID])
            self.mySwarms += [swarm]
            self.pendingTransfers += [transferData]
            self.accountBalance -= transferData.value
            for i in xrange(how_much):
                with AutoClient(neighbours[i].IP,neighbours[i].port) as client:
                    logging.info(("adding ",neighbours[i],"to new swarm",swarm))
                    client.addToSwarm(self.nodeID,swarm,transferData)
        else:
            raise NotEnoughMembersToMakeTransfer(membersAvailable=len(neighbours),membersRequested=how_much)
        logging.info(("makeSwarm exit: mySwarm is now: ",self.mySwarms))

    def unmakeSwarm(self,swarm):
        logging.info(("unmaking swarm", swarm))

        #get members
        for node in swarm.members:
            if(node != self.nodeID):
                try:
                    with AutoClient(node.IP,node.port) as client:
                        client.delSwarm(self.nodeID,swarm.transfer)
                except:
                    traceback.print_exc()
                    logging.info(("client not delted from swarm",sys.exc_info()[0],node.IP,node.port))
        self.delSwarm(self.nodeID,swarm.transfer)
        logging.info(("unmade swarm", swarm))

    def funeral(self,swarm,nodeID):
        #remove from swarm members
        swarm.members.remove(nodeID)
        #inform everyone
        for node in swarm.members:
            if(node != self.nodeID):
                try:
                    with AutoClient(node.IP,node.port) as client:
                        logging.info(("update swarm members for ",node,swarm.members))
                        client.updateSwarmMembers(self.nodeID,swarm)
                except:
                    traceback.print_exc()
                    logging.info(("client not informed about funeral...",sys.exc_info()[0],node.IP,node.port))

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
                    blacklisted =False
                    for node in blacklist:
                        if (ip==node.IP and port==node.port):
                            blacklisted = True
                    if blacklisted:
                        continue
                    logging.info(("getNeigh",ip,port,"will be checked",blacklist))
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
        logging.info(("usage ./app ip port balance configFile"))
        sys.exit(1)

    #run server
    logging.info((ip,port,balance))
    config = ConfigParser()
    config.read(config_file)

    handler = ServerHandler(ip,port,balance,config)
    processor = NodeService.Processor(handler)
    transport = TSocket.TServerSocket(host=ip,port=port)
    tfactory = TTransport.TBufferedTransportFactory()
    pfactory = TBinaryProtocol.TBinaryProtocolFactory()
    server = TServer.TThreadPoolServer(processor, transport, tfactory, pfactory)
    logging.info(('Starting the server...'))
    server.serve()
    logging.info(('done.'))
