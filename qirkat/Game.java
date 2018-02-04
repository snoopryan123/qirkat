package qirkat;

/* Author: P. N. Hilfinger */

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;

import static qirkat.Command.Type.*;
import static qirkat.Game.State.PLAYING;
import static qirkat.Game.State.SETUP;
import static qirkat.GameException.error;
import static qirkat.PieceColor.BLACK;
import static qirkat.PieceColor.WHITE;

/**
 * Controls the play of the game.
 *
 * @author Ryan Brill
 */
class Game {

    /**
     * Mapping of command types to methods that process them.
     */
    private final HashMap<Command.Type, Consumer<String[]>> _commands =
            new HashMap<>();
    /**
     * Input source.
     */
    private final CommandSources _inputs = new CommandSources();
    /**
     * My board and its read-only view.
     */
    private Board _board, _constBoard;
    /**
     * Indicate which players are manual players (as opposed to AIs).
     */
    private boolean _whiteIsManual, _blackIsManual;
    /**
     * Current game state.
     */
    private State _state;
    /**
     * Used to send messages to the user.
     */
    private Reporter _reporter;
    /**
     * Source of pseudo-random numbers (used by AIs).
     */
    private Random _randoms = new Random();

    {
        _commands.put(AUTO, this::doAuto);
        _commands.put(CLEAR, this::doClear);
        _commands.put(DUMP, this::doDump);
        _commands.put(HELP, this::doHelp);
        _commands.put(MANUAL, this::doManual);
        _commands.put(PIECEMOVE, this::doMove);
        _commands.put(SEED, this::doSeed);
        _commands.put(SETBOARD, this::doSet);
        _commands.put(START, this::doStart);
        _commands.put(LOAD, this::doLoad);
        _commands.put(QUIT, this::doQuit);
        _commands.put(ERROR, this::doError);
        _commands.put(EOF, this::doQuit);
    }

    /**
     * A new Game, using BOARD to play on, reading initially from
     * BASESOURCE and using REPORTER for error and informational messages.
     */
    Game(Board board, CommandSource baseSource, Reporter reporter) {
        _inputs.addSource(baseSource);
        _board = board;
        _constBoard = _board.constantView();
        _reporter = reporter;
        _whiteIsManual = true;
        _blackIsManual = false;
    }

    /**
     * Run a session of Qirkat gaming.
     */
    void process() {
        Player white, black;
        white = black = null;
        doClear(null);

        while (true) {
            while (_state == SETUP) {
                doCommand();
            }
            white = (_whiteIsManual)
                    ? new Manual(this, WHITE)
                    : new AI(this, WHITE);
            black = (_blackIsManual)
                    ? new Manual(this, BLACK)
                    : new AI(this, BLACK);

            while (_state != SETUP && !_board.gameOver()) {
                Move move = null;

                if (_state == PLAYING) {

                    move = (_board.whoseMove() == WHITE)
                            ? white.myMove() : black.myMove();

                    if (move == null) {
                        doCommand();
                    } else {
                        doMove(new String[]{move.toString()});
                    }

                    isGameOver();

                }
            }

            if (_state == PLAYING) {
                reportWinner();
            }
            _state = SETUP;
        }
    }

    /* Command Processors */

    /**
     * Return true iff the game is over.
     */
    private boolean isGameOver() {
        return _board.checkEndGame();
    }

    /**
     * Return a read-only view of my game board.
     */
    Board board() {
        return _constBoard;
    }

    /**
     * Perform the next command from our input source.
     */
    void doCommand() {
        try {
            Command cmnd = Command.parseCommand(_inputs.getLine("qirkat: "));
            _commands.get(cmnd.commandType()).accept(cmnd.operands());
        } catch (GameException excp) {
            _reporter.errMsg(excp.getMessage());
        }
    }

    /**
     * Read and execute commands until encountering a move or until
     * the game leaves playing state due to one of the commands. Return
     * the terminating move command, or null if the game first drops out
     * of playing mode. If appropriate to the current input source, use
     * PROMPT to prompt for input.
     */
    Command getMoveCmnd(String prompt) {
        while (_state == PLAYING) {
            try {
                Command cmnd = Command.parseCommand(_inputs.getLine(prompt));
                switch (cmnd.commandType()) {
                case PIECEMOVE:
                    return cmnd;
                default:
                    _commands.get(cmnd.commandType()).accept(cmnd.operands());
                }
            } catch (GameException excp) {
                _reporter.errMsg(excp.getMessage());
            }
        }
        return null;
    }

