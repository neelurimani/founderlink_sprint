# Branching and Versioning Guide

## Branches

- `main`: integration branch for ongoing development.
- `production`: stable branch used for production deployments.
- `microservice/user-service`: working branch dedicated to the user-service microservice.

## Recommended Flow

1. Create feature branches from `microservice/user-service`:
   - `feature/user-service/<short-description>`
2. Merge tested features into `microservice/user-service`.
3. Merge `microservice/user-service` into `main` when ready.
4. Merge `main` into `production` only for approved releases.

## Versioning per Microservice

- Current version is stored in `VERSION`.
- Follow semantic versioning: `MAJOR.MINOR.PATCH`.
  - MAJOR: breaking changes
  - MINOR: backward-compatible features
  - PATCH: backward-compatible fixes

## Release Tags

Create tags in this format:

- `user-service-v0.1.0`

Example:

```bash
git checkout production
git pull
git tag user-service-v0.1.0
git push origin user-service-v0.1.0
```
