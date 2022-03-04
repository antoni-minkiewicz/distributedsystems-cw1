package field;

import centralserver.ICentralServer;
import common.MessageInfo;
import java.io.IOException;
import java.net.ConnectException;
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


public class FieldUnit implements IFieldUnit {

  private ICentralServer central_server;

  /* Note: Could you discuss in one line of comment what do you think can be
   * an appropriate size for buffsize? What we are receiving is a string version of MessageInfo
   * this means that for each char we need 1byte. Float will be 8 characters max, 2 semicolons
   * Messages go up to lets say 10000. That means buffsize should be 20.
   *
   */


  private DatagramSocket UDPSocket = null;
  private LocationSensor locationSensor;

  private static final int bufferSize = 20;
  private boolean isFirstMessageInExchange = true;
  private ArrayList<MessageInfo> receivedMessages;
  private int totalMessagesExpectedInCurrentExchange = 0;

  private ArrayList<Integer> missingMessageIds;
  private ArrayList<Float> movingAverages;
  private ArrayList<Long> arrayOfTimesTaken;
  private ArrayList<Long> arrayOfTimesTakenToSendToCentralServer;


  public FieldUnit() {
    receivedMessages = new ArrayList<MessageInfo>();
    movingAverages = new ArrayList<Float>();
    missingMessageIds = new ArrayList<Integer>();
    arrayOfTimesTaken = new ArrayList<Long>();
    arrayOfTimesTakenToSendToCentralServer = new ArrayList<Long>();
    try {
      locationSensor = new LocationSensor();
    } catch (RemoteException e) {
      e.printStackTrace();
    }

  }

  @Override
  public void addMessage(MessageInfo msg) {
    receivedMessages.add(msg);
  }

