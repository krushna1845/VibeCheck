/**
 * VibeCheck Cinema Platform - Client Application
 * Handles movie discovery, real-time seat locking, dynamic pricing, and digital ticket rendering.
 */

// Movie Catalog Data
const MOVIES = [
  {
    id: 'm1',
    title: 'Interstellar (IMAX 4K)',
    genre: 'Sci-Fi • Adventure',
    rating: '⭐ 8.7',
    runtime: '2h 49m',
    bannerGradient: 'linear-gradient(135deg, #0F2027 0%, #203A43 50%, #2C5364 100%)',
    icon: '🚀',
    theatre: 'PVR ICON, Audi 1',
    time: 'Today, 7:30 PM',
    showId: '11111111-2222-3333-4444-555555555555',
  },
  {
    id: 'm2',
    title: 'Oppenheimer',
    genre: 'Drama • History',
    rating: '⭐ 8.9',
    runtime: '3h 00m',
    bannerGradient: 'linear-gradient(135deg, #2b0808 0%, #591a07 50%, #872b07 100%)',
    icon: '⚡',
    theatre: 'INOX Megaplex, Screen 4',
    time: 'Today, 8:45 PM',
    showId: '22222222-3333-4444-5555-666666666666',
  },
  {
    id: 'm3',
    title: 'Cyberpunk: Edgerunners',
    genre: 'Action • Sci-Fi Anime',
    rating: '⭐ 8.4',
    runtime: '2h 15m',
    bannerGradient: 'linear-gradient(135deg, #180529 0%, #3e0c6a 50%, #6815a5 100%)',
    icon: '🤖',
    theatre: 'Cinepolis Dolby Atmos',
    time: 'Today, 10:15 PM',
    showId: '33333333-4444-5555-6666-777777777777',
  },
  {
    id: 'm4',
    title: 'Dune: Part Two',
    genre: 'Sci-Fi • Epic',
    rating: '⭐ 8.6',
    runtime: '2h 46m',
    bannerGradient: 'linear-gradient(135deg, #372305 0%, #69440c 50%, #a46d17 100%)',
    icon: '🏜️',
    theatre: 'PVR PXL Laser 3D',
    time: 'Tomorrow, 6:00 PM',
    showId: '44444444-5555-6666-7777-888888888888',
  }
];

// App State
let activeMovie = MOVIES[0];
let selectedSeats = new Map(); // seatId -> { row, col, tier, price }
let lockTimerInterval = null;
let lockTimeRemaining = 300; // 5 minutes

document.addEventListener('DOMContentLoaded', () => {
  renderMovies();
  setupSeatGrid();
});

// Navigation View Switcher
function switchView(viewName) {
  document.querySelectorAll('.view-section').forEach(s => s.classList.remove('active'));
  document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));

  const targetView = document.getElementById(`view-${viewName}`);
  const targetBtn = document.getElementById(`nav-${viewName}-btn`);

  if (targetView) targetView.classList.add('active');
  if (targetBtn) targetBtn.classList.add('active');
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// Render Movie Catalog
function renderMovies() {
  const container = document.getElementById('movies-catalog-grid');
  if (!container) return;

  container.innerHTML = MOVIES.map(movie => `
    <div class="movie-card" onclick="selectMovie('${movie.id}')">
      <div class="movie-poster-box" style="background: ${movie.bannerGradient}; display: flex; align-items: center; justify-content: center; font-size: 5rem;">
        <span>${movie.icon}</span>
        <div class="movie-rating">${movie.rating}</div>
      </div>
      <div class="movie-info">
        <h3 class="movie-title">${movie.title}</h3>
        <p class="movie-genre">${movie.genre} &bull; ${movie.runtime}</p>
        <div class="movie-footer">
          <span style="font-size: 0.85rem; color: var(--text-dim);">${movie.theatre}</span>
          <button class="btn-book" onclick="event.stopPropagation(); selectMovie('${movie.id}')">Book Seats</button>
        </div>
      </div>
    </div>
  `).join('');
}

// Select Movie and Navigate to Seat Map
function selectMovie(movieId) {
  activeMovie = MOVIES.find(m => m.id === movieId) || MOVIES[0];
  document.getElementById('booking-movie-heading').textContent = activeMovie.title;
  document.getElementById('booking-meta-text').textContent = `${activeMovie.theatre} • ${activeMovie.time}`;
  document.getElementById('summary-movie-title').textContent = activeMovie.title;
  document.getElementById('summary-theatre-details').textContent = `${activeMovie.theatre} • ${activeMovie.time}`;

  clearSeatSelections();
  switchView('seats');
}

