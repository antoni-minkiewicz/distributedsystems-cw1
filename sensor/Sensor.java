package sensor;
/*
 * Created on Feb 2022
 */

import common.MessageInfo;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;

/* You can add/change/delete class attributes if you think it would be
 * appropriate.
 * You can also add helper methods and change the implementation of those
 * provided if you think it would be appropriate, as long as you DO NOT
 * CHANGE the provided interface.
 */

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
    /* TODO: Build Sensor Object */
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
    /* TODO: Send N measurements */
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

    /* TODO: Call constructor of sensor to build Sensor object*/
    Sensor sensor = new Sensor(address, port, totMsg);
    /* TODO: Use Run to send the messages */
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
      /* TODO: Build destination address object */
      InetAddress inetAddress = InetAddress.getByName(address);
      /* TODO: Build datagram packet to send */
      DatagramPacket datagramPacket = new DatagramPacket(toSend.getBytes(), toSend.length(),
          inetAddress, port);
      /* TODO: Send packet */
      this.socket.send(datagramPacket);
      System.out.printf("[Sensor] Sending message %s out of %s. Measure = %s\n", msg.getMessageNum(),
          msg.getTotalMessages(), msg.getMessage());
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
