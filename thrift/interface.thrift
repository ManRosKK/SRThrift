namespace * SRBanking.ThriftInterface

struct IPAddress
{
  1: list<byte> IP
}
struct NodeID
{
  1: IPAddress Address,
  2: i32	   Port
}

struct TransferID
{
	1: NodeID Sender,
	2: NodeID Receiver,
	3: i64	  LP
}
struct Swarm
{
  1: NodeID Leader,
  2: list<NodeID> Members,
  3: TransferID Transfer
}
struct TransferData
{
	1: TransferID transfer,
	2: i64 Value 
}
exception  NotSwarmMemeber{
  1: NodeID ReceiverNode,
  2: TransferID Transfer
}
exception  WrongSwarmLeader{
  1: NodeID ReceiverNode,
  2: NodeID Leader,
  3: TransferID Transfer
}
exception  AlreadySwarmMemeber{
  1: NodeID ReceiverNode,
  2: NodeID Leader,
  3: TransferID Transfer
}
service NodeService
{
    /**
	* pings node
	*/
  void Ping(),
  /**
  * pings Swarm and checks if leader is a leader
  */
  void PingSwarm(1:NodeID leader,2:TransferID transfer ) throws (1: NotSwarmMemeber exc),
  /**
  *
  */
  void UpdateSwarmMembers(1:Swarm swarm) throws (1: NotSwarmMemeber exc,2:WrongSwarmLeader exc2 ),
  /**
  *
  */
  void AddToSwarm(1:Swarm swarm) throws (1: AlreadySwarmMemeber exc),
  /**
  *
  */
  void DelSwarm(1:Swarm swarm) throws (1: NotSwarmMemeber exc,2:WrongSwarmLeader exc2 ),
  /**
  *
  */
  Swarm GetSwarm(1:TransferID transfer) throws (1: NotSwarmMemeber exc ),
  /**
  * returns true if candidateNodeID> current
  */
  bool ElectSwarmLeader(1:NodeID cadidate,2:TransferID Transfer) throws (1: NotSwarmMemeber exc ),
  /**
  * new leader broadcast that he is a leader
  */
  void ElectionEndedSwarm(1:Swarm swarm) throws (1: NotSwarmMemeber exc ),
  /**
  *
  */ 
  void MakeTransfer(1: TransferData transfer)

}
service DebugService
{
   list<Swarm> GetSwarmList(),
   void startSwarmElection(1:TransferID transfer) throws (1: NotSwarmMemeber exc ),
   list<TransferData>  GetTransfers(),
   void stop()
}