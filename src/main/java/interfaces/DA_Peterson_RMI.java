package interfaces;

import java.rmi.Remote;

/**
 * Created by jeroen on 11/22/17.
 */
public interface DA_Peterson_RMI extends Remote {
    void receive(Message m) throws java.rmi.RemoteException;
}