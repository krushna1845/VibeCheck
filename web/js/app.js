/* =============================================
   VIBECHECK — Full App Logic
   BookMyShow-style SPA
   ============================================= */

'use strict';

// ============================================================
// DATA
// ============================================================
const MOVIES = [
  {
    id: 0, title: 'Interstellar Returns', genre: ['Sci-Fi', 'Drama'],
    lang: ['English', 'Hindi'], formats: ['IMAX', '2D', '3D'],
    rating: 9.1, votes: '184K', cert: 'PG-13', duration: '2h 49m',
    filter: 'now', emoji: '🚀',
    poster: 'https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=300&q=80',
    heroBg: 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1400&q=80',
    desc: 'A team of explorers travel through a wormhole in space in an attempt to ensure humanity\'s survival as Earth becomes uninhabitable. A breathtaking journey through time, space and love.',
    cast: ['Matthew McConaughey', 'Anne Hathaway', 'Jessica Chastain', 'Michael Caine'],
    director: 'Christopher Nolan'
  },
  {
    id: 1, title: 'The Dark Horizon', genre: ['Action', 'Thriller'],
    lang: ['Hindi', 'English'], formats: ['2D', '3D', '4DX'],
    rating: 8.4, votes: '92K', cert: 'UA', duration: '2h 15m',
    filter: 'now', emoji: '⚔️',
    poster: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=300&q=80',
    heroBg: 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1400&q=80',
    desc: 'When a shadow organization threatens to plunge the world into darkness, one man must rise against impossible odds. A high-octane action thriller that will keep you on the edge of your seat.',
    cast: ['Hrithik Roshan', 'Deepika Padukone', 'Ranveer Singh'],
    director: 'Siddharth Anand'
  },
  {
    id: 2, title: 'Echoes of Eternity', genre: ['Sci-Fi', 'Horror'],
    lang: ['English'], formats: ['IMAX', '2D'],
    rating: 8.7, votes: '67K', cert: 'A', duration: '2h 32m',
    filter: 'top', emoji: '👁️',
    poster: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=300&q=80',
    heroBg: 'https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=1400&q=80',
    desc: 'A scientist discovers that parallel universes are bleeding into each other, causing terrifying anomalies. A masterful blend of science fiction and psychological horror.',
    cast: ['Cillian Murphy', 'Florence Pugh', 'Robert Downey Jr.'],
    director: 'Denis Villeneuve'
  },
  {
    id: 3, title: 'Kal Ho Na Ho 2', genre: ['Romance', 'Drama'],
    lang: ['Hindi'], formats: ['2D'],
    rating: 8.9, votes: '210K', cert: 'U', duration: '2h 58m',
    filter: 'top', emoji: '❤️',
    poster: 'https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=300&q=80',
    heroBg: 'https://images.unsplash.com/photo-1481833761820-0509d3217039?w=1400&q=80',
    desc: 'A heartwarming sequel that picks up where the original left off, exploring life, loss and love across generations. A tearjerker that will leave you smiling.',
    cast: ['Shah Rukh Khan', 'Preity Zinta', 'Saif Ali Khan'],
    director: 'Karan Johar'
  },
  {
    id: 4, title: 'Pushpa 3', genre: ['Action', 'Drama'],
    lang: ['Telugu', 'Hindi', 'Tamil'], formats: ['2D', '3D'],
    rating: 8.5, votes: '340K', cert: 'UA', duration: '3h 12m',
    filter: 'upcoming', emoji: '🔥',
    poster: 'https://images.unsplash.com/photo-1524601500432-1e1a4c71d692?w=300&q=80',
    heroBg: 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=1400&q=80',
    desc: 'The saga continues as Pushpa Raj faces new enemies and old rivals in a battle for supremacy. Bigger, louder and more electrifying than ever before.',
    cast: ['Allu Arjun', 'Rashmika Mandanna', 'Fahadh Faasil'],
    director: 'Sukumar'
  },
  {
    id: 5, title: 'Spider-Man: New Universe', genre: ['Action', 'Comedy'],
    lang: ['English', 'Hindi', 'Tamil', 'Telugu'], formats: ['IMAX', '3D', '4DX', '2D'],
    rating: 9.0, votes: '520K', cert: 'U', duration: '2h 24m',
    filter: 'upcoming', emoji: '🕷️',
    poster: 'https://images.unsplash.com/photo-1531259683007-016a7b628fc3?w=300&q=80',
    heroBg: 'https://images.unsplash.com/photo-1559583985-c80d8ad9b29f?w=1400&q=80',
    desc: 'Miles Morales returns in a multiverse-spanning adventure that tests his limits and reveals his true destiny. The most ambitious Spider-Man film ever made.',
    cast: ['Tom Holland', 'Zendaya', 'Benedict Cumberbatch'],
    director: 'Jon Watts'
  },
  {
    id: 6, title: 'Laapataa Ladies 2', genre: ['Comedy', 'Drama'],
    lang: ['Hindi'], formats: ['2D'],
    rating: 8.6, votes: '88K', cert: 'U', duration: '2h 04m',
    filter: 'now', emoji: '🌾',
    poster: 'https://images.unsplash.com/photo-1601758174493-49d2c36cc6ac?w=300&q=80',
    heroBg: 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&q=80',
    desc: 'A delightful follow-up bringing more laugh-out-loud situations as new characters navigate the chaos of modern rural India. A feel-good film for the whole family.',
    cast: ['Nitanshi Goel', 'Pratibha Ranta', 'Sparsh Shrivastava'],
    director: 'Kiran Rao'
  },
  {
    id: 7, title: 'Animal Kingdom', genre: ['Thriller', 'Action'],
    lang: ['Hindi', 'English'], formats: ['2D', '3D'],
    rating: 7.8, votes: '155K', cert: 'A', duration: '3h 21m',
    filter: 'now', emoji: '🐅',
    poster: 'https://images.unsplash.com/photo-1532635241-17e820acc59f?w=300&q=80',
    heroBg: 'https://images.unsplash.com/photo-1485846234645-a62644f84728?w=1400&q=80',
    desc: 'A visceral, intense exploration of power, obsession and family loyalty pushed to its absolute limits. Not for the faint-hearted.',
    cast: ['Ranbir Kapoor', 'Anil Kapoor', 'Bobby Deol', 'Rashmika Mandanna'],
    director: 'Sandeep Reddy Vanga'
  }
];

