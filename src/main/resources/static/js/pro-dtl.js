// Enhanced image error handling
document.addEventListener('DOMContentLoaded', function() {
    // Handle profile image loading errors
    const profileImg = document.querySelector('img[alt="Profile Photo"]');
    if (profileImg) {
        profileImg.addEventListener('error', function() {
            const fallbackDiv = this.nextElementSibling;
            if (fallbackDiv && fallbackDiv.classList.contains('position-absolute')) {
                this.style.display = 'none';
                fallbackDiv.style.display = 'flex';
            }
        });
        
        profileImg.addEventListener('load', function() {
            const fallbackDiv = this.nextElementSibling;
            if (fallbackDiv && fallbackDiv.classList.contains('position-absolute')) {
                fallbackDiv.style.display = 'none';
            }
        });
    }
    
    // Form validation before submission
    const bookingForms = document.querySelectorAll('form[action*="/client/bookings/create"]');
    bookingForms.forEach(form => {
        form.addEventListener('submit', function(e) {
            const professionalId = form.querySelector('input[name="professionalId"]');
            const availabilityId = form.querySelector('input[name="availabilityId"]');
            
            if (!professionalId || !professionalId.value || !availabilityId || !availabilityId.value) {
                e.preventDefault();
                alert('Missing required booking information. Please try again.');
                return false;
            }
        });
    });
});