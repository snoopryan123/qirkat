package qirkat;

import java.util.Observable;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Observer;
import static qirkat.Move.*;
import static qirkat.PieceColor.*;

/**
 * A Qirkat board.   The squares are labeled by column (a char value between
 * 'a' and 'e') and row (a char value between '1' and '5'.
 * <p>
 * For some purposes, it is useful to refer to squares using a single
 * integer, which we call its "linearized index", from 0 to 24.
 * This is simply the number of the square in row-major order
 * (with row 0 being the bottom row) counting from 0.
 * <p>
 * Moves on this board are denoted by Moves.
 *
 * @author Ryan Brill
 */
class Board extends Observable {

    /**
     * Convenience value giving values of pieces at each ordinal position.
     */
    static final PieceColor[] PIECE_VALUES = PieceColor.values();
    /**
     * When the game ends, make note of the winning color.
     */
    private PieceColor _winner = null;
    /** Return the winner on this board, if there is one. */
    PieceColor winner() {
        return _winner;
    }
    /**
     * A representation of the board as a lenth 25 array.
     * private PieceColor[] _board.
     */
    private PieceColor[] _board;

    /**
     * Map PieceColors to their Character representations.
     */
    private Map<PieceColor, Character> colorChar =
            new HashMap<PieceColor, Character>() { {
            put(WHITE, 'w');
            put(BLACK, 'b');
            put(EMPTY, '-');
        } };
    /**
     * Player that is on move.
     */
    private PieceColor _whoseMove;
    /**
     * Set true when game ends.
     */
    private boolean _gameOver;
    /** The size of the legend of the printed board. */
    private final int legendSize = 10;
    /** The char value of a. */
    private final int aChar = 49;
    /** The char value of e. */
    private final int eChar = 53;
    /**
     * A new, cleared board at the start of the game.
     */
    Board() {
        _board = new PieceColor[SIDE * SIDE];
        clear();
    }

    /**
     * A copy of B.
     */
    Board(Board b) {
        _board = new PieceColor[SIDE * SIDE];
        internalCopy(b);
    }

    /**
     * Return a constant view of me (allows any access method, but no
     * method that modifies it).
     */
    Board constantView() {
        return this.new ConstantBoard();
    }

    /**
     * Clear me to my starting state, with pieces in their initial
     * positions.
     */
    void clear() {
        _whoseMove = WHITE;
        _gameOver = false;
        horizontalBlocks = new ArrayList<>();

        setPieces("wwwwwwwwwwbb-wwbbbbbbbbbb", _whoseMove);

        setChanged();
        notifyObservers();
    }

    /**
     * Copy B into me.
     */
    void copy(Board b) {
        internalCopy(b);
    }

    /**
     * Copy B into me.
     */
    private void internalCopy(Board b) {
        _whoseMove = b._whoseMove;
        _gameOver = b._gameOver;
        for (Integer[] B : b.horizontalBlocks) {
            horizontalBlocks.add(B.clone());
        }
        for (int k = 0; k < SIDE * SIDE; k += 1) {
            _board[k] = b.get(k);
        }
    }

    /**
     * Set my contents as defined by STR.  STR consists of 25 characters,
     * each of which is b, w, or -, optionally interspersed with whitespace.
     * These give the contents of the Board in row-major order, starting
     * with the bottom row (row 1) and left column (column a). All squares
     * are initialized to allow horizontal movement in either direction.
     * NEXTMOVE indicates whose move it is.
     */
    void setPieces(String str, PieceColor nextMove) {
        if (nextMove == EMPTY || nextMove == null) {
            throw new IllegalArgumentException("bad player color");
        }
        str = str.replaceAll("\\s", "");
        if (!str.matches("[bw-]{25}")) {
            throw new IllegalArgumentException("bad board description");
        }
        _whoseMove = nextMove;
        for (int k = 0; k < str.length(); k += 1) {
            switch (str.charAt(k)) {
            case '-':
                set(k, EMPTY);
                break;
            case 'b':
            case 'B':
                set(k, BLACK);
                break;
            case 'w':
            case 'W':
                set(k, WHITE);
                break;
            default:
                break;
            }
        }
        horizontalBlocks = new ArrayList<>();
        setChanged();
        notifyObservers();
    }

