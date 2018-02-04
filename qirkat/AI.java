package qirkat;

import java.util.ArrayList;

import static qirkat.PieceColor.BLACK;
import static qirkat.PieceColor.WHITE;

/**
 * A Player that computes its own moves.
 *
 * @author Ryan Brill
 */
class AI extends Player {

    /**
     * Maximum minimax search depth before going to static evaluation.
     */
    private static final int MAX_DEPTH = 5;
    /**
     * A position magnitude indicating a win (for white if positive, black
     * if negative).
     */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /**
     * A magnitude greater than a normal value.
     */
    private static final int INFTY = Integer.MAX_VALUE;
    /**
     * The move found by the last call to one of the ...FindMove methods
     * below.
     */
    private Move _lastFoundMove;

    /**
     * A new AI for GAME that will play MYCOLOR.
     */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        Main.startTiming();
        Move move = findMove();
        Main.endTiming();
        return move;
    }

    /**
     * Return a move for me from the current position, assuming there
     * is a move.
     */
    private Move findMove() {
        Board b = new Board(board());
        if (myColor() == WHITE) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /**
     * Find a move from position BOARD and return its value, recording
     * the move found in _lastFoundMove iff SAVEMOVE. The move
     * should have maximal value or have value > BETA if SENSE==1,
     * and minimal value or value < ALPHA if SENSE==-1. Searches up to
     * DEPTH levels.  Searching at level 0 simply returns a static estimate
     * of the board value and does not set _lastMoveFound.
     * PseudoCode Credit: Wikipedia Alpha-Beta Pruning Article.
     */
    private int findMove(Board board, int depth, boolean saveMove,
                         int sense, int alpha, int beta) {
        Move best = null;
        int bestScore = Integer.MIN_VALUE;

        if (depth == 0) {
            return staticScore(board);
        }
        if (sense == 1) {
            board.setWhoseMove(WHITE);
            int S = -INFTY;
            for (Move M : board.getMoves()) {
                ArrayList<Integer[]> oldBlocks = board.copyBlocks();
                board.makeMoveAI(M);
                int S1 = findMove(board, depth - 1, false, -1, alpha, beta);
                if (S1 > S)  {
                    S = S1;
                    best = M;
                }
                alpha = Math.max(alpha, S);
                board.undoMoveAI(M);
                board.setBlocks(oldBlocks);
                if (beta <= alpha) {
                    break;
                }
            }
            bestScore = S;
        } else {
            board.setWhoseMove(BLACK);
            int S = INFTY;
            for (Move M : board.getMoves()) {
                ArrayList<Integer[]> oldBlocks = board.copyBlocks();
                board.makeMoveAI(M);
                int S1 = findMove(board, depth - 1, false, -1, alpha, beta);
                if (S1 < S)  {
                    S = S1;
                    best = M;
                }
                beta = Math.min(beta, S);
                board.undoMoveAI(M);
                board.setBlocks(oldBlocks);
                if (beta <= alpha) {
                    break;
                }
            }
            bestScore = S;
        }
        if (saveMove) {
            _lastFoundMove = best;
        }
        return bestScore;
    }

    /**
     * Return a heuristic value for BOARD.
     * The value will be #Whites - #Blacks on BOARD.
     */
    private int staticScore(Board board) {
        return board.staticScore();
    }

}