  @Override
  public void sMovingAverage(int k) {
    float sum = 0;
    float movingAverage = 0;
    int sizeOfReceivedMessages = this.receivedMessages.size();

    System.out.println("[Field Unit] Computing SMAs\n");
    /* Computes moving average for given number k*/
    for (int j = 0; j < sizeOfReceivedMessages; j++) {
      sum = 0;
      if ((j + 1) >= k) {
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

    this.UDPSocket = new DatagramSocket(port);
    this.UDPSocket.setSoTimeout(timeout);

    boolean listen = true;

    System.out.println("[Field Unit] Listening on port: " + port);
    MessageInfo msg = null;
    DatagramPacket packet = null;
    byte[] buffer = new byte[bufferSize];
    while (listen) {
      buffer = new byte[bufferSize];
      packet = new DatagramPacket(buffer, bufferSize);

      try {
        long startTime = System.nanoTime();
        UDPSocket.receive(packet);
        long totalTime = System.nanoTime() - startTime;
        this.arrayOfTimesTaken.add(totalTime);
        msg = new MessageInfo(new String(packet.getData()));
        if (this.isFirstMessageInExchange) {
          this.isFirstMessageInExchange = false;
          this.totalMessagesExpectedInCurrentExchange = msg.getTotalMessages();
        }

        System.out.println(
            "[Field Unit] Message " + msg.getMessageNum() + " out of " + msg.getTotalMessages()
                + " received. " + "Value = " + msg.getMessage());
      } catch (SocketTimeoutException e) {
        System.out.println("Timed out");
        break;
      } catch (IOException e) {
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
      this.receivedMessages.add(msg);

      if (msg.getMessageNum() == msg.getTotalMessages()) {
        listen = false;
      }

    }

    UDPSocket.close();
  }

  public static void main(String[] args) throws SocketException {
    if (args.length < 2) {
      System.out.println("Usage: ./fieldunit.sh <UDP rcv port> <RMI server HostName/IPAddress>");
      return;
    }
    /*This is the udp port of the sensor to which the fieldunit wants to connect*/
    int UDPPort = Integer.parseInt(args[0]);

    /*This is the RMI server of the centralserver the fieldunit wants to connect to
     * the format of the uri is rmi://hostname:port */
    String RMIServerAddress = args[1];

    FieldUnit fieldUnit = new FieldUnit();

    /* This initialises the RMI connection to the centralserver*/
    fieldUnit.initRMI(RMIServerAddress);

    boolean running = true;

    while (running) {
      /*This sets the fieldunit socket to receive messages from the sensor
       * It will stop when all messages are received or when it timesout*/
      fieldUnit.receiveMeasures(UDPPort, 50000);

      fieldUnit.printStats();
      fieldUnit.sMovingAverage(7);

      /* Sends averages to central server */
      fieldUnit.sendAverages();
      fieldUnit.reinitializeAttributes();
    }

  }


  @SuppressWarnings("removal")
  @Override
  public void initRMI(String address) {
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new SecurityManager());
    }

    Registry registry = null;
    try {
      /* Binds to central server*/
      URI uri = new URI(address);
      registry = LocateRegistry.getRegistry(uri.getHost(), uri.getPort());

      // For debugging, to see what registry it is connecting to, enable:
      //System.out.println(registry.toString());
      this.central_server = (ICentralServer) registry.lookup("CentralServer");
      central_server.setLocationSensor(this.locationSensor);
    } catch (RemoteException e) {
      e.printStackTrace();
    } catch (NotBoundException e) {
      e.printStackTrace();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    } catch (Exception e) {
      System.out.println("Unhandled exception: " + e.toString());
    }


  }

  @Override
  public void sendAverages() {
    /*Sends averages to central server*/
    System.out.println("[Field Unit] Sending SMAs to RMI\n\n");
    int number_of_moving_averages_to_send = this.movingAverages.size();
    MessageInfo msg = null;
    float movingAverage;
    for (int i = 1; i <= number_of_moving_averages_to_send; i++) {
      movingAverage = movingAverages.get(i - 1);
      msg = new MessageInfo(number_of_moving_averages_to_send, i, movingAverage);
      try {
        long timeStart = System.nanoTime();
        central_server.receiveMsg(msg);
        long timeToSend = System.nanoTime() - timeStart;
        this.arrayOfTimesTakenToSendToCentralServer.add(timeToSend);
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    }
    int sum = 0;
    for (int i = 0; i < this.arrayOfTimesTakenToSendToCentralServer.size(); i++) {
      sum += arrayOfTimesTakenToSendToCentralServer.get(i);
    }
    System.out.println("Average time taken to send rmi "
        + "messages to central server - "
        + sum / arrayOfTimesTakenToSendToCentralServer.size() + " nanoseconds or " +
        +((sum / arrayOfTimesTakenToSendToCentralServer.size()) * Math.pow(10, -9)) + " seconds");

  }

  @Override
  public void printStats() {
    int missing_messages =
        this.totalMessagesExpectedInCurrentExchange - this.receivedMessages.size();

    System.out.println("Total Missing Messages = " + missing_messages + " out of "
        + this.totalMessagesExpectedInCurrentExchange);

    if (missing_messages > 0) {
      int iterator = 0;
      for (int i = 1; i <= this.totalMessagesExpectedInCurrentExchange; i++) {
        if (this.receivedMessages.get(iterator).getMessageNum() == i) {
          iterator++;
        } else {
          missingMessageIds.add(i);
        }
      }
      System.out.println("Message Numbers of Missing Messages:" + missingMessageIds.toString());
    }
    int sum = 0;
    for (int i = 1; i < this.arrayOfTimesTaken.size(); i++) {
      sum += this.arrayOfTimesTaken.get(i);
    }
    if (0 <= arrayOfTimesTaken.size()) {
      System.out.println("Average time taken to receive udp "
          + "messages from sensor - (not including first message) "
          + sum / arrayOfTimesTaken.size() + " nanoseconds or " +
          +((sum / arrayOfTimesTaken.size()) * Math.pow(10, -9)) + " seconds");
    }
    System.out.println("===============================");
  }

  private void reinitializeAttributes() {
    totalMessagesExpectedInCurrentExchange = 0;
    isFirstMessageInExchange = true;
    receivedMessages.clear();
    movingAverages.clear();
    missingMessageIds.clear();
    arrayOfTimesTaken.clear();
  }


  // For testing purposes
  final public ArrayList<MessageInfo> getReceivedMessages() {
    return this.receivedMessages;
  }

  final public ArrayList<Float> getMovingAverages() {
    return this.movingAverages;
  }
}
