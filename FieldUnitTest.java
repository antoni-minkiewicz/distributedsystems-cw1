import static org.junit.jupiter.api.Assertions.*;

import common.MessageInfo;
import field.FieldUnit;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class FieldUnitTest {

  @Test
  public void addsOneMessage(){
    FieldUnit fieldUnit = new FieldUnit();
    MessageInfo msg = new MessageInfo(1, 1, 10);
    fieldUnit.addMessage(msg);
    assertEquals(msg, fieldUnit.getReceivedMessages().get(0));
    assertEquals(10, fieldUnit.getReceivedMessages().get(0).getMessage());
  }

  @Test
  public void computeAverageOfOneMessage(){
    FieldUnit fieldUnit = new FieldUnit();
    MessageInfo msg = new MessageInfo(1, 1, 10);
    fieldUnit.addMessage(msg);
    fieldUnit.sMovingAverage(7);
    List<Float> movingaverages = (List<Float>) List.of(10.0F);
    assertEquals(movingaverages, fieldUnit.getMovingAverages());
  }

  @Test
  public void computeAverageOfSeMessage(){
    FieldUnit fieldUnit = new FieldUnit();
    ArrayList<Float> movingaverages = new ArrayList<Float>();
    MessageInfo msg = new MessageInfo(1, 1, 10);
    for(int i = 0; i < 20; i++){
      msg = new MessageInfo(1, 1, 10);
      fieldUnit.addMessage(msg);
      movingaverages.add(10F);
    }
    fieldUnit.sMovingAverage(7);
    assertEquals(movingaverages, fieldUnit.getMovingAverages());
  }

  @Test public void printingOutSizeOfMessageInfo(){
    MessageInfo msg = new MessageInfo(512345, 523456, 2.555557876876F);
    System.out.println(msg.toString().getBytes());
    byte [] buff = new byte[23];
    for (int i = 0; i < 23; i++) {
      buff[i] = msg.toString().getBytes()[i];
    }
    System.out.println(new String(buff));
    /*Four bytes for the semicolons, 4 bytes for the float, 4 bytes per int so +8*/
    /*4+4+8 */
    String str1 = new String("hello");
    System.out.println("byte length" + str1.getBytes().length + "string length" + str1.length());
  }
}