Share-Management-System
=======================

Share Management System - SMS for short, is a simpel but effective system to manage shares to 1 access server.

## Changelog

### v1.2
- Tree scanning
- Performance improvements for db inserts
- Performance improvements for scanning

### v1.1
- History tracking
- Various bugfixes

### v1.0
- Basic implementation.

## Roadmap

### 1.3
- Improve lock file finding (maybe use lsof -F pn -c smbd | grep /mountpoint)
- Improve log statistics (Does not display file deletes/unlinks)

## 1.4
- Implement easier file renaming.
- Implement functionality for static file links.