const THEATRES = [
  { name: 'PVR: Phoenix Mall', features: ['Dolby Atmos', 'M-Ticket'] },
  { name: 'INOX: Megaplex', features: ['4K', 'Recliner'] },
  { name: 'Cinepolis: Grand', features: ['IMAX', 'D-BOX'] },
  { name: 'MovieTime: Central', features: ['2D', '3D'] }
];

const SHOW_TIMES = ['10:00 AM', '12:30 PM', '03:15 PM', '06:45 PM', '09:30 PM', '11:55 PM'];

const SEAT_CATEGORIES = [
  { name: 'Recliner', price: 550, rows: ['A', 'B'], cols: 8 },
  { name: 'Gold', price: 350, rows: ['C', 'D', 'E', 'F'], cols: 14 },
  { name: 'Silver', price: 200, rows: ['G', 'H', 'I', 'J', 'K'], cols: 16 }
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
  { emoji: '🏏', title: 'IPL Final 2026', sub: 'Wankhede · Dec 20', price: 'From ₹1,500' },
  { emoji: '🎭', title: 'Aladdin — The Musical', sub: 'Delhi · Dec 5–8', price: 'From ₹750' },
  { emoji: '🎻', title: 'A.R. Rahman Symphony', sub: 'Hyderabad · Nov 22', price: 'From ₹599' }
];

// ============================================================
// STATE
// ============================================================
let state = {
  currentPage: 'home',
  currentUser: null,
  selectedMovie: null,
  selectedTheatre: null,
  selectedDate: null,
  selectedTime: null,
  selectedSeats: [],
  foodCart: {},
  bookings: [],
  movieFilter: 'now',
  heroSlide: 0,
  heroTimer: null,
  loginTab: 'mobile',
  bookingTab: 'upcoming',
  selectedPayMethod: null,
  selectedUPIApp: null,
  selectedBank: null,
  convFeeRate: 0.05
};

// ============================================================
// INIT
// ============================================================
document.addEventListener('DOMContentLoaded', () => {
  renderMovies();
  renderEvents();
  startHeroTimer();
  loadUserFromStorage();
  populateDates();
  document.addEventListener('click', closeDropdowns);
});

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
  slides[state.heroSlide].classList.remove('active');
  dots[state.heroSlide].classList.remove('active');
  state.heroSlide = (state.heroSlide + 1) % slides.length;
  slides[state.heroSlide].classList.add('active');
  dots[state.heroSlide].classList.add('active');
  updateHeroTransform();
}

function prevSlide() {
  const slides = document.querySelectorAll('.hslide');
  const dots = document.querySelectorAll('.hdot');
  slides[state.heroSlide].classList.remove('active');
  dots[state.heroSlide].classList.remove('active');
  state.heroSlide = (state.heroSlide - 1 + slides.length) % slides.length;
  slides[state.heroSlide].classList.add('active');
  dots[state.heroSlide].classList.add('active');
  updateHeroTransform();
}

