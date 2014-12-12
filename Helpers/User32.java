package minesweeperbot.Helpers;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.platform.win32.WinDef.HWND;

public interface User32 extends StdCallLibrary {
    
    User32 INSTANCE = (User32) Native.loadLibrary("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);
    
    HWND FindWindow(String lpClassName, String lpWindowName);
    void SwitchToThisWindow(HWND hwnd, boolean b);
    int GetWindowRect(HWND hwnd, int[] rect);
}
