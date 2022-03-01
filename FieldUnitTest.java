import static org.junit.jupiter.api.Assertions.*;

import common.MessageInfo;
import field.FieldUnit;
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
}