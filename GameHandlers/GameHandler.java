package minesweeperbot.GameHandlers;

import minesweeperbot.BoardController;

public abstract class GameHandler {

    protected BoardController boardController;
    public int gameStatus; // -1 lost 0 won 1 running
    
    public void setBoardController(BoardController boardController) {
        this.boardController = boardController;
    }
    
    public abstract void openCell(int i, int j);
    public abstract void closeCell(int i, int j);
    
    public abstract void restart();
    public abstract void changeMode(int modeDiff);
    
    public abstract void updateGameStatus();
    public abstract void forceUpdateGameStatus();
    public abstract void nap(int i);
    
    public abstract int[][] getFullBoard();
    
}