function goSlide(i) {
  const slides = document.querySelectorAll('.hslide');
  const dots = document.querySelectorAll('.hdot');
  slides[state.heroSlide].classList.remove('active');
  dots[state.heroSlide].classList.remove('active');
  state.heroSlide = i;
  slides[state.heroSlide].classList.add('active');
  dots[state.heroSlide].classList.add('active');
  updateHeroTransform();
}

function updateHeroTransform() {
  document.getElementById('hSlides').style.transform = `translateX(-${state.heroSlide * 100}%)`;
}

// ============================================================
// CITY SELECTOR
// ============================================================
function toggleCityDropdown(e) {
  e.stopPropagation();
  const drop = document.getElementById('cityDrop');
  drop.style.display = drop.style.display === 'block' ? 'none' : 'block';
}

function selectCity(city) {
  document.getElementById('selCity').textContent = city;
  document.getElementById('cityDrop').style.display = 'none';
  document.querySelector('.ci.active')?.classList.remove('active');
  event.target.classList.add('active');
  document.getElementById('moviesHeading').textContent = `Now Showing in ${city}`;
  showToast(`📍 Location changed to ${city}`);
}

function filterCities(q) {
  document.querySelectorAll('.ci').forEach(el => {
    el.style.display = el.textContent.toLowerCase().includes(q.toLowerCase()) ? '' : 'none';
  });
}

function closeDropdowns(e) {
  if (!e.target.closest('#citySel')) {
    document.getElementById('cityDrop').style.display = 'none';
  }
  if (!e.target.closest('#userMenu')) {
    document.getElementById('userDrop')?.classList.remove('open');
    document.getElementById('userDrop').style.display = 'none';
  }
  if (!e.target.closest('.search-wrap')) {
    document.getElementById('searchResults').innerHTML = '';
  }
}

// ============================================================
// SEARCH
// ============================================================
function handleSearch(q) {
  const box = document.getElementById('searchResults');
  if (!q.trim()) { box.innerHTML = ''; return; }
  const results = MOVIES.filter(m =>
    m.title.toLowerCase().includes(q.toLowerCase()) ||
    m.genre.some(g => g.toLowerCase().includes(q.toLowerCase()))
  );
  box.innerHTML = results.map(m => `
    <div class="sr-item" onclick="openMovieDetail(${m.id}); document.getElementById('globalSearch').value=''; document.getElementById('searchResults').innerHTML='';">
      <span style="font-size:20px">${m.emoji}</span>
      <div style="flex:1">
        <div style="font-weight:600;font-size:13px">${m.title}</div>
        <div style="font-size:11px;color:#999">${m.genre.join(' · ')} · ${m.lang[0]}</div>
      </div>
      <span class="sr-type">Movie</span>
    </div>
  `).join('') || '<div class="sr-item" style="color:#999;justify-content:center;">No results found</div>';
}

// ============================================================
// NAV TABS
// ============================================================
function setTab(tab) {
  document.querySelectorAll('.hnav').forEach(el => el.classList.remove('active'));
  document.getElementById('hn-' + tab)?.classList.add('active');
  if (tab !== 'movies') showToast(`${tab.charAt(0).toUpperCase() + tab.slice(1)} section coming soon!`);
}

// ============================================================
// MOVIE FILTERS
// ============================================================
function setFilter(f) {
  state.movieFilter = f;
  document.querySelectorAll('.ftab').forEach(el => el.classList.remove('active'));
  document.getElementById('ft-' + f)?.classList.add('active');
  const titles = { now: 'Now Showing', upcoming: 'Upcoming Movies', top: 'Top Rated Movies' };
  const city = document.getElementById('selCity').textContent;
  document.getElementById('moviesHeading').textContent = `${titles[f]} in ${city}`;
  renderMovies();
}

function applyFilters() { renderMovies(); }

