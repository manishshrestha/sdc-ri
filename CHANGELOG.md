# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- _Releases_ and _How to get started_ sections to the project readme. (#125)
- Feature from BICEPS to send and receive periodic reports. (#51)
- Jetty server supports http and https connections on the same port. (#107)
- Additional utility method in `org.somda.sdc.dpws.soap.SoapUtil` to create new SoapMessage with reference parameters. (#140)
- Jetty server supports http and https connections on the same port. (#107)
- Additional utility method in `org.somda.sdc.dpws.soap.SoapUtil` to create new SoapMessage with reference parameters. (#140)
- Jetty server supports http and https connections on the same port (#107)
- Additional utility method in `org.somda.sdc.dpws.soap.SoapUtil` to create new SoapMessage with reference parameters. (#140)
- Jetty server supports http and https connections on the same port. (#107)
- Additional utility method in `org.somda.sdc.dpws.soap.SoapUtil` to create new SoapMessage with reference parameters. (#140)
- Additional utility methods `org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil#createAttributedQNameType()` and `org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil#getAddressUriString()`.
- WS Addressing constant `org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConstants#QNAME_ACTION` that describes the fully qualified name for the action type.

### Deprecated
- `org.somda.sdc.glue.provider.SdcDevice#getDiscoveryAccess()` and `#getHostingServiceAccess()`; see `SdcDevice` class comment for alternative access.
- `org.somda.sdc.dpws.CommunicationLogSink.getTargetStream()`; see method comment for alternative.
- `org.somda.sdc.glue.consumer.factory.SdcRemoteDeviceFactory#createSdcRemoteDevice()` without watchdog argument.

### Changed

- Report processing on consumer side, which now compares MDIB sequence IDs by using URI compare instead of string compare.
- Extracted namespace prefixes in `biceps` and `glue` package `CommonConstants`.
- Enable generating equals and hashcode for all models. (#140)

### Fixed

- `org.somda.sdc.dpws.soap.SoapMessage#getEnvelopeWithMappedHeaders()` did not retain additional header set in the original envelope. (#131)
- `SdcRemoteDevicesConnectorImpl` did not register disconnecting providers.
- Services did not shut down in an orderly manner, causing issues when shutting down a consumer. (#134)
- Jetty server could select incorrect adapter when running HTTPS. (#135)
- `org.somda.sdc.dpws.soap.wsaddressing.WsAddressingMapper#mapToJaxbSoapHeader()` could cause duplicate header entries, e.g. Action elements. (#140)
- SOAP engine did not respond with appropriate faults in case the action header was missing or the action was unknown. (#143)

## [1.0.1] - 2020-03-11

### Fixed

- `org.somda.sdc.dpws.CommunicationLogImpl.logMessage(Direction direction, TransportType transportType, CommunicationContext communicationContext, InputStream message)` did not close OutputStream and was logging trailing empty bytes (#126)
- `org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants.NAMESPACE` contained an extra trailing slash, not matching the actual WS-Discovery 1.1 namespace (#130)
