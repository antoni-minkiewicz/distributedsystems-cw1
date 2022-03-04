package sensor;

import common.MessageInfo;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;

public class Sensor implements ISensor {

  private final String addressOfDestination;
  private final int portOfDestination;
  private float measurement;

  private final static int max_measure = 50;
  private final static int min_measure = 10;

  private DatagramSocket socket = null;
  private byte[] buffer;

  /* Note: Could you discuss in one line of comment what do you think can be
   * an appropriate size for buffsize?
   * (Which is used to init DatagramPacket?)
   */
  /* TODO: What is this buffer even used for?*/
  private static final int buffsize = 2048;

  public Sensor(String address, int port, int totMsg) {
    this.addressOfDestination = address;
    this.portOfDestination = port;
    try {
      this.socket = new DatagramSocket();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void run(int N) throws InterruptedException {
    for (int i = 1; i <= N; i++) {
      float measurement = this.getMeasurement();
      MessageInfo messageInfo = new MessageInfo(N, i, measurement);
      sendMessage(this.addressOfDestination, this.portOfDestination, messageInfo);
    }
  }

  public static void main(String[] args) {
    if (args.length < 3) {
      System.out.println("Usage: ./sensor.sh field_unit_address port number_of_measures");
      return;
    }

    /* Parse input arguments */
    String address = args[0];
    int port = Integer.parseInt(args[1]);
    int totMsg = Integer.parseInt(args[2]);

    Sensor sensor = new Sensor(address, port, totMsg);
    /* TODO: (ANTONI) Is this good exception handling?*/
    try {
      sensor.run(totMsg);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void sendMessage(String address, int port, MessageInfo msg) {
    String toSend = msg.toString();

    try {
      InetAddress inetAddress = InetAddress.getByName(address);
      DatagramPacket datagramPacket = new DatagramPacket(toSend.getBytes(), toSend.length(),
          inetAddress, port);

      this.socket.send(datagramPacket);
      System.out.printf("[Sensor] Sending message %s out of %s. Measure = %s\n",
          msg.getMessageNum(), msg.getTotalMessages(), msg.getMessage());
    } catch (SocketException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }


  }

  @Override
  public float getMeasurement() {
    Random r = new Random();
    measurement = r.nextFloat() * (max_measure - min_measure) + min_measure;

    return measurement;
  }
}