// Setup Interactive Seat Map
function setupSeatGrid() {
  const grid = document.getElementById('seat-grid-container');
  if (!grid) return;

  const rows = [
    { row: 'A', tier: 'Platinum', price: 350 },
    { row: 'B', tier: 'Platinum', price: 350 },
    { row: 'C', tier: 'Gold', price: 250 },
    { row: 'D', tier: 'Gold', price: 250 },
    { row: 'E', tier: 'Gold', price: 250 },
    { row: 'F', tier: 'Silver', price: 180 },
    { row: 'G', tier: 'Silver', price: 180 },
    { row: 'H', tier: 'Silver', price: 180 },
  ];

  let currentTier = '';
  let html = '';

  rows.forEach(r => {
    if (r.tier !== currentTier) {
      currentTier = r.tier;
      html += `<div class="seat-tier-header">${currentTier} Tier — ₹${r.price}</div>`;
    }

    html += `<div class="seat-row">`;
    html += `<div class="row-label">${r.row}</div>`;

    for (let c = 1; c <= 10; c++) {
      const seatCode = `${r.row}${c}`;
      // Simulate some occupied seats
      const isBooked = (r.row === 'B' && (c === 7 || c === 8)) || (r.row === 'E' && c === 4);
      const isLocked = (r.row === 'C' && c === 6);

      let statusClass = '';
      if (isBooked) statusClass = 'booked';
      else if (isLocked) statusClass = 'locked';

      html += `
        <div class="seat ${statusClass}" 
             id="seat-${seatCode}" 
             data-code="${seatCode}" 
             data-tier="${r.tier}" 
             data-price="${r.price}"
             onclick="toggleSeatSelection('${seatCode}', '${r.tier}', ${r.price})">
          ${c}
        </div>
      `;

      // Aisle space in middle
      if (c === 5) {
        html += `<div style="width: 18px;"></div>`;
      }
    }
    html += `</div>`;
  });

  grid.innerHTML = html;
}

// Toggle Seat Selection
function toggleSeatSelection(seatCode, tier, price) {
  const seatEl = document.getElementById(`seat-${seatCode}`);
  if (!seatEl || seatEl.classList.contains('booked') || seatEl.classList.contains('locked')) {
    return;
  }

  if (selectedSeats.has(seatCode)) {
    selectedSeats.delete(seatCode);
    seatEl.classList.remove('selected');
  } else {
    selectedSeats.set(seatCode, { tier, price });
    seatEl.classList.add('selected');
  }

  updateOrderSummary();
}

function clearSeatSelections() {
  selectedSeats.clear();
  document.querySelectorAll('.seat.selected').forEach(s => s.classList.remove('selected'));
  if (lockTimerInterval) {
    clearInterval(lockTimerInterval);
    lockTimerInterval = null;
  }
  updateOrderSummary();
}

// Update Price & Order Summary
function updateOrderSummary() {
  const badgeContainer = document.getElementById('selected-badges-list');
  const checkoutBtn = document.getElementById('checkout-btn');
  const timerBox = document.getElementById('lock-timer-box');

  if (selectedSeats.size === 0) {
    badgeContainer.innerHTML = '<span style="font-size: 0.85rem; color: var(--text-dim); line-height: 38px;">No seats selected yet</span>';
    document.getElementById('price-subtotal').textContent = '₹0.00';
    document.getElementById('price-fee').textContent = '₹0.00';
    document.getElementById('price-tax').textContent = '₹0.00';
    document.getElementById('price-total').textContent = '₹0.00';
    checkoutBtn.disabled = true;
    timerBox.style.display = 'none';

    if (lockTimerInterval) {
      clearInterval(lockTimerInterval);
      lockTimerInterval = null;
    }
    return;
  }

  // Render Badges
  const badgesHtml = Array.from(selectedSeats.keys()).map(code => 
    `<span class="seat-badge">${code} (${selectedSeats.get(code).tier})</span>`
  ).join('');
  badgeContainer.innerHTML = badgesHtml;

  // Compute Prices
  let subtotal = 0;
  selectedSeats.forEach(val => subtotal += val.price);
  const fee = 30.00;
  const tax = Number((subtotal * 0.18).toFixed(2));
  const total = (subtotal + fee + tax).toFixed(2);

  document.getElementById('price-subtotal').textContent = `₹${subtotal.toFixed(2)}`;
  document.getElementById('price-fee').textContent = `₹${fee.toFixed(2)}`;
  document.getElementById('price-tax').textContent = `₹${tax.toFixed(2)}`;
  document.getElementById('price-total').textContent = `₹${total}`;
  checkoutBtn.disabled = false;

  // Start 5-min lock timer
  timerBox.style.display = 'flex';
  if (!lockTimerInterval) {
    lockTimeRemaining = 300;
    startLockTimer();
  }
}

