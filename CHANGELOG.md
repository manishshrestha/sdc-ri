# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- _Releases_ and _How to get started_ sections to the project readme. (#125)
- Feature from BICEPS to send and receive periodic reports. (#51)

### Changed

- Report processing on consumer side, which now compares MDIB sequence IDs by using URI compare instead of string compare.