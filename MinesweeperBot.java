package minesweeperbot;

import minesweeperbot.GameHandlers.Windows7GameHandler;

public class MinesweeperBot {

    public static void main(String[] args) {
        if( args.length == 4 ){
            bot( Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        } else {
//            bot( 9, 9, 10, 10 );
//            bot( 16, 16, 40, 1 );
            bot( 30, 16, 99, 1 );
        }
    }
    
    public static void bot( int width, int height, int mines, int numberOfGames ){
        int won = 0;
        for( int i=0 ; i<numberOfGames ; i++ ){
            BoardController boardController = new BoardController(new Windows7GameHandler(width, height), width, height, mines);
            if( boardController.solve() )
                won++;
            
            if( i != numberOfGames-1 )
                boardController.gameHandler.restart();
            
            System.out.println(width + "x" + height + ": " + won + "/" + (i+1) + " games won");
        }
        System.out.println("Results: " + width + "x" + height + ": " + won + "/" + numberOfGames + " games won");
    }
}
