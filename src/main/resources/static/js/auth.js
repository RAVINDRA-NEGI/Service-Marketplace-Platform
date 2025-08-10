document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('registrationForm');
    
    if (form) {
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            
            // Get form data
            const formData = new FormData(form);
            const data = Object.fromEntries(formData);
            
            // Check passwords match
            if (data.password !== data.confirmPassword) {
                alert('Passwords do not match!');
                return;
            }
            
            // Determine endpoint based on current page
            let endpoint = '/api/auth/client/register';
            if (window.location.pathname.includes('professional')) {
                endpoint = '/api/auth/professional/register';
            }
            
            // Send registration request
            fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    fullName: data.fullName,
                    username: data.username,
                    email: data.email,
                    password: data.password
                })
            })
            .then(response => response.json())
            .then(data => {
                if (data.message) {
                    alert('Registration successful! Please login.');
                    window.location.href = '/login';
                } else if (data.error) {
                    alert('Registration failed: ' + data.message);
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('An error occurred during registration.');
            });
        });
    }
});