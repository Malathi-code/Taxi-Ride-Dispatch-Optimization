const bookingForm = document.getElementById("bookingForm");
const result = document.getElementById("result");
const driverList = document.getElementById("driverList");
const bookButton = document.getElementById("bookButton");
const refreshButton = document.getElementById("refreshButton");
const bookingHistoryElement = document.getElementById("bookingHistory");


// Store recent bookings
const bookingHistory = [];


// ========================================
// LOAD DRIVERS
// ========================================

async function loadDrivers() {

    driverList.innerHTML =
        "<p class='muted'>Loading drivers...</p>";

    try {

        const response = await fetch("/api/drivers");

        if (!response.ok) {
            throw new Error("Unable to load drivers");
        }

        const drivers = await response.json();


        if (drivers.length === 0) {

            driverList.innerHTML =
                "<p class='muted'>No drivers available.</p>";

            return;
        }


        driverList.innerHTML = drivers.map(driver => `

            <article class="driver-card">

                <div class="driver-icon">
                    🚕
                </div>

                <h3>
                    ${escapeHtml(driver.name)}
                </h3>

                <p>
                    ${escapeHtml(driver.location)}
                </p>

                <p>
                    Driver ID: ${driver.id}
                </p>

                <span class="availability ${driver.available ? "" : "busy"}">
                    ${driver.available ? "Available" : "Assigned"}
                </span>

            </article>

        `).join("");


    } catch (error) {

        driverList.innerHTML =
            "<p class='muted'>Cannot connect to the Java server. Start the server first.</p>";
    }
}


// ========================================
// ESCAPE HTML
// ========================================

function escapeHtml(value) {

    return String(value).replace(
        /[&<>"']/g,

        character => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        })[character]
    );
}


// ========================================
// BOOK TAXI
// ========================================

bookingForm.addEventListener("submit", async event => {

    event.preventDefault();


    const x = Number(
        document.getElementById("pickupX").value
    );

    const y = Number(
        document.getElementById("pickupY").value
    );


    // Check coordinates
    if (!Number.isFinite(x) || !Number.isFinite(y)) {

        showResult(
            "Please enter valid pickup coordinates.",
            true
        );

        return;
    }


    // Disable button while searching
    bookButton.disabled = true;

    bookButton.textContent =
        "Finding nearest taxi...";


    showResult(
        "Searching for an available driver...",
        false
    );


    try {

        const response = await fetch("/api/assign", {

            method: "POST",

            headers: {
                "Content-Type": "application/json"
            },

            body: JSON.stringify({
                pickupX: x,
                pickupY: y
            })

        });


        const data = await response.json();


        if (!response.ok) {

            throw new Error(
                data.error || "Booking failed"
            );
        }


        // Show successful booking
        showResult(

            `Taxi booked successfully!

Driver: ${data.name}

Location: ${data.location}

Distance: ${Number(data.distance).toFixed(2)} units`,

            false

        );


        // Add booking to history
        addBookingToHistory(
            data,
            x,
            y
        );


        // Refresh driver availability
        await loadDrivers();


    } catch (error) {

        showResult(
            error.message,
            true
        );

    } finally {

        bookButton.disabled = false;

        bookButton.textContent =
            "Find & Book Taxi →";
    }

});


// ========================================
// SHOW RESULT
// ========================================

function showResult(message, isError) {

    result.textContent = message;

    result.classList.remove(
        "hidden",
        "error"
    );


    if (isError) {

        result.classList.add("error");
    }
}


// ========================================
// ADD BOOKING TO HISTORY
// ========================================

function addBookingToHistory(data, x, y) {

    bookingHistory.unshift({

        driver: data.name,

        location: data.location,

        distance: data.distance,

        x: x,

        y: y,

        time: new Date().toLocaleTimeString()

    });


    renderBookingHistory();
}


// ========================================
// DISPLAY BOOKING HISTORY
// ========================================

function renderBookingHistory() {

    if (!bookingHistoryElement) {
        return;
    }


    if (bookingHistory.length === 0) {

        bookingHistoryElement.innerHTML =
            "<p class='muted'>No bookings yet.</p>";

        return;
    }


    bookingHistoryElement.innerHTML =
        bookingHistory.map((booking, index) => `

            <div class="history-card">

                <div>

                    <strong>
                        Booking #${index + 1}
                    </strong>

                    <p>
                        Driver:
                        ${escapeHtml(booking.driver)}
                    </p>

                    <p>
                        Location:
                        ${escapeHtml(booking.location)}
                    </p>

                    <p>
                        Pickup:
                        (${booking.x}, ${booking.y})
                    </p>

                </div>


                <div class="history-right">

                    <span>
                        ${Number(booking.distance).toFixed(2)}
                        units
                    </span>

                    <small>
                        ${booking.time}
                    </small>

                </div>

            </div>

        `).join("");
}


// ========================================
// REFRESH BUTTON
// ========================================

refreshButton.addEventListener(
    "click",
    loadDrivers
);


// ========================================
// INITIAL PAGE LOAD
// ========================================

loadDrivers();


// Show empty booking history initially
renderBookingHistory();
