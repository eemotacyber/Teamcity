package com.cyberark.common;
import com.intellij.openapi.diagnostic.Logger;

public class LogUtil{
    private verbose;
    public LogUtil(boolean verbose){
        this.verbose = verbose;
    }
    // class name comes from class.getName()
    
    public void write(String className,String message){
        Logger.getInstance(className).info(message);
    }
    public void writeVerbose(String className,String message){
        if (this.verbose){
            Logger.getInstance(className).debug(message);
        }
    }
    public void writeError(String className,String message){
        Logger.getInstance(className).error(message);
    }
    public void writeWarn(String className,String message){
        Logger.getInstance(className).warn(message);
    }
}