package minesweeperbot.GameHandlers;

import com.sun.jna.platform.win32.WinDef.HWND;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import minesweeperbot.BoardController;
import minesweeperbot.Helpers.User32;

public class Windows7GameHandler extends GameHandler {

    private static final int TIMEOUT = 20;
    
    private Color[] cellColors = new Color[] {
        new Color(65, 79, 188),
        new Color(17, 106, 0),
        new Color(166, 7, 4),
        new Color(1, 1, 115),
        new Color(125, 0, 0),
        new Color(6, 124, 128)
    };
    
    private int width;
    private int height;
    private double cellWidth;
    private double cellHeight;
    
    private BufferedImage lastScreenshot;

    private Rectangle windowRect;
    private Robot robot;
    {
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            System.err.println("Impossible to create Robot");
            System.exit(1);
        }
    }

    
    public Windows7GameHandler(int width, int height) {
        HWND hwnd = User32.INSTANCE.FindWindow("Minesweeper", null);
        if( hwnd == null ){
            System.err.println("Game not found");
            System.exit(1);
        }
        
        User32.INSTANCE.SwitchToThisWindow(hwnd, false);
        
        int[] rect = {0,0,0,0};
        User32.INSTANCE.GetWindowRect(hwnd, rect);
        
            // we get the real rectangle by searching the black border of the game
        BufferedImage screenShot = robot.createScreenCapture(new Rectangle(rect[0], rect[1], rect[2]-rect[0], rect[3]-rect[1]));
        
            // we start in the middle left of the screen and go right until we got a black pixel
        int left = 20, top = (rect[3]-rect[1])/2;
        while( !isAlmostBlack( screenShot.getRGB(left, top)) )
            left++;
        
            // then we go up until we don't got a black pixel anymore
        while( isAlmostBlack( screenShot.getRGB(left, top)) )
            top--;

            // same thing for the bottom right
        int right = rect[2]-rect[0]-20, bottom = (rect[3]-rect[1])/2;
        while( !isAlmostBlack( screenShot.getRGB(right, bottom)) )
            right--;
        while( isAlmostBlack( screenShot.getRGB(right, bottom)) )
            bottom++;
        
        windowRect = new Rectangle(rect[0]+left, rect[1]+top, right-left, bottom-top);
        
        this.width = width;
        this.height = height;
        this.cellWidth = windowRect.width / (double)width;
        this.cellHeight = windowRect.height / (double)height;
        
        this.gameStatus = 1;
    }
    
    
    @Override
    public int[][] getFullBoard() {
        BufferedImage screenShot = robot.createScreenCapture(windowRect);
        try {   // for debug purposes
            ImageIO.write( screenShot, "jpg", new File("plop.jpg"));
        } catch (IOException ex) {}
//        
            // still debug
        for( int c=-2; c<cellColors.length+1 ; c++ )
            new File("cells/"+(c+1)).mkdirs();
        
        int[][] board = new int[width+1][height+1];
        for( int i=0 ; i<width ; i++ )
            for( int j=0 ; j<height ; j++ ){
                int found = getCell(screenShot, i, j);
                board[i+1][j+1] = found;
                try {
                    ImageIO.write(screenShot.getSubimage((int)(i*cellWidth), (int)(j*cellHeight), (int)cellWidth, (int)windowRect.height / height), "jpg", new File("cells/" + found + "/cell_" + found + "-" + i + "x" + j + ".jpg"));
                } catch (IOException ex) {}
            }

        return board;
    }
    
    private int getCell( BufferedImage screenShot, int i, int j ){
        int found = -1;

            // is there a white pixel on the bottom right ? if there is, there is a number (or its a blank cell)
        for( int x = (int)((i+0.4)*cellWidth) ; x < (int)((i+0.6)*cellWidth) && found == -1 ; x++ )
            for( int y = (int)((j+0.8)*cellHeight) ; y < (int)((j+1.0)*cellHeight) && found == -1 ; y++ )
                if( colorDiff( new Color(screenShot.getRGB(x, y)), Color.white ) < 60 )
                        found = 0;

            // which number ?
        for( int x = (int)((i+0.2)*cellWidth) ; x < (int)((i+0.8)*cellWidth) && found == 0 ; x++ )
            for( int y = (int)((j+0.2)*cellHeight) ; y < (int)((j+0.8)*cellHeight) && found == 0 ; y++ )
                for( int c=0 ; c<cellColors.length ; c++ )
                    if( colorDiff( new Color(screenShot.getRGB(x, y)), cellColors[c] ) < 20 )
                            found = c+1;

            // check if its a 3 or a 7
            // if its a 3, there is at least 2 vertically aligned white pixels
            // separated by some red pixels & each got some red pixels on their right
            // still a bit drunk, pls dont judge the style
        if( found == 3 ){
            boolean is3 = false;
            for( int x = (int)((i+0.2)*cellWidth) ; x < (int)((i+0.8)*cellWidth) ; x++ )
                for( int y = (int)((j+0.2)*cellHeight) ; y < (int)((j+0.8)*cellHeight) ; y++ )
                    if( colorDiff( new Color(screenShot.getRGB(x, y)), cellColors[2] ) > 100 ){ // not red
                        boolean redPixelOnTheRight = false;
                        for( int xx = x+1 ; xx < (int)((i+0.8)*cellWidth) && !redPixelOnTheRight ; xx++ )
                            if( colorDiff(new Color(screenShot.getRGB(xx, y)), cellColors[2] ) < 100 ) // red
                                redPixelOnTheRight = true;
                        if( redPixelOnTheRight ){
                            int yy = y+1;
                            while( yy < (int)((j+0.8)*cellHeight) 
                                    && colorDiff(new Color(screenShot.getRGB(x, yy)), cellColors[2] ) > 100 ) //not red
                                yy++;
                            if( yy < (int)((j+0.8)*cellHeight) ){
                                while( yy < (int)((j+0.8)*cellHeight) 
                                        && colorDiff(new Color(screenShot.getRGB(x, yy)), cellColors[2] ) < 100 ) //red
                                    yy++;
                                if( yy < (int)((j+0.8)*cellHeight) ){
                                    redPixelOnTheRight = false;
                                    for( int xx = x+1 ; xx < (int)((i+0.8)*cellWidth) && !redPixelOnTheRight ; xx++ )
                                        if( colorDiff(new Color(screenShot.getRGB(xx, yy)), cellColors[2] ) < 100 ) // red
                                            is3 = true;
                                }
                            }
                        }
                    }
            if( !is3 )
                found = 7;
        }
        
        return found;
    }
    
    private double colorDiff( Color a, Color b ){
        int rd = a.getRed() - b.getRed();
        int gd = a.getGreen() - b.getGreen();
        int bd = a.getBlue() - b.getBlue();
        return Math.sqrt( rd*rd + gd*gd + bd*bd );
    }
    
    private boolean isAlmostBlack( int rgb ){
        Color c = new Color(rgb);
        return c.getRed() + c.getGreen() + c.getBlue() < 80;
    }
    
    private void nap() {
        nap(TIMEOUT);
    }
    
    @Override
    public void nap(int timeout) {
        try {
            Thread.sleep( timeout );
        } catch (InterruptedException ex) {
            System.out.println("wtf");
        }
    }

    @Override
    public void openCell( int i, int j ){
        robot.mouseMove(windowRect.x + (int)((i+0.5)*cellWidth), windowRect.y + (int)((j+0.5)*cellHeight));
        nap();
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        nap();
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        nap();
        refresh(i, j);
    }

    @Override
    public void closeCell( int i, int j ){
        boardController.board[i+1][j+1] = -2;
        robot.mouseMove(windowRect.x + (int)((i+0.5)*cellWidth), windowRect.y + (int)((j+0.5)*cellHeight));
        nap();
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        nap();
        robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        nap();
    }
    
    private void refresh(int i, int j) {
        robot.mouseMove(0, 0);
        nap();
        this.lastScreenshot = robot.createScreenCapture(windowRect);
        updateGameStatus();
        expandingRefresh(i, j);
    }
    
    @Override
    public void forceUpdateGameStatus() {
        updateGameStatus();
    }
    
    @Override
    public void updateGameStatus() {
        nap(50);
        if( User32.INSTANCE.FindWindow(null, "Partie perdue") != null ){
//            System.out.println("Game lost =[");
            this.gameStatus = -1;
        } else if( User32.INSTANCE.FindWindow(null, "Partie gagnée") != null ){
//            System.out.println("Game won =]");
            this.gameStatus = 0;
        }
    }
    
    private void expandingRefresh( int i, int j ){
        if( boardController.board[i+1][j+1] == -1 ){
            int found = getCell(this.lastScreenshot, i, j);
            boardController.board[i+1][j+1] = found;
            if( found == 0 )
                for( int x=-1 ; x<=1 ; x++ )
                    for( int y=-1 ; y<=1 ; y++ )
                        if( x!=0 || y!=0)
                            expandingRefresh(i+x, j+y);
        }
    }

    
    @Override
    public void restart() {
        nap(500);
        robot.keyPress(KeyEvent.VK_SPACE);
        robot.keyRelease(KeyEvent.VK_SPACE);
        nap(500);
    }

    @Override
    public void changeMode(int modeDiff) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