    /**
     * Return the current contents of square C R, where 'a' <= C <= 'e',
     * and '1' <= R <= '5'.
     */
    PieceColor get(char c, char r) {
        assert validSquare(c, r);
        return get(index(c, r));
    }

    /**
     * Return the current contents of the square at linearized index K.
     */
    PieceColor get(int k) {
        assert validSquare(k);
        return _board[k];
    }

    /**
     * Set get(C, R) to V, where 'a' <= C <= 'e', and
     * '1' <= R <= '5'.
     */
    private void set(char c, char r, PieceColor v) {
        assert validSquare(c, r);
        set(index(c, r), v);
    }

    /**
     * Set get(K) to V, where K is the linearized index of a square.
     */
    private void set(int k, PieceColor v) {
        assert validSquare(k);
        _board[k] = v;
    }

    /**
     * Return true iff MOV is a valid jump sequence on the current board.
     * MOV must be a jump or null.  If ALLOWPARTIAL, allow jumps that
     * could be continued and are valid as far as they go.
     */
    private boolean checkJump(Move mov, boolean allowPartial) {
        if (mov == null) {
            return true;
        }
        int from = index(mov.col0(), mov.row0());
        int to = index(mov.col1(), mov.row1());
        PieceColor jumped = jumped(from, to);

        if (jumpTo.get(from).contains(to)
                && jumped.equals(whoseMove().opposite())
                && get(to).equals(EMPTY)) {
            if (!allowPartial) {
                return true;
            } else {
                Board copy = new Board(this);
                return copy.checkJumpRecursive(mov.jumpTail());
            }
        }
        return false;
    }

    /**
     * Given that MOV is a multi-step jump, return true iff
     * MOV is a valid jump sequence on the current board.
     * This function is only used on a copy of the board, so
     * no Undo necessary.
     */
    private boolean checkJumpRecursive(Move mov) {
        if (mov == null) {
            return true;
        }
        int from = index(mov.col0(), mov.row0());
        int to = index(mov.col1(), mov.row1());
        PieceColor jumped = jumped(from, to);

        return jumpTo.get(from).contains(to)
                && jumped.equals(whoseMove().opposite())
                && get(to).equals(EMPTY)
                && checkJumpRecursive(mov.jumpTail());
    }

    /**
     * Return a list of all legal [moves & jumps] from the current position.
     */
    ArrayList<Move> getMoves() {
        ArrayList<Move> result = new ArrayList<>();
        getMoves(result);
        return result;
    }

    /**
     * Add all legal [moves & jumps] from the current position to MOVES.
     */
    void getMoves(ArrayList<Move> moves) {
        if (gameOver()) {
            return;
        }
        if (jumpPossible()) {
            moves.addAll(getNonMoves());
        } else {
            moves.addAll(getNonJumps());
        }
    }

    /**
     * Return an ArrayList of all legal moves [not jumps]
     * from the current position.
     */
    ArrayList<Move> getNonJumps() {
        ArrayList<Move> moves = new ArrayList<>();
        for (int k = 0; k < SIDE * SIDE; k += 1) {
            if (get(k).equals(whoseMove())) {
                getMoves(moves, k);
            }
        }
        return moves;
    }

    /**
     * Return an ArrayList of all legal jumps [not moves]
     * from the current position.
     */
    ArrayList<Move> getNonMoves() {
        ArrayList<Move> moves = new ArrayList<>();
        for (int k = 0; k < SIDE * SIDE; k += 1) {
            if (get(k).equals(whoseMove())) {
                getJumps(moves, k);
            }
        }
        return moves;
    }

