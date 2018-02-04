package qirkat;

/**
 * A Player that receives its moves from its Game's getMoveCmnd method.
 *
 * @author Ryan Brill
 */
class Manual extends Player {

    /**
     * Identifies the player serving as a source of input commands.
     */
    private String _prompt;

    /**
     * A Player that will play MYCOLOR on GAME, taking its moves from
     * GAME.
     */
    Manual(Game game, PieceColor myColor) {
        super(game, myColor);
        _prompt = myColor + ": ";
    }

    @Override
    Move myMove() {
        Command inputtedMove = game().getMoveCmnd(_prompt);
        if (inputtedMove == null) {
            return null;
        }
        return Move.parseMove(inputtedMove.operands()[0]);
    }
}

