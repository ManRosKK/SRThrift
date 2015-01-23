namespace * SRBanking.ThriftInterface

typedef i64 AccountBalanceType
typedef string IPType

struct NodeID
{
    1: IPType IP,
    2: i32 port
}

struct TransferID
{
    1: NodeID sender,
    2: i64 counter
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
    2: NodeID receiver,
    3: AccountBalanceType value 
}

exception  NotSwarmMemeber{
    1: NodeID receiverNode,
    2: TransferID transfer
}
exception NotEnoughMembersToMakeTransfer{
    1: i64 membersAvailable,
    2: i64 membersRequested
}
exception NotEnoughMoney{
    1: AccountBalanceType moneyAvailable,
    2: AccountBalanceType moneyRequested
}
exception  WrongSwarmLeader{
    1: NodeID receiverNode,
    2: NodeID leader,
    3: TransferID transfer
}
exception  AlreadySwarmMemeber{
    1: NodeID receiverNode,
    2: NodeID leader,
    3: TransferID transfer
}
service NodeService
{
    /**
    * CLIENT INTERFACE
    */
      
    /**
    *
    */ 
    void makeTransfer(1: NodeID receiver,2: AccountBalanceType value) throws (1: NotEnoughMembersToMakeTransfer exc, 2: NotEnoughMoney exc2),

    /**
    *
    */ 
    AccountBalanceType getAccountBalance(),
  
  
  
    /**
    * INTERNODE INTERFACE
    */
    
    /**
    * pings node
    */
    void ping(1:NodeID sender),
    
    /**
    * pings Swarm and checks if sender is a leader
    */
    void pingSwarm(1:NodeID sender,2: TransferID transfer ) throws (1: NotSwarmMemeber exc),
    
    /**
    *  
    */
    void updateSwarmMembers(1:NodeID sender,2:Swarm swarm) throws (1: NotSwarmMemeber exc,2:WrongSwarmLeader exc2 ),
    
    /**
    *
    */
    void addToSwarm(1:NodeID sender, 2:Swarm swarm, 3: TransferData transferData) throws (1: AlreadySwarmMemeber exc),
    
    /**
    *
    */
    void delSwarm(1:NodeID sender, 2:TransferID swarmID) throws (1: NotSwarmMemeber exc,2:WrongSwarmLeader exc2 ),
    
    /**
    *
    */
    Swarm getSwarm(1:NodeID sender, 2:TransferID transfer) throws (1: NotSwarmMemeber exc ),
    
    /**
    * returns true if candidateNodeID> current
    */
    bool electSwarmLeader(1:NodeID sender, 2:NodeID cadidate, 3:TransferID Transfer) throws (1: NotSwarmMemeber exc ),
    
    /**
    * new leader broadcast that he is a leader
    */
    void electionEndedSwarm(1:NodeID sender, 2:Swarm swarm) throws (1: NotSwarmMemeber exc ),
    
    /**
    *
    */ 
    void deliverTransfer(1:NodeID sender, 2:TransferData transfer),

    /**
    * DEBUG
    */
    list<Swarm> getSwarmList(),

    void startSwarmElection(1:TransferID transfer) throws (1: NotSwarmMemeber exc ),

    list<TransferData> getTransfers(),

    /**
    *  It sets "blacklist" - list of nodes which are not accessible (in both directions) for the callee
    */
    void setBlacklist(1: list<NodeID> blacklist),

    /**
    *  The method simulates killing server - all non-debug methods should fail (maybe except getSwarm getAccountBalance)
    */
    void virtualStop(1: bool shouldStop),
    
    void stop()
}