    /**
     * Add all legal non-capturing moves from the position
     * with linearized index K to MOVES.
     */
    private void getMoves(ArrayList<Move> moves, int k) {
        ArrayList<Integer> to = null;
        if (get(k).equals(WHITE)) {
            to = whiteMoveTo.get(k);
        } else if (get(k).equals(BLACK)) {
            to = blackMoveTo.get(k);
        } else {
            return;
        }
        for (int i : to) {
            if (!blocks(k).contains(i)) {
                if (whoseMove().equals(WHITE) && (k / 5 != 4)) {
                    if (get(i).equals(EMPTY) && (i / 5 >= k / 5)) {
                        moves.add(Move.move(col(k), row(k), col(i), row(i)));
                    }
                } else if (whoseMove().equals(BLACK) && (k / 5 != 0)) {
                    if (get(i) == EMPTY && (i / 5 <= k / 5)) {
                        moves.add(Move.move(col(k), row(k), col(i), row(i)));
                    }
                }
            }
        }
    }

    /** Put into MOVES all possible jump sequences
     *  from linearized index FROM. */
    private void getJumps(ArrayList<Move> moves, int from) {
        for (int to : jumpTo.get(from)) {

            if (get(to).equals(EMPTY)
                    && jumped(from, to).equals(whoseMove().opposite())) {

                Move partial = Move.move(col(from), row(from),
                        col(to), row(to));
                partialJump(Move.move(col(from), row(from),
                        col(to), row(to)));

                ArrayList<Move> tails = new ArrayList<>();
                getJumps(tails, to);

                undoPartialJump(partial);

                if (tails.isEmpty()) {
                    moves.add(partial);
                }

                for (Move tail : tails) {
                    Move concatenation = Move.move(partial, tail);
                    moves.add(concatenation);
                }

            }
        }
    }

    /** Assuming MOV is a legal part of a jump,
     * perform this part of the jump. */
    private void partialJump(Move mov) {
        set(index(mov.col0(), mov.row0()), EMPTY);
        set(jumpedIndex(mov.col0(), mov.row0(),
                mov.col1(), mov.row1()), EMPTY);
        set(index(mov.col1(), mov.row1()), whoseMove());
    }

    /** Assuming MOV is a legal part of a jump, undo this part of the jump. */
    private void undoPartialJump(Move mov) {
        set(index(mov.col0(), mov.row0()), whoseMove());
        set(jumpedIndex(mov.col0(), mov.row0(), mov.col1(),
                mov.row1()), whoseMove().opposite());
        set(index(mov.col1(), mov.row1()), EMPTY);
    }

    /**
     * Return the color of the piece in the middle of the
     * jump from linearized index K to linearized index I.
     */
    private PieceColor jumped(int k, int i) {
        return get(jumpedIndex(k, i));
    }

    /**
     * Return the color of the piece in the middle of the
     * jump from node (C0, R0) to node (C1, R1).
     */
    private PieceColor jumped(char c0, char r0, char c1, char r1) {
        return jumped(index(c0, r0), index(c1, r1));
    }

    /**
     * Return the linearized index of the piece that was jumped,
     * given fromIndex K and toIndex I.
     */
    private int jumpedIndex(int k, int i) {
        int difference = Math.abs(i - k);
        return Math.min(i, k) + difference / 2;
    }
    /**
     * Return the linearized index of the piece that was jumped,
     * given from tile (C0, R0), to tile (C1, R1).
     */
    private int jumpedIndex(char c0, char r0, char c1, char r1) {
        int k = index(c0, r0);
        int i = index(c1, r1);
        int difference = Math.abs(i - k);
        return Math.min(i, k) + difference / 2;
    }

    /**
     * Return true iff a jump is possible for a piece at position C R.
     */
    boolean jumpPossible(char c, char r) {
        return jumpPossible(index(c, r));
    }

