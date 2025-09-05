document.addEventListener('DOMContentLoaded', function() {
    const profilePhotoInputs = [
        document.getElementById('profilePhoto'),
        document.getElementById('profilePhotoModal')
    ];
    const certificatesInputs = [
        document.getElementById('certificates'),
        document.getElementById('certificatesModalInput')
    ];
    
    // Profile photo validation
    profilePhotoInputs.forEach(input => {
        if (input) {
            input.addEventListener('change', function() {
                const file = this.files[0];
                if (file) {
                    if (file.size > 5 * 1024 * 1024) {
                        alert('Profile photo size cannot exceed 5MB');
                        this.value = '';
                        return;
                    }
                    if (!file.type.match(/^image\/(jpeg|png)$/)) {
                        alert('Only JPEG and PNG images are allowed for profile photo');
                        this.value = '';
                        return;
                    }
                    
                    // Show preview if possible
                    showImagePreview(file, this);
                }
            });
        }
    });
    
    // Certificates validation
    certificatesInputs.forEach(input => {
        if (input) {
            input.addEventListener('change', function() {
                const files = Array.from(this.files);
                for (let file of files) {
                    if (file.size > 10 * 1024 * 1024) {
                        alert(`Certificate file "${file.name}" size cannot exceed 10MB`);
                        this.value = '';
                        return;
                    }
                    if (!file.type.match(/^(application\/pdf|image\/(jpeg|png))$/)) {
                        alert(`File "${file.name}" must be PDF, JPEG, or PNG`);
                        this.value = '';
                        return;
                    }
                }
                
                // Show file count
                if (files.length > 0) {
                    const label = this.previousElementSibling;
                    if (label) {
                        label.textContent = `Choose Certificate Files (${files.length} selected)`;
                    }
                }
            });
        }
    });
    
    function showImagePreview(file, input) {
        const reader = new FileReader();
        reader.onload = function(e) {
            // You could add image preview functionality here
            console.log('Image loaded for preview');
        };
        reader.readAsDataURL(file);
    }
    
    // Form submission loading state
    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
        form.addEventListener('submit', function(e) {
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Processing...';
                
                // Re-enable after 5 seconds as fallback
                setTimeout(() => {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = submitBtn.innerHTML.replace(/Processing.../, 'Save Changes');
                }, 5000);
            }
        });
    });
});