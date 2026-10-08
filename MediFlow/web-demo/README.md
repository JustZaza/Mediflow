# MediFlow — Web Demo

A single self-contained HTML file that simulates the full MVP workflow in the browser
(no backend, no build step — just open `index.html`):

Login (pick a role) -> Patients -> Appointments -> Check-in & Queue -> Doctor consultation
-> Prescription -> Pharmacy dispensing -> Inventory update.

All state lives in memory and resets on page reload. It mirrors the business rules from the
Java services: no overlapping doctor appointments, priority-ordered queue (EMERGENCY > HIGH >
NORMAL, ties by check-in order), at least one item per prescription, and dispensing that
checks stock and expiry before reducing inventory (FEFO — earliest-expiring batch first).

Open `index.html` directly in any browser, or drag it into one.