    /**
     * Return true iff a jump is possible for a piece at position with
     * linearized index K.
     */
    boolean jumpPossible(int k) {
        for (int i : jumpTo.get(k)) {
            if (get(i).equals(EMPTY)
                    && jumped(k, i).equals(whoseMove().opposite())) {
                return true;

            }
        }
        return false;
    }

    /**
     * Return true iff a jump is possible from the current board.
     */
    boolean jumpPossible() {
        for (int k = 0; k <= MAX_INDEX; k += 1) {
            if (get(k).equals(whoseMove()) && jumpPossible(k)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     * other than pass, assumes that legalMove(C0, R0, C1, R1).
     */
    void makeMove(char c0, char r0, char c1, char r1) {
        makeMove(Move.move(c0, r0, c1, r1, null));
    }

    /**
     * Make the multi-jump C0 R0-C1 R1..., where NEXT is C1R1....
     * Assumes the result is legal.
     */
    void makeMove(char c0, char r0, char c1, char r1, Move next) {
        makeMove(Move.move(c0, r0, c1, r1, next));
    }

    /**
     * Return true iff MOV is legal on the current board.
     */
    boolean legalMove(Move mov) {
        int from = index(mov.col0(), mov.row0());
        int to = index(mov.col1(), mov.row1());
        if (jumpPossible() && !mov.isJump()) {
            return false;
        }
        if ((mov.isLeftMove() || mov.isRightMove())
                && blocks(from).contains(to)) {
            return false;
        }
        if (get(to) == EMPTY && get(from) == whoseMove()) {
            if (!mov.isJump()) {
                if (whoseMove().equals(WHITE)) {
                    return whiteMoveTo.get(from).contains(to);
                } else if (whoseMove().equals(BLACK)) {
                    return blackMoveTo.get(from).contains(to);
                }
            } else {
                return checkJump(mov, mov.jumpTail() != null);
            }
        }
        return false;
    }

    /** Return true iff MOV is legal during the setup phase. */
    boolean legalSetupMove(Move mov) {
        int from = index(mov.col0(), mov.row0());
        int to = index(mov.col1(), mov.row1());
        if (get(to) == EMPTY && get(from) == whoseMove()) {
            if (!mov.isJump()) {
                if (whoseMove().equals(WHITE)) {
                    return whiteMoveTo.get(from).contains(to);
                } else if (whoseMove().equals(BLACK)) {
                    return blackMoveTo.get(from).contains(to);
                }
            } else {
                return checkJump(mov, mov.jumpTail() != null);
            }
        }
        return false;
    }

    /**
     * Make the Move MOV on this Board, assuming it is legal.
     */
    void makeMove(Move mov) {
        int from = index(mov.col0(), mov.row0());
        int to = index(mov.col1(), mov.row1());
        set(from, EMPTY);
        set(to, whoseMove());
        if (mov.isLeftMove() || mov.isRightMove()) {
            horizontalBlocks.add(new Integer[]{to, from});
        }
        if (mov.isJump()) {
            clearHorizontalBlocks(from);
            set(jumpedIndex(from, to), EMPTY);
        } else {
            updateBlocks(from, to);
        }
        if (mov.jumpTail() != null) {
            makeJumpTail(mov.jumpTail());
        }
        _whoseMove = whoseMove().opposite();
        setChanged();
        notifyObservers();
    }

    /** Do the intermediate move MOV on this board, for then AI. */
    void makeMoveAI(Move mov) {
        if (mov == null) {
            return;
        }
        int from = index(mov.col0(), mov.row0());
        int to = index(mov.col1(), mov.row1());
        set(from, EMPTY);
        set(to, whoseMove());
        if (mov.isLeftMove() || mov.isRightMove()) {
            horizontalBlocks.add(new Integer[]{to, from});
        }
        if (!mov.isJump()) {
            updateBlocks(from, to);
        } else {
            set(jumpedIndex(mov.col0(), mov.row0(), mov.col1(),
                    mov.row1()), whoseMove().opposite());
            makeMoveAI(mov.jumpTail());
        }
    }

    /** Undo the move MOV on this board. */
    void undoMoveAI(Move mov) {
        if (mov == null) {
            return;
        }
        int from = index(mov.col0(), mov.row0());
        int to = index(mov.col1(), mov.row1());
        set(from, whoseMove().opposite());
        set(to, EMPTY);
        if (mov.isJump()) {
            set(jumpedIndex(mov.col0(), mov.row0(), mov.col1(),
                    mov.row1()), whoseMove());
            undoMoveAI(mov.jumpTail());
        }
    }

    /** Store which nodes cannot move horizontally to a certain position. */
    private ArrayList<Integer[]> horizontalBlocks = new ArrayList<>();

    /** Return the linearized indeces of the nodes
     *  for which node K cannot move to. */
    private ArrayList<Integer> blocks(int k) {
        ArrayList<Integer> result = new ArrayList<>();
        for (Integer[] A : horizontalBlocks) {
            if (A[0] == k) {
                result.add(A[1]);
            }
        }
        return result;
    }

    /** When move from index OLD to CURR, update its blocks to account for
     *  its new position. */
    private void updateBlocks(int old, int curr) {
        for (Integer[] A : horizontalBlocks) {
            if (A[0] == old) {
                A[0] = curr;
            }
        }
    }

    /** Return a copy of the horizontal blocks on this board. */
    ArrayList<Integer[]> copyBlocks() {
        ArrayList<Integer[]> copy = new ArrayList<>();
        for (Integer[] B : horizontalBlocks) {
            copy.add(B.clone());
        }
        return copy;
    }

    /** Set the horizontal blocks of this board to BLOCKS. */
    void setBlocks(ArrayList<Integer[]> blocks) {
        horizontalBlocks = blocks;
    }

    /** After node K has jumped, delete all of its blocked nodes. */
    private void clearHorizontalBlocks(int k) {
        ArrayList<Integer[]> toRemove = new ArrayList<>();
        for (Integer[] A : horizontalBlocks) {
            if (A[0] == k) {
                toRemove.add(A);
            }
        }
        horizontalBlocks.removeAll(toRemove);
    }

    /**
     * Perform the moves in a JumpTail MOV.
     */
    private void makeJumpTail(Move mov) {
        set(mov.col1(), mov.row1(), whoseMove());
        set(mov.col0(), mov.row0(), EMPTY);
        set(jumpedIndex(mov.col0(), mov.row0(), mov.col1(), mov.row1()), EMPTY);
        if (mov.jumpTail() != null) {
            makeJumpTail(mov.jumpTail());
        }
    }

    /**
     * Return true iff the current player can't move.
     */
    private boolean cantMove() {
        return getMoves().isEmpty() && !jumpPossible();
    }

    /**
     * Return true iff the current player has no pieces left on the board.
     */
    private boolean killed() {
        for (int k = 0; k < SIDE * SIDE; k += 1) {
            if (get(k).equals(whoseMove())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the game has ended by first
     * checking if all of a color has been killed,
     * and then if a color can't move,
     * and if so, set the winner. Return true iff
     * game has ended.
     */
    boolean checkEndGame() {
        if (killed() || cantMove()) {
            _winner = whoseMove().opposite();
            _gameOver = true;
            return true;
        }
        return false;
    }

    /** Return the static score of this board,
     *  i.e. the #Whites - #Blacks on this board. */
    int staticScore() {
        int numWhite = 0;
        int numBlack = 0;
        for (int i = 0; i < SIDE * SIDE; i += 1) {
            if (get(i).equals(WHITE)) {
                numWhite += 1;
            } else if (get(i).equals(BLACK)) {
                numBlack += 1;
            }
        }
        return numWhite - numBlack;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /**
     * Return a text depiction of the board.  If LEGEND, supply row and
     * column numbers around the edges.
     */
    String toString(boolean legend) {
        StringBuilder out = new StringBuilder(SIDE * SIDE);
        if (legend) {
            out = new StringBuilder(SIDE * SIDE + legendSize);
            out.append('x');
            out.append('a');
            out.append('b');
            out.append('c');
            out.append('d');
            out.append('e');
            for (int r = aChar; r <= eChar; r += 1) {
                out.append((char) r);
                for (int c = 0; c < SIDE; c += 1) {
                    out.append(' ');
                    PieceColor p = get((r - aChar) * 5 + c);
                    out.append(colorChar.get(p));
                }
                if (r > aChar) {
                    out.append("\n");
                }
            }
        } else {
            for (int r = eChar; r >= aChar; r -= 1) {
                out.append(' ');
                for (int c = 0; c < SIDE; c += 1) {
                    out.append(' ');
                    PieceColor p = get((r - aChar) * 5 + c);
                    out.append(colorChar.get(p));
                }
                if (r > aChar) {
                    out.append("\n");
                }
            }
        }
        return out.toString();
    }

    /**
     * Return the color of the player who has the next move.  The
     * value is arbitrary if gameOver().
     */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /** Set whoseMove to be color C. */
    void setWhoseMove(PieceColor C) {
        _whoseMove = C;
    }

    /**
     * Return true iff the game is over.
     */
    boolean gameOver() {
        return _gameOver;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Board) {
            Board b = (Board) o;
            return (
                    _whoseMove.toString().equals(b.whoseMove().toString())
                    && _gameOver == b.gameOver()
                            && b.toString().equals(toString()));
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int k = 0; k < SIDE * SIDE; k += 1) {
            if (get(k).equals(WHITE)) {
                h += (k + 1) * 3;
            } else if (get(k).equals(BLACK)) {
                h += (k + 1) * 7;
            }
        }
        return h;
    }

    /**
     * Set the variable GAMEOVER and LASTMOVES for testing purposes.
     */
    void setVariables(Boolean gameOver) {
        _gameOver = gameOver;
    }

    /**
     * One cannot create arrays of ArrayList<Move>, so we introduce
     * a specialized private list type for this purpose.
     */
    private static class MoveList extends ArrayList<Move> {
    }

    /**
     * A read-only view of a Board.
     */
    private class ConstantBoard extends Board implements Observer {
        /**
         * A constant view of this Board.
         */
        ConstantBoard() {
            super(Board.this);
            Board.this.addObserver(this);
        }

        @Override
        void copy(Board b) {
            assert false;
        }

        @Override
        void clear() {
            assert false;
        }

        @Override
        void makeMove(Move move) {
            assert false;
        }

        @Override
        public void update(Observable obs, Object arg) {
            super.copy((Board) obs);
            setChanged();
            notifyObservers(arg);
        }
    }

    /**
     * All nodes that WHITE can move to from linearized index k.
     */
    private final Map<Integer, ArrayList<Integer>> whiteMoveTo =
            new HashMap<Integer, ArrayList<Integer>>() { {
                put(0, new ArrayList<Integer>() { {
                        add(1);
                        add(5);
                        add(6);
                    } });
                put(1, new ArrayList<Integer>() { {
                        add(0);
                        add(2);
                        add(6);
                    } });
                put(2, new ArrayList<Integer>() { {
                        add(1);
                        add(3);
                        add(6);
                        add(7);
                        add(8);
                    } });
                put(3, new ArrayList<Integer>() { {
                        add(2);
                        add(4);
                        add(8);
                    } });
                put(4, new ArrayList<Integer>() { {
                        add(3);
                        add(8);
                        add(9);
                    } });
                put(5, new ArrayList<Integer>() { {
                        add(6);
                        add(10);
                    } });
                put(6, new ArrayList<Integer>() { {
                        add(5);
                        add(7);
                        add(10);
                        add(11);
                        add(12);
                    } });
                put(7, new ArrayList<Integer>() { {
                        add(6);
                        add(8);
                        add(12);
                    } });
                put(8, new ArrayList<Integer>() { {
                        add(7);
                        add(9);
                        add(12);
                        add(13);
                        add(14);
                    } });
                put(9, new ArrayList<Integer>() { {
                        add(8);
                        add(14);
                    } });
                put(10, new ArrayList<Integer>() { {
                        add(6);
                        add(11);
                        add(15);
                        add(16);
                    } });
                put(11, new ArrayList<Integer>() { {
                        add(10);
                        add(12);
                        add(16);
                    } });
                put(12, new ArrayList<Integer>() { {
                        add(11);
                        add(13);
                        add(16);
                        add(17);
                        add(18);
                    } });
                put(13, new ArrayList<Integer>() { {
                        add(12);
                        add(14);
                        add(18);
                    } });
                put(14, new ArrayList<Integer>() { {
                        add(13);
                        add(18);
                        add(19);
                    } });
                put(15, new ArrayList<Integer>() { {
                        add(16);
                        add(20);
                    } });
                put(16, new ArrayList<Integer>() { {
                        add(15);
                        add(17);
                        add(20);
                        add(21);
                        add(22);
                    } });
                put(17, new ArrayList<Integer>() { {
                        add(16);
                        add(18);
                        add(22);
                    } });
                put(18, new ArrayList<Integer>() { {
                        add(17);
                        add(19);
                        add(22);
                        add(23);
                        add(24);
                        } });
                put(19, new ArrayList<Integer>() { {
                        add(18);
                        add(24);
                    } });
                put(20, new ArrayList<Integer>());
                put(21, new ArrayList<Integer>());
                put(22, new ArrayList<Integer>());
                put(23, new ArrayList<Integer>());
                put(24, new ArrayList<Integer>());
            } };
    /**
     * All nodes that BLACK can move to from linearized index k.
     */
    private final Map<Integer, ArrayList<Integer>> blackMoveTo =
            new HashMap<Integer, ArrayList<Integer>>() { {
                put(0, new ArrayList<Integer>());
                put(1, new ArrayList<Integer>());
                put(2, new ArrayList<Integer>());
                put(3, new ArrayList<Integer>());
                put(4, new ArrayList<Integer>());
                put(5, new ArrayList<Integer>() { {
                        add(0);
                        add(6);
                    } });
                put(6, new ArrayList<Integer>() { {
                        add(0);
                        add(1);
                        add(2);
                        add(5);
                        add(7);
                    } });
                put(7, new ArrayList<Integer>() { {
                        add(2);
                        add(6);
                        add(8);
                    } });
                put(8, new ArrayList<Integer>() { {
                        add(2);
                        add(3);
                        add(4);
                        add(7);
                        add(9);
                    } });
                put(9, new ArrayList<Integer>() { {
                        add(4);
                        add(8);
                    } });
                put(10, new ArrayList<Integer>() { {
                        add(5);
                        add(6);
                        add(11);
                    } });
                put(11, new ArrayList<Integer>() { {
                        add(6);
                        add(10);
                        add(12);
                    } });
                put(12, new ArrayList<Integer>() { {
                        add(6);
                        add(7);
                        add(8);
                        add(11);
                        add(13);
                    } });
                put(13, new ArrayList<Integer>() { {
                        add(8);
                        add(12);
                        add(14);
                    } });
                put(14, new ArrayList<Integer>() { {
                        add(8);
                        add(9);
                        add(13);
                    } });
                put(15, new ArrayList<Integer>() { {
                        add(10);
                        add(16);
                    } });
                put(16, new ArrayList<Integer>() { {
                        add(10);
                        add(11);
                        add(12);
                        add(15);
                        add(17);
                    } });
                put(17, new ArrayList<Integer>() { {
                        add(12);
                        add(16);
                        add(18);
                    } });
                put(18, new ArrayList<Integer>() { {
                        add(12);
                        add(13);
                        add(14);
                        add(17);
                        add(19);
                    } });
                put(19, new ArrayList<Integer>() { {
                        add(14);
                        add(18);
                    } });
                put(20, new ArrayList<Integer>() { {
                        add(15);
                        add(16);
                        add(21);
                    } });
                put(21, new ArrayList<Integer>() { {
                        add(16);
                        add(20);
                        add(22);
                    } });
                put(22, new ArrayList<Integer>() { {
                        add(16);
                        add(17);
                        add(18);
                        add(21);
                        add(23);
                    } });
                put(23, new ArrayList<Integer>() { {
                        add(18);
                        add(22);
                        add(24);
                    } });
                put(24, new ArrayList<Integer>() { {
                        add(18);
                        add(19);
                        add(23);
                    } });
            } };
    /**
     * All nodes that ANY COLOR can jump to from linearized index k.
     */
    private final Map<Integer, ArrayList<Integer>> jumpTo =
            new HashMap<Integer, ArrayList<Integer>>() { {
                put(0, new ArrayList<Integer>() { {
                        add(2);
                        add(10);
                        add(12);
                    } });
                put(1, new ArrayList<Integer>() { {
                        add(3);
                        add(11);
                    } });
                put(2, new ArrayList<Integer>() { {
                        add(0);
                        add(4);
                        add(10);
                        add(12);
                        add(14);
                    } });
                put(3, new ArrayList<Integer>() { {
                        add(1);
                        add(13);
                    } });
                put(4, new ArrayList<Integer>() { {
                        add(2);
                        add(12);
                        add(14);
                    } });
                put(5, new ArrayList<Integer>() { {
                        add(7);
                        add(15);
                    } });
                put(6, new ArrayList<Integer>() { {
                        add(8);
                        add(16);
                        add(18);
                    } });
                put(7, new ArrayList<Integer>() { {
                        add(5);
                        add(9);
                        add(17);
                    } });
                put(8, new ArrayList<Integer>() { {
                        add(6);
                        add(16);
                        add(18);
                    } });
                put(9, new ArrayList<Integer>() { {
                        add(7);
                        add(19);
                    } });
                put(10, new ArrayList<Integer>() { {
                        add(0);
                        add(2);
                        add(12);
                        add(20);
                        add(22);
                    } });
                put(11, new ArrayList<Integer>() { {
                        add(1);
                        add(13);
                        add(21);
                    } });
                put(12, new ArrayList<Integer>() { {
                        add(0);
                        add(2);
                        add(4);
                        add(10);
                        add(14);
                        add(20);
                        add(22);
                        add(24);
                    } });
                put(13, new ArrayList<Integer>() { {
                        add(3);
                        add(11);
                        add(23);
                    } });
                put(14, new ArrayList<Integer>() { {
                        add(2);
                        add(4);
                        add(12);
                        add(22);
                        add(24);
                    } });
                put(15, new ArrayList<Integer>() { {
                        add(5);
                        add(17);
                    } });
                put(16, new ArrayList<Integer>() { {
                        add(6);
                        add(8);
                        add(18);
                    } });
                put(17, new ArrayList<Integer>() { {
                        add(7);
                        add(15);
                        add(19);
                    } });
                put(18, new ArrayList<Integer>() { {
                        add(6);
                        add(8);
                        add(16);
                    } });
                put(19, new ArrayList<Integer>() { {
                        add(9);
                        add(17);
                    } });
                put(20, new ArrayList<Integer>() { {
                        add(10);
                        add(12);
                        add(22);
                    } });
                put(21, new ArrayList<Integer>() { {
                        add(11);
                        add(23);
                    } });
                put(22, new ArrayList<Integer>() { {
                        add(10);
                        add(12);
                        add(14);
                        add(20);
                        add(24);
                    } });
                put(23, new ArrayList<Integer>() { {
                        add(13);
                        add(21);
                    } });
                put(24, new ArrayList<Integer>() { {
                        add(12);
                        add(14);
                        add(22);
                    } });
            } };
}
