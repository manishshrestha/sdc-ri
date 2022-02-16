package org.somda.sdc.glue.consumer.localization.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.glue.consumer.localization.LocalizationServiceProxy;

import javax.annotation.Nullable;

public interface LocalizationServiceProxyFactory {
    LocalizationServiceProxy createLocalizationServiceProxy(
            @Assisted HostingServiceProxy hostingServiceProxy,
            @Assisted("localizationServiceProxy") @Nullable HostedServiceProxy localizationServiceProxy);
}
