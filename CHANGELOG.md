# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `org.somda.sdc.common.util.AnyDateTime` and `org.somda.sdc.common.util.AnyDateTimeAdapter` to fully support XML Schema DateTime. (#151)

### Changed

- Use of `io.github.threetenjaxb.core.LocalDateTimeXmlAdapter` to `org.somda.sdc.common.util.AnyDateTimeAdapter` for any XML Schema DateTime in module `biceps-model`. (#151)
- Use log4j2-api instead of slf4j for logging. (#156)
- Communication log file names to include SOAP action information and XML to be pretty-printed. (#153)
- `GetContainmentTree` handling changed in order to allow traversal of the MDIB. (#150)
- Change names in `org.somda.sdc.dpws.soap.wseventing.WsEventingConstants` from `WSE_ACTION[...]` to `WSA_ACTION[...]`. (#157)
- `LocalAddressResolverImpl` resolves target addresses using the configured adapter only. (#69)

### Removed

- `org.somda.sdc.dpws.CommunicationLogSink.getTargetStream()`; use `org.somda.sdc.dpws.CommunicationLogSink.createTargetStream()` instead. (#153)

## 1.1.0 - 2020-04-18

### Added

- _Releases_ and _How to get started_ sections to the project readme. (#125)
- Feature from BICEPS to send and receive periodic reports. (#51)
- Jetty server supports http and https connections on the same port. (#107)
- Additional utility method in `org.somda.sdc.dpws.soap.SoapUtil` to create new SoapMessage with reference parameters. (#140)
- Additional utility methods `org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil#createAttributedQNameType()` and `org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil#getAddressUriString()`.
- WS Addressing constant `org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConstants#QNAME_ACTION` that describes the fully qualified name for the action type.
- `org.somda.sdc.dpws.http.HttpException` in order to transport HTTP status codes on SOAP faults. (#143)
- `org.somda.sdc.dpws.soap.SoapFaultHttpStatusCodeMapping` to map from SOAP faults to HTTP status codes. (#143)
- SOAP constants for VersionMismatch, MustUnderstand and DataEncodingUnknown SOAP Fault codes.
- `org.somda.sdc.dpws.soap.exception.SoapFaultException`: constructor that accepts a throwable cause. (#143)

### Changed

- Report processing on consumer side, which now compares MDIB sequence IDs by using URI compare instead of string compare.
- Extracted namespace prefixes in `biceps` and `glue` package `CommonConstants`.
- Enable generating equals and hashcode for all models. (#140)
- `org.somda.sdc.dpws.soap.TransportInfo` provides a  `List` of certificates instead of a `Collection`. (#147)

### Deprecated

- `org.somda.sdc.glue.provider.SdcDevice#getDiscoveryAccess()` and `#getHostingServiceAccess()`; see `SdcDevice` class comment for alternative access.
- `org.somda.sdc.dpws.CommunicationLogSink.getTargetStream()`; see method comment for alternative.
- `org.somda.sdc.glue.consumer.factory.SdcRemoteDeviceFactory#createSdcRemoteDevice()` without watchdog argument.
- `org.somda.sdc.dpws.soap.MarshallingService#handleRequestResponse()` as this function was only used by tests.
- `org.somda.sdc.dpws.http.jetty.JettyHttpServerHandler#getX509Certificates()` as it is supposed to be an internal function only.
- `org.somda.sdc.dpws.soap.TransportInfo` constructor using a collection. (#147)
- Jetty server supports http and https connections on the same port (#107)
- Additional utility method in `org.somda.sdc.dpws.soap.SoapUtil` to create new SoapMessage with reference parameters (#140)
- `org.somda.sdc.dpws.soap.HttpApplicationInfo(Map<String, String>)` and `org.somda.sdc.dpws.soap.HttpApplicationInfo#getHttpHeaders()`; use Multimap versions instead. (#147)
- `org.somda.sdc.biceps.common.preprocessing.DescriptorChildRemover#removeChildren(MdsDescriptor)` as this was not intended to be public. (#149)

### Fixed

- `org.somda.sdc.dpws.soap.SoapMessage#getEnvelopeWithMappedHeaders()` did not retain additional header set in the original envelope. (#131)
- `SdcRemoteDevicesConnectorImpl` did not register disconnecting providers.
- Services did not shut down in an orderly manner, causing issues when shutting down a consumer. (#134)
- Jetty server could select incorrect adapter when running HTTPS. (#135)
- `org.somda.sdc.dpws.soap.wsaddressing.WsAddressingMapper#mapToJaxbSoapHeader()` could cause duplicate header entries, e.g. Action elements. (#140)
- SOAP engine did not respond with appropriate faults in case the action header was missing or the action was unknown. (#143)
- `SdcRemoteDevicesConnectorImpl` did not register disconnecting providers
- Services did not shut down in an orderly manner, causing issues when shutting down a consumer (#134)
- Jetty server could select incorrect adapter when running https (#135)
- `org.somda.sdc.dpws.soap.wsaddressing.WsAddressingMapper#mapToJaxbSoapHeader()` could cause duplicate header entries, e.g. Action elements (#140)
- Http headers which occurred multiple times would only return the last value. (#146)

## [1.0.1] - 2020-03-11

### Fixed

- `org.somda.sdc.dpws.CommunicationLogImpl.logMessage(Direction direction, TransportType transportType, CommunicationContext communicationContext, InputStream message)` did not close OutputStream and was logging trailing empty bytes (#126)
- `org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants.NAMESPACE` contained an extra trailing slash, not matching the actual WS-Discovery 1.1 namespace (#130)
