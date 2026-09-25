/* =============================================
   VIBECHECK ADMIN — JavaScript Application
   ============================================= */

(function() {
  'use strict';

  // ─── State ───
  const state = {
    token: localStorage.getItem('admin_token') || null,
    user: JSON.parse(localStorage.getItem('admin_user') || 'null'),
    currentSection: 'dashboard',
    // Demo data (simulated since backend may not be running)
    movies: [],
    theatres: [],
    shows: [],
    bookings: [],
    users: [],
    payments: [],
    cities: [],
  };

  // ─── Config ───
  const config = {
    gatewayUrl: localStorage.getItem('admin_gateway') || 'http://localhost:8080',
  };

  // ─── Init ───
  document.addEventListener('DOMContentLoaded', init);

  function init() {
    seedDemoData();
    if (state.token && state.user) {
      showApp();
    } else {
      showLogin();
    }
    bindEvents();
  }

  // ─── Demo Data Seeding ───
  function seedDemoData() {
    if (state.movies.length) return;

    const movieTitles = [
      { title: 'Pushpa 2: The Rule', genre: 'Action', duration: 178, rating: 'UA', status: 'NOW_SHOWING', revenue: 245000, bookings: 1847 },
      { title: 'Stree 3', genre: 'Horror Comedy', duration: 152, rating: 'UA', status: 'NOW_SHOWING', revenue: 189000, bookings: 1235 },
      { title: 'Singham Again', genre: 'Action', duration: 163, rating: 'UA', status: 'NOW_SHOWING', revenue: 172000, bookings: 1156 },
      { title: 'War 2', genre: 'Action Thriller', duration: 148, rating: 'UA', status: 'COMING_SOON', revenue: 0, bookings: 0 },
      { title: 'Animal Park', genre: 'Crime Drama', duration: 195, rating: 'A', status: 'COMING_SOON', revenue: 0, bookings: 0 },
      { title: 'Dhoom 4', genre: 'Action', duration: 156, rating: 'UA', status: 'COMING_SOON', revenue: 0, bookings: 0 },
      { title: 'Jawan 2', genre: 'Action', duration: 168, rating: 'UA', status: 'ENDED', revenue: 320000, bookings: 2345 },
      { title: 'Pathaan Legacy', genre: 'Spy Thriller', duration: 145, rating: 'UA', status: 'ENDED', revenue: 280000, bookings: 2100 },
    ];

    state.movies = movieTitles.map((m, i) => ({
      id: crypto.randomUUID(),
      title: m.title,
      description: `A blockbuster ${m.genre} film.`,
      durationMinutes: m.duration,
      releaseDate: randomDate(2026, 1, 9),
      censorRating: m.rating,
      posterUrl: '',
      trailerUrl: '',
      status: m.status,
      genres: [m.genre],
      languages: ['Hindi', 'English'],
      createdAt: new Date().toISOString(),
      _revenue: m.revenue,
      _bookings: m.bookings,
    }));

    const cityNames = ['Mumbai', 'Delhi', 'Bangalore', 'Hyderabad', 'Chennai', 'Pune', 'Kolkata', 'Ahmedabad'];
    state.cities = cityNames.map((c, i) => ({
      id: i + 1,
      name: c,
      state: c === 'Mumbai' ? 'Maharashtra' : c === 'Delhi' ? 'Delhi' : c === 'Bangalore' ? 'Karnataka' : c === 'Hyderabad' ? 'Telangana' : c === 'Chennai' ? 'Tamil Nadu' : c === 'Pune' ? 'Maharashtra' : c === 'Kolkata' ? 'West Bengal' : 'Gujarat',
      country: 'India',
      pincode: String(400000 + i * 10000),
    }));

    const theatreNames = ['PVR Phoenix', 'INOX Megaplex', 'Cinepolis Forum', 'PVR Juhu', 'INOX Metro', 'Carnival Cinemas', 'Miraj Cinemas', 'Rajhans Cinemas', 'PVR Orion', 'INOX Lido'];
    state.theatres = theatreNames.map((t, i) => ({
      id: crypto.randomUUID(),
      name: t,
      cityId: state.cities[i % state.cities.length].id,
      cityName: state.cities[i % state.cities.length].name,
      address: `${Math.floor(Math.random() * 500) + 1}, ${t} Mall, ${state.cities[i % state.cities.length].name}`,
      status: 'ACTIVE',
      screens: Math.floor(Math.random() * 6) + 3,
      createdAt: new Date().toISOString(),
    }));

    const showStatuses = ['SCHEDULED', 'OPEN', 'FULL', 'CANCELLED'];
    state.shows = [];
    for (let i = 0; i < 15; i++) {
      const movie = state.movies[i % state.movies.length];
      const theatre = state.theatres[i % state.theatres.length];
      state.shows.push({
        id: crypto.randomUUID(),
        movieId: movie.id,
        movieTitle: movie.title,
        theatreId: theatre.id,
        theatreName: theatre.name,
        screenName: `Screen ${Math.floor(Math.random() * 5) + 1}`,
        startTime: new Date(Date.now() + (i * 3600000)).toISOString(),
        endTime: new Date(Date.now() + (i * 3600000) + (movie.durationMinutes * 60000)).toISOString(),
        language: ['Hindi', 'English', 'Tamil'][i % 3],
        status: showStatuses[i % 4],
      });
    }

    const bookingStatuses = ['CONFIRMED', 'CONFIRMED', 'CONFIRMED', 'PENDING', 'CANCELLED'];
    state.bookings = [];
    for (let i = 0; i < 20; i++) {
      const movie = state.movies[i % state.movies.length];
      const seats = Math.floor(Math.random() * 4) + 1;
      const amount = seats * (Math.floor(Math.random() * 200) + 150);
      state.bookings.push({
        id: crypto.randomUUID(),
        reference: `VCK${String(Date.now()).slice(-6)}${i}`,
        userId: crypto.randomUUID(),
        userName: ['Rahul Sharma', 'Priya Patel', 'Amit Kumar', 'Sneha Reddy', 'Vikram Singh', 'Ananya Das', 'Rohan Gupta', 'Kavita Nair'][i % 8],
        movieTitle: movie.title,
        seats: seats,
        totalAmount: amount,
        status: bookingStatuses[i % 5],
        createdAt: new Date(Date.now() - (i * 86400000 * Math.random())).toISOString(),
      });
    }

    state.users = [
      { id: crypto.randomUUID(), name: 'Admin User', email: 'admin@vibecheck.com', roles: ['ROLE_ADMIN'], status: 'ACTIVE', createdAt: '2026-01-15T10:00:00Z' },
      { id: crypto.randomUUID(), name: 'Theatre Manager', email: 'owner@pvr.com', roles: ['ROLE_THEATRE_OWNER'], status: 'ACTIVE', createdAt: '2026-02-10T10:00:00Z' },
      { id: crypto.randomUUID(), name: 'Rahul Sharma', email: 'rahul@gmail.com', roles: ['ROLE_USER'], status: 'ACTIVE', createdAt: '2026-03-05T10:00:00Z' },
      { id: crypto.randomUUID(), name: 'Priya Patel', email: 'priya@gmail.com', roles: ['ROLE_USER'], status: 'ACTIVE', createdAt: '2026-03-12T10:00:00Z' },
      { id: crypto.randomUUID(), name: 'Amit Kumar', email: 'amit@yahoo.com', roles: ['ROLE_USER'], status: 'SUSPENDED', createdAt: '2026-04-01T10:00:00Z' },
      { id: crypto.randomUUID(), name: 'Sneha Reddy', email: 'sneha@outlook.com', roles: ['ROLE_USER'], status: 'ACTIVE', createdAt: '2026-04-20T10:00:00Z' },
      { id: crypto.randomUUID(), name: 'Vikram Singh', email: 'vikram@gmail.com', roles: ['ROLE_USER', 'ROLE_THEATRE_OWNER'], status: 'ACTIVE', createdAt: '2026-05-01T10:00:00Z' },
    ];

    state.payments = state.bookings.filter(b => b.status === 'CONFIRMED').map((b, i) => ({
      id: crypto.randomUUID(),
      bookingRef: b.reference,
      userId: b.userId,
      userName: b.userName,
      gateway: ['RAZORPAY', 'STRIPE', 'PAYTM', 'UPI'][i % 4],
      amount: b.totalAmount,
      status: 'COMPLETED',
      createdAt: b.createdAt,
    }));
  }

  function randomDate(year, minMonth, maxMonth) {
    const m = minMonth + Math.floor(Math.random() * (maxMonth - minMonth + 1));
    const d = Math.floor(Math.random() * 28) + 1;
    return `${year}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
  }

  // ─── Auth ───
  function showLogin() {
    document.getElementById('loginScreen').classList.remove('hidden');
    document.getElementById('appShell').classList.add('hidden');
  }

  function showApp() {
    document.getElementById('loginScreen').classList.add('hidden');
    document.getElementById('appShell').classList.remove('hidden');
    document.getElementById('adminName').textContent = state.user?.name || 'Admin';
    document.querySelector('.avatar').textContent = (state.user?.name || 'A')[0].toUpperCase();
    renderDashboard();
  }

  function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;

    if (!email || !password) {
      showToast('Please fill all fields', 'error');
      return;
    }

    // Simulate login (in production, call /api/v1/auth/login)
    const btn = document.getElementById('loginBtn');
    btn.disabled = true;
    btn.innerHTML = '<span class="material-icons-round">hourglass_top</span> Signing In...';

    setTimeout(() => {
      // Demo: accept any credentials for admin
      state.token = 'demo-admin-token-' + Date.now();
      state.user = { name: 'Admin User', email: email, roles: ['ROLE_ADMIN'] };
      localStorage.setItem('admin_token', state.token);
      localStorage.setItem('admin_user', JSON.stringify(state.user));
      showToast('Welcome back, Admin!', 'success');
      showApp();
      btn.disabled = false;
      btn.innerHTML = '<span class="material-icons-round">login</span> Sign In';
    }, 800);
  }

  function handleLogout() {
    state.token = null;
    state.user = null;
    localStorage.removeItem('admin_token');
    localStorage.removeItem('admin_user');
    showLogin();
    showToast('Logged out successfully', 'info');
  }

  // ─── Navigation ───
  function navigateTo(section) {
    state.currentSection = section;
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    document.querySelector(`.nav-item[data-section="${section}"]`)?.classList.add('active');
    document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
    document.getElementById(`sec-${section}`)?.classList.add('active');

    switch (section) {
      case 'dashboard': renderDashboard(); break;
      case 'movies': renderMovies(); break;
      case 'theatres': renderTheatres(); break;
      case 'shows': renderShows(); break;
      case 'bookings': renderBookings(); break;
      case 'users': renderUsers(); break;
      case 'payments': renderPayments(); break;
      case 'cities': renderCities(); break;
    }
  }

  // ─── Dashboard ───
  function renderDashboard() {
    const now = new Date();
    document.getElementById('dashDate').textContent = now.toLocaleDateString('en-IN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });

    // KPIs
    animateValue('kpiMovies', state.movies.length);
    animateValue('kpiTheatres', state.theatres.filter(t => t.status === 'ACTIVE').length);
    animateValue('kpiBookings', state.bookings.length);
    const totalRevenue = state.movies.reduce((s, m) => s + (m._revenue || 0), 0);
    document.getElementById('kpiRevenue').textContent = '₹' + (totalRevenue / 1000).toFixed(0) + 'K';
    animateValue('kpiUsers', state.users.length);
    animateValue('kpiShows', state.shows.filter(s => s.status !== 'CANCELLED').length);

    // Bar Chart
    renderBarChart();

    // Donut Chart
    renderDonutChart();

    // Recent Bookings
    renderRecentBookings();

    // Top Movies
    renderTopMovies();
  }

  function animateValue(id, target) {
    const el = document.getElementById(id);
    let current = 0;
    const step = Math.max(1, Math.ceil(target / 30));
    const timer = setInterval(() => {
      current += step;
      if (current >= target) {
        current = target;
        clearInterval(timer);
      }
      el.textContent = current;
    }, 30);
  }

  function renderBarChart() {
    const container = document.getElementById('barChart');
    const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const values = [23, 35, 42, 28, 55, 78, 65];
    const max = Math.max(...values);

    container.innerHTML = values.map((v, i) => `
      <div class="bar-item">
        <span class="bar-val">${v}</span>
        <div class="bar" style="height: ${(v / max) * 170}px"></div>
        <span class="bar-label">${days[i]}</span>
      </div>
    `).join('');
  }

  function renderDonutChart() {
    const container = document.getElementById('donutChart');
    const data = [
      { label: 'Movies', value: 62, color: '#E31837' },
      { label: 'Events', value: 18, color: '#3B82F6' },
      { label: 'Plays', value: 12, color: '#10B981' },
      { label: 'Sports', value: 8, color: '#F59E0B' },
    ];
    const total = data.reduce((s, d) => s + d.value, 0);

    let gradient = '';
    let cumulative = 0;
    data.forEach(d => {
      const start = (cumulative / total) * 360;
      cumulative += d.value;
      const end = (cumulative / total) * 360;
      gradient += `${d.color} ${start}deg ${end}deg, `;
    });
    gradient = gradient.slice(0, -2);

    container.innerHTML = `
      <div class="donut-ring" style="background: conic-gradient(${gradient}); mask: radial-gradient(circle at center, transparent 40px, black 41px); -webkit-mask: radial-gradient(circle at center, transparent 40px, black 41px);"></div>
      <div class="donut-legend">
        ${data.map(d => `
          <div class="legend-item">
            <div class="legend-dot" style="background:${d.color}"></div>
            <span>${d.label}</span>
            <span style="margin-left:auto;color:var(--text);font-weight:600">${d.value}%</span>
          </div>
        `).join('')}
      </div>
    `;
  }

  function renderRecentBookings() {
    const tbody = document.getElementById('recentBookingsBody');
    const recent = [...state.bookings].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)).slice(0, 6);

    if (!recent.length) {
      tbody.innerHTML = '<tr><td colspan="6" class="empty-state">No bookings yet</td></tr>';
      return;
    }

    tbody.innerHTML = recent.map(b => `
      <tr>
        <td><strong>${b.reference}</strong></td>
        <td>${b.userName}</td>
        <td>${b.movieTitle}</td>
        <td>₹${b.totalAmount}</td>
        <td>${statusBadge(b.status)}</td>
        <td>${timeAgo(b.createdAt)}</td>
      </tr>
    `).join('');
  }

  function renderTopMovies() {
    const container = document.getElementById('topMoviesList');
    const top = [...state.movies].sort((a, b) => (b._bookings || 0) - (a._bookings || 0)).slice(0, 5);

    container.innerHTML = top.map((m, i) => {
      const rankClass = i === 0 ? 'gold' : i === 1 ? 'silver' : i === 2 ? 'bronze' : '';
      return `
        <div class="top-movie-item">
          <div class="top-movie-rank ${rankClass}">${i + 1}</div>
          <div class="top-movie-info">
            <div class="top-movie-title">${m.title}</div>
            <div class="top-movie-stats">${m._bookings} bookings · ₹${(m._revenue / 1000).toFixed(0)}K revenue</div>
          </div>
        </div>
      `;
    }).join('');
  }

  // ─── Movies ───
  function renderMovies() {
    const tbody = document.getElementById('moviesBody');
    let filtered = [...state.movies];

    const statusFilter = document.getElementById('movieStatusFilter').value;
    const search = document.getElementById('movieSearch').value.toLowerCase();

    if (statusFilter) filtered = filtered.filter(m => m.status === statusFilter);
    if (search) filtered = filtered.filter(m => m.title.toLowerCase().includes(search));

    if (!filtered.length) {
      tbody.innerHTML = '<tr><td colspan="7" class="empty-state">No movies match your filters.</td></tr>';
      return;
    }

    tbody.innerHTML = filtered.map(m => `
      <tr>
        <td><div class="poster-thumb" style="background:linear-gradient(135deg,#E31837,#1C1C1C);display:flex;align-items:center;justify-content:center;color:white;font-size:10px;font-weight:700">${m.title.substring(0,2).toUpperCase()}</div></td>
        <td><strong>${m.title}</strong><br><small style="color:var(--text-muted)">${(m.genres || []).join(', ')}</small></td>
        <td>${m.durationMinutes} min</td>
        <td>${m.releaseDate}</td>
        <td>${m.censorRating}</td>
        <td>${statusBadge(m.status)}</td>
        <td>
          <div class="action-btns">
            <button class="btn-icon" title="Edit" onclick="window._admin.editMovie('${m.id}')"><span class="material-icons-round">edit</span></button>
            <button class="btn-icon" title="Change Status" onclick="window._admin.changeMovieStatus('${m.id}')"><span class="material-icons-round">swap_horiz</span></button>
            <button class="btn-icon" title="Delete" onclick="window._admin.deleteMovie('${m.id}')"><span class="material-icons-round">delete</span></button>
          </div>
        </td>
      </tr>
    `).join('');
  }

  function showAddMovieModal(editId) {
    const movie = editId ? state.movies.find(m => m.id === editId) : null;
    const title = movie ? 'Edit Movie' : 'Add New Movie';

    openModal(title, `
      <div class="form-row">
        <div class="form-group">
          <label>Title *</label>
          <input type="text" id="mTitle" value="${movie?.title || ''}" placeholder="Movie title" required>
        </div>
        <div class="form-group">
          <label>Duration (minutes) *</label>
          <input type="number" id="mDuration" value="${movie?.durationMinutes || ''}" placeholder="120" min="1">
        </div>
      </div>
      <div class="form-group">
        <label>Description</label>
        <textarea id="mDesc" placeholder="Movie description...">${movie?.description || ''}</textarea>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>Release Date *</label>
          <input type="date" id="mReleaseDate" value="${movie?.releaseDate || ''}">
        </div>
        <div class="form-group">
          <label>Censor Rating *</label>
          <select id="mRating">
            <option value="U" ${movie?.censorRating === 'U' ? 'selected' : ''}>U</option>
            <option value="UA" ${(!movie || movie.censorRating === 'UA') ? 'selected' : ''}>UA</option>
            <option value="A" ${movie?.censorRating === 'A' ? 'selected' : ''}>A</option>
            <option value="S" ${movie?.censorRating === 'S' ? 'selected' : ''}>S</option>
          </select>
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>Poster URL</label>
          <input type="url" id="mPoster" value="${movie?.posterUrl || ''}" placeholder="https://...">
        </div>
        <div class="form-group">
          <label>Trailer URL</label>
          <input type="url" id="mTrailer" value="${movie?.trailerUrl || ''}" placeholder="https://...">
        </div>
      </div>
      <div class="form-group">
        <label>Status</label>
        <select id="mStatus">
          <option value="COMING_SOON" ${(!movie || movie.status === 'COMING_SOON') ? 'selected' : ''}>Coming Soon</option>
          <option value="NOW_SHOWING" ${movie?.status === 'NOW_SHOWING' ? 'selected' : ''}>Now Showing</option>
          <option value="ENDED" ${movie?.status === 'ENDED' ? 'selected' : ''}>Ended</option>
        </select>
      </div>
    `, [
      { label: 'Cancel', class: 'btn-secondary', action: closeModal },
      { label: movie ? 'Update Movie' : 'Create Movie', class: 'btn-primary', action: () => saveMovie(editId) },
    ]);
  }

  function saveMovie(editId) {
    const data = {
      title: document.getElementById('mTitle').value,
      description: document.getElementById('mDesc').value,
      durationMinutes: parseInt(document.getElementById('mDuration').value) || 0,
      releaseDate: document.getElementById('mReleaseDate').value,
      censorRating: document.getElementById('mRating').value,
      posterUrl: document.getElementById('mPoster').value,
      trailerUrl: document.getElementById('mTrailer').value,
      status: document.getElementById('mStatus').value,
    };

    if (!data.title || !data.durationMinutes || !data.releaseDate) {
      showToast('Please fill all required fields', 'error');
      return;
    }

    if (editId) {
      const idx = state.movies.findIndex(m => m.id === editId);
      if (idx !== -1) {
        state.movies[idx] = { ...state.movies[idx], ...data };
        showToast('Movie updated successfully', 'success');
      }
    } else {
      state.movies.push({
        id: crypto.randomUUID(),
        ...data,
        genres: [],
        languages: ['Hindi'],
        createdAt: new Date().toISOString(),
        _revenue: 0,
        _bookings: 0,
      });
      showToast('Movie created successfully', 'success');
    }

    closeModal();
    renderMovies();
  }

  function changeMovieStatus(id) {
    const movie = state.movies.find(m => m.id === id);
    if (!movie) return;

    const statuses = ['COMING_SOON', 'NOW_SHOWING', 'ENDED'];
    openModal('Change Movie Status', `
      <p style="margin-bottom:16px;color:var(--text-muted)">Current: <strong>${movie.title}</strong> — ${statusBadge(movie.status)}</p>
      <div class="form-group">
        <label>New Status</label>
        <select id="newMovieStatus">
          ${statuses.map(s => `<option value="${s}" ${s === movie.status ? 'selected' : ''}>${s.replace(/_/g, ' ')}</option>`).join('')}
        </select>
      </div>
    `, [
      { label: 'Cancel', class: 'btn-secondary', action: closeModal },
      { label: 'Update Status', class: 'btn-primary', action: () => {
        movie.status = document.getElementById('newMovieStatus').value;
        closeModal();
        renderMovies();
        showToast('Status updated', 'success');
      }},
    ]);
  }

  function deleteMovie(id) {
    const movie = state.movies.find(m => m.id === id);
    if (!movie) return;

    openModal('Delete Movie', `
      <p style="color:var(--text-muted)">Are you sure you want to delete <strong style="color:var(--white)">${movie.title}</strong>?</p>
      <p style="color:#EF4444;font-size:13px;margin-top:8px">This action cannot be undone.</p>
    `, [
      { label: 'Cancel', class: 'btn-secondary', action: closeModal },
      { label: 'Delete', class: 'btn-danger', action: () => {
        state.movies = state.movies.filter(m => m.id !== id);
        closeModal();
        renderMovies();
        showToast('Movie deleted', 'success');
      }},
    ]);
  }

  // ─── Theatres ───
  function renderTheatres() {
    const tbody = document.getElementById('theatresBody');
    let filtered = [...state.theatres];

    const cityFilter = document.getElementById('theatreCityFilter').value;
    const search = document.getElementById('theatreSearch').value.toLowerCase();

    // Populate city filter dropdown
    const citySelect = document.getElementById('theatreCityFilter');
    if (citySelect.options.length <= 1) {
      state.cities.forEach(c => {
        const opt = document.createElement('option');
        opt.value = c.id;
        opt.textContent = c.name;
        citySelect.appendChild(opt);
      });
    }

    if (cityFilter) filtered = filtered.filter(t => String(t.cityId) === cityFilter);
    if (search) filtered = filtered.filter(t => t.name.toLowerCase().includes(search));

    if (!filtered.length) {
      tbody.innerHTML = '<tr><td colspan="6" class="empty-state">No theatres found.</td></tr>';
      return;
    }

    tbody.innerHTML = filtered.map(t => `
      <tr>
        <td><strong>${t.name}</strong></td>
        <td>${t.cityName}</td>
        <td><small style="color:var(--text-muted)">${t.address}</small></td>
        <td>${t.screens}</td>
        <td>${statusBadge(t.status)}</td>
        <td>
          <div class="action-btns">
            <button class="btn-icon" title="Edit" onclick="window._admin.editTheatre('${t.id}')"><span class="material-icons-round">edit</span></button>
            <button class="btn-icon" title="Manage Screens" onclick="window._admin.manageScreens('${t.id}')"><span class="material-icons-round">tv</span></button>
            <button class="btn-icon" title="Delete" onclick="window._admin.deleteTheatre('${t.id}')"><span class="material-icons-round">delete</span></button>
          </div>
        </td>
      </tr>
    `).join('');
  }

  function showAddTheatreModal(editId) {
    const theatre = editId ? state.theatres.find(t => t.id === editId) : null;

    openModal(theatre ? 'Edit Theatre' : 'Add New Theatre', `
      <div class="form-group">
        <label>Theatre Name *</label>
        <input type="text" id="tName" value="${theatre?.name || ''}" placeholder="e.g. PVR Phoenix">
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>City *</label>
          <select id="tCity">
            ${state.cities.map(c => `<option value="${c.id}" ${theatre && theatre.cityId === c.id ? 'selected' : ''}>${c.name}</option>`).join('')}
          </select>
        </div>
        <div class="form-group">
          <label>Number of Screens</label>
          <input type="number" id="tScreens" value="${theatre?.screens || 4}" min="1" max="20">
        </div>
      </div>
      <div class="form-group">
        <label>Address *</label>
        <textarea id="tAddress" placeholder="Full address...">${theatre?.address || ''}</textarea>
      </div>
      <div class="form-group">
        <label>Status</label>
        <select id="tStatus">
          <option value="ACTIVE" ${(!theatre || theatre.status === 'ACTIVE') ? 'selected' : ''}>Active</option>
          <option value="INACTIVE" ${theatre?.status === 'INACTIVE' ? 'selected' : ''}>Inactive</option>
          <option value="MAINTENANCE" ${theatre?.status === 'MAINTENANCE' ? 'selected' : ''}>Maintenance</option>
        </select>
      </div>
    `, [
      { label: 'Cancel', class: 'btn-secondary', action: closeModal },
      { label: theatre ? 'Update Theatre' : 'Create Theatre', class: 'btn-primary', action: () => saveTheatre(editId) },
    ]);
  }

  function saveTheatre(editId) {
    const cityId = parseInt(document.getElementById('tCity').value);
    const city = state.cities.find(c => c.id === cityId);
    const data = {
      name: document.getElementById('tName').value,
      cityId: cityId,
      cityName: city?.name || '',
      screens: parseInt(document.getElementById('tScreens').value) || 4,
      address: document.getElementById('tAddress').value,
      status: document.getElementById('tStatus').value,
    };

    if (!data.name || !data.address) {
      showToast('Please fill all required fields', 'error');
      return;
    }

    if (editId) {
      const idx = state.theatres.findIndex(t => t.id === editId);
      if (idx !== -1) {
        state.theatres[idx] = { ...state.theatres[idx], ...data };
        showToast('Theatre updated', 'success');
      }
    } else {
      state.theatres.push({
        id: crypto.randomUUID(),
        ...data,
        createdAt: new Date().toISOString(),
      });
      showToast('Theatre created', 'success');
    }

    closeModal();
    renderTheatres();
  }

  function manageScreens(theatreId) {
    const theatre = state.theatres.find(t => t.id === theatreId);
    if (!theatre) return;

    openModal(`Screens — ${theatre.name}`, `
      <p style="color:var(--text-muted);margin-bottom:16px">This theatre has <strong>${theatre.screens}</strong> screens configured.</p>
      <p style="color:var(--text-dim);font-size:13px">Screen management requires the backend Theatre Service to be running.<br>
      Use the API: <code>POST /api/v1/theatres/${theatreId}/screens</code></p>
      <div style="margin-top:16px;padding:14px;background:var(--bg-input);border-radius:var(--radius-sm);border:1px solid var(--border)">
        <div style="font-size:12px;color:var(--text-muted);text-transform:uppercase;margin-bottom:8px">Sample Screens</div>
        ${Array.from({length: theatre.screens}, (_, i) => `
          <div style="display:flex;align-items:center;gap:10px;padding:8px 0;border-bottom:1px solid var(--border)">
            <span class="material-icons-round" style="font-size:18px;color:var(--cyan)">tv</span>
            <span>Screen ${i+1}</span>
            <span class="badge badge-blue" style="margin-left:auto">${['STANDARD','IMAX','4DX','DOLBY ATMOS','SCREEN X'][i % 5]}</span>
          </div>
        `).join('')}
      </div>
    `, [
      { label: 'Close', class: 'btn-secondary', action: closeModal },
    ]);
  }

  function deleteTheatre(id) {
    const theatre = state.theatres.find(t => t.id === id);
    if (!theatre) return;
    openModal('Delete Theatre', `
      <p style="color:var(--text-muted)">Are you sure you want to delete <strong style="color:var(--white)">${theatre.name}</strong>?</p>
    `, [
      { label: 'Cancel', class: 'btn-secondary', action: closeModal },
      { label: 'Delete', class: 'btn-danger', action: () => {
        state.theatres = state.theatres.filter(t => t.id !== id);
        closeModal();
        renderTheatres();
        showToast('Theatre deleted', 'success');
      }},
    ]);
  }

  // ─── Shows ───
  function renderShows() {
    const tbody = document.getElementById('showsBody');
    let filtered = [...state.shows];

    const dateFilter = document.getElementById('showDateFilter').value;
    if (dateFilter) {
      filtered = filtered.filter(s => s.startTime.startsWith(dateFilter));
    }

    if (!filtered.length) {
      tbody.innerHTML = '<tr><td colspan="7" class="empty-state">No shows found.</td></tr>';
      return;
    }

    tbody.innerHTML = filtered.map(s => {
      const start = new Date(s.startTime);
      return `
        <tr>
          <td><strong>${s.movieTitle}</strong></td>
          <td>${s.theatreName}</td>
          <td>${s.screenName}</td>
          <td>${start.toLocaleDateString('en-IN')} ${start.toLocaleTimeString('en-IN', {hour:'2-digit', minute:'2-digit'})}</td>
          <td>${s.language}</td>
          <td>${statusBadge(s.status)}</td>
          <td>
            <div class="action-btns">
              <button class="btn-icon" title="Edit" onclick="window._admin.editShow('${s.id}')"><span class="material-icons-round">edit</span></button>
              <button class="btn-icon" title="Cancel" onclick="window._admin.cancelShow('${s.id}')"><span class="material-icons-round">cancel</span></button>
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  function showAddShowModal(editId) {
    const show = editId ? state.shows.find(s => s.id === editId) : null;

    openModal(show ? 'Edit Show' : 'Schedule New Show', `
      <div class="form-group">
        <label>Movie *</label>
        <select id="sMovie">
          ${state.movies.filter(m => m.status !== 'ENDED').map(m => `<option value="${m.id}" ${show && show.movieId === m.id ? 'selected' : ''}>${m.title}</option>`).join('')}
        </select>
      </div>
      <div class="form-group">
        <label>Theatre *</label>
        <select id="sTheatre">
          ${state.theatres.filter(t => t.status === 'ACTIVE').map(t => `<option value="${t.id}" ${show && show.theatreId === t.id ? 'selected' : ''}>${t.name} (${t.cityName})</option>`).join('')}
        </select>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>Start Date & Time *</label>
          <input type="datetime-local" id="sStart" value="${show ? new Date(show.startTime).toISOString().slice(0,16) : ''}">
        </div>
        <div class="form-group">
          <label>Language *</label>
          <select id="sLang">
            ${['Hindi','English','Tamil','Telugu','Malayalam','Kannada','Bengali'].map(l => `<option value="${l}" ${show && show.language === l ? 'selected' : ''}>${l}</option>`).join('')}
          </select>
        </div>
      </div>
    `, [
      { label: 'Cancel', class: 'btn-secondary', action: closeModal },
      { label: show ? 'Update Show' : 'Schedule Show', class: 'btn-primary', action: () => saveShow(editId) },
    ]);
  }

  function saveShow(editId) {
    const movieId = document.getElementById('sMovie').value;
    const movie = state.movies.find(m => m.id === movieId);
    const theatreId = document.getElementById('sTheatre').value;
    const theatre = state.theatres.find(t => t.id === theatreId);
    const startTime = new Date(document.getElementById('sStart').value).toISOString();

    if (!movie || !theatre || !startTime) {
      showToast('Please fill all fields', 'error');
      return;
    }

    const data = {
      movieId, movieTitle: movie.title,
      theatreId, theatreName: theatre.name,
      screenName: `Screen ${Math.floor(Math.random() * theatre.screens) + 1}`,
      startTime,
      endTime: new Date(new Date(startTime).getTime() + movie.durationMinutes * 60000).toISOString(),
      language: document.getElementById('sLang').value,
      status: 'SCHEDULED',
    };

    if (editId) {
      const idx = state.shows.findIndex(s => s.id === editId);
      if (idx !== -1) { state.shows[idx] = { ...state.shows[idx], ...data }; showToast('Show updated', 'success'); }
    } else {
      state.shows.push({ id: crypto.randomUUID(), ...data });
      showToast('Show scheduled', 'success');
    }

    closeModal();
    renderShows();
  }

  function cancelShow(id) {
    const show = state.shows.find(s => s.id === id);
    if (!show) return;
    openModal('Cancel Show', `
      <p style="color:var(--text-muted)">Cancel show for <strong style="color:var(--white)">${show.movieTitle}</strong> at <strong>${show.theatreName}</strong>?</p>
    `, [
      { label: 'No', class: 'btn-secondary', action: closeModal },
      { label: 'Yes, Cancel', class: 'btn-danger', action: () => {
        show.status = 'CANCELLED';
        closeModal();
        renderShows();
        showToast('Show cancelled', 'success');
      }},
    ]);
  }

  // ─── Bookings ───
  function renderBookings() {
    const tbody = document.getElementById('bookingsBody');
    let filtered = [...state.bookings];

    const statusFilter = document.getElementById('bookingStatusFilter').value;
    const search = document.getElementById('bookingSearch').value.toLowerCase();

    if (statusFilter) filtered = filtered.filter(b => b.status === statusFilter);
    if (search) filtered = filtered.filter(b => b.reference.toLowerCase().includes(search) || b.userName.toLowerCase().includes(search));

    filtered.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

    if (!filtered.length) {
      tbody.innerHTML = '<tr><td colspan="8" class="empty-state">No bookings found.</td></tr>';
      return;
    }

    tbody.innerHTML = filtered.map(b => `
      <tr>
        <td><strong>${b.reference}</strong></td>
        <td><small>${b.userId.substring(0, 8)}...</small></td>
        <td>${b.movieTitle}</td>
        <td>${b.seats}</td>
        <td>₹${b.totalAmount}</td>
        <td>${statusBadge(b.status)}</td>
        <td>${new Date(b.createdAt).toLocaleDateString('en-IN')}</td>
        <td>
          <div class="action-btns">
            <button class="btn-icon" title="View Details" onclick="window._admin.viewBooking('${b.id}')"><span class="material-icons-round">visibility</span></button>
            <button class="btn-icon" title="Change Status" onclick="window._admin.changeBookingStatus('${b.id}')"><span class="material-icons-round">swap_horiz</span></button>
          </div>
        </td>
      </tr>
    `).join('');
  }

  function viewBooking(id) {
    const b = state.bookings.find(x => x.id === id);
    if (!b) return;

    openModal('Booking Details', `
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
        <div><span style="color:var(--text-muted);font-size:12px;text-transform:uppercase">Reference</span><br><strong>${b.reference}</strong></div>
        <div><span style="color:var(--text-muted);font-size:12px;text-transform:uppercase">Status</span><br>${statusBadge(b.status)}</div>
        <div><span style="color:var(--text-muted);font-size:12px;text-transform:uppercase">User</span><br>${b.userName}</div>
        <div><span style="color:var(--text-muted);font-size:12px;text-transform:uppercase">Movie</span><br>${b.movieTitle}</div>
        <div><span style="color:var(--text-muted);font-size:12px;text-transform:uppercase">Seats</span><br>${b.seats}</div>
        <div><span style="color:var(--text-muted);font-size:12px;text-transform:uppercase">Amount</span><br><strong style="color:var(--green)">₹${b.totalAmount}</strong></div>
        <div><span style="color:var(--text-muted);font-size:12px;text-transform:uppercase">User ID</span><br><small>${b.userId}</small></div>
        <div><span style="color:var(--text-muted);font-size:12px;text-transform:uppercase">Created</span><br>${new Date(b.createdAt).toLocaleString('en-IN')}</div>
      </div>
    `, [{ label: 'Close', class: 'btn-secondary', action: closeModal }]);
  }

  function changeBookingStatus(id) {
    const b = state.bookings.find(x => x.id === id);
    if (!b) return;

    openModal('Change Booking Status', `
      <p style="margin-bottom:16px;color:var(--text-muted)">Booking: <strong>${b.reference}</strong> — Current: ${statusBadge(b.status)}</p>
      <div class="form-group">
        <label>New Status</label>
        <select id="newBookingStatus">
          ${['CONFIRMED','PENDING','CANCELLED','EXPIRED'].map(s => `<option value="${s}" ${s === b.status ? 'selected' : ''}>${s}</option>`).join('')}
        </select>
      </div>
    `, [
      { label: 'Cancel', class: 'btn-secondary', action: closeModal },
      { label: 'Update', class: 'btn-primary', action: () => {
        b.status = document.getElementById('newBookingStatus').value;
        closeModal();
        renderBookings();
        showToast('Booking status updated', 'success');
      }},
    ]);
  }

  // ─── Users ───
  function renderUsers() {
    const tbody = document.getElementById('usersBody');
    let filtered = [...state.users];
    const search = document.getElementById('userSearch').value.toLowerCase();
    if (search) filtered = filtered.filter(u => u.name.toLowerCase().includes(search) || u.email.toLowerCase().includes(search));

    tbody.innerHTML = filtered.map(u => `
      <tr>
        <td><div class="avatar" style="width:32px;height:32px;font-size:12px">${u.name[0].toUpperCase()}</div></td>
        <td><strong>${u.name}</strong></td>
        <td>${u.email}</td>
        <td>${u.roles.map(r => `<span class="badge ${r.includes('ADMIN') ? 'badge-red' : r.includes('OWNER') ? 'badge-purple' : 'badge-blue'}">${r.replace('ROLE_', '')}</span>`).join(' ')}</td>
        <td>${statusBadge(u.status)}</td>
        <td>${new Date(u.createdAt).toLocaleDateString('en-IN')}</td>
        <td>
          <div class="action-btns">
            <button class="btn-icon" title="View" onclick="window._admin.viewUser('${u.id}')"><span class="material-icons-round">visibility</span></button>
          </div>
        </td>
      </tr>
    `).join('');
  }

  function viewUser(id) {
    const u = state.users.find(x => x.id === id);
    if (!u) return;
    openModal('User Details', `
      <div style="text-align:center;margin-bottom:20px">
        <div class="avatar" style="width:64px;height:64px;font-size:24px;margin:0 auto 12px">${u.name[0].toUpperCase()}</div>
        <h3 style="color:var(--white)">${u.name}</h3>
        <p style="color:var(--text-muted)">${u.email}</p>
      </div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
        <div><span style="color:var(--text-muted);font-size:12px;text-transform:uppercase">Roles</span><br>${u.roles.map(r => `<span class="badge badge-blue">${r.replace('ROLE_','')}</span>`).join(' ')}</div>
        <div><span style="color:var(--text-muted);font-size:12px;text-transform:uppercase">Status</span><br>${statusBadge(u.status)}</div>
        <div><span style="color:var(--text-muted);font-size:12px;text-transform:uppercase">User ID</span><br><small>${u.id}</small></div>
        <div><span style="color:var(--text-muted);font-size:12px;text-transform:uppercase">Joined</span><br>${new Date(u.createdAt).toLocaleDateString('en-IN')}</div>
      </div>
    `, [{ label: 'Close', class: 'btn-secondary', action: closeModal }]);
  }

  // ─── Payments ───
  function renderPayments() {
    const tbody = document.getElementById('paymentsBody');
    let filtered = [...state.payments];
    const statusFilter = document.getElementById('paymentStatusFilter').value;
    if (statusFilter) filtered = filtered.filter(p => p.status === statusFilter);

    if (!filtered.length) {
      tbody.innerHTML = '<tr><td colspan="8" class="empty-state">No payments found.</td></tr>';
      return;
    }

    tbody.innerHTML = filtered.map(p => `
      <tr>
        <td><small>${p.id.substring(0,8)}...</small></td>
        <td><strong>${p.bookingRef}</strong></td>
        <td>${p.userName}</td>
        <td><span class="badge badge-purple">${p.gateway}</span></td>
        <td>₹${p.amount}</td>
        <td>${statusBadge(p.status)}</td>
        <td>${new Date(p.createdAt).toLocaleDateString('en-IN')}</td>
        <td>
          <div class="action-btns">
            <button class="btn-icon" title="View"><span class="material-icons-round">visibility</span></button>
            <button class="btn-icon" title="Refund" onclick="window._admin.refundPayment('${p.id}')"><span class="material-icons-round">undo</span></button>
          </div>
        </td>
      </tr>
    `).join('');
  }

  function refundPayment(id) {
    const p = state.payments.find(x => x.id === id);
    if (!p) return;
    openModal('Process Refund', `
      <p style="color:var(--text-muted)">Refund payment <strong>₹${p.amount}</strong> for booking <strong>${p.bookingRef}</strong>?</p>
      <div class="form-group" style="margin-top:16px">
        <label>Refund Amount</label>
        <input type="number" id="refundAmount" value="${p.amount}" min="1" max="${p.amount}">
      </div>
    `, [
      { label: 'Cancel', class: 'btn-secondary', action: closeModal },
      { label: 'Process Refund', class: 'btn-danger', action: () => {
        p.status = 'REFUNDED';
        closeModal();
        renderPayments();
        showToast('Refund processed', 'success');
      }},
    ]);
  }

  // ─── Cities ───
  function renderCities() {
    const container = document.getElementById('citiesGrid');
    if (!state.cities.length) {
      container.innerHTML = '<div class="empty-state">No cities found.</div>';
      return;
    }

    container.innerHTML = state.cities.map(c => {
      const theatreCount = state.theatres.filter(t => t.cityId === c.id).length;
      return `
        <div class="city-card">
          <h4>${c.name}</h4>
          <p>${c.state}, ${c.country}</p>
          <div class="city-meta">
            <span class="city-theatres-count">${theatreCount} Theatre${theatreCount !== 1 ? 's' : ''}</span>
            <span style="color:var(--text-dim);font-size:12px">${c.pincode || ''}</span>
          </div>
        </div>
      `;
    }).join('');
  }

  function showAddCityModal() {
    openModal('Add New City', `
      <div class="form-group">
        <label>City Name *</label>
        <input type="text" id="cName" placeholder="e.g. Jaipur">
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>State *</label>
          <input type="text" id="cState" placeholder="e.g. Rajasthan">
        </div>
        <div class="form-group">
          <label>Pincode</label>
          <input type="text" id="cPincode" placeholder="302001">
        </div>
      </div>
    `, [
      { label: 'Cancel', class: 'btn-secondary', action: closeModal },
      { label: 'Add City', class: 'btn-primary', action: () => {
        const name = document.getElementById('cName').value;
        const stateName = document.getElementById('cState').value;
        if (!name || !stateName) { showToast('Fill required fields', 'error'); return; }
        state.cities.push({
          id: state.cities.length + 1,
          name, state: stateName,
          country: 'India',
          pincode: document.getElementById('cPincode').value,
        });
        closeModal();
        renderCities();
        showToast('City added', 'success');
      }},
    ]);
  }

  // ─── Modal System ───
  function openModal(title, bodyHtml, buttons) {
    document.getElementById('modalTitle').textContent = title;
    document.getElementById('modalBody').innerHTML = bodyHtml;
    const footer = document.getElementById('modalFooter');
    footer.innerHTML = '';
    (buttons || []).forEach(b => {
      const btn = document.createElement('button');
      btn.className = b.class || 'btn-secondary';
      btn.innerHTML = b.label;
      btn.onclick = b.action;
      footer.appendChild(btn);
    });
    document.getElementById('modalOverlay').classList.remove('hidden');
  }

  function closeModal() {
    document.getElementById('modalOverlay').classList.add('hidden');
  }

  // ─── Toast System ───
  function showToast(msg, type = 'info') {
    const container = document.getElementById('toastContainer');
    const icon = type === 'success' ? 'check_circle' : type === 'error' ? 'error' : 'info';
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `<span class="material-icons-round">${icon}</span> ${msg}`;
    container.appendChild(toast);
    setTimeout(() => { toast.style.opacity = '0'; setTimeout(() => toast.remove(), 300); }, 3000);
  }

  // ─── Helpers ───
  function statusBadge(status) {
    const map = {
      'ACTIVE': 'badge-green', 'NOW_SHOWING': 'badge-green', 'CONFIRMED': 'badge-green', 'COMPLETED': 'badge-green',
      'SCHEDULED': 'badge-blue', 'OPEN': 'badge-blue',
      'COMING_SOON': 'badge-yellow', 'PENDING': 'badge-yellow', 'MAINTENANCE': 'badge-yellow',
      'ENDED': 'badge-gray', 'EXPIRED': 'badge-gray', 'INACTIVE': 'badge-gray', 'FULL': 'badge-purple',
      'CANCELLED': 'badge-red', 'FAILED': 'badge-red', 'SUSPENDED': 'badge-red', 'REFUNDED': 'badge-yellow',
    };
    return `<span class="badge ${map[status] || 'badge-gray'}">${(status || '').replace(/_/g, ' ')}</span>`;
  }

  function timeAgo(dateStr) {
    const diff = Date.now() - new Date(dateStr).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'Just now';
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    const days = Math.floor(hrs / 24);
    return `${days}d ago`;
  }

  // ─── Event Bindings ───
  function bindEvents() {
    // Login
    document.getElementById('loginForm').addEventListener('submit', handleLogin);

    // Logout
    document.getElementById('logoutBtn').addEventListener('click', (e) => { e.preventDefault(); handleLogout(); });

    // Sidebar nav
    document.querySelectorAll('.nav-item').forEach(item => {
      item.addEventListener('click', (e) => {
        e.preventDefault();
        navigateTo(item.dataset.section);
        // Close mobile sidebar
        document.getElementById('sidebar').classList.remove('open');
      });
    });

    // Sidebar toggle
    document.getElementById('sidebarToggle').addEventListener('click', () => {
      document.getElementById('sidebar').classList.toggle('open');
    });

    // User dropdown
    document.getElementById('topbarUser').addEventListener('click', () => {
      document.getElementById('userDropdown').classList.toggle('hidden');
    });
    document.addEventListener('click', (e) => {
      if (!e.target.closest('.topbar-actions')) {
        document.getElementById('userDropdown').classList.add('hidden');
      }
    });

    // Modal close
    document.getElementById('modalClose').addEventListener('click', closeModal);
    document.getElementById('modalOverlay').addEventListener('click', (e) => {
      if (e.target === e.currentTarget) closeModal();
    });

    // Add buttons
    document.getElementById('addMovieBtn').addEventListener('click', () => showAddMovieModal());
    document.getElementById('addTheatreBtn').addEventListener('click', () => showAddTheatreModal());
    document.getElementById('addShowBtn').addEventListener('click', () => showAddShowModal());
    document.getElementById('addCityBtn').addEventListener('click', showAddCityModal);

    // Filters
    document.getElementById('movieStatusFilter').addEventListener('change', renderMovies);
    document.getElementById('movieSearch').addEventListener('input', renderMovies);
    document.getElementById('theatreSearch').addEventListener('input', renderTheatres);
    document.getElementById('theatreCityFilter').addEventListener('change', renderTheatres);
    document.getElementById('showDateFilter').addEventListener('change', renderShows);
    document.getElementById('bookingStatusFilter').addEventListener('change', renderBookings);
    document.getElementById('bookingSearch').addEventListener('input', renderBookings);
    document.getElementById('userSearch').addEventListener('input', renderUsers);
    document.getElementById('paymentStatusFilter').addEventListener('change', renderPayments);

    // Settings save
    document.getElementById('saveSettingsBtn')?.addEventListener('click', () => showToast('Settings saved', 'success'));
    document.getElementById('saveEndpointsBtn')?.addEventListener('click', () => {
      config.gatewayUrl = document.getElementById('settingGatewayUrl').value;
      localStorage.setItem('admin_gateway', config.gatewayUrl);
      showToast('API endpoints saved', 'success');
    });
  }

  // ─── Public API (for inline onclick) ───
  window._admin = {
    editMovie: showAddMovieModal,
    changeMovieStatus,
    deleteMovie,
    editTheatre: showAddTheatreModal,
    manageScreens,
    deleteTheatre,
    editShow: showAddShowModal,
    cancelShow,
    viewBooking,
    changeBookingStatus,
    viewUser,
    refundPayment,
  };

})();
