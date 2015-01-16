using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Reflection;
using log4net;

namespace BankingNode
{
    class BalanceManager
    {
        private readonly ILog logerr = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        public Int64 Balance { set; get; }
        private Int64 counter = 0;
        private object _lock = new object();
        public List<SRBanking.ThriftInterface.TransferData> BaseTransactions
        {
            get
            {
                List<SRBanking.ThriftInterface.TransferData> t = new List<SRBanking.ThriftInterface.TransferData>();
                foreach (TransferData d in Transacitons)
                {
                    t.Add(d.ToBase());
                }
                return t;
            }
        }
        public List<TransferData> Transacitons = new List<TransferData>();
        public TransferID generateTransactionID()
        {
            TransferID t = new TransferID();
            lock (_lock)
            {
                t.Counter = counter++;
                t.Sender = ConfigLoader.Instance.ConfigGetSelfId();
            }
            return t;
        }
        public void checkTransfer(TransferData transaction)
        {
            if (Balance - transaction.Value < 0)
            {
                throw new SRBanking.ThriftInterface.NotEnoughMoney();
            }
        }
        public void CommitTransfer(TransferData transaction)
        {
            logerr.Info(transaction.TransferID.Sender.ToString() + " vs " + transaction.Receiver.ToString());
            if (transaction.TransferID.Sender == transaction.Receiver)
                return;
            if (transaction.TransferID.Sender == ConfigLoader.Instance.ConfigGetSelfId())
            {
                if (Balance - transaction.Value < 0)
                {
                    throw new SRBanking.ThriftInterface.NotEnoughMoney();
                }
                //Transacitons.Add(transaction);
                Balance -= transaction.Value;
            }
            else
            {
                Transacitons.Add(transaction);
                Balance += transaction.Value;
            }
            
        }
    }
}
