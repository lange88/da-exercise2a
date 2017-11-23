import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;

/**
 * Created by jeroen on 11/22/17.
 * Main class for Peterson algorithm.
 */
public class DA_Peterson_main {
    public static void main(String... args) {
        OptionParser parser = new OptionParser();

        parser.accepts("t", "Total number of processes in DS")
                .withRequiredArg().ofType(Integer.class);
        parser.accepts("i", "IDs of processes in DS")
                .withRequiredArg().ofType(String.class);

        OptionSet options = parser.parse(args);

        if (!options.has("i") || !options.has("t")) {
            return;
        }

        int totalProcesses;
        totalProcesses = (Integer) options.valueOf("t");
        String ids = (String) options.valueOf("i");
        int[] processIds = Arrays.stream(ids.split(" "))
                .map(String::trim).mapToInt(Integer::parseInt).toArray();

        // create local registry so RMI can register itself
        try {
            java.rmi.registry.LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
            System.out.println("RemoteException occurred while starting the registry.");
            e.printStackTrace();
        }

        for (int i = 0; i < totalProcesses; i++) {
            String name = "rmi://localhost/DA_Peterson_" + processIds[i];

            DA_Peterson da = null;
            try {
                if (i == totalProcesses - 1) {
                    da = new DA_Peterson(processIds[i], processIds[0]);
                } else {
                    da = new DA_Peterson(processIds[i], processIds[i + 1]);
                }

                java.rmi.Naming.bind(name, da);
            } catch (AlreadyBoundException e) {
                System.out.println("AlreadyBoundException occurred while binding object with RMI name: " + name);
                e.printStackTrace();
            } catch (MalformedURLException e) {
                System.out.println("MalformedURLException occurred while binding object with RMI name: " + name);
                e.printStackTrace();
            } catch (RemoteException e) {
                System.out.println("RemoteException occurred while binding object with RMI name: " + name);
                e.printStackTrace();
            }

            if (da == null) {
                return;
            }

            // finally start the worker thread of the process
            System.out.println("Starting process node with id=" + processIds[i]
                    + ", totalProcesses=" + totalProcesses);
            new Thread(da).start();
        }
    }
}
