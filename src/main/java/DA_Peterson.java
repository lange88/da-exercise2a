import interfaces.DA_Peterson_RMI;
import interfaces.Message;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by jeroen on 11/22/17.
 */
public class DA_Peterson extends UnicastRemoteObject implements DA_Peterson_RMI, Runnable {
    int tid, id;
    int nextProcessId;

    DA_Peterson(int id, int nextProcessId) throws RemoteException {
        this.id = id;
        this.nextProcessId = nextProcessId;
        this.tid = 0;
    }

    @Override
    public void run() {

    }

    @Override
    public void receive(Message m) throws RemoteException {

    }
}