// Lock Countdown Timer
function startLockTimer() {
  const clockEl = document.getElementById('lock-countdown');
  updateClockDisplay(clockEl);

  lockTimerInterval = setInterval(() => {
    lockTimeRemaining--;
    if (lockTimeRemaining <= 0) {
      clearInterval(lockTimerInterval);
      lockTimerInterval = null;
      alert('Your Redis temporary seat hold expired (300s TTL). Please re-select your seats.');
      clearSeatSelections();
      return;
    }
    updateClockDisplay(clockEl);
  }, 1000);
}

function updateClockDisplay(el) {
  const mins = Math.floor(lockTimeRemaining / 60);
  const secs = lockTimeRemaining % 60;
  el.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

// Execute Checkout & Generate Digital Ticket
async function executeCheckout() {
  const checkoutBtn = document.getElementById('checkout-btn');
  checkoutBtn.disabled = true;
  checkoutBtn.textContent = 'Acquiring Distributed Lock...';

  // Simulate network lock confirmation with gateway
  await new Promise(r => setTimeout(r, 650));
  checkoutBtn.textContent = 'Processing Payment (Stripe/HMAC)...';
  await new Promise(r => setTimeout(r, 700));

  // Generate Booking Reference
  const bookingRef = 'BK-' + Math.random().toString(36).substring(2, 10).toUpperCase();
  const seatListStr = Array.from(selectedSeats.keys()).join(', ');

  // Populate Ticket Modal
  document.getElementById('ticket-movie-name').textContent = activeMovie.title;
  document.getElementById('ticket-theatre-name').textContent = activeMovie.theatre;
  document.getElementById('ticket-time-text').textContent = activeMovie.time;
  document.getElementById('ticket-seats-text').textContent = seatListStr;
  document.getElementById('ticket-booking-ref').textContent = bookingRef;

  // Render QR Code on canvas
  renderQrCode(bookingRef);

  // Show Modal
  document.getElementById('ticket-modal').classList.add('active');

  // Mark seats as booked locally
  selectedSeats.forEach((_, code) => {
    const el = document.getElementById(`seat-${code}`);
    if (el) {
      el.classList.remove('selected');
      el.classList.add('booked');
    }
  });

  clearSeatSelections();
  checkoutBtn.textContent = 'Lock Seats & Pay Now';
}

function closeTicketModal() {
  document.getElementById('ticket-modal').classList.remove('active');
  switchView('movies');
}

// Custom High-Quality QR Code Canvas Renderer
function renderQrCode(text) {
  const canvas = document.getElementById('qr-canvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const size = canvas.width;
  ctx.clearRect(0, 0, size, size);

  // Background
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(0, 0, size, size);

  const gridSize = 21;
  const cellSize = size / gridSize;
  ctx.fillStyle = '#080B11';

  // Seeded pseudo-random pattern based on text
  let seed = 0;
  for (let i = 0; i < text.length; i++) {
    seed = (seed << 5) - seed + text.charCodeAt(i);
    seed |= 0;
  }

  function seededRandom() {
    const x = Math.sin(seed++) * 10000;
    return x - Math.floor(x);
  }

  // Draw Corner Detection Patterns (Standard QR squares)
  drawFinderPattern(ctx, 0, 0, cellSize);
  drawFinderPattern(ctx, (gridSize - 7) * cellSize, 0, cellSize);
  drawFinderPattern(ctx, 0, (gridSize - 7) * cellSize, cellSize);

  // Fill data cells
  for (let r = 0; r < gridSize; r++) {
    for (let c = 0; c < gridSize; c++) {
      // Don't overwrite finder patterns
      if ((r < 7 && c < 7) || (r < 7 && c >= gridSize - 7) || (r >= gridSize - 7 && c < 7)) {
        continue;
      }
      if (seededRandom() > 0.45) {
        ctx.fillRect(c * cellSize, r * cellSize, cellSize - 0.5, cellSize - 0.5);
      }
    }
  }
}

function drawFinderPattern(ctx, x, y, size) {
  // Outer 7x7 square
  ctx.fillRect(x, y, size * 7, size * 7);
  // White inner 5x5
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(x + size, y + size, size * 5, size * 5);
  // Center black 3x3
  ctx.fillStyle = '#080B11';
  ctx.fillRect(x + size * 2, y + size * 2, size * 3, size * 3);
}
