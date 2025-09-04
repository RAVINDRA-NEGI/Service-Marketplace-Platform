document.addEventListener('DOMContentLoaded', function() {
    const profilePhotoInput = document.getElementById('profilePhotoModal');
    const certificatesInput = document.getElementById('certificatesModalInput');
    
    // Profile photo validation
    if (profilePhotoInput) {
        profilePhotoInput.addEventListener('change', function() {
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
            }
        });
    }
    
    // Certificates validation
    if (certificatesInput) {
        certificatesInput.addEventListener('change', function() {
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
        });
    }
});
