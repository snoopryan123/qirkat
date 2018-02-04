/* Author: Paul N. Hilfinger.  (C) 2008. */

package qirkat;

import org.junit.Test;

import static org.junit.Assert.*;
import static qirkat.Move.move;
import static qirkat.Move.parseMove;

/**
 * Test Move creation.
 *
 * @author Ryan Brill
 */
public class MoveTest {

    @Test
    public void testMove1() {
        Move m = move('a', '3', 'b', '2');
        assertNotNull(m);
        assertFalse("move should not be jump", m.isJump());
    }

    @Test
    public void testJump1() {
        Move m = move('a', '3', 'a', '5');
        assertNotNull(m);
        assertTrue("move should be jump", m.isJump());
    }

    @Test
    public void testString() {
        assertEquals("a3-b2", move('a', '3', 'b', '2').toString());
        assertEquals("a3-a5", move('a', '3', 'a', '5').toString());
        assertEquals("a3-a5-c3", move('a', '3', 'a', '5',
                move('a', '5', 'c', '3')).toString());
    }

    @Test
    public void testParseString() {
        assertEquals("a3-b2", parseMove("a3-b2").toString());
        assertEquals("a3-a5", parseMove("a3-a5").toString());
        assertEquals("a3-a5-c3", parseMove("a3-a5-c3").toString());
        assertEquals("a3-a5-c3-e1", parseMove("a3-a5-c3-e1").toString());
    }

    @Test
    public void testConcatenate() {
        Move M1 = Move.parseMove("c1-a1");
        Move M2 = Move.parseMove("a1-a3");
        Move M3 = Move.parseMove("a3-c5");
        Move M4 = Move.parseMove("c5-c3");

        Move M12 = Move.move(M1, M2);
        assertEquals(Move.parseMove("c1-a1-a3"), M12);
        Move M123 = Move.move(M12, M3);
        assertEquals(Move.parseMove("c1-a1-a3-c5"), M123);
        Move M1234 = Move.move(M123, M4);
        assertEquals(Move.parseMove("c1-a1-a3-c5-c3"), M1234);

        Move M34 = Move.move(M3, M4);
        assertEquals(Move.parseMove("a3-c5-c3"), M34);
        Move M234 = Move.move(M2, M34);
        assertEquals(Move.parseMove("a1-a3-c5-c3"), M234);
        Move M12342 = Move.move(M1, M234);
        assertEquals(Move.parseMove("c1-a1-a3-c5-c3"), M12342);
    }
}
