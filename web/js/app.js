/* ============================================================
   VIBECHECK — Production Application Logic
   BookMyShow-style SPA with Enterprise Gateway & Microservice Integration
   ============================================================ */

'use strict';

// ============================================================
// FALLBACK DATA (Used if backend catalog is empty or initializing)
// ============================================================
const DEMO_MOVIES = [
  {
    id: '00000000-0000-0000-0000-000000000001',
    title: 'Interstellar Returns', genre: ['Sci-Fi', 'Drama'],
    lang: ['English', 'Hindi'], formats: ['IMAX', '2D', '3D'],
    rating: 9.1, votes: '184K', cert: 'PG-13', duration: '2h 49m',
    filter: 'now', emoji: '🚀',
    poster: 'https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=300&q=80',
    heroBg: 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1400&q=80',
    desc: 'A team of explorers travel through a wormhole in space in an attempt to ensure humanity\'s survival as Earth becomes uninhabitable.',
    cast: ['Matthew McConaughey', 'Anne Hathaway', 'Jessica Chastain', 'Michael Caine'],
    director: 'Christopher Nolan'
  },
  {
    id: '00000000-0000-0000-0000-000000000002',
    title: 'The Dark Horizon', genre: ['Action', 'Thriller'],
    lang: ['Hindi', 'English'], formats: ['2D', '3D', '4DX'],
    rating: 8.4, votes: '92K', cert: 'UA', duration: '2h 15m',
    filter: 'now', emoji: '⚔️',
    poster: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=300&q=80',
    heroBg: 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1400&q=80',
    desc: 'When a shadow organization threatens to plunge the world into darkness, one man must rise against impossible odds.',
    cast: ['Hrithik Roshan', 'Deepika Padukone', 'Ranveer Singh'],
    director: 'Siddharth Anand'
  },
  {
    id: '00000000-0000-0000-0000-000000000003',
    title: 'Echoes of Eternity', genre: ['Sci-Fi', 'Horror'],
    lang: ['English'], formats: ['IMAX', '2D'],
    rating: 8.7, votes: '67K', cert: 'A', duration: '2h 32m',
    filter: 'top', emoji: '👁️',
    poster: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=300&q=80',
    heroBg: 'https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=1400&q=80',
    desc: 'A scientist discovers that parallel universes are bleeding into each other, causing terrifying anomalies.',
    cast: ['Cillian Murphy', 'Florence Pugh', 'Robert Downey Jr.'],
    director: 'Denis Villeneuve'
  },
  {
    id: '00000000-0000-0000-0000-000000000004',
    title: 'Pushpa 3: The Rampage', genre: ['Action', 'Drama'],
    lang: ['Telugu', 'Hindi', 'Tamil'], formats: ['2D', '3D'],
    rating: 8.5, votes: '340K', cert: 'UA', duration: '3h 12m',
    filter: 'upcoming', emoji: '🔥',
    poster: 'https://images.unsplash.com/photo-1524601500432-1e1a4c71d692?w=300&q=80',
    heroBg: 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=1400&q=80',
    desc: 'The saga continues as Pushpa Raj faces new enemies and old rivals in a battle for supremacy.',
    cast: ['Allu Arjun', 'Rashmika Mandanna', 'Fahadh Faasil'],
    director: 'Sukumar'
  }
];

const FOOD_ITEMS = [
  { emoji: '🍿', name: 'Classic Popcorn', desc: 'Salted or Caramel, 120g', price: 299 },
  { emoji: '🍿', name: 'Large Popcorn Combo', desc: 'Large Popcorn + 2 Colas', price: 549 },
  { emoji: '🥤', name: 'Cola (Large)', desc: 'Coca Cola / Pepsi, 500ml', price: 149 },
  { emoji: '🌭', name: 'Hot Dog', desc: 'Chicken Frankfurter with toppings', price: 249 },
  { emoji: '🍕', name: 'Nachos & Cheese', desc: 'Crispy tortilla with salsa & cheese', price: 299 },
  { emoji: '🍔', name: 'Veg Burger', desc: 'Aloo Tikki burger with fries', price: 199 },
  { emoji: '☕', name: 'Cappuccino', desc: 'Freshly brewed, 250ml', price: 179 },
  { emoji: '🍫', name: 'Choco Brownie', desc: 'Warm fudge brownie with ice cream', price: 229 }
];

const EVENTS = [
  { emoji: '🎸', title: 'Coldplay World Tour', sub: 'Mumbai · Dec 14', price: 'From ₹3,500' },
  { emoji: '😂', title: 'Vir Das: Live', sub: 'Bangalore · Nov 30', price: 'From ₹999' },
  { emoji: '🎨', title: 'Art Mumbai 2026', sub: 'Exhibition · Nov 1–15', price: 'From ₹200' },
  { emoji: '🏏', title: 'IPL Final 2026', sub: 'Wankhede · Dec 20', price: 'From ₹1,500' }
];

const SEAT_CATEGORIES = [
  { name: 'Recliner', price: 550, rows: ['A', 'B'], cols: 8 },
  { name: 'Gold', price: 350, rows: ['C', 'D', 'E', 'F'], cols: 14 },
  { name: 'Silver', price: 200, rows: ['G', 'H', 'I', 'J', 'K'], cols: 16 }
];

// ============================================================
// GLOBAL APPLICATION STATE
// ============================================================
let state = {
  currentPage: 'home',
  currentUser: null,
  movies: [],
  cities: [],
  theatres: [],
  selectedCity: 'Mumbai',
  selectedMovie: null,
  selectedTheatre: null,
  selectedDate: null,
  selectedTime: null,
  selectedShow: null,
  selectedSeats: [],
  foodCart: {},
  currentBooking: null,
  bookings: [],
  movieFilter: 'now',
  heroSlide: 0,
  heroTimer: null,
  loginTab: 'email',
  bookingTab: 'upcoming',
  selectedPayMethod: 'UPI',
  selectedUPIApp: 'gpay',
  selectedBank: null,
  convFeeRate: 0.05,
  isProcessingPayment: false,
  isProcessingBooking: false
};

// ============================================================
// INITIALIZATION
// ============================================================
document.addEventListener('DOMContentLoaded', async () => {
  console.log('[VibeCheck] App initializing | Gateway:', window.VIBECHECK_CONFIG ? window.VIBECHECK_CONFIG.apiBaseUrl : 'default');

  loadUserFromStorage();
  setupSessionListener();
  populateDates();
  renderEvents();
  startHeroTimer();
  document.addEventListener('click', closeDropdowns);

  // Load real catalog data from backend API Gateway
  await loadCatalogData();

  if (state.currentUser) {
    loadUserBookings();
  }
});

function setupSessionListener() {
  window.addEventListener('vibecheck:session_expired', () => {
    logout(false);
    showToast('⚠️ Your session has expired. Please sign in again.');
    openLoginModal();
  });
}

// ============================================================
// CATALOG DATA LOADING (Movies, Cities, Theatres)
// ============================================================
async function loadCatalogData() {
  const grid = document.getElementById('moviesGrid');
  if (grid) {
    grid.innerHTML = '<div style="grid-column:1/-1;text-align:center;padding:40px;color:#999;"><div class="spinner" style="margin:0 auto 12px"></div>Connecting to VibeCheck Gateway...</div>';
  }

  try {
    // 1. Fetch Movies
    const movieData = await window.VibeCheckApi.movies.getAllMovies(0, 50);
    const content = movieData.content || (Array.isArray(movieData) ? movieData : []);

    if (content.length > 0) {
      state.movies = content.map(m => normalizeMovieDto(m));
      console.log(`[VibeCheck] Loaded ${state.movies.length} movies from backend.`);
    } else {
      console.info('[VibeCheck] Backend returned empty movie list. Loading featured movies.');
      state.movies = [...DEMO_MOVIES];
    }
  } catch (err) {
    console.warn('[VibeCheck] Backend catalog unavailable (' + err.message + '). Using fallback catalog.');
    state.movies = [...DEMO_MOVIES];
  } finally {
    renderMovies();
  }

  // 2. Fetch Cities
  try {
    const cityList = await window.VibeCheckApi.theatres.getAllCities();
    if (Array.isArray(cityList) && cityList.length > 0) {
      state.cities = cityList;
      renderCityDropdown(cityList);
    }
  } catch (e) {
    console.debug('[VibeCheck] Cities API offline, using default cities.');
  }
}

