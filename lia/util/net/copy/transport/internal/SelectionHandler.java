package lia.util.net.copy.transport.internal;



public interface SelectionHandler {
    
    public void handleSelection(FDTSelectionKey fdtSelectionKey);
    public void canceled(FDTSelectionKey fdtSelectionKey);
    
}
