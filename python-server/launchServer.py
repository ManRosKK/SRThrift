import sys, glob, os, signal
import logging
from multiprocessing import freeze_support
import threading
from threading import Thread
from SRBanking.ThriftInterface import NodeService
from SRBanking.ThriftInterface.ttypes import TransferData, NodeID, TransferID
from SRBanking.ThriftInterface import NodeService
logging.basicConfig()
sys.path.append('../thrift/gen-py/')

#sys.path.insert(0, glob.glob('../thrift/thrift_binary/lib/py/build/lib.*')[0])

from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol
from thrift.server import TServer

def getClient(address, port):
        transport = TSocket.TSocket(address, port)
        transport = TTransport.TBufferedTransport(transport)
        protocol = TBinaryProtocol.TBinaryProtocol(transport)
        client = NodeService.Client(protocol)
        transport.open()
        return client




class ServerHandler:
    """
    List of fields:
    - nodeID
    - accountBalance
    - counter
    - swarmList
    -
    """
    def __init__(self, ip, port, accountBalance):
        self.nodeID = NodeID(address=ip,port=port)
        self.accountBalance = accountBalance
        self.counter = 0;
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
        address = receiver.address.IP
        port = receiver.port
        self.accountBalance -= value
        try:
            client = getClient(address,port)

            #make transfer
            client.deliverTransfer(transferData);
        except:
            #receiver offline
            self.makeSwarm(transferData)
        print("transfer complete")
    def makeSwarm(self,transferData):
        pass

how_much_args = 4
if len(sys.argv) == how_much_args + 1: #script name is first arg
    ip = sys.argv[1]
    port = int(sys.argv[2])
    balance =  int(sys.argv[3])
    config_file=  int(sys.argv[4])
else:
    print("usage ./app ip port balance configFile")
    sys.exit(1)

print(ip,port,balance)
handler = ServerHandler(ip,port,balance)
processor = NodeService.Processor(handler)
transport = TSocket.TServerSocket(host=ip,port=port)
tfactory = TTransport.TBufferedTransportFactory()
pfactory = TBinaryProtocol.TBinaryProtocolFactory()

# You could do one of these for a multithreaded server
server = TServer.TThreadPoolServer(processor, transport, tfactory, pfactory)

#server = TServer.TThreadPoolServer(processor, transport, tfactory, pfactory)

#server = TProcessPoolServer(processor, transport, tfactory, pfactory)


print('Starting the server...')
server.serve()
print('done.')