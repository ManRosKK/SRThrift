namespace * SRBanking.ThriftInterface

typedef i64 AccountBalanceType

struct IPAddress
{
  1: string IP
}
struct NodeID
{
  1: IPAddress address,
  2: i32  port
}

struct TransferID
{
    1: NodeID sender,
    2: NodeID receiver,
    3: i64 counter
}
struct Swarm
{
   1: TransferID transfer
   2: NodeID leader,
   3: list<NodeID> members,
}
struct TransferData
{
    1: TransferID transferID,
    2: AccountBalanceType Value 
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
   * CLIENT INTERFACE
   */
      
  /**
  *
  */ 
  void MakeTransfer(1: NodeID receiver,2: AccountBalanceType value),

  /**
  *
  */ 
  AccountBalanceType GetAccountBalance(),
  
  /**
  * pings node
  */
  void Ping(),
  /**
  * pings Swarm and checks if leader is a leader
  */
  void PingSwarm(1: NodeID leader,2: TransferID transfer ) throws (1: NotSwarmMemeber exc),
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
  void DeliverTransfer(1: TransferData transfer),

  list<Swarm> GetSwarmList(),
  
  void startSwarmElection(1:TransferID transfer) throws (1: NotSwarmMemeber exc ),
  
  list<TransferData>  GetTransfers(),
  
  void stop()
}