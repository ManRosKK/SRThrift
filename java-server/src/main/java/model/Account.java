package model;

import SRBanking.ThriftInterface.TransferID;
import SRBanking.ThriftInterface.TransferData;

import java.io.Serializable;
import java.util.*;

/**
 * Created by MarJan
 */
public class Account implements Serializable{

    private long balance;
    private Map<String, TransferData> transferHistory;

    public Account(long balance)
    {
        this.balance = balance;
        transferHistory = new HashMap<String, TransferData>();
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public boolean isTransferPossible(long transferMoney)
    {
        return balance > transferMoney;
    }

    public String makeTransferKey(TransferID transferID)
    {
        return transferID.getSender().getIP() + transferID.getSender().getPort() + transferID.getCounter();
    }

    public List<TransferData> getTransferHistory() {
        return new ArrayList<TransferData>(transferHistory.values());
    }

    public void addTransferToHistory(TransferData transferData)
    {
        transferHistory.put(makeTransferKey(transferData.getTransferID()), transferData);
    }

    public boolean isTransferInHistory(TransferData transferData)
    {
        String key = makeTransferKey(transferData.getTransferID());
        return transferHistory.containsKey(key);
    }

    public synchronized void takeTransfer(TransferData transferData)
    {
        balance += transferData.getValue();
        transferHistory.put(makeTransferKey(transferData.getTransferID()), transferData);
    }
}
