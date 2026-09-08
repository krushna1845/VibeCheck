# GitHub Repository Secrets — Setup Guide

This document lists all secrets required by the Milestone 22 CI/CD workflows.
Add them at: **GitHub → Repository → Settings → Secrets and variables → Actions → New repository secret**

---

## Required Secrets

### Core Application Secrets

| Secret Name | Description | Required By | Example Format |
|-------------|-------------|-------------|----------------|
| `JWT_SECRET` | JWT token signing key (≥ 32 chars) | `ci.yml` — unit tests | `9a4f2c8d7e6b5a4c3f2e1d0c9b8a7f6e5d4c3b2a` |
| `INTERNAL_SECURITY_SECRET` | Internal service-to-service perimeter secret | `ci.yml` — unit tests | `vibecheck-internal-secret-key-min-32-chars` |

> [!IMPORTANT]
> `JWT_SECRET` must be **at least 32 characters** long. Shorter secrets will fail the HMAC-SHA256 key validation at startup.

---

### Docker Hub Secrets (optional — needed for image push to Docker Hub)

| Secret Name | Description | Required By | Where to Get |
|-------------|-------------|-------------|--------------|
| `DOCKERHUB_USERNAME` | Your Docker Hub username | `ci.yml` — docker-build job | hub.docker.com → Account Settings |
| `DOCKERHUB_TOKEN` | Docker Hub access token (**not** your password) | `ci.yml` — docker-build job | hub.docker.com → Account Settings → Security → New Access Token |

> [!NOTE]
> If these secrets are **not set**, the pipeline will still **build** Docker images locally (for validation) but will **skip the push step**. This is safe for development branches.

---

### Security Scanning (optional — improves OWASP scan speed)

| Secret Name | Description | Required By | Where to Get |
|-------------|-------------|-------------|--------------|
| `NVD_API_KEY` | NIST NVD API key for dependency check | `ci.yml`, `security-audit.yml` | https://nvd.nist.gov/developers/request-an-api-key (free) |

> [!NOTE]
> Without `NVD_API_KEY`, OWASP Dependency-Check still works but NVD API requests are rate-limited (slower scans). For CI environments, an API key is strongly recommended to avoid timeout failures.

---

## How to Add Secrets

```bash
# Using GitHub CLI (gh):
gh secret set JWT_SECRET --body "your-secret-value" --repo krushna1845/VibeCheck
gh secret set INTERNAL_SECURITY_SECRET --body "your-secret-value" --repo krushna1845/VibeCheck
gh secret set DOCKERHUB_USERNAME --body "your-dockerhub-username" --repo krushna1845/VibeCheck
gh secret set DOCKERHUB_TOKEN --body "your-dockerhub-access-token" --repo krushna1845/VibeCheck
gh secret set NVD_API_KEY --body "your-nvd-api-key" --repo krushna1845/VibeCheck
```

Or manually via: `https://github.com/krushna1845/VibeCheck/settings/secrets/actions`

---

## Minimum Setup (CI works without Docker push)

For the CI pipeline to run and tests to pass, you only need:
1. `JWT_SECRET` — set to any 32+ char string
2. `INTERNAL_SECURITY_SECRET` — set to any string

The build will succeed; only the Docker image push to Docker Hub will be skipped.

---

## Security Best Practices

- ✅ Never commit secrets to the repository (the `.env` file in `.gitignore` handles local dev)  
- ✅ Use long, randomly generated strings for JWT_SECRET (min 32 chars)  
- ✅ Rotate Docker Hub tokens periodically (every 90 days)  
- ✅ Use repository-scoped secrets, not organisation-level, for isolation  
- ✅ Audit secret usage regularly via `Settings → Security → Secret scanning`