function renderMovies() {
  const genre = document.getElementById('genreSel')?.value;
  const lang = document.getElementById('langSel')?.value;
  const fmt = document.getElementById('fmtSel')?.value;

  let movies = MOVIES.filter(m => m.filter === state.movieFilter);
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
    <div class="movie-card" onclick="openMovieDetail(${m.id})">
      <div class="mc-poster-ph">${m.emoji}</div>
      <div class="mc-body">
        <div class="mc-title">${m.title}</div>
        <div class="mc-genres">${m.genre.join(' / ')} · ${m.lang[0]}</div>
        <div class="mc-rating">
          <span class="mc-stars">${getStars(m.rating)}</span>
          <span class="mc-score">${m.rating}/10</span>
          <span class="mc-votes">(${m.votes})</span>
        </div>
        <button class="btn-mc-book" onclick="event.stopPropagation();openMovieDetail(${m.id})">Book tickets</button>
      </div>
    </div>
  `).join('');
}

function getStars(rating) {
  const full = Math.floor(rating / 2);
  const half = rating % 2 >= 0.5 ? 1 : 0;
  return '★'.repeat(full) + (half ? '½' : '') + '☆'.repeat(5 - full - half);
}

// ============================================================
// EVENTS
// ============================================================
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
// MOVIE DETAIL MODAL
// ============================================================
function openMovieDetail(id) {
  const m = MOVIES.find(x => x.id === id);
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
  renderTheatreShows(m);

  document.getElementById('movieOverlay').classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

function closeMovieModal() {
  document.getElementById('movieOverlay').classList.add('hidden');
  document.body.style.overflow = '';
}

// ============================================================
// DATE TABS
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

  document.getElementById('dateTabs').innerHTML = dates.map((d, i) => `
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
// THEATRE SHOWS
// ============================================================
function renderTheatreShows(movie) {
  const formats = movie.formats;
  document.getElementById('theatreShows').innerHTML = THEATRES.map((t, ti) => `
    <div class="th-show">
      <div class="th-name">
        <span>${t.name}</span>
        <div class="th-features">${t.features.map(f => `<span class="th-feat">${f}</span>`).join('')}</div>
      </div>
      <div class="time-slots">
        ${SHOW_TIMES.slice(0, 4 + ti % 3).map(ts => `
          <button class="time-slot" onclick="selectShowTime('${t.name}', '${ts}', this, ${movie.id})">
            ${ts}
          </button>
        `).join('')}
      </div>
      <div style="margin-top:8px;font-size:11px;color:#999">
        ${formats.map(f => `<span style="margin-right:8px">${f}</span>`).join('')}
      </div>
    </div>
  `).join('');
}

function selectShowTime(theatre, time, el, movieId) {
  if (!state.currentUser) {
    closeMovieModal();
    openLoginModal();
    showToast('Please sign in to book tickets');
    return;
  }
  document.querySelectorAll('.time-slot').forEach(s => s.classList.remove('active'));
  el.classList.add('active');
  state.selectedTheatre = theatre;
  state.selectedTime = time;
  setTimeout(() => {
    closeMovieModal();
    startBooking();
  }, 400);
}

// ============================================================
// BOOKING FLOW
// ============================================================
function startBooking() {
  if (!state.currentUser) { openLoginModal(); showToast('Please sign in to continue'); return; }
  if (!state.selectedMovie) return;

  // Reset
  state.selectedSeats = [];
  state.foodCart = {};

  // Set step 1
  setStep(1);
  showPage('booking');

  // Populate sidebar
  const m = state.selectedMovie;
  document.getElementById('seatMovieName').textContent = m.title;
  document.getElementById('seatInfo').textContent = `${state.selectedTheatre || 'PVR: Phoenix Mall'} · ${state.selectedDate || 'Today'} · ${state.selectedTime || '06:45 PM'}`;

  ['sc1', 'sc3'].forEach(prefix => {
    const el = document.getElementById(prefix + 'Title');
    if (el) el.textContent = m.title;
    const th = document.getElementById(prefix + 'Theatre');
    if (th) th.textContent = state.selectedTheatre || 'PVR: Phoenix Mall';
    const dt = document.getElementById(prefix + 'DateTime');
    if (dt) dt.textContent = `${state.selectedDate || 'Today'} · ${state.selectedTime || '06:45 PM'}`;
    const poster = document.getElementById(prefix.replace('sc', 'sc') + 'Poster') || document.getElementById(prefix + 'Poster');
    if (poster) poster.src = m.poster;
  });

  renderSeatMap();
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
  if (n === 2) document.getElementById('stepFood').classList.remove('hidden');
  if (n === 3) document.getElementById('stepPayment').classList.remove('hidden');
}

// ============================================================
// SEAT MAP
// ============================================================
function renderSeatMap() {
  const bookedSeats = generateBookedSeats();
  let html = '';

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
        const isBooked = bookedSeats.has(seatId);
        rowHtml += `<button class="seat ${isBooked ? 'booked' : ''}" id="seat-${seatId}"
          data-id="${seatId}" data-price="${cat.price}" data-cat="${cat.name}"
          onclick="${isBooked ? '' : `toggleSeat(this)`}"
          title="${isBooked ? 'Booked' : seatId}">${c}</button>`;
      }
      rowHtml += `</div>`;
      html += rowHtml;
    });

    html += `</div>`;
  });

  document.getElementById('seatMap').innerHTML = html;
}

