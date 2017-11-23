import interfaces.DA_Peterson_RMI;
import interfaces.Message;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by jeroen on 11/22/17.
 * Implementation class for Peterson's algorithm.
 */
public class DA_Peterson extends UnicastRemoteObject implements DA_Peterson_RMI, Runnable {
    private int tid = 0, id;
    private int nextProcessId;
    private int ntid = -1, nntid = -1;
    private boolean elected = false, relay = false;
    private ReentrantLock lock = new ReentrantLock();

    DA_Peterson(int id, int nextProcessId) throws RemoteException {
        this.id = id;
        this.nextProcessId = nextProcessId;
    }

    /**
     * Send id through RMI.
     * @param id ID to send.
     */
    private void send(int id) {
        String name = "rmi://localhost/DA_Peterson_" + nextProcessId;
        try {
            Message m = new Message(id);
            DA_Peterson_RMI o = (DA_Peterson_RMI) java.rmi.Naming.lookup(name);
            o.receive(m);
        } catch (NotBoundException e1) {
            System.out.println("NotBoundException while sending message for name: " + name);
            e1.printStackTrace();
        } catch (MalformedURLException e1) {
            System.out.println("MalformedURLException while sending message for name: " + name);
            e1.printStackTrace();
        } catch (RemoteException e1) {
            System.out.println("RemoteException while sending message for name: " + name);
            e1.printStackTrace();
        }
    }

    /**
     * Function that blocks until it either receives ntid or nntid.
     * @param lookupntid iff true block until ntid is received,
     *                   else block until nntid is received
     */
    private void gettid(boolean lookupntid) {
        while (true) {
            try {
                lock.lock();
                if (lookupntid) {
                    if (ntid != -1) {
                        break;
                    }
                } else {
                    if (nntid != -1) {
                        break;
                    }
                }

            } finally {
                lock.unlock();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Thread worker function that runs the algorithm.
     */
    @Override
    public void run() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (true) {
            if (!relay) {
                // process is active
                tid = id;
                while (true) {
                    send(tid);
                    gettid(true);
                    if (ntid == id) {
                        elected = true;
                        System.out.println("Process id=" + id + " has been elected!");
                        return;
                    }
                    send(Math.max(tid, ntid));
                    gettid(false);
                    if (nntid == id) {
                        elected = true;
                        System.out.println("Process id=" + id + " has been elected!");
                        return;
                    }
                    if (ntid >= tid && ntid >= nntid) {
                        tid = ntid;
                    } else {
                        relay = true;
                        break;
                    }
                    ntid = -1;
                    nntid = -1;
                }
            } else {
                // process is a relay
                while (true) {
                    gettid(true);
                    if (ntid == id) {
                        elected = true;
                        System.out.println("Process id=" + id + " has been elected!");
                        return;
                    }
                    send(ntid);
                    ntid = -1; // reset ntid so receive() knows to set ntid when receiving
                }
            }
        }
    }

    /**
     * Receive a message through RMI.
     * @param m Message to be received.
     * @throws RemoteException Exception occurred with RMI interface.
     */
    @Override
    public void receive(Message m) throws RemoteException {
        try {
            lock.lock();
            if (ntid == -1) {
                ntid = m.id;
                System.out.println("[" + id + "] received ntid=" + ntid);
            } else if (nntid == -1){
                nntid = m.id;
                System.out.println("[" + id + "] received nntid=" + nntid);
            }
        } finally {
            lock.unlock();
        }
    }
}
