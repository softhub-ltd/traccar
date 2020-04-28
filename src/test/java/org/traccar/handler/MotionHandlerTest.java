package org.traccar.handler;

import org.junit.Test;
import org.traccar.model.Position;

import static org.junit.Assert.assertEquals;

public class MotionHandlerTest {

    @Test
    public void testCalculateMotion() {

        MotionHandler motionHandler = new MotionHandler(0.01,  null);

        Position position = motionHandler.handlePosition(new Position());

        assertEquals(false, position.getAttributes().get(Position.KEY_MOTION));

    }

}