function generateBookedSeats() {
  const booked = new Set();
  const allSeats = [];
  SEAT_CATEGORIES.forEach(cat => {
    cat.rows.forEach(row => {
      for (let c = 1; c <= cat.cols; c++) allSeats.push(`${row}${c}`);
    });
  });
  const count = Math.floor(allSeats.length * 0.35);
  const shuffled = allSeats.sort(() => Math.random() - 0.5);
  shuffled.slice(0, count).forEach(s => booked.add(s));
  return booked;
}

function toggleSeat(el) {
  const id = el.dataset.id;
  const price = parseInt(el.dataset.price);
  const cat = el.dataset.cat;

  if (el.classList.contains('selected')) {
    el.classList.remove('selected');
    state.selectedSeats = state.selectedSeats.filter(s => s.id !== id);
  } else {
    if (state.selectedSeats.length >= 10) { showToast('Maximum 10 seats per booking'); return; }
    el.classList.add('selected');
    state.selectedSeats.push({ id, price, cat });
  }
  updateBookingSummary();
}

function updateBookingSummary() {
  const total = state.selectedSeats.reduce((s, x) => s + x.price, 0);
  const conv = Math.round(total * state.convFeeRate);
  const grand = total + conv;
  const seatsLabel = state.selectedSeats.length ? state.selectedSeats.map(s => s.id).join(', ') : '—';

  document.getElementById('sc1Seats').textContent = seatsLabel;
  document.getElementById('sc1Ticket').textContent = `₹${total}`;
  document.getElementById('sc1Conv').textContent = `₹${conv}`;
  document.getElementById('sc1Total').textContent = `₹${grand}`;

  const btn = document.getElementById('btnProceed');
  btn.disabled = state.selectedSeats.length === 0;
}

// ============================================================
// FOOD
// ============================================================
function goToFood() {
  if (state.selectedSeats.length === 0) { showToast('Please select at least one seat'); return; }
  setStep(2);
  renderFoodGrid();
  updateFoodSummary();
}

