package centralserver;

import common.MessageInfo;
import field.ILocationSensor;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class CentralServer extends UnicastRemoteObject implements ICentralServer {

  private ArrayList<MessageInfo> receivedMessages;
  int totalNumberOfMessagesExpected = 0;
  private ILocationSensor locationSensor;
  private int msgReceivedCounter = 0;

  protected CentralServer() throws RemoteException {
    super();
  }

  @SuppressWarnings("removal")
  public static void main(String[] args) throws RemoteException {
    CentralServer cs = new CentralServer();

    int port = Integer.parseInt(args[0]);

    /* Configures Security Manager */
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new SecurityManager());
    }

    /* Configures registry*/
    Registry registry = null;
    try {
      registry = LocateRegistry.createRegistry(port);
    } catch (RemoteException e) {
      e.printStackTrace();
    }
    registry.rebind("CentralServer", cs);

    System.out.println("Central Server is running...");
    //For debugging enable:
    //System.out.println(registry.toString());
  }


  @Override
  public void receiveMsg(MessageInfo msg) {
    System.out.println("[Central Server] Received message " + (msg.getMessageNum()) + " out of "
        + msg.getTotalMessages() + ". Measure = " + msg.getMessage());

    /*Resets message counter and initialises message storing structure*/
    if (msg.getMessageNum() == 1) {
      totalNumberOfMessagesExpected = msg.getTotalMessages();
      msgReceivedCounter = 0;
      this.receivedMessages = new ArrayList<MessageInfo>();
    }

    this.receivedMessages.add(msg);
    msgReceivedCounter++;

    if (msgReceivedCounter == totalNumberOfMessagesExpected) {
      printStats();
    }

  }

  public void printStats() {

    int numberOfMissingMessages = totalNumberOfMessagesExpected - receivedMessages.size();
    System.out.println("[Central Server] Total Missing Messages = "
              + numberOfMissingMessages
              +" out of " + totalNumberOfMessagesExpected + "\n");

    try {
      this.printLocationOfSensor();
    } catch (RemoteException e) {
      e.printStackTrace();
    }

    reinitializeAttributes();
  }

  private void reinitializeAttributes() {
    msgReceivedCounter = 0;
    totalNumberOfMessagesExpected = 0;
    receivedMessages.clear();
  }

  @Override
  public void setLocationSensor(ILocationSensor locationSensor) throws RemoteException {

    this.locationSensor = locationSensor;
    System.out.println("Location Sensor Set");
  }

  public void printLocationOfSensor() throws RemoteException {
    System.out.println(
        "[Field Unit] Current Location: lat = " + locationSensor.getCurrentLocation().getLatitude()
            + " long = " + locationSensor.getCurrentLocation().getLongitude() + "\n");
  }
}
