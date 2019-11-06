package org.somda.sdc.dpws.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.DpwsFramework;

import javax.annotation.Nullable;
import java.net.NetworkInterface;

/**
 * Factory to create {@code org.somda.sdc.dpws.DpwsFramework} instances.
 */
public interface DpwsFrameworkFactory {
    /**
     * Creates a {@code org.somda.sdc.dpws.DpwsFramework} instance bound to the give network interface.
     *
     * @param networkInterface the network interface to bind to. Binds to the loopback adapter if null.
     * @return the created {@code org.somda.sdc.dpws.DpwsFramework} instance.
     */
    DpwsFramework createDpwsFramework(@Assisted @Nullable NetworkInterface networkInterface);

    /**
     * Creates a {@code org.somda.sdc.dpws.DpwsFramework} instance bound to the loopback adapter.
     *
     * @return the created {@code org.somda.sdc.dpws.DpwsFramework} instance.
     */
    DpwsFramework createDpwsFramework();
}
