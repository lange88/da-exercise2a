import interfaces.DA_Peterson_RMI;
import interfaces.Message;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by jeroen on 11/22/17.
 * Implementation class for Peterson's algorithm.
 */
public class DA_Peterson extends UnicastRemoteObject implements DA_Peterson_RMI, Runnable {
    private static boolean elected = false;

    private int id;
    private int nextProcessId;

    private int tid = -1;
    private int ntid = -1;

    private boolean active = true;
    private boolean receivingNtid = true;

    private DA_Peterson_RMI nextProcess = null;

    private List<Message> messageList = Collections.synchronizedList(new ArrayList<Message>());

    DA_Peterson(int id, int nextProcessId) throws RemoteException {
        this.id = id;
        this.nextProcessId = nextProcessId;
    }

    /**
     * Thread worker function that runs the algorithm.
     */
    @Override
    public void run() {
        //It is possible that messages have been received by this process before
        //this code is called, so we have to ensure the tid message is the first message,
        //by putting it in front of the queue
        messageList.add(0, new Message(id));

        while (!elected) {
            sendMessages();
            try {
                //This algorithm should be able to deal with any timeout, so you can play around with the
                //actual sleep time
                Thread.sleep(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Receive a message through RMI. The message is added to a message queue in order to ensure the order is
     * preserved.
     * @param m Message to be received.
     * @throws RemoteException Exception occurred with RMI interface.
     */
    @Override
    public void receive(Message m) throws RemoteException {
        //Initialize tid for comparison purposes
        if (tid == -1) {
            tid = id;
        }

        if (receivingNtid) {
            System.out.println("[" + id + "] received ntid=" + m.id);
            if (checkElected(m.id)) return;
            if (active) {
                //Send max of tid and ntid to next process.
                messageList.add(new Message(Math.max(tid, m.id)));
                //Update ntid for relay calculation
                ntid = m.id;
                receivingNtid = false;
            } else /*process is a relay*/ {
                messageList.add(new Message(m.id));
            }

        } else /*receiving nntid*/{
            System.out.println("[" + id + "] received nntid=" + m.id);
            if (checkElected(m.id)) return;
            if (ntid >= tid && ntid == m.id) {
                //Since m.id == max(ntid, nntid), ntid is always <= m.id
                //Coming here means ntid = m.id, so the direct neighbour has the highest identity
                //We stay active and assume its identity
                tid = ntid;
                messageList.add(new Message(m.id));
            } else {
                System.out.println("Process [" + id + "] switched to relay mode");
                //The second neighbour has the highest identity, so we take up that identity
                tid = m.id;
                //Then we switch to relay mode
                active = false;
            }
            receivingNtid = true;
        }
    }

    private void sendMessages() {
        if (messageList.isEmpty() || elected) {
            return;
        }

        if (nextProcess == null) {
            try {
                String name = "rmi://localhost/DA_Peterson_" + nextProcessId;
                nextProcess = (DA_Peterson_RMI) java.rmi.Naming.lookup(name);
            } catch (Exception e) {
                System.out.println("Failed to connect to next process, retrying later");
                return;
            }
        }
        while (!messageList.isEmpty()) {
            Message nextMessage = messageList.remove(0);
            boolean success = false;
            while (!success) {
                try {
                    nextProcess.receive(nextMessage);
                    success = true;
                } catch (RemoteException e) {
                    System.out.println("Failed to deliver message, retrying in 1 second");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        System.out.println("Thread await was interrupted, retrying immediately");
                    }
                }
            }
        }
    }

    private boolean checkElected(int electionId) {
        if (electionId == id) {
            System.out.println("*************** Process id=" + id + " has been elected! *****************");
            elected = true;
            return true;
        }
        return false;
    }
}
