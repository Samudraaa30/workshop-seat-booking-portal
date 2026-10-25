(() => {
  const status = document.getElementById('live-status');
  const dot = document.getElementById('live-dot');
  const sessionCount = document.getElementById('session-count');
  let refreshingForNewListing = false;
  if (!status || !window.EventSource) return;

  function render(workshops) {
    workshops.forEach((workshop) => {
      const card = document.querySelector(`[data-workshop-id="${workshop.id}"]`);
      if (!card) {
        // The server may have imported a newly announced event after this page rendered.
        // Reload once so Thymeleaf can add the complete card and booking form.
        if (!refreshingForNewListing) {
          refreshingForNewListing = true;
          window.location.reload();
        }
        return;
      }

      const seatPill = document.getElementById(`seat-pill-${workshop.id}`);
      const bookedCount = document.getElementById(`booked-count-${workshop.id}`);
      const fill = document.getElementById(`capacity-fill-${workshop.id}`);
      const button = document.getElementById(`book-button-${workshop.id}`);
      if (seatPill) seatPill.textContent = `${workshop.availableSeats} ${workshop.availableSeats === 1 ? 'seat' : 'seats'} left`;
      if (bookedCount) bookedCount.textContent = `${workshop.bookedSeats} of ${workshop.capacity} seats booked`;
      if (fill) fill.style.width = `${workshop.capacity ? (workshop.bookedSeats * 100 / workshop.capacity) : 0}%`;
      if (button) {
        button.disabled = workshop.availableSeats < 1;
        button.textContent = workshop.availableSeats < 1 ? 'Fully booked' : 'Reserve my seat →';
      }
    });
    if (sessionCount) sessionCount.textContent = `${workshops.length} sessions available`;
  }

  const events = new EventSource('/api/workshops/events');
  events.addEventListener('availability', (message) => {
    try {
      render(JSON.parse(message.data));
      status.textContent = 'Live availability';
      dot.classList.add('connected');
    } catch (error) {
      status.textContent = 'Availability update failed';
      dot.classList.remove('connected');
    }
  });
  events.onerror = () => {
    status.textContent = 'Reconnecting to live availability…';
    dot.classList.remove('connected');
  };

  // Re-sync after a brief disconnect; EventSource also reconnects automatically.
  window.addEventListener('online', () => {
    fetch('/api/workshops', { cache: 'no-store' })
      .then((response) => response.ok ? response.json() : Promise.reject())
      .then(render)
      .catch(() => {});
  });
})();
