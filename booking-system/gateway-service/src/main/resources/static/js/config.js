/**
 * VibeCheck Configuration
 * Environment and runtime settings for API Gateway connectivity.
 */
(function(window) {
  'use strict';

  function determineApiBaseUrl() {
    // 1. Explicit window-level environment variable (e.g. injected by server/container)
    if (window.__ENV__ && window.__ENV__.GATEWAY_URL) {
      return window.__ENV__.GATEWAY_URL.replace(/\/+$/, '');
    }

    // 2. Local storage override (useful for debugging or switching targets)
    const stored = localStorage.getItem('vibecheck_gateway_url');
    if (stored && stored.trim()) {
      return stored.trim().replace(/\/+$/, '');
    }

    // 3. Same-origin detection: if accessed via Gateway (port 8079) or standard reverse proxy (80/443)
    if (window.location && window.location.port === '8079') {
      return window.location.origin;
    }

    // 4. Default standard local development gateway port
    return 'http://localhost:8079';
  }

  const VIBECHECK_CONFIG = {
    apiBaseUrl: determineApiBaseUrl(),
    timeoutMs: 12000,
    maxRetries: 2,
    version: '1.0.0-m33',

    setGatewayUrl(url) {
      if (!url) {
        localStorage.removeItem('vibecheck_gateway_url');
      } else {
        localStorage.setItem('vibecheck_gateway_url', url.trim().replace(/\/+$/, ''));
      }
      this.apiBaseUrl = determineApiBaseUrl();
      console.log('[VibeCheck Config] API Gateway URL updated to:', this.apiBaseUrl);
    }
  };

  window.VIBECHECK_CONFIG = VIBECHECK_CONFIG;
})(window);
