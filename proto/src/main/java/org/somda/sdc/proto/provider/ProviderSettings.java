package org.somda.sdc.proto.provider;

import java.net.InetSocketAddress;

public class ProviderSettings {

    private final InetSocketAddress networkAddress;
    private final String providerName;

    public ProviderSettings(ProviderSettingsBuilder builder) {
        this.networkAddress = builder.networkAddress;
        this.providerName = builder.providerName;
    }

    public InetSocketAddress getNetworkAddress() {
        return networkAddress;
    }

    public String getProviderName() {
        return providerName;
    }

    public static ProviderSettingsBuilder builder() {
        return new ProviderSettingsBuilder();
    }

    public static class ProviderSettingsBuilder {

        private InetSocketAddress networkAddress;
        private String providerName;

        ProviderSettingsBuilder() {
            this.networkAddress = null;
        }

        public ProviderSettingsBuilder setNetworkAddress(InetSocketAddress address) {
            this.networkAddress = address;
            return this;
        }

        public ProviderSettingsBuilder setProviderName(String name) {
            this.providerName = name;
            return this;
        }

        public ProviderSettings build() {
            return new ProviderSettings(this);
        }

    }
}