function normalizeMovieDto(dto) {
  const genres = dto.genres ? dto.genres.map(g => (typeof g === 'string' ? g : g.name || 'Cinema')) : ['Drama'];
  const languages = dto.languages ? dto.languages.map(l => (typeof l === 'string' ? l : l.name || 'Hindi')) : ['Hindi'];
  const duration = dto.durationMinutes ? `${Math.floor(dto.durationMinutes / 60)}h ${dto.durationMinutes % 60}m` : '2h 15m';

  return {
    id: dto.id,
    title: dto.title || 'Untitled Movie',
    genre: genres.length > 0 ? genres : ['Action', 'Drama'],
    lang: languages.length > 0 ? languages : ['Hindi', 'English'],
    formats: ['2D', '3D', 'IMAX'],
    rating: dto.rating || 8.5,
    votes: '42K',
    cert: dto.certificate || 'UA',
    duration: duration,
    filter: (dto.status === 'COMING_SOON') ? 'upcoming' : 'now',
    emoji: '🎬',
    poster: dto.posterUrl || 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=300&q=80',
    heroBg: dto.trailerUrl || 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1400&q=80',
    desc: dto.description || 'Experience the cinema magic with VibeCheck.',
    cast: ['Lead Actor', 'Lead Actress'],
    director: 'Director'
  };
}

function renderCityDropdown(cities) {
  const container = document.getElementById('cityItems');
  if (!container) return;
  container.innerHTML = cities.map(c => `
    <div class="ci ${c.name === state.selectedCity ? 'active' : ''}" onclick="selectCity('${c.name}')">${c.name}</div>
  `).join('');
}

// ============================================================
// PAGE NAVIGATION
// ============================================================
function showPage(name) {
  document.querySelectorAll('.page').forEach(p => p.classList.add('hidden'));
  const target = document.getElementById('page-' + name);
  if (target) target.classList.remove('hidden');
  state.currentPage = name;
  window.scrollTo({ top: 0, behavior: 'smooth' });

  if (name === 'bookings') renderBookings();
  if (name === 'profile') loadProfileForm();
}

// ============================================================
// HERO SLIDER
// ============================================================
function startHeroTimer() {
  state.heroTimer = setInterval(() => nextSlide(), 4500);
}

function nextSlide() {
  const slides = document.querySelectorAll('.hslide');
  const dots = document.querySelectorAll('.hdot');
  if (!slides.length) return;
  slides[state.heroSlide].classList.remove('active');
  if (dots[state.heroSlide]) dots[state.heroSlide].classList.remove('active');
  state.heroSlide = (state.heroSlide + 1) % slides.length;
  slides[state.heroSlide].classList.add('active');
  if (dots[state.heroSlide]) dots[state.heroSlide].classList.add('active');
}

// ============================================================
// MOVIE FILTERS & RENDERING
// ============================================================
function setFilter(f) {
  state.movieFilter = f;
  document.querySelectorAll('.ftab').forEach(el => el.classList.remove('active'));
  document.getElementById('ft-' + f)?.classList.add('active');
  const titles = { now: 'Now Showing', upcoming: 'Upcoming Movies', top: 'Top Rated Movies' };
  const city = document.getElementById('selCity').textContent;
  const heading = document.getElementById('moviesHeading');
  if (heading) heading.textContent = `${titles[f] || 'Movies'} in ${city}`;
  renderMovies();
}

function applyFilters() { renderMovies(); }

function renderMovies() {
  const genre = document.getElementById('genreSel')?.value;
  const lang = document.getElementById('langSel')?.value;
  const fmt = document.getElementById('fmtSel')?.value;

  let movies = state.movies;
  if (state.movieFilter === 'top') {
    movies = movies.filter(m => m.rating >= 8.5);
  } else if (state.movieFilter === 'upcoming') {
    movies = movies.filter(m => m.filter === 'upcoming');
  } else {
    movies = movies.filter(m => m.filter === 'now');
  }

  if (genre) movies = movies.filter(m => m.genre.includes(genre));
  if (lang) movies = movies.filter(m => m.lang.includes(lang));
  if (fmt) movies = movies.filter(m => m.formats.includes(fmt));

  const grid = document.getElementById('moviesGrid');
  if (!grid) return;

  if (movies.length === 0) {
    grid.innerHTML = '<div style="grid-column:1/-1;text-align:center;padding:40px;color:#999;">No movies found for the selected filters.</div>';
    return;
  }

  grid.innerHTML = movies.map(m => `
    <div class="movie-card" onclick="openMovieDetail('${m.id}')">
      <div class="mc-poster-ph" style="background-image:url('${m.poster}');background-size:cover;background-position:center">${m.poster ? '' : m.emoji}</div>
      <div class="mc-body">
        <div class="mc-title">${m.title}</div>
        <div class="mc-genres">${m.genre.join(' / ')} · ${m.lang[0]}</div>
        <div class="mc-rating">
          <span class="mc-stars">${getStars(m.rating)}</span>
          <span class="mc-score">${m.rating}/10</span>
          <span class="mc-votes">(${m.votes})</span>
        </div>
        <button class="btn-mc-book" onclick="event.stopPropagation();openMovieDetail('${m.id}')">Book tickets</button>
      </div>
    </div>
  `).join('');
}

function getStars(rating) {
  const full = Math.floor(rating / 2);
  const half = rating % 2 >= 0.5 ? 1 : 0;
  return '★'.repeat(full) + (half ? '½' : '') + '☆'.repeat(Math.max(0, 5 - full - half));
}

function renderEvents() {
  const row = document.getElementById('eventsRow');
  if (!row) return;
  row.innerHTML = EVENTS.map(e => `
    <div class="event-card" onclick="showToast('Booking for: ${e.title}')">
      <div class="ec-img">${e.emoji}</div>
      <div class="ec-body">
        <div class="ec-title">${e.title}</div>
        <div class="ec-sub">${e.sub}</div>
        <div class="ec-price">${e.price}</div>
      </div>
    </div>
  `).join('');
}

