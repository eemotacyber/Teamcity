package com.cyberark;

public class ConjurJspKey {
    public ConjurJspKey() {
        this.namespace = "whynot";
    }

    public String namespace = "namespace";
    public String authmethod = "ho";

    public String getAuthmethod(){
        return this.authmethod;
    }

    public String getNamespace(){
        return this.namespace;
    }
}