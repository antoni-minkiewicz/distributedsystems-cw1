package field;
/*
 * Created on Feb 2022
 */

import centralserver.ICentralServer;
import common.MessageInfo;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;


/* You can add/change/delete class attributes if you think it would be
 * appropriate.
 * You can also add helper methods and change the implementation of those
 * provided if you think it would be appropriate, as long as you DO NOT
 * CHANGE the provided interface.
 */


public class FieldUnit implements IFieldUnit {

  private ICentralServer central_server;

  /* Note: Could you discuss in one line of comment what do you think can be
   * an appropriate size for buffsize?
   * (Which is used to init DatagramPacket?)
   */

  private static final int buffsize = 40;
  private ArrayList<MessageInfo> receivedMessages;
  private int totalMessagesExpected = 0;
  private ArrayList<Float> movingAverages;
  private DatagramSocket UDPSocket = null;
  private LocationSensor locationSensor;
  private ArrayList<Integer> missingMessagesNumbers;


  public FieldUnit() {
    /* TODO: Initialise data structures */
    receivedMessages = new ArrayList<MessageInfo>();
    movingAverages = new ArrayList<Float>();
    missingMessagesNumbers = new ArrayList<Integer>();
    try {
      locationSensor = new LocationSensor();
    } catch (RemoteException e) {
      e.printStackTrace();
    }

  }

  @Override
  public void addMessage(MessageInfo msg) {
    /* TODO: Save received message in receivedMessages */
    receivedMessages.add(msg);
  }

  @Override
  public void sMovingAverage(int k) {
    /* TODO: Compute SMA and store values in a class attribute */
    float sum = 0;
    float movingAverage = 0;
    int received_messages = this.receivedMessages.size();

    System.out.println("[Field Unit] Computing SMAs\n");

    for (int j = 0; j < received_messages; j++ ) {
      sum = 0;
      if((j+1) >= k) {
        for (int i = j; i > (j - k); i--) {
          sum += this.receivedMessages.get(i).getMessage();
        }
        movingAverage = sum / k;
        movingAverages.add(movingAverage);
      } else {
        movingAverages.add(this.receivedMessages.get(j).getMessage());
      }
    }

  }