    /**
     * Return random integer between 0 (inclusive) and MAX>0 (exclusive).
     */
    int nextRandom(int max) {
        return _randoms.nextInt(max);
    }

    /**
     * Report a move, using a message formed from FORMAT and ARGS as
     * for String.format.
     */
    void reportMove(String format, Object... args) {
        _reporter.moveMsg(format, args);
    }

    /**
     * Report an error, using a message formed from FORMAT and ARGS as
     * for String.format.
     */
    void reportError(String format, Object... args) {
        _reporter.errMsg(format, args);
    }

    /**
     * Perform the command 'auto OPERANDS[0]'.
     */
    void doAuto(String[] operands) {
        _state = SETUP;
        if (operands[0].toLowerCase().equals("black")) {
            _blackIsManual = false;
        } else if (operands[0].toLowerCase().equals("white")) {
            _whiteIsManual = false;
        }
    }

    /**
     * Perform a 'help' command.
     */
    void doHelp(String[] unused) {
        InputStream helpIn =
                Game.class.getClassLoader().getResourceAsStream(
                        "qirkat/help.txt");
        if (helpIn == null) {
            System.err.println("No help available.");
        } else {
            try {
                BufferedReader r
                        = new BufferedReader(new InputStreamReader(helpIn));
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    System.out.println(line);
                }
                r.close();
            } catch (IOException e) {
                /* Ignore IOException */
            }
        }
    }

    /**
     * Perform the command 'load OPERANDS[0]'.
     */
    void doLoad(String[] operands) {
        try {
            FileReader reader = new FileReader(operands[0]);
            _inputs.addSource(new ReaderSource(reader, false));
        } catch (IOException e) {
            throw error("Cannot open file %s", operands[0]);
        }
    }

    /**
     * Perform the command 'manual OPERANDS[0]'.
     */
    void doManual(String[] operands) {
        _state = SETUP;
        if (operands[0].toLowerCase().equals("black")) {
            _blackIsManual = true;
        } else if (operands[0].toLowerCase().equals("white")) {
            _whiteIsManual = true;
        }
    }

    /**
     * Exit the program.
     */
    void doQuit(String[] unused) {
        Main.reportTotalTimes();
        System.exit(0);
    }

    /**
     * Perform the command 'start'.
     */
    void doStart(String[] unused) {
        isGameOver();
        _state = PLAYING;
    }

    /**
     * Perform the move OPERANDS[0].
     */
    void doMove(String[] operands) {
        Move move = Move.parseMove(operands[0]);
        if (!_board.legalMove(move)) {
            System.out.println("Illegal move.");
            return;
        }
        _board.makeMove(move);
        if (_state != SETUP) {
            if (_board.whoseMove().equals(BLACK) && !_whiteIsManual) {
                System.out.println("White moves "
                        + operands[0].toLowerCase() + '.');
            } else if (_board.whoseMove().equals(WHITE) && !_blackIsManual) {
                System.out.println("Black moves "
                        + operands[0].toLowerCase() + '.');
            }
        }
    }

    /**
     * Perform the command 'clear'.
     */
    void doClear(String[] unused) {
        Main.reportTotalTimes();
        _state = SETUP;
        _board.clear();
    }

    /**
     * Perform the command 'set OPERANDS[0] OPERANDS[1]'.
     */
    void doSet(String[] operands) {
        _state = SETUP;
        PieceColor whoseMove = PieceColor.fromString(operands[0]);
        _board.setPieces(operands[1], whoseMove);
    }

    /**
     * Perform the command 'dump'.
     */
    void doDump(String[] unused) {
        System.out.println("===");
        System.out.println(_board.toString());
        System.out.println("===");
    }

    /**
     * Execute 'seed OPERANDS[0]' command, where the operand is a string
     * of decimal digits. Silently substitutes another value if
     * too large.
     */
    void doSeed(String[] operands) {
        try {
            _randoms.setSeed(Long.parseLong(operands[0]));
        } catch (NumberFormatException e) {
            _randoms.setSeed(Long.MAX_VALUE);
        }
    }

    /**
     * Execute the artificial 'error' command.
     */
    void doError(String[] unused) {
        throw error("Command not understood");
    }

    /**
     * Report the outcome of the current game.
     */
    void reportWinner() {
        String msg;
        msg = _board.winner().toString() + " wins.";
        _reporter.outcomeMsg(msg);
    }
    /**
     * States of play.
     */
    static enum State {
        SETUP, PLAYING;
    }
}
