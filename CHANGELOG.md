# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- _Releases_ and _How to get started_ sections to the project readme. (#125)
- Feature from BICEPS to send and receive periodic reports. (#51)

### Deprecated
- `org.somda.sdc.glue.provider.SdcDevice#getDiscoveryAccess()` and `#getHostingServiceAccess()`; see `SdcDevice` class comment for alternative access.
- `org.somda.sdc.dpws.CommunicationLogSink.getTargetStream()`; see method comment for alternative
### Changed

- Report processing on consumer side, which now compares MDIB sequence IDs by using URI compare instead of string compare.

## [1.0.1] - 2020-03-11

### Fixed

- `org.somda.sdc.dpws.CommunicationLogImpl.logMessage(Direction direction, TransportType transportType, CommunicationContext communicationContext, InputStream message)` did not close OutputStream and was logging trailing empty bytes (#126)
- `org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants.NAMESPACE` contained an extra trailing slash, not matching the actual WS-Discovery 1.1 namespace (#130)