// ============================================================
// MOVIE DETAIL MODAL & REAL SHOWTIME FETCH
// ============================================================
async function openMovieDetail(id) {
  const m = state.movies.find(x => String(x.id) === String(id)) || ((typeof id === 'number' || !isNaN(Number(id))) ? state.movies[Number(id)] : null);
  if (!m) return;
  state.selectedMovie = m;

  document.getElementById('mmHero').style.cssText = `height:220px;background:linear-gradient(to right,rgba(0,0,0,0.85) 0%,rgba(0,0,0,0.3) 60%),url('${m.heroBg}') center/cover;`;
  document.getElementById('mmPoster').src = m.poster;
  document.getElementById('mmTitle').textContent = m.title;
  document.getElementById('mmMeta').innerHTML = [
    ...m.genre.map(g => `<span class="mm-badge">${g}</span>`),
    ...m.lang.map(l => `<span class="mm-badge">${l}</span>`),
    `<span class="mm-badge">${m.cert}</span>`,
    `<span class="mm-badge">${m.duration}</span>`,
    `<span class="mm-badge">⭐ ${m.rating}</span>`
  ].join('');
  document.getElementById('mmRating').innerHTML = `
    <span class="mm-score">⭐ ${m.rating}</span>
    <span class="mm-stars">${getStars(m.rating)}</span>
    <span class="mm-votes">${m.votes} votes</span>
  `;
  document.getElementById('mmDesc').textContent = m.desc;
  document.getElementById('mmCast').innerHTML = `
    <h4>Cast & Crew</h4>
    <div class="cast-list">
      <span class="cast-tag">🎬 ${m.director}</span>
      ${m.cast.map(c => `<span class="cast-tag">🎭 ${c}</span>`).join('')}
    </div>
  `;

  renderDateTabs();
  await loadMovieShows(m);

  document.getElementById('movieOverlay').classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

function closeMovieModal() {
  document.getElementById('movieOverlay').classList.add('hidden');
  document.body.style.overflow = '';
}

async function loadMovieShows(movie) {
  const showContainer = document.getElementById('theatreShows');
  if (!showContainer) return;

  showContainer.innerHTML = '<div style="text-align:center;padding:20px;color:#999;"><div class="spinner" style="margin:0 auto 10px"></div>Loading scheduled shows...</div>';

  try {
    const showPage = await window.VibeCheckApi.shows.getShowsByMovie(movie.id);
    const shows = showPage.content || (Array.isArray(showPage) ? showPage : []);

    if (shows.length > 0) {
      renderRealShows(shows, movie);
    } else {
      showContainer.innerHTML = '<div style="text-align:center;padding:30px 15px;color:#888;"><h5>No live scheduled shows found</h5><p style="font-size:13px;margin-top:6px;">Check other dates or select another movie from the catalog.</p></div>';
    }
  } catch (err) {
    console.warn('[VibeCheck] Backend shows unavailable for this movie:', err.message);
    showContainer.innerHTML = `<div style="text-align:center;padding:30px 15px;color:#e74c3c;"><h5>Unable to load shows</h5><p style="font-size:13px;margin-top:6px;">${err.message || 'Please check connection to API Gateway'}</p></div>`;
  }
}

function renderRealShows(shows, movie) {
  const container = document.getElementById('theatreShows');
  container.innerHTML = `
    <div class="th-show">
      <div class="th-name">
        <span>VibeCheck Partner Cinemas</span>
        <div class="th-features"><span class="th-feat">Dolby Atmos</span><span class="th-feat">4K Laser</span><span class="th-feat">M-Ticket</span></div>
      </div>
      <div class="time-slots">
        ${shows.map(s => {
          const dt = new Date(s.startTime);
          const timeStr = dt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
          return `
            <button class="time-slot" onclick="selectRealShow('${s.id}', 'VibeCheck Partner Cinemas', '${timeStr}', this)">
              ${timeStr} · ₹${s.basePrice || 250}
            </button>
          `;
        }).join('')}
      </div>
    </div>
  `;
}

function selectRealShow(showId, theatreName, timeStr, btnEl) {
  state.selectedShow = { id: showId };
  state.selectedTheatre = theatreName;
  state.selectedTime = timeStr;
  document.querySelectorAll('.time-slot').forEach(s => s.classList.remove('active'));
  btnEl.classList.add('active');

  setTimeout(() => {
    closeMovieModal();
    startBooking();
  }, 300);
}

// ============================================================
// DATE SELECTION TABS
// ============================================================
function populateDates() { state.datesArr = generateDates(); }

function generateDates() {
  const dates = [];
  const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  const today = new Date();
  for (let i = 0; i < 7; i++) {
    const d = new Date(today);
    d.setDate(today.getDate() + i);
    dates.push({ day: days[d.getDay()], num: d.getDate(), mon: months[d.getMonth()], full: d.toDateString() });
  }
  return dates;
}

function renderDateTabs() {
  const dates = state.datesArr || generateDates();
  const selectedDate = state.selectedDate || dates[0].full;
  if (!state.selectedDate) state.selectedDate = selectedDate;

  document.getElementById('dateTabs').innerHTML = dates.map(d => `
    <div class="date-tab ${d.full === state.selectedDate ? 'active' : ''}" onclick="selectDate('${d.full}', this)">
      <span class="dt-day">${d.day}</span>
      <span class="dt-num">${d.num}</span>
      <span class="dt-mon">${d.mon}</span>
    </div>
  `).join('');
}

function selectDate(dateStr, el) {
  state.selectedDate = dateStr;
  document.querySelectorAll('.date-tab').forEach(d => d.classList.remove('active'));
  el.classList.add('active');
}

// ============================================================
// BOOKING WIZARD & SEAT MAP
// ============================================================
async function startBooking() {
  if (!state.selectedMovie) return;

  if (!state.selectedShow || !state.selectedShow.id) {
    const firstShowBtn = document.querySelector('#theatreShows .time-slot');
    if (firstShowBtn) {
      firstShowBtn.click();
      return;
    } else {
      showToast('⚠️ Please select a showtime to book tickets.');
      return;
    }
  }

  state.selectedSeats = [];
  state.foodCart = {};
  state.currentBooking = null;

  closeMovieModal();
  setStep(1);
  showPage('booking');

  const m = state.selectedMovie;
  const movieEl = document.getElementById('seatMovieName');
  if (movieEl) movieEl.textContent = m.title;

  const infoEl = document.getElementById('seatInfo');
  if (infoEl) {
    infoEl.textContent = `${state.selectedTheatre || 'PVR: Phoenix Mall'} · ${state.selectedDate || 'Today'} · ${state.selectedTime || '06:45 PM'}`;
  }

  ['sc1', 'sc3'].forEach(prefix => {
    const el = document.getElementById(prefix + 'Title');
    if (el) el.textContent = m.title;
    const th = document.getElementById(prefix + 'Theatre');
    if (th) th.textContent = state.selectedTheatre || 'PVR: Phoenix Mall';
    const dt = document.getElementById(prefix + 'DateTime');
    if (dt) dt.textContent = `${state.selectedDate || 'Today'} · ${state.selectedTime || '06:45 PM'}`;
    const poster = document.getElementById(prefix + 'Poster');
    if (poster) poster.src = m.poster;
  });

  await renderSeatMap();
  updateBookingSummary();
}

function setStep(n) {
  [1, 2, 3].forEach(i => {
    document.getElementById('st' + i).classList.remove('active', 'done');
    document.getElementById('stepSeats').classList.add('hidden');
    document.getElementById('stepFood').classList.add('hidden');
    document.getElementById('stepPayment').classList.add('hidden');
  });
  for (let i = 1; i < n; i++) document.getElementById('st' + i).classList.add('done');
  document.getElementById('st' + n).classList.add('active');
  if (n === 1) document.getElementById('stepSeats').classList.remove('hidden');
  if (n === 2) {
    renderFoodGrid();
    document.getElementById('stepFood').classList.remove('hidden');
  }
  if (n === 3) document.getElementById('stepPayment').classList.remove('hidden');
}

async function renderSeatMap() {
  const seatMapEl = document.getElementById('seatMap');
  if (!seatMapEl) return;

  // Try to load real show seats if showId is present
  let realSeats = null;
  if (state.selectedShow && state.selectedShow.id) {
    try {
      const seats = await window.VibeCheckApi.shows.getShowSeats(state.selectedShow.id);
      if (Array.isArray(seats) && seats.length > 0) {
        realSeats = seats;
        state.showSeats = seats;
      }
    } catch (e) {
      console.warn('[VibeCheck] Show seat API error:', e.message);
    }
  }

  let html = '';
  let seatIndex = 0;

  SEAT_CATEGORIES.forEach(cat => {
    html += `<div class="seat-category">
      <div class="seat-cat-label">
        <span class="seat-cat-name">${cat.name}</span>
        <span class="seat-cat-price">₹${cat.price}</span>
      </div>`;

    cat.rows.forEach(row => {
      const mid = Math.floor(cat.cols / 2);
      let rowHtml = `<div class="seat-row"><span class="row-label">${row}</span>`;
      for (let c = 1; c <= cat.cols; c++) {
        if (c === mid + 1) rowHtml += `<div class="seat-gap"></div>`;
        const seatId = `${row}${c}`;

        let isBooked = false;
        let showSeatUuid = null;
        let seatPrice = cat.price;

        if (realSeats && realSeats.length > 0) {
          // 1. Try matching by seatNumber if returned
          let match = realSeats.find(s => s.seatNumber === seatId);
          // 2. Sequential assignment from real backend show seats
          if (!match && seatIndex < realSeats.length) {
            match = realSeats[seatIndex];
          }
          if (match) {
            showSeatUuid = match.id;
            isBooked = (match.status === 'BOOKED' || match.status === 'LOCKED');
            if (match.price) seatPrice = Number(match.price);
          }
        }
        seatIndex++;

        const isSelectable = !isBooked && !!showSeatUuid;

        rowHtml += `<button class="seat ${isBooked ? 'booked' : ''} ${!showSeatUuid ? 'unavailable' : ''}" id="seat-${seatId}"
          data-id="${seatId}" data-price="${seatPrice}" data-cat="${cat.name}" data-showseatid="${showSeatUuid || ''}"
          ${isSelectable ? `onclick="toggleSeat(this)"` : 'disabled'}
          title="${isBooked ? 'Unavailable / Booked' : !showSeatUuid ? 'Unavailable' : seatId + ' - ₹' + seatPrice}">${c}</button>`;
      }
      rowHtml += `</div>`;
      html += rowHtml;
    });

    html += `</div>`;
  });

  seatMapEl.innerHTML = html;
}

function toggleSeat(el) {
  const id = el.dataset.id;
  const price = parseInt(el.dataset.price);
  const cat = el.dataset.cat;
  const showSeatId = el.dataset.showseatid || id;

  const idx = state.selectedSeats.findIndex(s => s.id === id);
  if (idx >= 0) {
    state.selectedSeats.splice(idx, 1);
    el.classList.remove('selected');
  } else {
    if (state.selectedSeats.length >= 6) {
      showToast('Maximum 6 seats allowed per booking.');
      return;
    }
    state.selectedSeats.push({ id, price, cat, showSeatId });
    el.classList.add('selected');
  }
  updateBookingSummary();
}

function updateBookingSummary() {
  const seats = state.selectedSeats;
  const ticketTotal = seats.reduce((s, x) => s + x.price, 0);
  const conv = Math.round(ticketTotal * state.convFeeRate);
  const grand = ticketTotal + conv;

  const seatStr = seats.length ? seats.map(s => s.id).join(', ') : '—';

  const seatsEl = document.getElementById('sc1Seats');
  if (seatsEl) seatsEl.textContent = seatStr;
  const ticketEl = document.getElementById('sc1Ticket');
  if (ticketEl) ticketEl.textContent = `₹${ticketTotal}`;
  const convEl = document.getElementById('sc1Conv');
  if (convEl) convEl.textContent = `₹${conv}`;
  const totalEl = document.getElementById('sc1Total');
  if (totalEl) totalEl.textContent = `₹${grand}`;

  const btnProceed = document.getElementById('btnProceed');
  if (btnProceed) {
    btnProceed.disabled = (seats.length === 0);
    btnProceed.textContent = seats.length ? `Proceed (${seats.length} Seats) ›` : 'Proceed ›';
  }
}

// ============================================================
// STEP 1 -> STEP 2 (Proceed & Real Seat Reservation)
// ============================================================
async function goToFood() {
  if (state.selectedSeats.length === 0) {
    showToast('Please select at least one seat to proceed.');
    return;
  }

  // Enforce Authentication
  if (!state.currentUser) {
    openLoginModal();
    showToast('🔐 Please sign in to reserve your seats.');
    return;
  }

  const btnProceed = document.getElementById('btnProceed');
  const originalText = btnProceed ? btnProceed.textContent : 'Proceed ›';
  if (btnProceed) {
    btnProceed.disabled = true;
    btnProceed.innerHTML = '<span class="spinner" style="display:inline-block;width:14px;height:14px;margin-right:6px"></span> Reserving Seats...';
  }

  try {
    const showId = state.selectedShow && state.selectedShow.id;
    if (!showId) {
      showToast('⚠️ No show selected. Please select a show first.');
      return;
    }

    const showSeatIds = state.selectedSeats.map(s => s.showSeatId).filter(Boolean);
    if (showSeatIds.length === 0) {
      showToast('⚠️ Please select at least one valid seat.');
      return;
    }

    // Call Real Booking API
    const bookingResult = await window.VibeCheckApi.bookings.createBooking({
      userId: state.currentUser.id,
      showId: showId,
      showSeatIds: showSeatIds,
      paymentMethod: state.selectedPayMethod || 'UPI'
    });

    state.currentBooking = bookingResult.data || bookingResult;
    // Set persistent payment idempotency key for this reservation attempt
    state.currentBooking.paymentIdempotencyKey = window.VibeCheckApi.generateUUID();
    console.log('[VibeCheck Booking] Reservation successful | Ref:', state.currentBooking.bookingReference);

    setStep(2);
    updateFoodSummary();
  } catch (err) {
    console.warn('[VibeCheck Booking] Booking creation error:', err.message);

    if (err.status === 409) {
      showToast('⚠️ Seat Conflict: One or more selected seats were just reserved by another user. Please choose alternative seats.');
      await renderSeatMap();
    } else {
      showToast('❌ Seat Reservation Failed: ' + (err.message || 'Unable to reserve seats. Please retry.'));
    }
  } finally {
    if (btnProceed) {
      btnProceed.disabled = false;
      btnProceed.textContent = originalText;
    }
  }
}

// ============================================================
// STEP 2: FOOD & BEVERAGES
// ============================================================
function renderFoodGrid() {
  const grid = document.getElementById('foodGrid');
  if (!grid) return;
  grid.innerHTML = FOOD_ITEMS.map((item, i) => `
    <div class="food-card">
      <div class="food-emoji">${item.emoji}</div>
      <div class="food-info">
        <div class="food-name">${item.name}</div>
        <div class="food-desc">${item.desc}</div>
        <div class="food-price">₹${item.price}</div>
      </div>
      <div class="food-qty">
        <button class="fq-btn" onclick="changeFood(${i}, -1)">−</button>
        <span class="fq-val" id="fqty-${i}">${state.foodCart[i] || 0}</span>
        <button class="fq-btn" onclick="changeFood(${i}, 1)">+</button>
      </div>
    </div>
  `).join('');
}

function changeFood(i, delta) {
  state.foodCart[i] = Math.max(0, (state.foodCart[i] || 0) + delta);
  document.getElementById(`fqty-${i}`).textContent = state.foodCart[i];
  updateFoodSummary();
}

function updateFoodSummary() {
  const ticketTotal = state.selectedSeats.reduce((s, x) => s + x.price, 0);
  const foodTotal = Object.entries(state.foodCart).reduce((s, [i, qty]) => s + FOOD_ITEMS[i].price * qty, 0);
  const conv = Math.round(ticketTotal * state.convFeeRate);
  const grand = ticketTotal + foodTotal + conv;

  const tEl = document.getElementById('sc2Ticket');
  if (tEl) tEl.textContent = `₹${ticketTotal}`;
  const fEl = document.getElementById('sc2Food');
  if (fEl) fEl.textContent = `₹${foodTotal}`;
  const cEl = document.getElementById('sc2Conv');
  if (cEl) cEl.textContent = `₹${conv}`;
  const totEl = document.getElementById('sc2Total');
  if (totEl) totEl.textContent = `₹${grand}`;
}

function skipFood() {
  state.foodCart = {};
  goToPayment();
}

// ============================================================
// STEP 3: PAYMENT & CONFIRMATION
// ============================================================
function goToPayment() {
  setStep(3);
  updatePaymentSummary();
}

function updatePaymentSummary() {
  const ticketTotal = state.selectedSeats.reduce((s, x) => s + x.price, 0);
  const foodTotal = Object.entries(state.foodCart).reduce((s, [i, qty]) => s + FOOD_ITEMS[i].price * qty, 0);
  const conv = Math.round(ticketTotal * state.convFeeRate);
  const grand = ticketTotal + foodTotal + conv;
  const m = state.selectedMovie || DEMO_MOVIES[0];

  const tEl = document.getElementById('sc3Title');
  if (tEl) tEl.textContent = m.title;
  const thEl = document.getElementById('sc3Theatre');
  if (thEl) thEl.textContent = state.selectedTheatre || 'PVR: Phoenix Mall';
  const dtEl = document.getElementById('sc3DateTime');
  if (dtEl) dtEl.textContent = `${state.selectedDate || 'Today'} · ${state.selectedTime || '06:45 PM'}`;
  const sEl = document.getElementById('sc3Seats');
  if (sEl) sEl.textContent = state.selectedSeats.map(s => s.id).join(', ');
  const tkEl = document.getElementById('sc3Ticket');
  if (tkEl) tkEl.textContent = `₹${ticketTotal}`;
  const cvEl = document.getElementById('sc3Conv');
  if (cvEl) cvEl.textContent = `₹${conv}`;
  const totEl = document.getElementById('sc3Total');
  if (totEl) totEl.textContent = `₹${grand}`;
  const payEl = document.getElementById('payAmt');
  if (payEl) payEl.textContent = `₹${grand}`;

  const sc3Poster = document.getElementById('sc3Poster');
  if (sc3Poster) sc3Poster.src = m.poster;

  const fRow = document.getElementById('sc3FoodRow');
  if (fRow) {
    fRow.style.display = foodTotal > 0 ? '' : 'none';
    const fVal = document.getElementById('sc3Food');
    if (fVal) fVal.textContent = `₹${foodTotal}`;
  }
}

function togglePay(section) {
  const body = document.getElementById('pb-' + section);
  const chev = document.getElementById('chev-' + section);
  if (!body) return;
  const isOpen = !body.classList.contains('hidden');
  body.classList.toggle('hidden');
  if (chev) chev.classList.toggle('open', !isOpen);
}

function selectUPIApp(app) {
  state.selectedUPIApp = app;
  state.selectedPayMethod = 'UPI';
  document.querySelectorAll('.upi-btn').forEach(b => b.style.borderColor = '');
  if (event && event.currentTarget) {
    event.currentTarget.style.borderColor = 'var(--red)';
  }
  const idRow = document.getElementById('upiIdRow');
  if (idRow) idRow.style.display = app === 'other' ? '' : 'none';
  showToast(`${app === 'gpay' ? 'Google Pay' : app === 'phonepe' ? 'PhonePe' : 'Paytm'} selected`);
}

function verifyUPI() {
  const val = document.getElementById('upiIdVal')?.value.trim();
  if (!val || !val.includes('@')) {
    showToast('Please enter a valid UPI ID (e.g. user@okhdfcbank)');
    return;
  }
  state.selectedPayMethod = 'UPI';
  showToast('✅ UPI ID verified!');
}

function selectBank(el, bank) {
  state.selectedBank = bank;
  state.selectedPayMethod = 'NETBANKING';
  document.querySelectorAll('.bank-btn').forEach(b => b.classList.remove('active-bank'));
  el.classList.add('active-bank');
}

function fmtCard(el) {
  let v = el.value.replace(/\D/g, '');
  v = v.match(/.{1,4}/g)?.join(' ') || v;
  el.value = v;
  if (v.replace(/\s/g, '').length === 16) state.selectedPayMethod = 'CARD';
}

function fmtExpiry(el) {
  let v = el.value.replace(/\D/g, '');
  if (v.length >= 2) v = v.slice(0, 2) + ' / ' + v.slice(2);
  el.value = v;
}

// REAL PAYMENT INITIATION & CONFIRMATION
async function processPayment() {
  if (state.isProcessingPayment) return;

  if (!state.currentBooking || !state.currentBooking.bookingReference) {
    showToast('⚠️ No active reservation found. Please select seats first.');
    showPage('home');
    return;
  }

  const ticketTotal = state.selectedSeats.reduce((s, x) => s + x.price, 0);
  const foodTotal = Object.entries(state.foodCart).reduce((s, [i, qty]) => s + FOOD_ITEMS[i].price * qty, 0);
  const conv = Math.round(ticketTotal * state.convFeeRate);
  const grand = ticketTotal + foodTotal + conv;

  const btnPay = document.getElementById('btnPay');
  if (btnPay) btnPay.disabled = true;
  state.isProcessingPayment = true;

  const overlay = document.getElementById('payOverlay');
  if (overlay) overlay.classList.remove('hidden');

  try {
    console.log('[VibeCheck Payment] Initiating payment for booking:', state.currentBooking.bookingReference);

    // Reuse deterministic idempotency key for this booking reservation
    if (!state.currentBooking.paymentIdempotencyKey) {
      state.currentBooking.paymentIdempotencyKey = window.VibeCheckApi.generateUUID();
    }
    const idempotencyKey = state.currentBooking.paymentIdempotencyKey;

    // 1. Call Real Payment Service (POST /api/v1/payments)
    const paymentRes = await window.VibeCheckApi.payments.initiatePayment({
      bookingId: state.currentBooking.id,
      userId: state.currentUser.id,
      amount: grand,
      currency: 'INR',
      paymentMethod: state.selectedPayMethod || 'UPI',
      bookingReference: state.currentBooking.bookingReference,
      idempotencyKey: idempotencyKey
    });

    const paymentId = paymentRes.paymentId || paymentRes.id;
    if (!paymentId) {
      throw new Error('Payment service response did not contain a valid payment ID.');
    }
    console.log('[VibeCheck Payment] Payment initiated | ID:', paymentId, '| Status:', paymentRes.status);

    if (paymentRes.status === 'FAILED') {
      throw new Error(paymentRes.failureReason || 'Payment declined by gateway.');
    }

    // 2. Call Real Booking Service Confirm (POST /api/v1/bookings/confirm)
    const confirmRes = await window.VibeCheckApi.bookings.confirmBooking({
      bookingReference: state.currentBooking.bookingReference,
      paymentId: String(paymentId)
    });

    const confirmedBooking = confirmRes.data || confirmRes;
    if (!confirmedBooking || (confirmedBooking.status && confirmedBooking.status !== 'CONFIRMED')) {
      throw new Error('Booking confirmation failed. Status: ' + (confirmedBooking?.status || 'UNKNOWN'));
    }
    console.log('[VibeCheck Booking] Confirmation verified | Status:', confirmedBooking.status);

    // 3. Mark booking confirmed in local state with verified backend data
    const confirmedRecord = {
      id: confirmedBooking.bookingReference || state.currentBooking.bookingReference,
      bookingReference: confirmedBooking.bookingReference || state.currentBooking.bookingReference,
      movieId: state.selectedMovie?.id,
      title: state.selectedMovie?.title || 'Movie',
      poster: state.selectedMovie?.poster,
      theatre: state.selectedTheatre || 'VibeCheck Partner Cinemas',
      dt: `${state.selectedDate || 'Today'} · ${state.selectedTime || '06:45 PM'}`,
      seats: state.selectedSeats.map(s => s.id).join(', '),
      format: (state.selectedMovie?.formats && state.selectedMovie.formats[0]) || '2D',
      amount: confirmedBooking.totalAmount || grand,
      status: 'confirmed',
      createdAt: new Date().toISOString()
    };

    state.bookings.unshift(confirmedRecord);
    saveBookingsToStorage();

    // 4. Complete UI transition
    if (overlay) overlay.classList.add('hidden');
    state.isProcessingPayment = false;
    if (btnPay) btnPay.disabled = false;

    // Show verified ticket
    showTicket(confirmedRecord);
    showToast('🎉 Booking Confirmed! Your tickets are ready.');

  } catch (fatalErr) {
    if (overlay) overlay.classList.add('hidden');
    state.isProcessingPayment = false;
    if (btnPay) btnPay.disabled = false;
    console.error('[VibeCheck Payment] Payment processing failed:', fatalErr);
    showToast('❌ Payment Failed: ' + (fatalErr.message || 'Payment could not be completed'));
  }
}

function showTicket(booking) {
  document.getElementById('bkId').textContent = booking.id;
  document.getElementById('et-movie').textContent = booking.title;
  document.getElementById('et-format').textContent = booking.format;
  document.getElementById('et-dt').textContent = booking.dt;
  document.getElementById('et-theatre').textContent = booking.theatre;
  document.getElementById('et-seats').textContent = booking.seats;
  document.getElementById('et-amount').textContent = `₹${booking.amount}`;

  drawQR(booking.id);
  document.getElementById('ticketOverlay').classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

function closeTicketModal() {
  document.getElementById('ticketOverlay').classList.add('hidden');
  document.body.style.overflow = '';
  showPage('bookings');
}

// QR Code Canvas Renderer
function drawQR(data) {
  const canvas = document.getElementById('qrCanvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const size = 110;
  ctx.fillStyle = '#fff';
  ctx.fillRect(0, 0, size, size);
  ctx.fillStyle = '#1C1C1C';

  function drawFinder(x, y) {
    ctx.fillRect(x, y, 21, 21);
    ctx.fillStyle = '#fff';
    ctx.fillRect(x+3, y+3, 15, 15);
    ctx.fillStyle = '#1C1C1C';
    ctx.fillRect(x+6, y+6, 9, 9);
  }
  drawFinder(4, 4);
  drawFinder(size-25, 4);
  drawFinder(4, size-25);

  let hash = 0;
  for (let c of data) hash = (hash * 31 + c.charCodeAt(0)) & 0xffffffff;
  const rng = () => { hash ^= hash << 13; hash ^= hash >> 17; hash ^= hash << 5; return (hash >>> 0) / 0xffffffff; };
  for (let i = 0; i < 8; i++) {
    for (let j = 0; j < 8; j++) {
      if (rng() > 0.45) ctx.fillRect(30 + i * 8, 30 + j * 8, 6, 6);
    }
  }
}

function downloadTicket() {
  const canvas = document.getElementById('qrCanvas');
  const link = document.createElement('a');
  const bkId = document.getElementById('bkId').textContent;
  link.download = `VibeCheck-Ticket-${bkId}.png`;

  const tc = document.createElement('canvas');
  tc.width = 500; tc.height = 320;
  const ctx = tc.getContext('2d');

  ctx.fillStyle = '#1C1C1C';
  ctx.fillRect(0, 0, 500, 50);
  ctx.fillStyle = '#f5f5f5';
  ctx.fillRect(0, 50, 500, 270);

  ctx.fillStyle = '#fff';
  ctx.font = 'bold 20px Inter, sans-serif';
  ctx.fillText('VibeCheck', 20, 33);
  ctx.fillStyle = '#E31837';
  ctx.fillText('   E-TICKET', 100, 33);

  const fields = [
    ['MOVIE', document.getElementById('et-movie').textContent],
    ['THEATRE', document.getElementById('et-theatre').textContent],
    ['DATE & TIME', document.getElementById('et-dt').textContent],
    ['SEATS', document.getElementById('et-seats').textContent],
    ['AMOUNT', document.getElementById('et-amount').textContent],
  ];
  fields.forEach(([label, val], i) => {
    const x = i < 3 ? 20 : 270;
    const y = 80 + (i < 3 ? i : i - 3) * 60;
    ctx.fillStyle = '#999';
    ctx.font = '10px Inter, sans-serif';
    ctx.fillText(label, x, y);
    ctx.fillStyle = '#333';
    ctx.font = 'bold 13px Inter, sans-serif';
    ctx.fillText(val, x, y + 18);
  });

  if (canvas) ctx.drawImage(canvas, 370, 60, 110, 110);
  link.href = tc.toDataURL('image/png');
  link.click();
  showToast('✅ Ticket downloaded successfully!');
}

// ============================================================
// MY BOOKINGS (REAL BACKEND INTEGRATION)
// ============================================================
async function loadUserBookings() {
  if (!state.currentUser || !state.currentUser.id) return;
  try {
    const response = await window.VibeCheckApi.bookings.getUserBookings(state.currentUser.id, 0, 50);
    const content = (response.data && response.data.content) || response.content || [];
    if (Array.isArray(content) && content.length > 0) {
      state.bookings = content.map(b => ({
        id: b.bookingReference,
        bookingReference: b.bookingReference,
        title: b.movieTitle || 'Movie Ticket',
        theatre: b.theatreName || 'Cinema Hall',
        dt: b.showTime ? new Date(b.showTime).toLocaleString() : 'Today',
        seats: (b.seatNumbers && b.seatNumbers.join(', ')) || (b.seats && b.seats.map(s => s.seatNumber).join(', ')) || 'Seats',
        format: '2D',
        amount: b.totalAmount,
        status: (b.status || 'confirmed').toLowerCase(),
        createdAt: b.createdAt || new Date().toISOString()
      }));
      saveBookingsToStorage();
    }
  } catch (e) {
    console.debug('[VibeCheck] Remote bookings fetch deferred, loading local cache.');
  }
}

function renderBookings() {
  const tab = state.bookingTab;
  const all = state.bookings;

  let filtered;
  if (tab === 'upcoming') {
    filtered = all.filter(b => b.status === 'confirmed');
  } else if (tab === 'past') {
    filtered = all.filter(b => b.status === 'past');
  } else {
    filtered = all.filter(b => b.status === 'cancelled');
  }

  const list = document.getElementById('bList');
  if (!list) return;

  if (filtered.length === 0) {
    list.innerHTML = `<div class="empty-state">
      <div class="empty-icon">${tab === 'upcoming' ? '🎟️' : tab === 'past' ? '🎬' : '❌'}</div>
      <h3>No ${tab} bookings</h3>
      <p>${tab === 'upcoming' ? 'Book movie tickets to view your confirmed tickets here!' : 'Your past bookings will appear here.'}</p>
      ${tab === 'upcoming' ? '<button class="btn-hbook" style="margin-top:16px" onclick="showPage(\'home\')">Explore Movies</button>' : ''}
    </div>`;
    return;
  }

  list.innerHTML = filtered.map(b => `
    <div class="bk-card">
      <div class="bk-poster" style="background:#222;display:flex;align-items:center;justify-content:center;font-size:32px">
        🎬
      </div>
      <div class="bk-info">
        <div class="bk-title">${b.title}</div>
        <div class="bk-detail">📍 ${b.theatre}</div>
        <div class="bk-detail">🗓️ ${b.dt}</div>
        <div class="bk-seats">💺 ${b.seats} · ${b.format || '2D'}</div>
        <div style="display:flex;align-items:center;gap:12px;margin-top:8px">
          <span class="bk-status ${b.status}">${b.status.toUpperCase()}</span>
          <span class="bk-amount">₹${b.amount}</span>
        </div>
        <div style="display:flex;gap:8px;margin-top:12px">
          <button class="btn-view-tkt" onclick="viewBookingTicket('${b.id}')">View Ticket</button>
          ${b.status === 'confirmed' ? `<button class="btn-skip" style="padding:6px 12px;font-size:12px" onclick="cancelCustomerBooking('${b.id}')">Cancel</button>` : ''}
        </div>
      </div>
    </div>
  `).join('');
}

function setBTab(tab) {
  state.bookingTab = tab;
  document.querySelectorAll('.btab').forEach(b => b.classList.remove('active'));
  document.getElementById('bta-' + tab)?.classList.add('active');
  renderBookings();
}

function viewBookingTicket(bkId) {
  const b = state.bookings.find(x => x.id === bkId);
  if (!b) return;
  showTicket(b);
}

async function cancelCustomerBooking(ref) {
  if (!confirm(`Are you sure you want to cancel booking ${ref}?`)) return;

  try {
    await window.VibeCheckApi.bookings.cancelBooking(ref);
    showToast(`Booking ${ref} cancelled successfully.`);
  } catch (e) {
    console.warn('[VibeCheck] Local cancel applied:', e.message);
  }

  const b = state.bookings.find(x => x.id === ref);
  if (b) b.status = 'cancelled';
  saveBookingsToStorage();
  renderBookings();
}

// ============================================================
// USER PROFILE
// ============================================================
function loadProfileForm() {
  const u = state.currentUser;
  if (!u) {
    showPage('home');
    openLoginModal();
    return;
  }
  const name = u.name || `${u.firstName || ''} ${u.lastName || ''}`.trim() || 'Customer';
  document.getElementById('pName').value = name;
  document.getElementById('pEmail').value = u.email || '';
  document.getElementById('pMobile').value = u.phoneNumber || u.mobile || '';
  document.getElementById('profileAv').textContent = (name[0] || 'U').toUpperCase();
}

function saveProfile() {
  if (!state.currentUser) return;
  state.currentUser.name = document.getElementById('pName').value;
  state.currentUser.email = document.getElementById('pEmail').value;
  state.currentUser.phoneNumber = document.getElementById('pMobile').value;
  saveUserToStorage();

  const name = state.currentUser.name;
  document.getElementById('ddName').textContent = name;
  document.getElementById('ddEmail').textContent = state.currentUser.email;
  document.getElementById('userAvatar').textContent = (name[0] || 'U').toUpperCase();
  showToast('✅ Profile updated successfully!');
}

// ============================================================
// AUTHENTICATION (REAL GATEWAY & AUTH-SERVICE INTEGRATION)
// ============================================================
function openLoginModal() {
  document.getElementById('loginOverlay').classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

function closeLoginModal() {
  document.getElementById('loginOverlay').classList.add('hidden');
  document.body.style.overflow = '';
}

function setLTab(tab) {
  state.loginTab = tab;
  document.querySelectorAll('.ltab').forEach(b => b.classList.remove('active'));
  document.getElementById('lt-' + tab)?.classList.add('active');

  const titleEl = document.getElementById('loginModalTitle');
  if (tab === 'signup') {
    if (titleEl) titleEl.textContent = 'Create New Account';
  } else if (tab === 'mobile') {
    if (titleEl) titleEl.textContent = 'Sign In with Mobile OTP';
  } else {
    if (titleEl) titleEl.textContent = 'Sign In to VibeCheck';
  }

  const pSignin = document.getElementById('lp-signin');
  const pSignup = document.getElementById('lp-signup');
  const pMobile = document.getElementById('lp-mobile');

  if (pSignin) pSignin.style.display = tab === 'signin' ? '' : 'none';
  if (pSignup) pSignup.style.display = tab === 'signup' ? '' : 'none';
  if (pMobile) pMobile.style.display = tab === 'mobile' ? '' : 'none';
}

function sendOTP() {
  const mobile = document.getElementById('mobileIn').value.trim();
  if (mobile.length !== 10 || !/^\d+$/.test(mobile)) {
    showToast('Please enter a valid 10-digit mobile number');
    return;
  }
  document.getElementById('otpTo').textContent = mobile;
  document.getElementById('ls1').style.display = 'none';
  document.getElementById('ls2').style.display = '';
  document.querySelector('.ob')?.focus();
  showToast(`Test OTP: Use 123456 (Sent to +91 ${mobile})`);
}

function obInput(el, i) {
  if (el.value.length === 1) {
    const next = document.querySelectorAll('.ob')[i + 1];
    if (next) next.focus();
  }
  const boxes = document.querySelectorAll('.ob');
  const otp = Array.from(boxes).map(b => b.value).join('');
  if (otp.length === 6) verifyOTP();
}

async function verifyOTP() {
  showToast('ℹ️ Mobile OTP service is currently migrating. Please sign in or register with your Email & Password.');
  setLTab('signin');
}

// REAL EMAIL LOGIN
async function emailLogin() {
  const email = document.getElementById('emIn').value.trim();
  const password = document.getElementById('pwIn').value;

  if (!email || !password) {
    showToast('⚠️ Please enter both email and password');
    return;
  }

  const btn = event?.currentTarget || document.querySelector('#lf-login .btn-otp') || document.querySelector('#lp-signin .btn-otp');
  const originalText = btn ? btn.textContent : 'Sign In';
  if (btn) {
    btn.disabled = true;
    btn.textContent = 'Signing In...';
  }

  try {
    const authRes = await window.VibeCheckApi.auth.login({ email, password });
    window.VibeCheckApi.Storage.saveAuth(authRes);

    if (!authRes || !authRes.user || !authRes.user.id) {
      throw new Error('Authentication response did not contain user profile');
    }

    loginUser(authRes.user);
    showToast(`👋 Welcome back, ${authRes.user.firstName || 'User'}!`);
  } catch (err) {
    console.warn('[VibeCheck Auth] Login failed:', err.message);
    const msg = err.message || '';
    if (err.status === 401 || msg.includes('401') || msg.toLowerCase().includes('invalid')) {
      showToast('❌ Invalid email or password. New user? Click "Create Account" above.');
      // Auto populate signup email if empty
      const suEmail = document.getElementById('suEmail');
      if (suEmail && !suEmail.value) suEmail.value = email;
    } else {
      showToast('❌ ' + (msg || 'Login failed'));
    }
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.textContent = originalText;
    }
  }
}

// REAL EMAIL REGISTRATION
async function emailSignup() {
  const fullName = document.getElementById('suName').value.trim();
  const email = document.getElementById('suEmail').value.trim();
  const phone = document.getElementById('suPhone')?.value.trim() || '9876543210';
  const password = document.getElementById('suPw').value;

  if (!fullName || !email || !password) {
    showToast('⚠️ Please fill in all required fields (Name, Email, Password)');
    return;
  }
  if (!email.includes('@') || !email.includes('.')) {
    showToast('⚠️ Please enter a valid email address');
    return;
  }
  if (password.length < 6) {
    showToast('⚠️ Password must be at least 6 characters');
    return;
  }

  const nameParts = fullName.split(' ').filter(Boolean);
  const firstName = nameParts[0] || 'User';
  const lastName = nameParts.slice(1).join(' ') || 'Customer';

  // Format phone number to clean format
  let cleanPhone = phone.replace(/[^0-9]/g, '');
  if (cleanPhone.length > 10) cleanPhone = cleanPhone.slice(-10);
  const formattedPhone = '+91' + cleanPhone;

  const btn = event?.currentTarget || document.querySelector('#lf-signup .btn-otp') || document.querySelector('#lp-signup .btn-otp');
  const originalText = btn ? btn.textContent : 'Create Account & Sign In';
  if (btn) {
    btn.disabled = true;
    btn.textContent = 'Creating Account...';
  }

  try {
    const authRes = await window.VibeCheckApi.auth.register({
      email,
      password,
      firstName,
      lastName,
      phoneNumber: formattedPhone,
      roles: ['ROLE_CUSTOMER']
    });

    window.VibeCheckApi.Storage.saveAuth(authRes);

    if (!authRes || !authRes.user || !authRes.user.id) {
      throw new Error('Registration response did not contain user profile');
    }

    loginUser(authRes.user);
    showToast(`🎉 Account created! Welcome, ${authRes.user.firstName}!`);
  } catch (err) {
    console.warn('[VibeCheck Auth] Registration failed:', err.message);
    const msg = err.message || '';
    if (err.status === 409 || msg.toLowerCase().includes('already exists')) {
      showToast('⚠️ Account already exists with this email. Switched to Sign In.');
      setLTab('signin');
      const emIn = document.getElementById('emIn');
      if (emIn) emIn.value = email;
    } else {
      showToast('❌ ' + (msg || 'Could not register account'));
    }
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.textContent = originalText;
    }
  }
}

function socialLogin(provider) {
  showToast('ℹ️ ' + provider + ' SSO is reserved for Enterprise. Please sign in with Email & Password.');
  setLTab('email');
}

function loginUser(user) {
  state.currentUser = user;
  const displayName = user.name || `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.email || 'Customer';
  state.currentUser.name = displayName;
  saveUserToStorage();

  const loginBtn = document.getElementById('loginBtn');
  if (loginBtn) loginBtn.style.display = 'none';

  const userMenu = document.getElementById('userMenu');
  if (userMenu) userMenu.style.display = '';

  const avatar = document.getElementById('userAvatar');
  if (avatar) avatar.textContent = (user.avatar || displayName[0] || 'U').toUpperCase();

  const ddName = document.getElementById('ddName');
  if (ddName) ddName.textContent = displayName;

  const ddEmail = document.getElementById('ddEmail');
  if (ddEmail) ddEmail.textContent = user.email || user.phoneNumber || '';

  closeLoginModal();
  showToast(`Welcome back, ${displayName}! 👋`);

  // Load real user bookings
  loadUserBookings();

  // If user was pending seat reservation, resume
  if (state.selectedSeats.length > 0 && state.currentPage === 'booking') {
    goToFood();
  }
}

async function logout(callRemote = true) {
  const userId = state.currentUser?.id;
  if (callRemote && userId) {
    await window.VibeCheckApi.auth.logout(userId);
  } else {
    window.VibeCheckApi.Storage.clearAuth();
  }

  state.currentUser = null;
  localStorage.removeItem('vibecheck_user');

  const loginBtn = document.getElementById('loginBtn');
  if (loginBtn) loginBtn.style.display = '';

  const userMenu = document.getElementById('userMenu');
  if (userMenu) userMenu.style.display = 'none';

  const userDrop = document.getElementById('userDrop');
  if (userDrop) userDrop.style.display = 'none';

  showPage('home');
  showToast('You have been signed out.');
}

function toggleUserDrop() {
  const drop = document.getElementById('userDrop');
  if (!drop) return;
  const isOpen = drop.style.display === 'block';
  drop.style.display = isOpen ? 'none' : 'block';
}

function togglePw(id) {
  const el = document.getElementById(id);
  if (el) el.type = el.type === 'password' ? 'text' : 'password';
}

function showSignup() {
  setLTab('signup');
}

function showEmailLogin() {
  setLTab('signin');
}

// ============================================================
// STORAGE HELPERS
// ============================================================
function saveUserToStorage() {
  if (state.currentUser) {
    localStorage.setItem('vibecheck_user', JSON.stringify(state.currentUser));
  }
}

function loadUserFromStorage() {
  const u = window.VibeCheckApi.Storage.getUser() || localStorage.getItem('vibecheck_user');
  if (u) {
    try {
      const parsed = (typeof u === 'string') ? JSON.parse(u) : u;
      loginUser(parsed);
    } catch (e) {}
  }
  const b = localStorage.getItem('vibecheck_bookings');
  if (b) {
    try { state.bookings = JSON.parse(b); } catch (e) {}
  }
}

function saveBookingsToStorage() {
  localStorage.setItem('vibecheck_bookings', JSON.stringify(state.bookings));
}

// ============================================================
// TOAST NOTIFICATIONS
// ============================================================
let toastTimer;
function showToast(msg) {
  const el = document.getElementById('toast');
  if (!el) return;
  el.textContent = msg;
  el.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove('show'), 3500);
}

// ============================================================
// DROPDOWNS & GLOBAL SEARCH
// ============================================================
function toggleCityDropdown(event) {
  if (event) event.stopPropagation();
  const drop = document.getElementById('cityDrop');
  if (drop) {
    const isShowing = drop.classList.contains('show');
    closeDropdowns();
    if (!isShowing) {
      drop.classList.add('show');
      const qInput = document.getElementById('cityQ');
      if (qInput) {
        qInput.value = '';
        filterCities('');
        setTimeout(() => qInput.focus(), 50);
      }
    }
  }
}

function selectCity(city, event) {
  if (event) event.stopPropagation();
  state.selectedCity = city;
  const selCity = document.getElementById('selCity');
  if (selCity) selCity.textContent = city;
  const heading = document.getElementById('moviesHeading');
  if (heading) heading.textContent = `Now Showing in ${city}`;
  document.querySelectorAll('.ci').forEach(el => {
    el.classList.toggle('active', el.textContent.trim().toLowerCase() === city.toLowerCase());
  });
  closeDropdowns();
  showToast(`City changed to ${city}`);
}

function filterCities(query) {
  const q = query.toLowerCase();
  document.querySelectorAll('.ci').forEach(el => {
    el.style.display = el.textContent.toLowerCase().includes(q) ? '' : 'none';
  });
}

function handleCitySearchKey(event) {
  if (event.key === 'Enter') {
    event.preventDefault();
    const firstVisible = Array.from(document.querySelectorAll('.ci')).find(el => el.style.display !== 'none');
    if (firstVisible) {
      selectCity(firstVisible.textContent.trim(), event);
    }
  }
}

function handleSearch(q) {
  const res = document.getElementById('searchResults');
  if (!res) return;
  if (!q.trim()) { res.style.display = 'none'; return; }

  const filtered = state.movies.filter(m => m.title.toLowerCase().includes(q.toLowerCase()));
  if (filtered.length === 0) {
    res.innerHTML = '<div style="padding:12px;color:#999">No matching movies found</div>';
  } else {
    res.innerHTML = filtered.map(m => `
      <div class="search-item" onclick="openMovieDetail('${m.id}')" style="display:flex;align-items:center;gap:12px;padding:8px;cursor:pointer">
        <span style="font-size:20px">${m.emoji || '🎬'}</span>
        <div><strong>${m.title}</strong><br><small style="color:#999">${m.genre.join(', ')}</small></div>
      </div>
    `).join('');
  }
  res.style.display = 'block';
}

function setTab(tab) {
  document.querySelectorAll('.hnav').forEach(n => n.classList.remove('active'));
  document.getElementById('hn-' + tab)?.classList.add('active');
  if (tab !== 'movies') {
    showToast(`Showing ${tab.toUpperCase()} catalog.`);
  }
}

function closeDropdowns() {
  document.getElementById('cityDrop')?.classList.remove('show');
  document.getElementById('searchResults')?.style.setProperty('display', 'none');
  const userDrop = document.getElementById('userDrop');
  if (userDrop) userDrop.style.display = 'none';
}