function renderFoodGrid() {
  const grid = document.getElementById('foodGrid');
  if (!grid) return;
  grid.innerHTML = FOOD_ITEMS.map((item, i) => `
    <div class="food-item">
      <div class="food-emoji">${item.emoji}</div>
      <div class="food-name">${item.name}</div>
      <div class="food-desc">${item.desc}</div>
      <div class="food-price">₹${item.price}</div>
      <div class="food-controls">
        <button class="food-qty-btn" onclick="changeFood(${i}, -1)">−</button>
        <span class="food-qty" id="fqty-${i}">${state.foodCart[i] || 0}</span>
        <button class="food-qty-btn" onclick="changeFood(${i}, 1)">+</button>
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

  document.getElementById('sc2Ticket').textContent = `₹${ticketTotal}`;
  document.getElementById('sc2Food').textContent = `₹${foodTotal}`;
  document.getElementById('sc2Conv').textContent = `₹${conv}`;
  document.getElementById('sc2Total').textContent = `₹${grand}`;
}

function skipFood() {
  state.foodCart = {};
  goToPayment();
}

// ============================================================
// PAYMENT
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
  const m = state.selectedMovie;

  document.getElementById('sc3Title').textContent = m.title;
  document.getElementById('sc3Theatre').textContent = state.selectedTheatre || 'PVR: Phoenix Mall';
  document.getElementById('sc3DateTime').textContent = `${state.selectedDate || 'Today'} · ${state.selectedTime || '06:45 PM'}`;
  document.getElementById('sc3Seats').textContent = state.selectedSeats.map(s => s.id).join(', ');
  document.getElementById('sc3Ticket').textContent = `₹${ticketTotal}`;
  document.getElementById('sc3Conv').textContent = `₹${conv}`;
  document.getElementById('sc3Total').textContent = `₹${grand}`;
  document.getElementById('payAmt').textContent = `₹${grand}`;

  const sc3Poster = document.getElementById('sc3Poster');
  if (sc3Poster) sc3Poster.src = m.poster;

  if (foodTotal > 0) {
    document.getElementById('sc3FoodRow').style.display = '';
    document.getElementById('sc3Food').textContent = `₹${foodTotal}`;
  } else {
    document.getElementById('sc3FoodRow').style.display = 'none';
  }
}

function togglePay(section) {
  const body = document.getElementById('pb-' + section);
  const chev = document.getElementById('chev-' + section);
  const isOpen = !body.classList.contains('hidden');
  body.classList.toggle('hidden');
  chev.classList.toggle('open', !isOpen);
}

function selectUPIApp(app) {
  state.selectedUPIApp = app;
  state.selectedPayMethod = 'upi';
  document.querySelectorAll('.upi-btn').forEach(b => b.style.borderColor = '');
  event.currentTarget.style.borderColor = 'var(--red)';
  document.getElementById('upiIdRow').style.display = app === 'other' ? '' : 'none';
  if (app !== 'other') showToast(`${app === 'gpay' ? 'Google Pay' : app === 'phonepe' ? 'PhonePe' : 'Paytm'} selected`);
}

function verifyUPI() {
  const val = document.getElementById('upiIdVal').value.trim();
  if (!val || !val.includes('@')) { showToast('Please enter a valid UPI ID'); return; }
  state.selectedPayMethod = 'upi';
  showToast('✅ UPI ID verified!');
}

function selectBank(el, bank) {
  state.selectedBank = bank;
  state.selectedPayMethod = 'bank';
  document.querySelectorAll('.bank-btn').forEach(b => b.classList.remove('active-bank'));
  el.classList.add('active-bank');
}

function fmtCard(el) {
  let v = el.value.replace(/\D/g, '');
  v = v.match(/.{1,4}/g)?.join(' ') || v;
  el.value = v;
  if (v.replace(/\s/g, '').length === 16) state.selectedPayMethod = 'card';
}

function fmtExpiry(el) {
  let v = el.value.replace(/\D/g, '');
  if (v.length >= 2) v = v.slice(0, 2) + ' / ' + v.slice(2);
  el.value = v;
}

function processPayment() {
  // Validate a method is selected
  const ticketTotal = state.selectedSeats.reduce((s, x) => s + x.price, 0);
  const foodTotal = Object.entries(state.foodCart).reduce((s, [i, qty]) => s + FOOD_ITEMS[i].price * qty, 0);
  const conv = Math.round(ticketTotal * state.convFeeRate);
  const grand = ticketTotal + foodTotal + conv;

  // Show processing
  document.getElementById('payOverlay').classList.remove('hidden');

  setTimeout(() => {
    document.getElementById('payOverlay').classList.add('hidden');
    confirmBooking(grand);
  }, 2200);
}

// ============================================================
// BOOKING CONFIRMATION
// ============================================================
function confirmBooking(amount) {
  const bkId = 'VC' + Date.now().toString().slice(-8).toUpperCase();
  const m = state.selectedMovie;
  const theatre = state.selectedTheatre || 'PVR: Phoenix Mall';
  const dt = `${state.selectedDate || new Date().toDateString()} · ${state.selectedTime || '06:45 PM'}`;
  const seats = state.selectedSeats.map(s => s.id).join(', ');
  const format = m.formats[0];

  // Save to bookings
  const booking = {
    id: bkId, movieId: m.id, title: m.title, poster: m.poster,
    theatre, dt, seats, format, amount,
    status: 'confirmed',
    createdAt: new Date()
  };
  state.bookings.unshift(booking);
  saveBookingsToStorage();

  // Populate ticket
  document.getElementById('bkId').textContent = bkId;
  document.getElementById('et-movie').textContent = m.title;
  document.getElementById('et-format').textContent = format;
  document.getElementById('et-dt').textContent = dt;
  document.getElementById('et-theatre').textContent = theatre;
  document.getElementById('et-seats').textContent = seats;
  document.getElementById('et-amount').textContent = `₹${amount}`;

  // Draw QR
  drawQR(bkId);

  // Show ticket modal
  document.getElementById('ticketOverlay').classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

function closeTicketModal() {
  document.getElementById('ticketOverlay').classList.add('hidden');
  document.body.style.overflow = '';
  showPage('home');
}

// Simple QR drawing
function drawQR(data) {
  const canvas = document.getElementById('qrCanvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const size = 110;
  ctx.fillStyle = '#fff';
  ctx.fillRect(0, 0, size, size);
  ctx.fillStyle = '#1C1C1C';

  // Position detection patterns
  function drawFinder(x, y) {
    ctx.fillRect(x, y, 21, 21);
    ctx.fillStyle = '#fff';
    ctx.fillRect(x+3, y+3, 15, 15);
    ctx.fillStyle = '#1C1C1C';
    ctx.fillRect(x+6, y+6, 9, 9);
  }
  const scale = size / 110;
  drawFinder(4, 4);
  drawFinder(size-25, 4);
  drawFinder(4, size-25);

  // Data modules from hash
  let hash = 0;
  for (let c of data) hash = (hash * 31 + c.charCodeAt(0)) & 0xffffffff;
  const rng = () => { hash ^= hash << 13; hash ^= hash >> 17; hash ^= hash << 5; return (hash >>> 0) / 0xffffffff; };
  for (let i = 0; i < 8; i++) {
    for (let j = 0; j < 8; j++) {
      if (rng() > 0.5) ctx.fillRect(30 + i * 8, 30 + j * 8, 6, 6);
    }
  }
}

function downloadTicket() {
  const canvas = document.getElementById('qrCanvas');
  const link = document.createElement('a');
  link.download = `VibeCheck-Ticket-${document.getElementById('bkId').textContent}.png`;

  // Create a larger canvas for the full ticket
  const tc = document.createElement('canvas');
  tc.width = 500; tc.height = 320;
  const ctx = tc.getContext('2d');

  // Background
  ctx.fillStyle = '#1C1C1C';
  ctx.fillRect(0, 0, 500, 50);
  ctx.fillStyle = '#f5f5f5';
  ctx.fillRect(0, 50, 500, 270);

  // Title
  ctx.fillStyle = '#fff';
  ctx.font = 'bold 20px Inter, sans-serif';
  ctx.fillText('VibeCheck', 20, 33);
  ctx.fillStyle = '#E31837';
  ctx.fillText('   E-TICKET', 100, 33);

  // Info
  ctx.fillStyle = '#333';
  ctx.font = '13px Inter, sans-serif';
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

  // QR
  ctx.drawImage(canvas, 370, 60, 110, 110);

  link.href = tc.toDataURL('image/png');
  link.click();
  showToast('Ticket downloaded!');
}

// ============================================================
// MY BOOKINGS
// ============================================================
function renderBookings() {
  const tab = state.bookingTab;
  const all = state.bookings;
  const now = new Date();

  let filtered;
  if (tab === 'upcoming') {
    filtered = all.filter(b => b.status === 'confirmed' && new Date(b.createdAt) >= now - 86400000);
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
      <p>${tab === 'upcoming' ? 'Book a movie ticket to see it here!' : 'Your past bookings will appear here.'}</p>
    </div>`;
    return;
  }

  list.innerHTML = filtered.map(b => `
    <div class="bk-card">
      <div class="bk-poster" style="background:#eee;display:flex;align-items:center;justify-content:center;font-size:28px">
        ${MOVIES.find(m => m.id === b.movieId)?.emoji || '🎬'}
      </div>
      <div class="bk-info">
        <div class="bk-title">${b.title}</div>
        <div class="bk-detail">📍 ${b.theatre}</div>
        <div class="bk-detail">🗓️ ${b.dt}</div>
        <div class="bk-seats">💺 ${b.seats} · ${b.format}</div>
        <div style="display:flex;align-items:center;gap:12px;margin-top:8px">
          <span class="bk-status ${b.status}">${b.status.charAt(0).toUpperCase() + b.status.slice(1)}</span>
          <span class="bk-amount">₹${b.amount}</span>
        </div>
        <button class="btn-view-tkt" onclick="viewBookingTicket('${b.id}')">View Ticket</button>
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
  document.getElementById('bkId').textContent = b.id;
  document.getElementById('et-movie').textContent = b.title;
  document.getElementById('et-format').textContent = b.format;
  document.getElementById('et-dt').textContent = b.dt;
  document.getElementById('et-theatre').textContent = b.theatre;
  document.getElementById('et-seats').textContent = b.seats;
  document.getElementById('et-amount').textContent = `₹${b.amount}`;
  drawQR(b.id);
  document.getElementById('ticketOverlay').classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

// ============================================================
// PROFILE
// ============================================================
function loadProfileForm() {
  const u = state.currentUser;
  if (!u) return;
  document.getElementById('pName').value = u.name || '';
  document.getElementById('pEmail').value = u.email || '';
  document.getElementById('pMobile').value = u.mobile || '';
  document.getElementById('profileAv').textContent = (u.name || 'U')[0].toUpperCase();
}

function saveProfile() {
  if (!state.currentUser) return;
  state.currentUser.name = document.getElementById('pName').value;
  state.currentUser.email = document.getElementById('pEmail').value;
  state.currentUser.mobile = document.getElementById('pMobile').value;
  saveUserToStorage();
  document.getElementById('ddName').textContent = state.currentUser.name;
  document.getElementById('ddEmail').textContent = state.currentUser.email;
  document.getElementById('userAvatar').textContent = (state.currentUser.name || 'U')[0].toUpperCase();
  showToast('✅ Profile updated successfully!');
}

// ============================================================
// AUTH
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
  document.getElementById('lp-mobile').style.display = tab === 'mobile' ? '' : 'none';
  document.getElementById('lp-email').style.display = tab === 'email' ? '' : 'none';
}

function sendOTP() {
  const mobile = document.getElementById('mobileIn').value.trim();
  if (mobile.length !== 10 || !/^\d+$/.test(mobile)) {
    showToast('Please enter a valid 10-digit mobile number'); return;
  }
  document.getElementById('otpTo').textContent = mobile;
  document.getElementById('ls1').style.display = 'none';
  document.getElementById('ls2').style.display = '';
  document.querySelector('.ob').focus();
  showToast(`OTP sent to +91 ${mobile}`);
}

function obInput(el, i) {
  if (el.value.length === 1) {
    const next = document.querySelectorAll('.ob')[i + 1];
    if (next) next.focus();
  }
  // Auto-verify if all filled
  const boxes = document.querySelectorAll('.ob');
  const otp = Array.from(boxes).map(b => b.value).join('');
  if (otp.length === 6) verifyOTP();
}

function verifyOTP() {
  const otp = Array.from(document.querySelectorAll('.ob')).map(b => b.value).join('');
  if (otp.length < 6) { showToast('Please enter the 6-digit OTP'); return; }
  // Simulate OTP verification (accept any 6-digit OTP)
  const mobile = document.getElementById('mobileIn').value;
  loginUser({ name: 'User', email: '', mobile: '+91 ' + mobile, avatar: 'U' });
}

function emailLogin() {
  const email = document.getElementById('emIn').value.trim();
  const pw = document.getElementById('pwIn').value;
  if (!email || !pw) { showToast('Please enter email and password'); return; }
  const name = email.split('@')[0];
  loginUser({ name: name.charAt(0).toUpperCase() + name.slice(1), email, mobile: '', avatar: name[0].toUpperCase() });
}

function emailSignup() {
  const name = document.getElementById('suName').value.trim();
  const email = document.getElementById('suEmail').value.trim();
  const pw = document.getElementById('suPw').value;
  if (!name || !email || !pw) { showToast('Please fill in all fields'); return; }
  if (pw.length < 6) { showToast('Password must be at least 6 characters'); return; }
  loginUser({ name, email, mobile: '', avatar: name[0].toUpperCase() });
}

function socialLogin(provider) {
  loginUser({ name: 'Google User', email: 'user@gmail.com', mobile: '', avatar: 'G' });
}

function loginUser(user) {
  state.currentUser = user;
  saveUserToStorage();
  document.getElementById('loginBtn').style.display = 'none';
  document.getElementById('userMenu').style.display = '';
  document.getElementById('userAvatar').textContent = user.avatar || user.name[0].toUpperCase();
  document.getElementById('ddName').textContent = user.name;
  document.getElementById('ddEmail').textContent = user.email || user.mobile;
  closeLoginModal();
  showToast(`Welcome back, ${user.name}! 👋`);
}

function logout() {
  state.currentUser = null;
  localStorage.removeItem('vibecheck_user');
  document.getElementById('loginBtn').style.display = '';
  document.getElementById('userMenu').style.display = 'none';
  document.getElementById('userDrop').style.display = 'none';
  showPage('home');
  showToast('You have been signed out.');
}

function toggleUserDrop() {
  const drop = document.getElementById('userDrop');
  const isOpen = drop.style.display === 'block';
  drop.style.display = isOpen ? 'none' : 'block';
}

function togglePw(id) {
  const el = document.getElementById(id);
  el.type = el.type === 'password' ? 'text' : 'password';
}

function showSignup() {
  document.getElementById('lf-login').style.display = 'none';
  document.getElementById('lf-signup').style.display = '';
}

function showEmailLogin() {
  document.getElementById('lf-signup').style.display = 'none';
  document.getElementById('lf-login').style.display = '';
}

// ============================================================
// LOCAL STORAGE
// ============================================================
function saveUserToStorage() {
  if (state.currentUser) localStorage.setItem('vibecheck_user', JSON.stringify(state.currentUser));
}

function loadUserFromStorage() {
  const u = localStorage.getItem('vibecheck_user');
  if (u) {
    try { loginUser(JSON.parse(u)); } catch (e) {}
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
// TOAST
// ============================================================
let toastTimer;
function showToast(msg) {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove('show'), 3000);
}

// ============================================================
// HERO SLIDES CSS DRIVE
// ============================================================
(function initHeroCSS() {
  const style = document.createElement('style');
  style.textContent = `
    .hero-slides { display: flex; width: 100%; transition: transform 0.5s ease; }
    .hslide { min-width: 100%; }
  `;
  document.head.appendChild(style);
})();
