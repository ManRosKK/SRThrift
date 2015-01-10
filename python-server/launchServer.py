import sys, glob
from SRBanking.ThriftInterface.ttypes import NodeID
from SRBanking.ThriftInterface import NodeService
sys.path.append('../thrift/gen-py')

#sys.path.insert(0, glob.glob('../../lib/py/build/lib.*')[0])

from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol
from thrift.server import TServer

class ServerHandler:
    def __init__(self, ip, port, accountBalance):
        self.nodeID = NodeID(ip,port)
        print(self.nodeID.port)
        self.accountBalance = accountBalance

ip = "localhost"
port = 9090
balance = 500
handler = ServerHandler(ip,port,balance)
processor = NodeService.Processor(handler)
transport = TSocket.TServerSocket(port=port)
tfactory = TTransport.TBufferedTransportFactory()
pfactory = TBinaryProtocol.TBinaryProtocolFactory()

# You could do one of these for a multithreaded server
server = TServer.TThreadedServer(processor, transport, tfactory, pfactory)
#server = TServer.TThreadPoolServer(processor, transport, tfactory, pfactory)

print('Starting the server...')
server.serve()
print('done.')