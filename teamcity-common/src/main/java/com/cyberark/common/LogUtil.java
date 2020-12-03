package com.cyberark.common;
// import com.intellij.openapi.diagnostic.Logger;

import jetbrains.buildServer.agent.BuildProgressLogger;

public class LogUtil {
    protected  BuildProgressLogger agentLogger;
    protected boolean verbose = false;

    public LogUtil(BuildProgressLogger agentLogger, boolean verbose){
        this.agentLogger = agentLogger;
        this.verbose = verbose;
    }

    public void Verbose(String message) {
        this.Log("VERBOSE", message);
    }

    public void Log(String level, String message) {
        message = String.format("%s: %s", level, message);
        this.agentLogger.message(message);
    }

    private String getClassName(Object object) {
        return object.getClass().getName();
    }



//    public void write(Object object, String message){
//        Logger.getInstance(getClassName(object)).info(message);
//    }
//
//    public void writeVerbose(Object object, String message){
//        if (this.verbose){
//            Logger.getInstance(getClassName(object)).debug(message);
//        }
//    }
//
//    public void writeError(Object object, String message){
//        Logger.getInstance(getClassName(object)).error(message);
//    }
//
//    public void writeWarn(Object object, String message){
//        Logger.getInstance(getClassName(object)).warn(message);
//    }
}
