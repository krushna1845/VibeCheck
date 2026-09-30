# Milestone 33 — UI Action & Interactive Controls Matrix

**Date:** 2026-09-29  
**Platform:** VibeCheck Movie Booking Platform  
**Target Milestone:** 33 — Production Application E2E Hardening  
**Scope:** Complete inventory of all user-facing buttons, links, forms, and modal interactions  

---

## 1. UI Control Audit Matrix

| Action | Component | Expected Behavior | API / Navigation | Loading State | Error Handling | Duplicate Protection | Result |
|:---|:---|:---|:---|:---:|:---:|:---:|:---:|
| **Click Logo** | Header (`.logo`) | Navigate back to home view | Navigation: `showPage('home')` | Instant | N/A | None needed | **WORKING** |
| **Select City** | Header (`#citySel`) | Open dropdown, select city, update catalog header | Local state + `renderMovies()` | Instant | N/A | Dropdown toggle | **WORKING** |
| **Search Movies** | Header (`#globalSearch`) | Real-time partial matching on movie titles | Search filter on `state.movies` | Instant | "No movies found" | Input debounced | **WORKING** |
| **Click Sign In** | Header (`#loginBtn`) | Open authentication modal | `openLoginModal()` | Instant | N/A | Modal state | **WORKING** |
| **Click User Avatar** | Header (`#userAvatar`) | Toggle user profile & navigation menu | `toggleUserDrop()` | Instant | N/A | Dropdown toggle | **WORKING** |
| **Click My Bookings** | User Menu (`.ud-item`) | Open bookings dashboard and load user history | `showPage('bookings')` -> `loadUserBookings()` | Spinner | Toast alert | Guarded | **WORKING** |
| **Click My Profile** | User Menu (`.ud-item`) | Open profile form | `showPage('profile')` | Instant | Redirect if logged out | Single view | **WORKING** |
| **Click Logout** | User Menu (`.ud-item.red`) | Invalidate session, clear tokens, return to home | `POST /api/v1/auth/logout/{id}` | Toast | Graceful fallback | State cleared | **WORKING** |
| **Header Nav Tabs** | Header (`.hnav`) | Switch between Movies, Events, Sports, Plays | `setTab()` | Instant | Toast alert for sub-catalogs | Tab class toggle | **WORKING** |
| **Hero "Book Tickets"**| Hero Banner (`.btn-hbook`)| Open details modal for featured movie | `openMovieDetail(id)` | Modal | N/A | Click debounce | **WORKING** |
| **Filter Tabs** | Home (`.ftab`) | Filter by Now Showing, Top Rated, Coming Soon | `setFilter()` -> `renderMovies()` | Instant | Empty grid message | Active class toggle | **WORKING** |
| **Dropdown Filters**| Home (`#genreSel`, etc.) | Filter by genre, language, format | `applyFilters()` | Instant | Empty grid message | Select change event | **WORKING** |
| **Click Movie Card** | Home (`.movie-card`) | Open movie detail popup with showtimes | `openMovieDetail(id)` -> `loadMovieShows()` | Spinner | Fallback shows | Modal state | **WORKING** |
| **Select Show Date** | Movie Modal (`.date-tab`) | Update selected date for shows | `selectDate()` | Instant | N/A | Tab active class | **WORKING** |
| **Select Showtime** | Movie Modal (`.time-slot`)| Set show, transition to `#page-booking` | `selectRealShow()` -> `startBooking()` | Delay | N/A | Single selection | **WORKING** |
| **Select Seat** | Booking Step 1 (`.seat`) | Toggle seat selection, recalculate totals | `toggleSeat()` -> `updateBookingSummary()` | Instant | 6-seat maximum check | Disabled if booked | **WORKING** |
| **Proceed from Seats**| Booking Step 1 (`#btnProceed`)| Reserve seats in backend; advance to Step 2 | `POST /api/v1/bookings` | Spinner + Disabled | 409 Conflict toast | Button disabled | **WORKING** |
| **Add / Remove Food** | Booking Step 2 (`.fq-btn`)| Update snack quantity and recalculate order total | `changeFood()` -> `updateFoodSummary()` | Instant | Floor at 0 | Quantity bounds | **WORKING** |
| **Proceed to Pay** | Booking Step 2 (`.btn-proceed`)| Advance to payment summary step | `goToPayment()` | Instant | Summary validation | Step transition | **WORKING** |
| **Skip Food** | Booking Step 2 (`.btn-skip`)| Clear food cart; advance to Step 3 | `skipFood()` | Instant | N/A | Cart reset | **WORKING** |
| **Toggle Pay Method** | Booking Step 3 (`.pay-sec-hdr`)| Accordion expand/collapse for UPI/Card/NetBank | `togglePay()` | Animation | N/A | Accordion toggle | **WORKING** |
| **Select UPI App** | Booking Step 3 (`.upi-btn`)| Select GPay / PhonePe / Paytm / Other | `selectUPIApp()` | Toast | N/A | Single active | **WORKING** |
| **Verify UPI ID** | Booking Step 3 (`.btn-verify`)| Verify user UPI VPA | `verifyUPI()` | Toast | Regex validation check | Disabled if invalid | **WORKING** |
| **Select Bank** | Booking Step 3 (`.bank-btn`)| Select netbanking institution | `selectBank()` | Active class | N/A | Single active | **WORKING** |
| **Format Card Input**| Booking Step 3 (`#cardNum`)| Auto-format credit card spacing (4-4-4-4) | `fmtCard()` | Instant | Length validation | 19-char max | **WORKING** |
| **Click Pay** | Booking Step 3 (`#btnPay`)| Execute payment and confirm booking | `POST /api/v1/payments` + `/bookings/confirm` | Overlay + Disabled | Error toast + re-enable | `isProcessingPayment` flag | **WORKING** |
| **View Ticket** | Bookings (`.btn-view-tkt`)| Open E-ticket modal with dynamic QR code | `viewBookingTicket()` -> `showTicket()` | Canvas | N/A | Modal state | **WORKING** |
| **Cancel Booking** | Bookings (`.btn-skip`)| Cancel confirmed booking, release seats | `POST /api/v1/bookings/{ref}/cancel` | Confirm prompt | Error toast | Confirmation alert | **WORKING** |
| **Download Ticket** | Ticket Modal (`.btn-dl`)| Render ticket canvas to PNG and trigger download | `downloadTicket()` | Canvas export | Success toast | Auto-download link | **WORKING** |
| **Submit Login** | Login Modal (`#lf-login .btn-otp`)| Authenticate user credentials with JWT | `POST /api/v1/auth/login` | Text change + Disabled | 401 toast alert | Button disabled | **WORKING** |
| **Submit Signup** | Login Modal (`#lf-signup .btn-otp`)| Register new customer account | `POST /api/v1/auth/register` | Text change + Disabled | Validation / 409 toast | Button disabled | **WORKING** |
| **Save Profile** | Profile (`#btnSaveProfile`)| Save user profile updates | `saveProfile()` | Toast | N/A | Single submission | **WORKING** |

---

## 2. Audit Summary

- Total User-Facing Controls Inspected: **32**
- Status: **32 WORKING / 0 BROKEN / 0 PARTIAL**
- Every form submit, reservation, and payment submission includes visual progress indication and duplicate-click protection.
