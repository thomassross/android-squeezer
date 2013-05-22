package uk.org.ngo.squeezer.framework;

import uk.org.ngo.squeezer.service.SqueezeService;
import android.os.RemoteException;

public interface OrderPages {
    /**
     * Implementations must start an asynchronous fetch of items, when this is called.
     * 
     * @throws RemoteException
     * @param start Position in list to start the fetch. Pass this on to {@link SqueezeService}
     */
    abstract void orderPage(int start) throws RemoteException;

    /**
     * Order page at specified position, if it has not already been ordered.
     * 
     * @param pagePosition
     */
    public void maybeOrderPage(int pagePosition);

    /**
     * Clear all information about which pages has been ordered, and reorder the first page
     */
    public void reorderItems();
}
