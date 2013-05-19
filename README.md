Share-Management-System
=======================

Share Management System - SMS for short, is a simpel but effective system to manage shares to 1 access server.

## Roadmap

### 1.0

- Stabilize so the new system can be used.
- Implement file history tracking.

### 1.1

- Improve lock file finding (maybe use lsof -F pn -c smbd | grep /mountpoint)
- Improve log statistics (Does not display file deletes/unlinks)

## 1.2

- Implement easier file renaming.
- Implement functionality for static file links.

## 1.3 

- Implement dynamic level depth scanning.