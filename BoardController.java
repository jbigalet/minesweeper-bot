package minesweeperbot;

import java.awt.List;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.text.Position;
import minesweeperbot.GameHandlers.GameHandler;

public class BoardController {
    
    public GameHandler gameHandler;
    private int width;
    private int height;
    private int mineNumber;
    
    public int[][] board;

    public BoardController(GameHandler gameHandler, int width, int height, int mineNumber) {
        this.gameHandler = gameHandler;
        this.gameHandler.setBoardController(this);
        this.width = width;
        this.height = height;
        this.mineNumber = mineNumber;
        
        this.board = new int[width+2][height+2];
        for(int i=0 ; i<width+2 ; i++ )
            for(int j=0 ; j<height+2 ; j++ )
                this.board[i][j] = (i==0 || j==0 || i==width+1 || j==height+1) ? -3 : -1;
    }
    
    
        // with bruteforce implementation
    public boolean solve() {
        gameHandler.openCell( width/2, height/2 );
//        printBoard(board);
        long sTime = System.currentTimeMillis();
        while( gameHandler.gameStatus == 1 /*&& System.currentTimeMillis() - sTime <= 100000 */){
            if( !isBoardValid() ){
                printBoard(board);
                System.out.println("Board not valid");
                System.exit(1);
            }
            
            boolean found = false;
            for( int i=0 ; i<width && !found ; i++ )
                for( int j=0 ; j<height && !found ; j++ )
                    if( board[i+1][j+1] >= 1 ){
                        int[] adj = adjacentLookup(i, j);

                        if( adj[0] == board[i+1][j+1] ){ // open every other cells if mines = number
                            if( openOrCloseCellsAround( i, j, true ) )
                                found = true;
                        } else if( adj[0] + adj[1] == board[i+1][j+1] ){ // mine every other cells if mines + closed = number
                            if( openOrCloseCellsAround( i, j, false ) )
                                found = true;
                        }
                }

            if( !found )
                bruteforceSolve();
        }
        
        return gameHandler.gameStatus == 0;
    }
    

    public void bruteforceSolve() {
        long sTime = System.currentTimeMillis();
        
        ArrayList<Point> initSet = new ArrayList<>();
        for( int i=0 ; i<width ; i++ )
            for( int j=0 ; j<height ; j++ )
                if( board[i+1][j+1] == -1 ){    // it needs to be closed
                    int[] adj = adjacentLookup(i, j);
                    if( adj[2] >= 1 && adj[1] >= 1 )   // if there is at least one number adjacent to it && if there is at least one closed cell
                        initSet.add(new Point(i, j));
            }

        cellsToFlag = new HashMap<>();
        cellsToOpen = new HashMap<>();
        totalGuessed = 0;
        bruteforceRec( initSet, new ArrayList<Point>(), new ArrayList<Point>() );

        Point bestGuessCell = null;
        int bestGuessCount = 0;
        boolean bestGuessToFlag = false;
        for( Map.Entry<Point, Integer> toFlag: cellsToFlag.entrySet() )
            if( toFlag.getValue() > bestGuessCount ){
                bestGuessCount = toFlag.getValue();
                bestGuessCell = toFlag.getKey();
                bestGuessToFlag = true;
            }
        for( Map.Entry<Point, Integer> toOpen: cellsToOpen.entrySet() )
            if( toOpen.getValue() > bestGuessCount ){
                bestGuessCount = toOpen.getValue();
                bestGuessCell = toOpen.getKey();
                bestGuessToFlag = false;
            }
        
        if( bestGuessCell == null ){  // happens when we got no information on the remaining cells.
            System.out.println("Bruteforce failed, guessing randomly");
            
                // debug
            System.out.println(cellsToFlag.toString());
            System.out.println(cellsToOpen.toString());
            System.out.println(initSet.toString());
            System.out.println(isBoardValid());
            printBoard(board);
            
            for( int i=0 ; i<width ; i++ )
                for( int j=0 ; j<width ; j++ )
                    if( board[i+1][j+1] == -1 ){
                        gameHandler.openCell(i, j);
                        gameHandler.forceUpdateGameStatus();
//                        System.exit(1);
                        return;
                    }
        }
        
        int guessProba = (int)((100*bestGuessCount)/totalGuessed);
        long time = System.currentTimeMillis() - sTime;
        
        System.out.print("Backtracking done in " + time + "ms.");

            // if we're sure, we open/close each sure cell
        if( guessProba == 100 ){
            for( Map.Entry<Point, Integer> toFlag: cellsToFlag.entrySet() )
                if( toFlag.getValue() == totalGuessed )
                    gameHandler.closeCell(toFlag.getKey().x, toFlag.getKey().y);
            for( Map.Entry<Point, Integer> toOpen: cellsToOpen.entrySet() )
                if( toOpen.getValue() == totalGuessed )
                    gameHandler.openCell(toOpen.getKey().x, toOpen.getKey().y);
        } else {
            if( bestGuessToFlag )
                gameHandler.closeCell(bestGuessCell.x, bestGuessCell.y);
            else
                gameHandler.openCell(bestGuessCell.x, bestGuessCell.y);
        }
        
        if( guessProba != 100 ){
            System.out.print(" Guessing with a probability of " + guessProba + "%.");
//            System.out.println(cellsToFlag.toString());
//            System.out.println(cellsToOpen.toString());
//            System.out.println(bestGuessToFlag);
            gameHandler.forceUpdateGameStatus();
        }
        
        System.out.println();
    }
    
