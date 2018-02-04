package qirkat;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests of the Board class.
 *
 * @author Ryan Brill
 */
public class BoardTest {

    private static final String INIT_BOARD =
            "  b b b b b\n  b b b b b\n  b b - w w\n  w w w w w\n  w w w w w";

    private static final String[] GAME1 = {"c2-c3", "c4-c2", "c1-c3", "a3-c1",
                                           "c3-a3", "c5-c4", "a3-c5-c3"};

    private static final String GAME1_BOARD =
            "  b b - b b\n  b - - b b\n  - - w w w\n  w - - w w\n  w w b w w";

    private static void makeMoves(Board b, String[] moves) {
        for (String s : moves) {
            b.makeMove(Move.parseMove(s));
        }
    }

    @Test
    public void testInit1() {
        Board b0 = new Board();
        assertEquals(INIT_BOARD, b0.toString());
    }

    @Test
    public void testMoves() {
        Board b0 = new Board();
        makeMoves(b0, GAME1);
        assertEquals(GAME1_BOARD, b0.toString());
    }

    @Test
    public void testSomething() {
        Board b = new Board();

        b.makeMove(Move.parseMove("d3-c3"));
        Board b1 = new Board();
        b1.setPieces("wwwwwwwwwwbbw-wbbbbbbbbbb", PieceColor.BLACK);
        assertEquals(b1, b);

        b.makeMove(Move.parseMove("b3-d3"));
        b.makeMove(Move.parseMove("e3-c3"));
        b1.setPieces("wwwwwwwwwwb-w--bbbbbbbbbb", PieceColor.BLACK);
        assertEquals(b1, b);

        b.makeMove(Move.parseMove("e4-e3"));
        b1.setPieces("wwwwwwwwwwb-w-bbbbb-bbbbb", PieceColor.WHITE);
        assertEquals(b1, b);
    }

    @Test
    public void testGetMoves() {
        Board b = new Board();
        b.setPieces("-bw--bb---------bb-------", PieceColor.WHITE);
        ArrayList<Move> expected = new ArrayList<>();
        expected.add(Move.parseMove("c1-a1-c3-a5"));
        expected.add(Move.parseMove("c1-a1-a3-c1"));
        expected.add(Move.parseMove("c1-a3-c5-c3"));
        expected.add(Move.parseMove("c1-a3-a1-c1"));
        expected.add(Move.parseMove("c1-a1-c3-c5-a3-a1"));
        expected.add(Move.parseMove("c1-a1-a3-c5-c3-a1"));

        ArrayList<Move> moves = b.getMoves();

        for (Move e : expected) {
            assertTrue(moves.contains(e));
        }

        for (Move m : moves) {
            assertTrue(expected.contains(m));
        }

    }

    @Test
    public void testGetMoves2() {
        Board b = new Board();
        b.setPieces("-bw--bb------------------", PieceColor.WHITE);
        ArrayList<Move> expected = new ArrayList<>();
        expected.add(Move.parseMove("c1-a3-a1-c1"));
        expected.add(Move.parseMove("c1-a1-a3-c1"));
        expected.add(Move.parseMove("c1-a1-c3"));
        ArrayList<Move> moves = b.getMoves();

        for (Move e : expected) {
            assertTrue(moves.contains(e));
        }

        for (Move m : moves) {
            assertTrue(expected.contains(m));
        }

    }

}
