using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using log4net;
using System.Reflection;

namespace BankingNode
{
    class TransferData
    {
        private readonly ILog logerr = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        private SRBanking.ThriftInterface.TransferData internalObject = null;
        public TransferID TransferID
        {
            set
            {
                if (value == null)
                {
                    internalObject.TransferID = null;
                }
                else
                    internalObject.TransferID = value.ToBase();
            }
            get
            {
                return new TransferID(internalObject.TransferID);
            }
        }
        public NodeID Receiver
        {
            set
            {
                if (value != null)
                    internalObject.Receiver = value.ToBase();
                else
                    internalObject.Receiver = null;
            }
            get
            {
                return new NodeID(internalObject.Receiver);
            }
        }
        public long Value
        {
            set
            {
                internalObject.Value = value;
            }
            get
            {
                return internalObject.Value;
            }
        }
        public static bool operator ==(TransferData c1, TransferData c2)
        {
            if (((object)c1) == ((object)c2))
            {
                return true;
            }
            if (((object)c1) == null)
            {
                return false;
            }
            return c1.Equals(c2);
        }
        public static bool operator!= (TransferData c1, TransferData c2)
        {
            
            return !(c1 == c2);
        }
        public TransferData(SRBanking.ThriftInterface.TransferData node)
        {
            if (node == null)
            {
                internalObject = new SRBanking.ThriftInterface.TransferData();
            }
            else
            {
                internalObject = node;
            }
        }
        public override bool Equals(object obj)
        {
            if ((obj) == null)
            {
                return false;
            }
            return (((TransferData)obj).Receiver == Receiver && ((TransferData)obj).TransferID == TransferID && ((TransferData)obj).Value == Value); 
        }
        public int CompareTo(object obj)
        {
            return Equals(this, obj)?1:0;
        }
        public override int GetHashCode()
        {
            return this.TransferID.GetHashCode()+this.Receiver.GetHashCode()+this.Value.GetHashCode();
        }
        public SRBanking.ThriftInterface.TransferData ToBase()
        {
            return internalObject;
        }
        public override string ToString()
        {
            return TransferID.ToString()+"("+Value.ToString()+")----->"+Receiver.ToString();
        }
    }
}
