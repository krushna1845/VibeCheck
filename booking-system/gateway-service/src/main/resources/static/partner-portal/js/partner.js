/* =============================================
   VIBECHECK PARTNER PORTAL — JavaScript Application
   ============================================= */

(function() {
  'use strict';

  // ─── State Management ───
  const state = {
    token: localStorage.getItem('vibe_partner_token') || null,
    user: JSON.parse(localStorage.getItem('vibe_partner_user') || 'null') || {
      name: 'PVR Cinema Manager',
      email: 'owner@pvr.com',
      mobile: '+91 98765 43210',
      org: 'PVR Inox Ltd.',
      role: 'ROLE_THEATRE_OWNER'
    },
    theatres: [
      {
        id: 'th-1',
        name: 'PVR Icon: Phoenix Palladium',
        city: 'Mumbai',
        address: '462, Senapati Bapat Marg, Lower Parel',
        screensCount: 7,
        totalSeats: 1240,
        activeShows: 28,
        todayRev: 184500
      },
      {
        id: 'th-2',
        name: 'INOX Megaplex: Inorbit Mall',
        city: 'Mumbai',
        address: 'New Link Rd, Malad West',
        screensCount: 5,
        totalSeats: 980,
        activeShows: 20,
        todayRev: 132000
      },
      {
        id: 'th-3',
        name: 'PVR Superplex: Mall of India',
        city: 'Delhi-NCR',
        address: 'Sector 18, Noida',
        screensCount: 8,
        totalSeats: 1560,
        activeShows: 34,
        todayRev: 215000
      }
    ],
    screens: [
      { id: 'sc-1', theatreId: 'th-1', name: 'Audi 1 (IMAX Laser)', format: 'IMAX 3D', sound: 'Dolby Atmos 12.1', rows: 8, cols: 12, capacity: 96 },
      { id: 'sc-2', theatreId: 'th-1', name: 'Audi 2 (4DX Motion)', format: '4DX', sound: 'Dolby 7.1', rows: 6, cols: 10, capacity: 60 },
      { id: 'sc-3', theatreId: 'th-1', name: 'Audi 3 (Gold Class)', format: 'Gold Class', sound: 'Dolby Atmos', rows: 5, cols: 8, capacity: 40 },
      { id: 'sc-4', theatreId: 'th-2', name: 'Screen 1 (Laser Atmos)', format: 'Dolby Cinema', sound: 'Dolby Atmos', rows: 8, cols: 12, capacity: 96 },
      { id: 'sc-5', theatreId: 'th-2', name: 'Screen 2 (Insignia)', format: 'Insignia', sound: 'Dolby 7.1', rows: 5, cols: 8, capacity: 40 },
      { id: 'sc-6', theatreId: 'th-3', name: 'Audi 1 (P[XL] Giant)', format: 'P[XL]', sound: 'Dolby Atmos', rows: 10, cols: 14, capacity: 140 }
    ],
    movies: [
      { id: 'm-1', title: 'Jawan', lang: 'Hindi', cert: 'UA', duration: '169 min', poster: 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=300' },
      { id: 'm-2', title: 'Salaar: Part 1 - Ceasefire', lang: 'Telugu / Hindi', cert: 'A', duration: '175 min', poster: 'https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=300' },
      { id: 'm-3', title: 'Oppenheimer', lang: 'English', cert: 'UA', duration: '180 min', poster: 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=300' },
      { id: 'm-4', title: 'Fighter', lang: 'Hindi', cert: 'UA', duration: '166 min', poster: 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=300' },
      { id: 'm-5', title: 'Leo', lang: 'Tamil / Hindi', cert: 'UA', duration: '164 min', poster: 'https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=300' }
    ],
    showtimes: [
      { id: 'sh-101', movieId: 'm-1', theatreId: 'th-1', screenId: 'sc-1', date: '2026-09-26', time: '10:30 AM', lang: 'Hindi 2D', status: 'ACTIVE', bookedSeats: 72, totalSeats: 96, price: 350 },
      { id: 'sh-102', movieId: 'm-3', theatreId: 'th-1', screenId: 'sc-1', date: '2026-09-26', time: '02:15 PM', lang: 'English IMAX', status: 'ACTIVE', bookedSeats: 90, totalSeats: 96, price: 550 },
      { id: 'sh-103', movieId: 'm-2', theatreId: 'th-1', screenId: 'sc-2', date: '2026-09-26', time: '06:45 PM', lang: 'Hindi 4DX', status: 'HOUSEFULL', bookedSeats: 60, totalSeats: 60, price: 650 },
      { id: 'sh-104', movieId: 'm-4', theatreId: 'th-1', screenId: 'sc-3', date: '2026-09-26', time: '09:30 PM', lang: 'Hindi Gold', status: 'ACTIVE', bookedSeats: 28, totalSeats: 40, price: 800 },
      { id: 'sh-105', movieId: 'm-1', theatreId: 'th-2', screenId: 'sc-4', date: '2026-09-26', time: '11:00 AM', lang: 'Hindi 2D', status: 'ACTIVE', bookedSeats: 64, totalSeats: 96, price: 280 },
      { id: 'sh-106', movieId: 'm-5', theatreId: 'th-2', screenId: 'sc-4', date: '2026-09-26', time: '03:30 PM', lang: 'Tamil 2D', status: 'ACTIVE', bookedSeats: 58, totalSeats: 96, price: 280 }
    ],
    pricing: {
      silver: { weekday: 150, weekend: 200, holiday: 250 },
      gold: { weekday: 250, weekend: 320, holiday: 400 },
      platinum: { weekday: 350, weekend: 450, holiday: 550 },
      recliner: { weekday: 500, weekend: 650, holiday: 750 }
    },
    transactions: [
      { ref: 'VB-8921', movie: 'Jawan', seats: 'B4, B5', amount: '₹700', payment: 'UPI (PhonePe)', time: '10 mins ago' },
      { ref: 'VB-8920', movie: 'Oppenheimer', seats: 'E7, E8, E9', amount: '₹1,650', payment: 'Credit Card', time: '24 mins ago' },
      { ref: 'VB-8919', movie: 'Fighter', seats: 'A1, A2', amount: '₹1,600', payment: 'Box Office (Cash)', time: '45 mins ago' },
      { ref: 'VB-8918', movie: 'Salaar', seats: 'D3', amount: '₹650', payment: 'NetBanking', time: '1 hr ago' },
      { ref: 'VB-8917', movie: 'Jawan', seats: 'C5, C6, C7, C8', amount: '₹1,400', payment: 'UPI (GPay)', time: '2 hrs ago' }
    ],
    selectedScreen: null,
    boSelectedShow: null,
    boSelectedSeats: []
  };

  // ─── DOM References ───
  const els = {
    loginScreen: document.getElementById('loginScreen'),
    appShell: document.getElementById('appShell'),
    loginForm: document.getElementById('loginForm'),
    loginEmail: document.getElementById('loginEmail'),
    loginPassword: document.getElementById('loginPassword'),
    sidebar: document.getElementById('sidebar'),
    sidebarToggle: document.getElementById('sidebarToggle'),
    navItems: document.querySelectorAll('.nav-item'),
    sections: document.querySelectorAll('.section'),
    topbarUser: document.getElementById('topbarUser'),
    userDropdown: document.getElementById('userDropdown'),
    logoutBtn: document.getElementById('logoutBtn'),
    partnerName: document.getElementById('partnerName'),
    dashDate: document.getElementById('dashDate'),
    modalOverlay: document.getElementById('modalOverlay'),
    modal: document.getElementById('modal'),
    modalTitle: document.getElementById('modalTitle'),
    modalBody: document.getElementById('modalBody'),
    modalFooter: document.getElementById('modalFooter'),
    modalClose: document.getElementById('modalClose'),
    toastContainer: document.getElementById('toastContainer')
  };

  // ─── Initialization ───
  function init() {
    bindEvents();
    if (state.token) {
      showApp();
    } else {
      showLogin();
    }
  }

  // ─── Event Binding ───
  function bindEvents() {
    // Login form
    if (els.loginForm) {
      els.loginForm.addEventListener('submit', handleLogin);
    }

    // Sidebar Toggle
    if (els.sidebarToggle) {
      els.sidebarToggle.addEventListener('click', () => {
        if (window.innerWidth <= 768) {
          els.sidebar.classList.toggle('mobile-open');
        } else {
          els.sidebar.classList.toggle('collapsed');
        }
      });
    }

    // User dropdown
    if (els.topbarUser) {
      els.topbarUser.addEventListener('click', (e) => {
        e.stopPropagation();
        els.userDropdown.classList.toggle('hidden');
      });
    }
    document.addEventListener('click', () => {
      if (els.userDropdown && !els.userDropdown.classList.contains('hidden')) {
        els.userDropdown.classList.add('hidden');
      }
    });

    // Logout
    if (els.logoutBtn) {
      els.logoutBtn.addEventListener('click', (e) => {
        e.preventDefault();
        handleLogout();
      });
    }

    // Navigation
    els.navItems.forEach(item => {
      item.addEventListener('click', (e) => {
        e.preventDefault();
        const sectionId = item.getAttribute('data-section');
        switchSection(sectionId);
        if (window.innerWidth <= 768 && els.sidebar) {
          els.sidebar.classList.remove('mobile-open');
        }
      });
    });

    // Modal close
    if (els.modalClose) {
      els.modalClose.addEventListener('click', closeModal);
    }
    if (els.modalOverlay) {
      els.modalOverlay.addEventListener('click', (e) => {
        if (e.target === els.modalOverlay) closeModal();
      });
    }

    // Section specific button hooks
    bindSectionButtons();
  }

  function bindSectionButtons() {
    // Register Theatre button
    const addTheatreBtn = document.getElementById('addTheatreBtn');
    if (addTheatreBtn) {
      addTheatreBtn.addEventListener('click', openAddTheatreModal);
    }

    // Add Screen button
    const addScreenBtn = document.getElementById('addScreenBtn');
    if (addScreenBtn) {
      addScreenBtn.addEventListener('click', openAddScreenModal);
    }

    // Screen Theatre Filter
    const screenTheatreFilter = document.getElementById('screenTheatreFilter');
    if (screenTheatreFilter) {
      screenTheatreFilter.addEventListener('change', (e) => {
        renderScreensGrid(e.target.value);
      });
    }

    // Add Showtime button
    const addShowtimeBtn = document.getElementById('addShowtimeBtn');
    if (addShowtimeBtn) {
      addShowtimeBtn.addEventListener('click', openAddShowtimeModal);
    }

    // Showtime Filters
    const showtimeTheatreFilter = document.getElementById('showtimeTheatreFilter');
    if (showtimeTheatreFilter) {
      showtimeTheatreFilter.addEventListener('change', renderShowtimesTable);
    }
    const showtimeDateFilter = document.getElementById('showtimeDateFilter');
    if (showtimeDateFilter) {
      showtimeDateFilter.value = new Date().toISOString().split('T')[0];
      showtimeDateFilter.addEventListener('change', renderShowtimesTable);
    }

    // Save Pricing button
    const savePricingBtn = document.getElementById('savePricingBtn');
    if (savePricingBtn) {
      savePricingBtn.addEventListener('click', savePricing);
    }

    // Box Office Show Select
    const boShowSelect = document.getElementById('boShowSelect');
    if (boShowSelect) {
      boShowSelect.addEventListener('change', handleBoShowSelect);
    }

    // Box Office Confirm
    const boConfirmBtn = document.getElementById('boConfirmBtn');
    if (boConfirmBtn) {
      boConfirmBtn.addEventListener('click', handleBoConfirm);
    }

    // Save Profile
    const saveProfileBtn = document.getElementById('saveProfileBtn');
    if (saveProfileBtn) {
      saveProfileBtn.addEventListener('click', saveProfile);
    }

    // Revenue Period change
    const revPeriod = document.getElementById('revPeriod');
    if (revPeriod) {
      revPeriod.addEventListener('change', renderRevenue);
    }
  }

  // ─── Authentication ───
  function handleLogin(e) {
    e.preventDefault();
    const email = els.loginEmail.value.trim();
    const password = els.loginPassword.value;

    if (!email || !password) {
      showToast('Please provide email and password', 'error');
      return;
    }

    // Set demo token
    const token = 'partner_jwt_' + Math.random().toString(36).substring(2);
    state.token = token;
    state.user.email = email;
    state.user.name = email.split('@')[0].toUpperCase() + ' Operations';

    localStorage.setItem('vibe_partner_token', token);
    localStorage.setItem('vibe_partner_user', JSON.stringify(state.user));

    showToast('Signed in successfully as Theatre Partner', 'success');
    showApp();
  }

  function handleLogout() {
    state.token = null;
    localStorage.removeItem('vibe_partner_token');
    showToast('Signed out of partner portal', 'info');
    showLogin();
  }

  function showLogin() {
    if (els.loginScreen) els.loginScreen.classList.remove('hidden');
    if (els.appShell) els.appShell.classList.add('hidden');
  }

  function showApp() {
    if (els.loginScreen) els.loginScreen.classList.add('hidden');
    if (els.appShell) els.appShell.classList.remove('hidden');

    if (els.partnerName) els.partnerName.textContent = state.user.name;
    if (els.dashDate) {
      const options = { weekday: 'long', year: 'numeric', month: 'short', day: 'numeric' };
      els.dashDate.textContent = new Date().toLocaleDateString('en-IN', options);
    }

    // Populate theatre filter dropdowns
    populateTheatreFilters();

    // Render initial active section
    switchSection('dashboard');
  }

  function switchSection(sectionId) {
    els.navItems.forEach(item => {
      if (item.getAttribute('data-section') === sectionId) {
        item.classList.add('active');
      } else {
        item.classList.remove('active');
      }
    });

    els.sections.forEach(sec => {
      if (sec.id === 'sec-' + sectionId) {
        sec.classList.add('active');
      } else {
        sec.classList.remove('active');
      }
    });

    // Refresh section data
    switch (sectionId) {
      case 'dashboard':
        renderDashboard();
        break;
      case 'my-theatres':
        renderTheatresGrid();
        break;
      case 'screens':
        populateTheatreFilters();
        const curTheatre = document.getElementById('screenTheatreFilter').value;
        renderScreensGrid(curTheatre);
        break;
      case 'showtimes':
        populateTheatreFilters();
        renderShowtimesTable();
        break;
      case 'seat-pricing':
        loadPricingValues();
        break;
      case 'box-office':
        populateBoShows();
        break;
      case 'revenue':
        renderRevenue();
        break;
      case 'profile':
        loadProfileData();
        break;
    }
  }

  // ─── Filter Dropdowns ───
  function populateTheatreFilters() {
    const filters = [
      document.getElementById('screenTheatreFilter'),
      document.getElementById('showtimeTheatreFilter')
    ];

    filters.forEach(sel => {
      if (!sel) return;
      const curVal = sel.value;
      const isScreenFilter = (sel.id === 'screenTheatreFilter');
      sel.innerHTML = isScreenFilter ? '<option value="">Select Theatre</option>' : '<option value="">All Theatres</option>';

      state.theatres.forEach(th => {
        const opt = document.createElement('option');
        opt.value = th.id;
        opt.textContent = th.name + ' (' + th.city + ')';
        sel.appendChild(opt);
      });

      if (curVal && state.theatres.some(t => t.id === curVal)) {
        sel.value = curVal;
      } else if (isScreenFilter && state.theatres.length > 0) {
        sel.value = state.theatres[0].id;
      }
    });
  }

  // ─── Dashboard ───
  function renderDashboard() {
    const totalScreens = state.screens.length;
    const totalShowsToday = state.showtimes.length;
    const totalTheatres = state.theatres.length;
    let totalBooked = 0;
    let totalCapacity = 0;

    state.showtimes.forEach(s => {
      totalBooked += s.bookedSeats;
      totalCapacity += s.totalSeats;
    });

    const avgOccupancy = totalCapacity > 0 ? Math.round((totalBooked / totalCapacity) * 100) : 0;
    const todayRevenue = state.showtimes.reduce((acc, s) => acc + (s.bookedSeats * s.price), 0);

    const kpiTheatres = document.getElementById('kpiTheatres');
    const kpiScreens = document.getElementById('kpiScreens');
    const kpiShows = document.getElementById('kpiShows');
    const kpiOccupancy = document.getElementById('kpiOccupancy');
    const kpiTickets = document.getElementById('kpiTickets');
    const kpiDailyRev = document.getElementById('kpiDailyRev');

    if (kpiTheatres) kpiTheatres.textContent = totalTheatres;
    if (kpiScreens) kpiScreens.textContent = totalScreens;
    if (kpiShows) kpiShows.textContent = totalShowsToday;
    if (kpiOccupancy) kpiOccupancy.textContent = avgOccupancy + '%';
    if (kpiTickets) kpiTickets.textContent = totalBooked.toLocaleString('en-IN');
    if (kpiDailyRev) kpiDailyRev.textContent = '₹' + todayRevenue.toLocaleString('en-IN');

    // Revenue Bar Chart
    renderDashboardChart();

    // Screen Occupancy List
    renderOccupancyList();

    // Today's Schedule Timeline
    renderDashboardTimeline();
  }

  function renderDashboardChart() {
    const container = document.getElementById('revenueChart');
    if (!container) return;

    const days = [
      { label: 'Mon', rev: 145000 },
      { label: 'Tue', rev: 162000 },
      { label: 'Wed', rev: 138000 },
      { label: 'Thu', rev: 195000 },
      { label: 'Fri', rev: 280000 },
      { label: 'Sat', rev: 410000 },
      { label: 'Sun', rev: 375000 }
    ];

    const maxRev = Math.max(...days.map(d => d.rev));

    container.innerHTML = days.map(d => {
      const heightPercent = Math.round((d.rev / maxRev) * 100);
      return `
        <div class="bar-group">
          <div class="bar-wrapper">
            <div class="bar-fill" style="height: ${heightPercent}%;" data-tooltip="₹${(d.rev/1000).toFixed(0)}k"></div>
          </div>
          <span class="bar-label">${d.label}</span>
        </div>
      `;
    }).join('');
  }

  function renderOccupancyList() {
    const container = document.getElementById('occupancyList');
    if (!container) return;

    container.innerHTML = state.screens.slice(0, 4).map(sc => {
      const relatedShows = state.showtimes.filter(s => s.screenId === sc.id);
      let occ = 75;
      if (relatedShows.length > 0) {
        const booked = relatedShows.reduce((a, s) => a + s.bookedSeats, 0);
        const cap = relatedShows.reduce((a, s) => a + s.totalSeats, 0);
        occ = cap > 0 ? Math.round((booked / cap) * 100) : 0;
      }
      return `
        <div class="occ-item">
          <div class="occ-header">
            <span>${sc.name}</span>
            <b>${occ}%</b>
          </div>
          <div class="occ-bar">
            <div class="occ-fill" style="width: ${occ}%; background: ${occ > 80 ? 'var(--accent)' : 'var(--primary)'}"></div>
          </div>
        </div>
      `;
    }).join('');
  }

  function renderDashboardTimeline() {
    const container = document.getElementById('todayTimeline');
    if (!container) return;

    if (state.showtimes.length === 0) {
      container.innerHTML = '<div class="empty-state">No showtimes scheduled for today.</div>';
      return;
    }

    container.innerHTML = state.showtimes.map(st => {
      const movie = state.movies.find(m => m.id === st.movieId) || { title: 'Unknown Movie' };
      const screen = state.screens.find(sc => sc.id === st.screenId) || { name: 'Screen' };
      const theatre = state.theatres.find(th => th.id === st.theatreId) || { name: 'Theatre' };
      const statusBadge = st.status === 'HOUSEFULL' ? 'badge-red' : 'badge-green';

      return `
        <div class="timeline-item">
          <span class="tl-time">${st.time}</span>
          <div class="tl-info">
            <div class="tl-movie">${movie.title} <span class="badge ${statusBadge}">${st.status}</span></div>
            <div class="tl-screen">${theatre.name} — ${screen.name} (${st.lang})</div>
          </div>
          <div style="text-align: right">
            <span style="font-size: 13px; font-weight: 700; color: var(--white);">${st.bookedSeats}/${st.totalSeats} booked</span>
            <div style="font-size: 12px; color: var(--text-muted);">₹${st.price} / seat</div>
          </div>
        </div>
      `;
    }).join('');
  }

  // ─── My Theatres ───
  function renderTheatresGrid() {
    const container = document.getElementById('theatresGrid');
    if (!container) return;

    if (state.theatres.length === 0) {
      container.innerHTML = '<div class="empty-state">No theatres registered yet. Click "Register Theatre" to add one.</div>';
      return;
    }

    container.innerHTML = state.theatres.map(th => `
      <div class="theatre-card">
        <div class="theatre-card-header">
          <div>
            <div class="theatre-card-title">${th.name}</div>
            <div class="theatre-card-city">
              <span class="material-icons-round">place</span> ${th.city} • ${th.address}
            </div>
          </div>
          <span class="badge badge-cyan">ACTIVE</span>
        </div>
        <div class="theatre-card-body">
          <div class="theatre-stat-row">
            <span class="theatre-stat-label">Total Screens</span>
            <span class="theatre-stat-val">${th.screensCount}</span>
          </div>
          <div class="theatre-stat-row">
            <span class="theatre-stat-label">Total Seating Capacity</span>
            <span class="theatre-stat-val">${th.totalSeats.toLocaleString('en-IN')} seats</span>
          </div>
          <div class="theatre-stat-row">
            <span class="theatre-stat-label">Active Shows Today</span>
            <span class="theatre-stat-val">${th.activeShows} shows</span>
          </div>
          <div class="theatre-stat-row">
            <span class="theatre-stat-label">Estimated Day Revenue</span>
            <span class="theatre-stat-val" style="color:var(--accent);">₹${th.todayRev.toLocaleString('en-IN')}</span>
          </div>
        </div>
        <div class="theatre-card-footer">
          <button class="btn-secondary" style="flex:1" onclick="window.vibePartner.viewScreens('${th.id}')">
            <span class="material-icons-round">tv</span> Screens
          </button>
          <button class="btn-secondary" style="flex:1" onclick="window.vibePartner.viewShowtimes('${th.id}')">
            <span class="material-icons-round">schedule</span> Shows
          </button>
          <button class="btn-danger" onclick="window.vibePartner.deleteTheatre('${th.id}')" title="Delete Theatre">
            <span class="material-icons-round">delete</span>
          </button>
        </div>
      </div>
    `).join('');
  }

  function openAddTheatreModal() {
    openModal('Register New Theatre', `
      <div class="form-group">
        <label>Theatre / Multiplex Name</label>
        <input type="text" id="mThName" placeholder="e.g. PVR Gold Cinemas" required>
      </div>
      <div class="form-group">
        <label>City</label>
        <input type="text" id="mThCity" placeholder="e.g. Mumbai, Bengaluru, Delhi" required>
      </div>
      <div class="form-group">
        <label>Full Address</label>
        <input type="text" id="mThAddress" placeholder="e.g. Phoenix Marketcity, Kurla West" required>
      </div>
      <div class="form-group">
        <label>Number of Screens</label>
        <input type="number" id="mThScreens" value="4" min="1" max="20" required>
      </div>
    `, `
      <button class="btn-secondary" onclick="window.vibePartner.closeModal()">Cancel</button>
      <button class="btn-primary" onclick="window.vibePartner.confirmAddTheatre()">Register Theatre</button>
    `);
  }

  function confirmAddTheatre() {
    const name = document.getElementById('mThName').value.trim();
    const city = document.getElementById('mThCity').value.trim();
    const address = document.getElementById('mThAddress').value.trim();
    const screensCount = parseInt(document.getElementById('mThScreens').value) || 1;

    if (!name || !city || !address) {
      showToast('Please fill out all theatre fields', 'error');
      return;
    }

    const newId = 'th-' + Date.now();
    const newTheatre = {
      id: newId,
      name,
      city,
      address,
      screensCount,
      totalSeats: screensCount * 120,
      activeShows: 0,
      todayRev: 0
    };

    state.theatres.push(newTheatre);
    closeModal();
    showToast(`Theatre "${name}" registered successfully!`, 'success');
    populateTheatreFilters();
    renderTheatresGrid();
  }

  function deleteTheatre(theatreId) {
    if (!confirm('Are you sure you want to remove this theatre and its associated screens?')) return;
    state.theatres = state.theatres.filter(t => t.id !== theatreId);
    state.screens = state.screens.filter(s => s.theatreId !== theatreId);
    showToast('Theatre deleted', 'info');
    populateTheatreFilters();
    renderTheatresGrid();
  }

  // ─── Screens & Interactive Seat Layout ───
  function renderScreensGrid(theatreId) {
    const container = document.getElementById('screensGrid');
    const layoutArea = document.getElementById('seatLayoutArea');
    if (!container) return;

    if (!theatreId) {
      container.innerHTML = '<div class="empty-state">Please select a theatre above to view and configure screens.</div>';
      if (layoutArea) layoutArea.classList.add('hidden');
      return;
    }

    const filtered = state.screens.filter(s => s.theatreId === theatreId);

    if (filtered.length === 0) {
      container.innerHTML = '<div class="empty-state">No screens configured for this theatre. Click "Add Screen" to create one.</div>';
      if (layoutArea) layoutArea.classList.add('hidden');
      return;
    }

    container.innerHTML = filtered.map(sc => `
      <div class="screen-card ${state.selectedScreen === sc.id ? 'selected' : ''}" onclick="window.vibePartner.selectScreen('${sc.id}')">
        <div class="screen-card-header">
          <span class="screen-name">${sc.name}</span>
          <span class="screen-type-badge">${sc.format}</span>
        </div>
        <div class="screen-meta">
          <span><span class="material-icons-round">event_seat</span> ${sc.capacity} Seats</span>
          <span><span class="material-icons-round">grid_view</span> ${sc.rows}×${sc.cols}</span>
          <span><span class="material-icons-round">speaker</span> ${sc.sound}</span>
        </div>
      </div>
    `).join('');

    // If a screen is selected, render layout
    if (state.selectedScreen && filtered.some(s => s.id === state.selectedScreen)) {
      renderSeatLayout(state.selectedScreen);
    } else if (filtered.length > 0) {
      selectScreen(filtered[0].id);
    }
  }

  function selectScreen(screenId) {
    state.selectedScreen = screenId;
    const curTheatre = document.getElementById('screenTheatreFilter').value;
    renderScreensGrid(curTheatre);
    renderSeatLayout(screenId);
  }

  function renderSeatLayout(screenId) {
    const layoutArea = document.getElementById('seatLayoutArea');
    const container = document.getElementById('seatLayout');
    const layoutScreenName = document.getElementById('layoutScreenName');
    if (!layoutArea || !container) return;

    const screen = state.screens.find(s => s.id === screenId);
    if (!screen) return;

    layoutArea.classList.remove('hidden');
    if (layoutScreenName) layoutScreenName.textContent = screen.name + ' (' + screen.format + ')';

    const rowLetters = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L'];
    let html = '<div class="screen-curved-bar"></div><div class="seat-matrix">';

    for (let r = 0; r < screen.rows; r++) {
      const rowName = rowLetters[r] || ('R' + (r + 1));
      let tier = 'silver';
      if (r >= screen.rows - 1) tier = 'recliner';
      else if (r >= screen.rows - 3) tier = 'platinum';
      else if (r >= screen.rows - 5) tier = 'gold';

      html += `<div class="seat-row"><span class="seat-row-label">${rowName}</span>`;

      for (let c = 1; c <= screen.cols; c++) {
        // Add aisle gap in the middle
        if (c === Math.floor(screen.cols / 2) + 1) {
          html += `<div class="seat-cell aisle"></div>`;
        }
        const seatId = `${rowName}${c}`;
        html += `
          <div class="seat-cell ${tier}" title="${seatId} (${tier.toUpperCase()})" onclick="window.vibePartner.toggleSeatSeat('${seatId}', this)">
            ${c}
          </div>
        `;
      }
      html += `</div>`;
    }

    html += '</div>';
    container.innerHTML = html;
  }

  function toggleSeatSeat(seatId, el) {
    if (el.classList.contains('inactive')) {
      el.classList.remove('inactive');
      showToast(`Seat ${seatId} enabled`, 'info');
    } else {
      el.classList.add('inactive');
      showToast(`Seat ${seatId} disabled for maintenance`, 'info');
    }
  }

  function openAddScreenModal() {
    const curTheatre = document.getElementById('screenTheatreFilter').value;
    if (!curTheatre) {
      showToast('Please select a theatre first', 'error');
      return;
    }

    openModal('Add New Screen', `
      <div class="form-group">
        <label>Screen / Audi Name</label>
        <input type="text" id="mScName" placeholder="e.g. Audi 4 (Dolby Atmos)" required>
      </div>
      <div class="form-group">
        <label>Screen Format</label>
        <select id="mScFormat">
          <option value="IMAX 3D">IMAX 3D</option>
          <option value="Dolby Atmos" selected>Dolby Atmos</option>
          <option value="4DX">4DX Motion</option>
          <option value="Gold Class">Gold Class</option>
          <option value="Standard 2D">Standard 2D</option>
        </select>
      </div>
      <div class="form-group">
        <label>Sound System</label>
        <input type="text" id="mScSound" value="Dolby Atmos 7.1" required>
      </div>
      <div style="display:flex;gap:12px">
        <div class="form-group" style="flex:1">
          <label>Rows</label>
          <input type="number" id="mScRows" value="8" min="3" max="20" required>
        </div>
        <div class="form-group" style="flex:1">
          <label>Seats per Row</label>
          <input type="number" id="mScCols" value="12" min="4" max="25" required>
        </div>
      </div>
    `, `
      <button class="btn-secondary" onclick="window.vibePartner.closeModal()">Cancel</button>
      <button class="btn-primary" onclick="window.vibePartner.confirmAddScreen()">Create Screen</button>
    `);
  }

  function confirmAddScreen() {
    const curTheatre = document.getElementById('screenTheatreFilter').value;
    const name = document.getElementById('mScName').value.trim();
    const format = document.getElementById('mScFormat').value;
    const sound = document.getElementById('mScSound').value.trim();
    const rows = parseInt(document.getElementById('mScRows').value) || 8;
    const cols = parseInt(document.getElementById('mScCols').value) || 12;

    if (!name || !sound) {
      showToast('Please fill out all screen fields', 'error');
      return;
    }

    const newScreen = {
      id: 'sc-' + Date.now(),
      theatreId: curTheatre,
      name,
      format,
      sound,
      rows,
      cols,
      capacity: rows * cols
    };

    state.screens.push(newScreen);
    closeModal();
    showToast(`Screen "${name}" added successfully!`, 'success');
    renderScreensGrid(curTheatre);
    selectScreen(newScreen.id);
  }

  // ─── Showtimes Management ───
  function renderShowtimesTable() {
    const tbody = document.getElementById('showtimesBody');
    const theatreFilter = document.getElementById('showtimeTheatreFilter').value;
    const dateFilter = document.getElementById('showtimeDateFilter').value;

    if (!tbody) return;

    let filtered = state.showtimes;
    if (theatreFilter) {
      filtered = filtered.filter(st => st.theatreId === theatreFilter);
    }
    if (dateFilter) {
      filtered = filtered.filter(st => st.date === dateFilter);
    }

    if (filtered.length === 0) {
      tbody.innerHTML = '<tr><td colspan="9" class="empty-state">No showtimes found for the selected filter. Click "Add Showtime" to create one.</td></tr>';
      return;
    }

    tbody.innerHTML = filtered.map(st => {
      const movie = state.movies.find(m => m.id === st.movieId) || { title: 'Unknown Movie' };
      const theatre = state.theatres.find(t => t.id === st.theatreId) || { name: 'Theatre' };
      const screen = state.screens.find(s => s.id === st.screenId) || { name: 'Screen' };
      const statusBadge = st.status === 'HOUSEFULL' ? 'badge-red' : (st.status === 'CANCELLED' ? 'badge-orange' : 'badge-green');

      return `
        <tr>
          <td><b>${movie.title}</b></td>
          <td>${theatre.name}</td>
          <td>${screen.name}</td>
          <td>${st.date}</td>
          <td><span style="color:var(--primary);font-weight:600">${st.time}</span></td>
          <td>${st.lang}</td>
          <td><span class="badge ${statusBadge}">${st.status}</span></td>
          <td><b>${st.bookedSeats}</b> / ${st.totalSeats}</td>
          <td>
            <div style="display:flex;gap:6px">
              <button class="btn-secondary" style="padding:4px 8px;font-size:11px" onclick="window.vibePartner.toggleShowStatus('${st.id}')">Toggle Status</button>
              <button class="btn-danger" style="padding:4px 8px;font-size:11px" onclick="window.vibePartner.deleteShowtime('${st.id}')">Delete</button>
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  function openAddShowtimeModal() {
    const movieOptions = state.movies.map(m => `<option value="${m.id}">${m.title} (${m.lang})</option>`).join('');
    const theatreOptions = state.theatres.map(t => `<option value="${t.id}">${t.name}</option>`).join('');

    openModal('Schedule New Showtime', `
      <div class="form-group">
        <label>Select Movie</label>
        <select id="mStMovie">${movieOptions}</select>
      </div>
      <div class="form-group">
        <label>Select Theatre</label>
        <select id="mStTheatre" onchange="window.vibePartner.populateModalScreens(this.value)">${theatreOptions}</select>
      </div>
      <div class="form-group">
        <label>Select Screen</label>
        <select id="mStScreen"></select>
      </div>
      <div style="display:flex;gap:12px">
        <div class="form-group" style="flex:1">
          <label>Date</label>
          <input type="date" id="mStDate" value="${new Date().toISOString().split('T')[0]}" required>
        </div>
        <div class="form-group" style="flex:1">
          <label>Showtime</label>
          <input type="time" id="mStTime" value="18:30" required>
        </div>
      </div>
      <div style="display:flex;gap:12px">
        <div class="form-group" style="flex:1">
          <label>Language & Format</label>
          <input type="text" id="mStLang" value="Hindi 2D" required>
        </div>
        <div class="form-group" style="flex:1">
          <label>Base Price (₹)</label>
          <input type="number" id="mStPrice" value="250" min="50" max="2000" required>
        </div>
      </div>
    `, `
      <button class="btn-secondary" onclick="window.vibePartner.closeModal()">Cancel</button>
      <button class="btn-primary" onclick="window.vibePartner.confirmAddShowtime()">Schedule Show</button>
    `);

    if (state.theatres.length > 0) {
      populateModalScreens(state.theatres[0].id);
    }
  }

  function populateModalScreens(theatreId) {
    const screenSel = document.getElementById('mStScreen');
    if (!screenSel) return;
    const screens = state.screens.filter(s => s.theatreId === theatreId);
    screenSel.innerHTML = screens.map(s => `<option value="${s.id}">${s.name} (${s.capacity} seats)</option>`).join('');
  }

  function confirmAddShowtime() {
    const movieId = document.getElementById('mStMovie').value;
    const theatreId = document.getElementById('mStTheatre').value;
    const screenId = document.getElementById('mStScreen').value;
    const date = document.getElementById('mStDate').value;
    const rawTime = document.getElementById('mStTime').value;
    const lang = document.getElementById('mStLang').value.trim();
    const price = parseInt(document.getElementById('mStPrice').value) || 250;

    if (!movieId || !theatreId || !screenId || !date || !rawTime) {
      showToast('Please fill out all showtime details', 'error');
      return;
    }

    const screen = state.screens.find(s => s.id === screenId);
    const capacity = screen ? screen.capacity : 80;

    // Format time to 12hr AM/PM
    const [h, m] = rawTime.split(':');
    const hour = parseInt(h);
    const ampm = hour >= 12 ? 'PM' : 'AM';
    const formattedHour = hour % 12 || 12;
    const timeFormatted = `${formattedHour}:${m} ${ampm}`;

    const newShow = {
      id: 'sh-' + Date.now(),
      movieId,
      theatreId,
      screenId,
      date,
      time: timeFormatted,
      lang,
      status: 'ACTIVE',
      bookedSeats: 0,
      totalSeats: capacity,
      price
    };

    state.showtimes.unshift(newShow);
    closeModal();
    showToast('Showtime scheduled successfully!', 'success');
    renderShowtimesTable();
  }

  function toggleShowStatus(showId) {
    const show = state.showtimes.find(s => s.id === showId);
    if (!show) return;
    if (show.status === 'ACTIVE') show.status = 'HOUSEFULL';
    else if (show.status === 'HOUSEFULL') show.status = 'CANCELLED';
    else show.status = 'ACTIVE';

    showToast(`Show status updated to ${show.status}`, 'info');
    renderShowtimesTable();
  }

  function deleteShowtime(showId) {
    if (!confirm('Are you sure you want to cancel and remove this showtime?')) return;
    state.showtimes = state.showtimes.filter(s => s.id !== showId);
    showToast('Showtime deleted', 'info');
    renderShowtimesTable();
  }

  // ─── Seat Pricing ───
  function loadPricingValues() {
    const p = state.pricing;
    const map = {
      priceSilverWd: p.silver.weekday,
      priceSilverWe: p.silver.weekend,
      priceSilverHol: p.silver.holiday,
      priceGoldWd: p.gold.weekday,
      priceGoldWe: p.gold.weekend,
      priceGoldHol: p.gold.holiday,
      pricePlatWd: p.platinum.weekday,
      pricePlatWe: p.platinum.weekend,
      pricePlatHol: p.platinum.holiday,
      priceRecWd: p.recliner.weekday,
      priceRecWe: p.recliner.weekend,
      priceRecHol: p.recliner.holiday
    };

    for (const [id, val] of Object.entries(map)) {
      const el = document.getElementById(id);
      if (el) el.value = val;
    }
  }

  function savePricing() {
    state.pricing.silver.weekday = parseInt(document.getElementById('priceSilverWd').value) || 150;
    state.pricing.silver.weekend = parseInt(document.getElementById('priceSilverWe').value) || 200;
    state.pricing.silver.holiday = parseInt(document.getElementById('priceSilverHol').value) || 250;

    state.pricing.gold.weekday = parseInt(document.getElementById('priceGoldWd').value) || 250;
    state.pricing.gold.weekend = parseInt(document.getElementById('priceGoldWe').value) || 320;
    state.pricing.gold.holiday = parseInt(document.getElementById('priceGoldHol').value) || 400;

    state.pricing.platinum.weekday = parseInt(document.getElementById('pricePlatWd').value) || 350;
    state.pricing.platinum.weekend = parseInt(document.getElementById('pricePlatWe').value) || 450;
    state.pricing.platinum.holiday = parseInt(document.getElementById('pricePlatHol').value) || 550;

    state.pricing.recliner.weekday = parseInt(document.getElementById('priceRecWd').value) || 500;
    state.pricing.recliner.weekend = parseInt(document.getElementById('priceRecWe').value) || 650;
    state.pricing.recliner.holiday = parseInt(document.getElementById('priceRecHol').value) || 750;

    showToast('Seat pricing tier matrix saved and updated live across portals!', 'success');
  }

  // ─── Box Office (Walk-in POS) ───
  function populateBoShows() {
    const select = document.getElementById('boShowSelect');
    if (!select) return;

    select.innerHTML = '<option value="">Choose a show...</option>';
    state.showtimes.forEach(st => {
      const movie = state.movies.find(m => m.id === st.movieId) || { title: 'Movie' };
      const theatre = state.theatres.find(t => t.id === st.theatreId) || { name: 'Theatre' };
      const screen = state.screens.find(s => s.id === st.screenId) || { name: 'Screen' };

      const opt = document.createElement('option');
      opt.value = st.id;
      opt.textContent = `${movie.title} • ${st.time} • ${screen.name} (${theatre.name})`;
      select.appendChild(opt);
    });

    state.boSelectedShow = null;
    state.boSelectedSeats = [];
    updateBoSummary();
  }

  function handleBoShowSelect(e) {
    const showId = e.target.value;
    const seatsContainer = document.getElementById('boSeats');
    state.boSelectedSeats = [];

    if (!showId) {
      state.boSelectedShow = null;
      if (seatsContainer) seatsContainer.innerHTML = '<p class="text-muted">Select a show first</p>';
      updateBoSummary();
      return;
    }

    const show = state.showtimes.find(s => s.id === showId);
    state.boSelectedShow = show;

    const screen = state.screens.find(sc => sc.id === show.screenId);
    if (!screen || !seatsContainer) return;

    // Render POS interactive seat grid
    const rows = ['A', 'B', 'C', 'D', 'E'];
    const cols = 8;
    let html = '<div style="font-size:12px;color:var(--text-muted);margin-bottom:12px">Click available seats to select for walk-in guest</div>';
    html += '<div class="screen-curved-bar" style="margin-bottom:20px"></div>';

    rows.forEach((r, rIdx) => {
      html += `<div class="seat-row"><span class="seat-row-label">${r}</span>`;
      for (let c = 1; c <= cols; c++) {
        const seatNum = `${r}${c}`;
        // Deterministic booked state
        const isBooked = (rIdx === 1 && (c === 3 || c === 4)) || (rIdx === 3 && c === 5);
        if (isBooked) {
          html += `<div class="seat-cell booked" title="${seatNum} (Already Booked)">${c}</div>`;
        } else {
          html += `<div class="seat-cell silver" data-seat="${seatNum}" onclick="window.vibePartner.toggleBoSeat('${seatNum}', this)">${c}</div>`;
        }
      }
      html += `</div>`;
    });

    seatsContainer.innerHTML = html;
    updateBoSummary();
  }

  function toggleBoSeat(seatNum, el) {
    if (el.classList.contains('selected')) {
      el.classList.remove('selected');
      state.boSelectedSeats = state.boSelectedSeats.filter(s => s !== seatNum);
    } else {
      el.classList.add('selected');
      state.boSelectedSeats.push(seatNum);
    }
    updateBoSummary();
  }

  function updateBoSummary() {
    const container = document.getElementById('boSummary');
    const confirmBtn = document.getElementById('boConfirmBtn');
    if (!container) return;

    if (!state.boSelectedShow || state.boSelectedSeats.length === 0) {
      container.innerHTML = '<p class="text-muted" style="text-align:center;padding:20px 0;">No seats selected</p>';
      if (confirmBtn) confirmBtn.disabled = true;
      return;
    }

    const show = state.boSelectedShow;
    const movie = state.movies.find(m => m.id === show.movieId) || { title: 'Movie' };
    const unitPrice = show.price || 250;
    const subtotal = state.boSelectedSeats.length * unitPrice;
    const gst = Math.round(subtotal * 0.18);
    const grandTotal = subtotal + gst;

    container.innerHTML = `
      <div style="font-weight:700;color:var(--white);margin-bottom:10px">${movie.title}</div>
      <div class="bo-summary-row">
        <span>Selected Seats (${state.boSelectedSeats.length})</span>
        <b style="color:var(--primary)">${state.boSelectedSeats.join(', ')}</b>
      </div>
      <div class="bo-summary-row">
        <span>Base Ticket Price</span>
        <span>₹${unitPrice} × ${state.boSelectedSeats.length}</span>
      </div>
      <div class="bo-summary-row">
        <span>GST (18%)</span>
        <span>₹${gst}</span>
      </div>
      <div class="bo-summary-row total">
        <span>Total Payable</span>
        <span style="color:var(--accent)">₹${grandTotal}</span>
      </div>
      <div style="margin-top:14px">
        <label style="font-size:11px;color:var(--text-muted);display:block;margin-bottom:6px">PAYMENT METHOD</label>
        <select id="boPayMethod" style="width:100%;padding:8px;background:var(--bg-input);border:1px solid var(--border);border-radius:6px;color:var(--white);font-size:12px;">
          <option value="Cash">Cash at Counter</option>
          <option value="UPI">UPI / QR Code</option>
          <option value="POS Card">Credit / Debit Card (EDC)</option>
        </select>
      </div>
    `;

    if (confirmBtn) confirmBtn.disabled = false;
  }

  function handleBoConfirm() {
    if (!state.boSelectedShow || state.boSelectedSeats.length === 0) return;

    const show = state.boSelectedShow;
    const movie = state.movies.find(m => m.id === show.movieId) || { title: 'Movie' };
    const payMethod = document.getElementById('boPayMethod') ? document.getElementById('boPayMethod').value : 'Cash';
    const ref = 'POS-' + Math.floor(1000 + Math.random() * 9000);
    const amount = (state.boSelectedSeats.length * show.price * 1.18).toFixed(0);

    // Update show booked seats
    show.bookedSeats += state.boSelectedSeats.length;
    if (show.bookedSeats >= show.totalSeats) {
      show.status = 'HOUSEFULL';
    }

    // Add transaction
    state.transactions.unshift({
      ref,
      movie: movie.title,
      seats: state.boSelectedSeats.join(', '),
      amount: '₹' + amount,
      payment: `Box Office (${payMethod})`,
      time: 'Just now'
    });

    openModal('Walk-in Booking Confirmed!', `
      <div style="text-align:center;padding:10px 0">
        <span class="material-icons-round" style="font-size:56px;color:var(--accent)">check_circle</span>
        <h3 style="color:var(--white);margin:10px 0 4px">Booking Ref: ${ref}</h3>
        <p style="color:var(--text-muted);font-size:13px">Walk-in physical ticket issued successfully</p>
      </div>
      <div style="background:var(--bg);padding:16px;border-radius:8px;margin-top:16px;font-size:13px">
        <div style="display:flex;justify-content:space-between;margin-bottom:8px"><span>Movie:</span><b>${movie.title}</b></div>
        <div style="display:flex;justify-content:space-between;margin-bottom:8px"><span>Seats:</span><b style="color:var(--primary)">${state.boSelectedSeats.join(', ')}</b></div>
        <div style="display:flex;justify-content:space-between;margin-bottom:8px"><span>Paid Amount:</span><b style="color:var(--accent)">₹${amount}</b></div>
        <div style="display:flex;justify-content:space-between"><span>Payment Mode:</span><b>${payMethod}</b></div>
      </div>
    `, `
      <button class="btn-primary" onclick="window.vibePartner.closeModal(); window.vibePartner.populateBoShows();">Done</button>
    `);

    showToast(`Booking ${ref} confirmed!`, 'success');
  }

  // ─── Revenue Analytics ───
  function renderRevenue() {
    const periodSel = document.getElementById('revPeriod');
    const period = periodSel ? periodSel.value : '30';

    const revTotal = document.getElementById('revTotal');
    const revTickets = document.getElementById('revTickets');
    const revAvgPrice = document.getElementById('revAvgPrice');
    const revOccupancy = document.getElementById('revOccupancy');

    if (period === '7') {
      if (revTotal) revTotal.textContent = '₹1,42,800';
      if (revTickets) revTickets.textContent = '840';
      if (revAvgPrice) revAvgPrice.textContent = '₹285';
      if (revOccupancy) revOccupancy.textContent = '68%';
    } else if (period === '90') {
      if (revTotal) revTotal.textContent = '₹14,90,000';
      if (revTickets) revTickets.textContent = '9,850';
      if (revAvgPrice) revAvgPrice.textContent = '₹310';
      if (revOccupancy) revOccupancy.textContent = '76%';
    } else {
      if (revTotal) revTotal.textContent = '₹4,85,000';
      if (revTickets) revTickets.textContent = '3,240';
      if (revAvgPrice) revAvgPrice.textContent = '₹295';
      if (revOccupancy) revOccupancy.textContent = '72%';
    }

    // Trend chart
    const trendContainer = document.getElementById('revTrendChart');
    if (trendContainer) {
      const weeks = [
        { label: 'W1', rev: 98000 },
        { label: 'W2', rev: 125000 },
        { label: 'W3', rev: 142000 },
        { label: 'W4', rev: 120000 }
      ];
      const maxW = Math.max(...weeks.map(w => w.rev));
      trendContainer.innerHTML = weeks.map(w => `
        <div class="bar-group">
          <div class="bar-wrapper">
            <div class="bar-fill" style="height: ${Math.round((w.rev/maxW)*100)}%;" data-tooltip="₹${(w.rev/1000).toFixed(0)}k"></div>
          </div>
          <span class="bar-label">${w.label}</span>
        </div>
      `).join('');
    }

    // Top movies
    const topContainer = document.getElementById('revTopMovies');
    if (topContainer) {
      topContainer.innerHTML = state.movies.slice(0, 3).map((m, idx) => {
        const revs = ['₹2,45,000', '₹1,35,000', '₹1,05,000'];
        return `
          <div class="top-movie-item">
            <span class="top-movie-title">#${idx + 1} ${m.title}</span>
            <span class="top-movie-rev">${revs[idx]}</span>
          </div>
        `;
      }).join('');
    }

    // Transactions table
    const tbody = document.getElementById('revTransactions');
    if (tbody) {
      tbody.innerHTML = state.transactions.map(t => `
        <tr>
          <td><b style="color:var(--primary)">${t.ref}</b></td>
          <td>${t.movie}</td>
          <td>${t.seats}</td>
          <td><b>${t.amount}</b></td>
          <td>${t.payment}</td>
          <td><span style="color:var(--text-muted);font-size:12px">${t.time}</span></td>
        </tr>
      `).join('');
    }
  }

  // ─── Profile ───
  function loadProfileData() {
    const profName = document.getElementById('profName');
    const profEmail = document.getElementById('profEmail');
    const profMobile = document.getElementById('profMobile');
    const profOrg = document.getElementById('profOrg');

    if (profName) profName.value = state.user.name;
    if (profEmail) profEmail.value = state.user.email;
    if (profMobile) profMobile.value = state.user.mobile;
    if (profOrg) profOrg.value = state.user.org;

    const profileName = document.getElementById('profileName');
    const profileEmail = document.getElementById('profileEmail');
    if (profileName) profileName.textContent = state.user.name;
    if (profileEmail) profileEmail.textContent = state.user.email;
  }

  function saveProfile() {
    state.user.name = document.getElementById('profName').value.trim();
    state.user.email = document.getElementById('profEmail').value.trim();
    state.user.mobile = document.getElementById('profMobile').value.trim();
    state.user.org = document.getElementById('profOrg').value.trim();

    localStorage.setItem('vibe_partner_user', JSON.stringify(state.user));
    if (els.partnerName) els.partnerName.textContent = state.user.name;
    loadProfileData();
    showToast('Partner profile updated successfully!', 'success');
  }

  // ─── Modals ───
  function openModal(title, bodyHtml, footerHtml) {
    if (!els.modalOverlay || !els.modal) return;
    els.modalTitle.textContent = title;
    els.modalBody.innerHTML = bodyHtml;
    els.modalFooter.innerHTML = footerHtml;
    els.modalOverlay.classList.remove('hidden');
  }

  function closeModal() {
    if (els.modalOverlay) {
      els.modalOverlay.classList.add('hidden');
    }
  }

  // ─── Toast Notifications ───
  function showToast(message, type = 'info') {
    if (!els.toastContainer) return;
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    let icon = 'info';
    if (type === 'success') icon = 'check_circle';
    if (type === 'error') icon = 'error_outline';

    toast.innerHTML = `<span class="material-icons-round">${icon}</span><span>${message}</span>`;
    els.toastContainer.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(100%)';
      setTimeout(() => toast.remove(), 300);
    }, 3200);
  }

  // ─── Global Exposure for Inline HTML Handlers ───
  window.vibePartner = {
    viewScreens: (thId) => {
      switchSection('screens');
      const filter = document.getElementById('screenTheatreFilter');
      if (filter) {
        filter.value = thId;
        renderScreensGrid(thId);
      }
    },
    viewShowtimes: (thId) => {
      switchSection('showtimes');
      const filter = document.getElementById('showtimeTheatreFilter');
      if (filter) {
        filter.value = thId;
        renderShowtimesTable();
      }
    },
    deleteTheatre,
    confirmAddTheatre,
    closeModal,
    selectScreen,
    toggleSeatSeat,
    confirmAddScreen,
    populateModalScreens,
    confirmAddShowtime,
    toggleShowStatus,
    deleteShowtime,
    toggleBoSeat,
    populateBoShows
  };

  // Start app
  document.addEventListener('DOMContentLoaded', init);
})();