    Map<Point, Integer> cellsToFlag;    // with the count of the board positions they're in
    Map<Point, Integer> cellsToOpen;
    int totalGuessed;
    private void bruteforceRec( ArrayList<Point> remainingCells, ArrayList<Point> flaggedCells, ArrayList<Point> openedCells ){
//        System.out.println(remainingCells.size());
        if( !isBoardValid() )
            return;
        
        if( remainingCells.isEmpty() ){
            totalGuessed++;
            for( Point c: flaggedCells ){
                Integer f = cellsToFlag.get(c);
                if( f == null )
                    f = 0;
                cellsToFlag.put(c, f+1);
            }
            for( Point c: openedCells ){
                Integer f = cellsToOpen.get(c);
                if( f == null )
                    f = 0;
                cellsToOpen.put(c, f+1);
            }
            return;
        }
            
        ArrayList<Point> newRC = new ArrayList<>(remainingCells);
        Point p = newRC.remove(0);
        
            // try to flag it
        board[p.x+1][p.y+1] = -2;
        ArrayList<Point> newFC = new ArrayList<>(flaggedCells);
        newFC.add(p);
        bruteforceRec( newRC, newFC, openedCells);
        
            // try to open it (we assume its a border)
        board[p.x+1][p.y+1] = -3;
        ArrayList<Point> newOC = new ArrayList<>(openedCells);
        newOC.add(p);
        bruteforceRec( newRC, flaggedCells, newOC);
        
        board[p.x+1][p.y+1] = -1;     // we put it back to closed
    }
    
    
        // direct deduction
    public boolean solve1() {
        gameHandler.openCell( width/2, height/2 );
//        printBoard(board);
        long sTime = System.currentTimeMillis();
        while( gameHandler.gameStatus == 1 && System.currentTimeMillis() - sTime <= 100000 ){
            boolean found = false;
            for( int i=0 ; i<width && !found ; i++ )
                for( int j=0 ; j<height && !found ; j++ )
                    if( board[i+1][j+1] >= 1 ){
                        int[] adj = adjacentLookup(i, j);

                        if( adj[0] == board[i+1][j+1] ){ // open every other cells if mines = number
                            if( openOrCloseCellsAround( i, j, true ) )
                                found = true;
                        } else if( adj[0] + adj[1] == board[i+1][j+1] ){ // mine every other cells if mines + closed = number
                            if( openOrCloseCellsAround( i, j, false ) )
                                found = true;
                        }
                }
            
            if( !found ){
                System.out.println("Impossible to solve, opening the first possible one");
                boolean oneOpenned = false;
                for( int i=0 ; i<width && !oneOpenned ; i++ )
                    for( int j=0 ; j<height && !oneOpenned ; j++ )
                        if( board[i+1][j+1] == -1 ){
                            gameHandler.openCell(i, j);
                            oneOpenned = true;
                        }
                try {
                    Thread.sleep( 200 );
                } catch (InterruptedException ex) {}
                gameHandler.updateGameStatus();
            }
        }
        
        return gameHandler.gameStatus == 0;
    }

        // return if there was any action made
    private boolean openOrCloseCellsAround( int i, int j, boolean open ){
        boolean isAction = false;
        for( int x=-1 ; x<=1 ; x++ )
            for( int y=-1 ; y<=1 ; y++ )
                if( x!=0 || y!=0 )
                    if( board[i+x+1][j+y+1] == -1 ){
                        isAction = true;
                        if( open )
                            gameHandler.openCell(i+x, j+y);
                        else
                            gameHandler.closeCell(i+x, j+y);
                    }
        return isAction;
    }
    
    private boolean isBoardValid() {
//        gameHandler.getFullBoard();
        for( int i=0 ; i<width ; i++ )
            for( int j=0 ; j<height ; j++ ){
                int c = board[i+1][j+1];
                if( c >= 0 ){
                    int[] adj = adjacentLookup(i, j);
                        // if there is more mine than the number, or if there is not enough place to get enough mine
                    if( adj[0] > c || adj[0]+adj[1] < c ){
//                        System.out.println(i + "x" + j + ":" + c + " => " + Arrays.toString(adj));
                        return false;
                    }
                }
            }
        return true;
    }

    
        // return [number of mines, number of closed cell, number of numbered cell]
    private int[] adjacentLookup( int i, int j ){
        int[] res = new int[3];
        for( int x=-1 ; x<=1 ; x++ )
            for( int y=-1 ; y<=1 ; y++ )
                if( x!=0 || y!=0 ){
                    int c = board[i+x+1][j+y+1];
                    if( c == -2 )
                        res[0]++;
                    else if( c == -1 )
                        res[1]++;
                    else if( c >= 0 )
                        res[2]++;
                }
        return res;
    }
   
        // for debug
    private void printBoard( int[][] board ) {
        for( int j=0 ; j<board[0].length ; j++ ){
            for( int i=0 ; i<board.length ; i++ ){
                int c = board[i][j];
                String s = "";
                if( board[i][j] >= 0 )
                    s += board[i][j];
                else if( board[i][j] == -1 )
                    s += "?";
                else if( board[i][j] == -2 )
                    s += "X";
                else if( board[i][j] == -3 )
                    s += "+";
                
                if( i!=0 && j!=0 && i!=board.length-1 && j!=board[0].length-1 && c>=0 ){
                    int[] adj = adjacentLookup(i-1, j-1);
                    if( adj[0] > c || adj[0]+adj[1] < c )
                        s += "P";
                    else
                        s += " ";
                } else {
                    s += " ";
                }
                
                System.out.print(s);
            }
            System.out.println();
        }
    }
}
