/**
 * VibeCheck Unified API Client
 * Enterprise HTTP client with JWT lifecycle, central 401 token refresh,
 * request correlation, error normalization, and timeout controls.
 */
(function(window) {
  'use strict';

  class ApiError extends Error {
    constructor(message, status = 0, data = null) {
      super(message);
      this.name = 'ApiError';
      this.status = status;
      this.data = data;
    }
  }

  function generateUUID() {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
      return crypto.randomUUID();
    }
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }

  // --- Auth & Token Storage ---
  const Storage = {
    getAccessToken() {
      return localStorage.getItem('vibecheck_token') || null;
    },
    getRefreshToken() {
      return localStorage.getItem('vibecheck_refresh_token') || null;
    },
    getUser() {
      const u = localStorage.getItem('vibecheck_user');
      if (!u) return null;
      try {
        return JSON.parse(u);
      } catch (e) {
        return null;
      }
    },
    saveAuth(authData) {
      if (authData.accessToken) {
        localStorage.setItem('vibecheck_token', authData.accessToken);
      }
      if (authData.refreshToken) {
        localStorage.setItem('vibecheck_refresh_token', authData.refreshToken);
      }
      if (authData.user) {
        localStorage.setItem('vibecheck_user', JSON.stringify(authData.user));
      }
    },
    clearAuth() {
      localStorage.removeItem('vibecheck_token');
      localStorage.removeItem('vibecheck_refresh_token');
      localStorage.removeItem('vibecheck_user');
    },
    isAuthenticated() {
      return !!this.getAccessToken();
    },
    getToken() {
      return this.getAccessToken();
    },
    setToken(token) {
      if (token) localStorage.setItem('vibecheck_token', token);
      else localStorage.removeItem('vibecheck_token');
    }
  };

  // --- Central HTTP Fetch with Auto-Refresh & Error Handling ---
  let isRefreshing = false;
  let refreshQueue = [];

  function processQueue(error, token = null) {
    refreshQueue.forEach(prom => {
      if (error) {
        prom.reject(error);
      } else {
        prom.resolve(token);
      }
    });
    refreshQueue = [];
  }

  async function fetchWithAuth(endpoint, options = {}, isRetry = false) {
    const baseUrl = (window.VIBECHECK_CONFIG && window.VIBECHECK_CONFIG.apiBaseUrl) || 'http://localhost:8079';
    const cleanEndpoint = endpoint.startsWith('/') ? endpoint : '/' + endpoint;
    const url = baseUrl + cleanEndpoint;

    const headers = new Headers(options.headers || {});
    if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
      headers.set('Content-Type', 'application/json');
    }
    if (!headers.has('Accept')) {
      headers.set('Accept', 'application/json');
    }

    // Attach correlation ID
    if (!headers.has('X-Correlation-ID')) {
      headers.set('X-Correlation-ID', generateUUID());
    }

    // Attach Bearer token if user is authenticated
    const token = Storage.getAccessToken();
    if (token && !headers.has('Authorization')) {
      headers.set('Authorization', `Bearer ${token}`);
    }

    // Setup Timeout
    const timeoutMs = (window.VIBECHECK_CONFIG && window.VIBECHECK_CONFIG.timeoutMs) || 12000;
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

    const fetchConfig = {
      ...options,
      headers,
      signal: controller.signal
    };

    try {
      const response = await fetch(url, fetchConfig);
      clearTimeout(timeoutId);

      // Handle 401 Unauthorized (Token Expiration / Invalid Token)
      if (response.status === 401 && !isRetry && !endpoint.includes('/auth/login') && !endpoint.includes('/auth/register') && !endpoint.includes('/auth/refresh')) {
        const refreshToken = Storage.getRefreshToken();
        if (refreshToken) {
          if (isRefreshing) {
            // Wait for existing refresh to resolve
            return new Promise((resolve, reject) => {
              refreshQueue.push({ resolve, reject });
            }).then(() => fetchWithAuth(endpoint, options, true));
          }

          isRefreshing = true;
          try {
            const refreshResult = await authApi.refreshToken(refreshToken);
            Storage.saveAuth(refreshResult);
            isRefreshing = false;
            processQueue(null, refreshResult.accessToken);
            return fetchWithAuth(endpoint, options, true);
          } catch (refreshErr) {
            isRefreshing = false;
            processQueue(refreshErr, null);
            Storage.clearAuth();
            window.dispatchEvent(new CustomEvent('vibecheck:session_expired'));
            throw new ApiError('Your session has expired. Please sign in again.', 401);
          }
        } else {
          Storage.clearAuth();
          window.dispatchEvent(new CustomEvent('vibecheck:session_expired'));
        }
      }

      // Parse JSON if possible
      let data = null;
      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        data = await response.json();
      } else if (response.status !== 204) {
        data = await response.text();
      }

      if (!response.ok) {
        let errorMsg = `Request failed with status ${response.status}`;
        if (data && typeof data === 'object') {
          // RFC 9457 ProblemDetail or Spring standard error
          errorMsg = data.detail || data.title || data.message ||
                     (data.error && data.error.message) || (data.errors && JSON.stringify(data.errors)) || errorMsg;
        } else if (typeof data === 'string' && data.trim()) {
          errorMsg = data;
        }
        throw new ApiError(errorMsg, response.status, data);
      }

      return data;
    } catch (err) {
      clearTimeout(timeoutId);
      if (err.name === 'AbortError') {
        throw new ApiError('Network request timed out. Please check your connection and try again.', 408);
      }
      if (err instanceof ApiError) {
        throw err;
      }
      throw new ApiError(err.message || 'Unable to connect to VibeCheck Gateway. Please verify the gateway is running.', 0);
    }
  }

  // --- API Domain Services ---

  // 1. AUTHENTICATION SERVICE
  const authApi = {
    async register(userData) {
      return fetchWithAuth('/api/v1/auth/register', {
        method: 'POST',
        body: JSON.stringify(userData)
      });
    },

    async login(credentials) {
      return fetchWithAuth('/api/v1/auth/login', {
        method: 'POST',
        body: JSON.stringify(credentials)
      });
    },

    async refreshToken(refreshToken) {
      return fetchWithAuth('/api/v1/auth/refresh', {
        method: 'POST',
        body: JSON.stringify({ refreshToken })
      });
    },

    async logout(userId) {
      try {
        if (userId) {
          await fetchWithAuth(`/api/v1/auth/logout/${userId}`, { method: 'POST' });
        }
      } catch (e) {
        console.warn('[VibeCheck Auth] Remote logout error:', e);
      } finally {
        Storage.clearAuth();
      }
    },

    async getUserProfile(userId) {
      return fetchWithAuth(`/api/v1/auth/users/${userId}`);
    }
  };

  // 2. MOVIE SERVICE
  const movieApi = {
    async getAllMovies(page = 0, size = 50) {
      return fetchWithAuth(`/api/v1/movies?page=${page}&size=${size}&sort=createdAt,desc`);
    },

    async getMovieById(id) {
      return fetchWithAuth(`/api/v1/movies/${id}`);
    },

    async getNowShowing() {
      return fetchWithAuth('/api/v1/movies/now-showing');
    },

    async getComingSoon() {
      return fetchWithAuth('/api/v1/movies/coming-soon');
    },

    async searchMovies(keyword) {
      return fetchWithAuth(`/api/v1/movies/search?keyword=${encodeURIComponent(keyword)}`);
    }
  };

  // 3. THEATRE SERVICE
  const theatreApi = {
    async getAllCities() {
      return fetchWithAuth('/api/v1/cities');
    },

    async getAllTheatres(page = 0, size = 50) {
      return fetchWithAuth(`/api/v1/theatres?page=${page}&size=${size}`);
    },

    async getTheatresByCity(cityId) {
      return fetchWithAuth(`/api/v1/theatres/city/${cityId}`);
    },

    async getTheatreById(id) {
      return fetchWithAuth(`/api/v1/theatres/${id}`);
    }
  };

  // 4. SHOW SERVICE
  const showApi = {
    async getShowsByMovie(movieId) {
      return fetchWithAuth(`/api/v1/shows/movie/${movieId}?size=50`);
    },

    async getShowsByDate(dateStr) {
      return fetchWithAuth(`/api/v1/shows/date/${dateStr}`);
    },

    async getShowsByTheatreAndDate(theatreId, dateStr) {
      return fetchWithAuth(`/api/v1/shows/theatre/${theatreId}/date/${dateStr}`);
    },

    async getShowById(id) {
      return fetchWithAuth(`/api/v1/shows/${id}`);
    },

    async getShowSeats(showId) {
      return fetchWithAuth(`/api/v1/shows/${showId}/seats`);
    }
  };

  // 5. BOOKING SERVICE
  const bookingApi = {
    async createBooking({ userId, showId, showSeatIds, paymentMethod = 'UPI', idempotencyKey = null }) {
      const key = idempotencyKey || generateUUID();
      return fetchWithAuth('/api/v1/bookings', {
        method: 'POST',
        headers: {
          'Idempotency-Key': key
        },
        body: JSON.stringify({
          userId,
          showId,
          showSeatIds,
          paymentMethod,
          idempotencyKey: key
        })
      });
    },

    async getBookingByReference(reference) {
      return fetchWithAuth(`/api/v1/bookings/${reference}`);
    },

    async confirmBooking({ bookingReference, paymentId }) {
      return fetchWithAuth('/api/v1/bookings/confirm', {
        method: 'POST',
        body: JSON.stringify({ bookingReference, paymentId })
      });
    },

    async cancelBooking(reference, reason = 'Customer cancelled from Web') {
      return fetchWithAuth(`/api/v1/bookings/${reference}/cancel?reason=${encodeURIComponent(reason)}`, {
        method: 'POST'
      });
    },

    async getUserBookings(userId, page = 0, size = 50) {
      return fetchWithAuth(`/api/v1/bookings/user/${userId}?page=${page}&size=${size}&sort=createdAt,desc`);
    }
  };

  // 6. PAYMENT SERVICE
  const paymentApi = {
    async initiatePayment({ bookingId, userId, amount, currency = 'INR', paymentMethod = 'UPI', bookingReference, idempotencyKey = null }) {
      const key = idempotencyKey || generateUUID();
      return fetchWithAuth('/api/v1/payments', {
        method: 'POST',
        headers: {
          'Idempotency-Key': key
        },
        body: JSON.stringify({
          bookingId,
          userId,
          amount,
          currency,
          paymentMethod,
          bookingReference,
          idempotencyKey: key
        })
      });
    },

    async getPaymentById(paymentId) {
      return fetchWithAuth(`/api/v1/payments/${paymentId}`);
    },

    async getPaymentByBookingId(bookingId) {
      return fetchWithAuth(`/api/v1/payments/booking/${bookingId}`);
    }
  };

  // Export API client to global window
  const clientInstance = {
    ApiError,
    Storage,
    getToken: () => Storage.getAccessToken(),
    setToken: (t) => Storage.setToken(t),
    clearAuth: () => Storage.clearAuth(),
    refreshToken: () => authApi.refreshToken(),
    generateUUID,
    fetchWithAuth,
    auth: authApi,
    movies: movieApi,
    theatres: theatreApi,
    shows: showApi,
    bookings: bookingApi,
    payments: paymentApi
  };

  window.VibeCheckApi = clientInstance;
  window.ApiClient = clientInstance;

})(window);