  @Override
  public void receiveMeasures(int port, int timeout) throws SocketException {

    /* TODO: Create UDP socket and bind to local port 'port' */
    this.UDPSocket = new DatagramSocket(port);
    this.UDPSocket.setSoTimeout(timeout);

    boolean listen = true;

    System.out.println("[Field Unit] Listening on port: " + port);
    MessageInfo msg = null;
    DatagramPacket packet = null;
    byte[] buffer = new byte[buffsize];
    while (listen) {
      buffer = new byte[buffsize];
      packet = new DatagramPacket(buffer, buffsize);

      /* TODO: Receive until all messages in the transmission (msgTot) have been received or
                until there is nothing more to be received */
      try {
        UDPSocket.receive(packet);

      } catch (SocketTimeoutException e) {
        e.printStackTrace();
        break;
      } catch (IOException e) {
        e.printStackTrace();
      }

      /* TODO: If this is the first message, initialise the receive data structure before storing it. ??*/

      try {
        msg = new MessageInfo(new String(packet.getData()));
        System.out.println("[Field Unit] Message "
            + msg.getMessageNum() + " out of "
            + msg.getTotalMessages() +
            " received. "
            + "Value = "
            + msg.getMessage());
        if (this.totalMessagesExpected == 0) {
          this.totalMessagesExpected = msg.getTotalMessages();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      /* TODO: Store the message */
      this.receivedMessages.add(msg);
      /* TODO: Keep listening UNTIL done with receiving  */
      if (msg.getMessageNum() == msg.getTotalMessages()) {
        break;
      }
      msg = null;

    }

    /* TODO: Close socket  */
    UDPSocket.close();
  }

  public static void main(String[] args) throws SocketException {
    if (args.length < 2) {
      System.out.println("Usage: ./fieldunit.sh <UDP rcv port> <RMI server HostName/IPAddress>");
      return;
    }

    /* TODO: Parse arguments */
    int UDPPort = Integer.parseInt(args[0]);
    /* TODO: Check if this is a valid RMI address*/
    String RMIServerAddress = args[1];

    /* TODO: Construct Field Unit Object */
    FieldUnit fieldUnit = new FieldUnit();

    /* TODO: Call initRMI on the Field Unit Object */
    fieldUnit.initRMI(RMIServerAddress);

    boolean running = true;

    while (running) {
      /* TODO: Wait for incoming transmission */
      fieldUnit.receiveMeasures(UDPPort, 50000);
      /* TODO: Compute and print stats */
      fieldUnit.printStats();
      /* TODO: Compute Averages - call sMovingAverage()
            on Field Unit object */
      fieldUnit.sMovingAverage(7);
      /* TODO: Send data to the Central Serve via RMI and
       *        wait for incoming transmission again
       */
      fieldUnit.sendAverages();
      fieldUnit.reinitializeAttributes();
    }

  }


  @SuppressWarnings("removal")
  @Override
  public void initRMI(String address) {
    /* If you are running the program within an IDE instead of using the
     * provided bash scripts, you can use the following line to set
     * the policy file
     */

    System.setProperty("java.security.policy", "file:./policy\n");

    /* TODO: Initialise Security Manager */
    Registry registry = null;
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new SecurityManager());
    }
    try {
      /* TODO: Bind to RMIServer */
      URI uri = new URI(address);
      registry = LocateRegistry.getRegistry(uri.getHost(), uri.getPort());
      if (registry == null) {
        throw new RuntimeException();
      }
      //For debugging, to see what registry it is connecting to, enable:
      System.out.println(registry.toString());
      this.central_server = (ICentralServer) registry.lookup("CentralServer");
      /* TODO: Send pointer to LocationSensor to RMI Server */
      central_server.setLocationSensor(this.locationSensor);
    } catch (RemoteException e) {
      e.printStackTrace();
    } catch (NotBoundException e) {
      e.printStackTrace();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }


  }

  @Override
  public void sendAverages() {
    /* TODO: Attempt to send messages the specified number of times */
    System.out.println("[Field Unit] Sending SMAs to RMI\n\n");
    int number_of_moving_averages_to_send = this.movingAverages.size();
    MessageInfo msg = null;
    float movingAverage;
    for (int i = 1; i <= number_of_moving_averages_to_send; i++) {
      movingAverage = movingAverages.get(i - 1);
      msg = new MessageInfo(number_of_moving_averages_to_send, i, movingAverage);
      try {
        central_server.receiveMsg(msg);
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    }

  }

  @Override
  public void printStats() {
    /* TODO: Find out how many messages were missing (HOW TO DO THIS?) */
    int missing_messages = this.totalMessagesExpected - this.receivedMessages.size();
    System.out.println("Total Missing Messages = "
        + missing_messages + " out of "
        + this.totalMessagesExpected);
    /* TODO: Print stats (i.e. how many message missing?
     * do we know their sequence number? etc.) I can figure this out */
    if (missing_messages > 0) {
      int iterator = 0;
      for (int i = 1; i <= this.totalMessagesExpected; i++) {
        if (this.receivedMessages.get(iterator).getMessageNum() == i) {
          iterator++;
        } else {
          missingMessagesNumbers.add(i);
        }
      }
      System.out.println("Message Numbers of Missing Messages:" + missingMessagesNumbers.toString())
    }
    System.out.println("===============================");
  }

  public void reinitializeAttributes(){
    /* TODO: Now re-initialise data structures for next time */
    totalMessagesExpected = 0;
    receivedMessages.clear();
    movingAverages.clear();
    missingMessagesNumbers.clear();
  }


  // For testing
  final public ArrayList<MessageInfo> getReceivedMessages() {
    return this.receivedMessages;
  }

  final public ArrayList<Float> getMovingAverages() {
    return this.movingAverages;
  }
}
