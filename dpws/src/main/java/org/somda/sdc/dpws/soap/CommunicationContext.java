package org.somda.sdc.dpws.soap;

public class CommunicationContext {

    private final ApplicationInfo applicationInfo;
    private final TransportInfo transportInfo;

    public CommunicationContext(ApplicationInfo applicationInfo, TransportInfo transportInfo) {
        this.applicationInfo = applicationInfo;
        this.transportInfo = transportInfo;
    }

    public ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }

    public TransportInfo getTransportInfo() {
        return transportInfo;
    }
}
