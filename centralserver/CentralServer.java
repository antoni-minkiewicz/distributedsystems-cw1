package centralserver;

import common.MessageInfo;
import field.ILocationSensor;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

/* You can add/change/delete class attributes if you think it would be
 * appropriate.
 * You can also add helper methods and change the implementation of those
 * provided if you think it would be appropriate, as long as you DO NOT
 * CHANGE the provided interface.
 */

/* TODO extend appropriate classes and implement the appropriate interfaces */
public class CentralServer extends UnicastRemoteObject implements ICentralServer {

  private final ArrayList<MessageInfo> receivedMessages;
  int totalNumberOfMessages = 0;
  private ILocationSensor locationSensor;
  private int msgCounter = 0;

  protected CentralServer() throws RemoteException {
    super();
    /* TODO: Initialise Array receivedMessages */
    receivedMessages = new ArrayList<MessageInfo>();
  }

  @SuppressWarnings("removal")
  public static void main(String[] args) throws RemoteException {
    CentralServer cs = new CentralServer();

    /* If you are running the program within an IDE instead of using the
     * provided bash scripts, you can use the following line to set
     * the policy file
     */

    /* System.setProperty("java.security.policy","file:./policy\n"); */

    /* TODO: Configure Security Manager */
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new SecurityManager());
    }
    Registry registry = null;
    /* TODO: Create (or Locate) Registry */
    try {
      registry = LocateRegistry.createRegistry(1000);
    } catch (RemoteException e) {
      e.printStackTrace();
    }

    /* TODO: Bind to Registry */
    registry.rebind("CentralServer", cs);

    System.out.println("Central Server is running...");
    System.out.println(registry.toString());
  }


  @Override
  public void receiveMsg(MessageInfo msg) {
    System.out.println("[Central Server] Received message " + (msg.getMessageNum()) + " out of "
        + msg.getTotalMessages() + ". Measure = " + msg.getMessage());

    /* TODO: If this is the first message, reset counter and initialise data structure. */
    if (msg.getMessageNum() == 1) {
      totalNumberOfMessages = msg.getTotalMessages();
      /*TODO Receive data structure?*/
    }
    /* TODO: Save current message */
    this.receivedMessages.add(msg);
    msgCounter++;
    /* TODO: If I received everything that there was to be received, prints stats.
        What about the case when the last message is dropped?*/
    if (msgCounter == totalNumberOfMessages) {
      printStats();
    }

  }

  public void printStats() {
    /* TODO: Find out how many messages were missing */
    int missingMessages = totalNumberOfMessages - receivedMessages.size();
    System.out.println(receivedMessages.size());
    /* TODO: Print stats (i.e. how many message missing?
     * do we know their sequence number? etc.) */
    System.out.println("[Central Server] Total Missing Messages = "
              + missingMessages
              +" out of " + totalNumberOfMessages + "\n");
    if (missingMessages > 0) {
      /* TODO: iterate and print out sequence numbers*/
    }

    /* TODO: Print the location of the Field Unit that sent the messages */
    try {
      this.printLocation();
    } catch (RemoteException e) {
      e.printStackTrace();
    }
    /* TODO: Now re-initialise data structures for next time */
    msgCounter = 0;
    totalNumberOfMessages = 0;
    receivedMessages.clear();
  }

  @Override
  public void setLocationSensor(ILocationSensor locationSensor) throws RemoteException {

    /* TODO: Set location sensor */
    this.locationSensor = locationSensor;
    System.out.println("Location Sensor Set");
  }

  public void printLocation() throws RemoteException {
    /* TODO: Print location on screen from remote reference */
    System.out.println(
        "[Field Unit] Current Location: lat = " + locationSensor.getCurrentLocation().getLatitude()
            + " long = " + locationSensor.getCurrentLocation().getLongitude() + "\n");
  }
}
