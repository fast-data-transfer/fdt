package lia.util.net.common;



public interface FDTCloseable {

    public boolean close(String downMessage, Throwable downCause);
    public boolean isClosed();
    
}
