
document.addEventListener('DOMContentLoaded', function() {
    const today = new Date().toISOString().split('T')[0];
    
    // Set min date to today for all date inputs
    document.querySelectorAll('input[type="date"]').forEach(input => {
        if (!input.min) {
            input.min = today;
        }
    });
    
    // Initialize form handlers
    initializeSingleAvailabilityForm();
    initializeBulkAvailabilityForm();
});

// Single Availability Form Validation
function initializeSingleAvailabilityForm() {
    const form = document.getElementById('singleAvailabilityForm');
    const startTimeInput = document.getElementById('startTime');
    const endTimeInput = document.getElementById('endTime');
    const dateInput = document.getElementById('date');
    
    // Real-time validation
    endTimeInput.addEventListener('change', function() {
        validateSingleTimeRange();
    });
    
    startTimeInput.addEventListener('change', function() {
        validateSingleTimeRange();
    });
    
    dateInput.addEventListener('change', function() {
        validateSingleDate();
    });
    
    // Form submission validation
    form.addEventListener('submit', function(e) {
        if (!validateSingleForm()) {
            e.preventDefault();
        }
    });
}

// Bulk Availability Form Validation
function initializeBulkAvailabilityForm() {
    const form = document.getElementById('bulkAvailabilityForm');
    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');
    const bulkStartTimeInput = document.getElementById('bulkStartTime');
    const bulkEndTimeInput = document.getElementById('bulkEndTime');
    const weeklyRecurrenceCheckbox = document.getElementById('weeklyRecurrence');
    const weeklyOptions = document.getElementById('weeklyOptions');
    
    // Show/hide weekly options
    weeklyRecurrenceCheckbox.addEventListener('change', function() {
        if (this.checked) {
            weeklyOptions.style.display = 'block';
        } else {
            weeklyOptions.style.display = 'none';
        }
        updateDatePreview();
    });
    
    // Real-time validation and preview
    [startDateInput, endDateInput, bulkStartTimeInput, bulkEndTimeInput].forEach(input => {
        input.addEventListener('change', function() {
            validateBulkForm();
            updateDatePreview();
        });
    });
    
    document.getElementById('recurrenceWeeks').addEventListener('change', function() {
        updateDatePreview();
    });
    
    // Form submission
    form.addEventListener('submit', function(e) {
        if (!validateBulkForm()) {
            e.preventDefault();
            return;
        }
        
        // Generate and set the dates array
        const dates = generateBulkDates();
        if (dates.length === 0) {
            showBulkError('No valid dates could be generated');
            e.preventDefault();
            return;
        }
        
        // Set the generated dates in the hidden field
        document.getElementById('generatedDates').value = dates.join(',');
    });
}

// Validation Functions
function validateSingleTimeRange() {
    const startTime = document.getElementById('startTime').value;
    const endTime = document.getElementById('endTime').value;
    const errorElement = document.getElementById('endTimeError');
    const endTimeInput = document.getElementById('endTime');
    
    if (startTime && endTime) {
        if (startTime >= endTime) {
            showFieldError(endTimeInput, errorElement, 'End time must be after start time');
            return false;
        } else {
            hideFieldError(endTimeInput, errorElement);
            return true;
        }
    }
    return true;
}

function validateSingleDate() {
    const dateInput = document.getElementById('date');
    const errorElement = document.getElementById('dateError');
    const selectedDate = new Date(dateInput.value);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    if (selectedDate < today) {
        showFieldError(dateInput, errorElement, 'Date cannot be in the past');
        return false;
    } else {
        hideFieldError(dateInput, errorElement);
        return true;
    }
}

function validateSingleForm() {
    const isTimeValid = validateSingleTimeRange();
    const isDateValid = validateSingleDate();
    
    return isTimeValid && isDateValid;
}

function validateBulkForm() {
    let isValid = true;
    
    // Validate date range
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');
    const startDateError = document.getElementById('startDateError');
    const endDateError = document.getElementById('endDateError');
    
    if (startDate && endDate) {
        if (new Date(startDate) > new Date(endDate)) {
            showFieldError(endDateInput, endDateError, 'End date must be after start date');
            isValid = false;
        } else {
            hideFieldError(endDateInput, endDateError);
        }
        
        // Check if start date is not in the past
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        if (new Date(startDate) < today) {
            showFieldError(startDateInput, startDateError, 'Start date cannot be in the past');
            isValid = false;
        } else {
            hideFieldError(startDateInput, startDateError);
        }
    }
    
    // Validate time range
    const bulkStartTime = document.getElementById('bulkStartTime').value;
    const bulkEndTime = document.getElementById('bulkEndTime').value;
    const bulkStartTimeInput = document.getElementById('bulkStartTime');
    const bulkEndTimeInput = document.getElementById('bulkEndTime');
    const bulkStartTimeError = document.getElementById('bulkStartTimeError');
    const bulkEndTimeError = document.getElementById('bulkEndTimeError');
    
    if (bulkStartTime && bulkEndTime) {
        if (bulkStartTime >= bulkEndTime) {
            showFieldError(bulkEndTimeInput, bulkEndTimeError, 'End time must be after start time');
            isValid = false;
        } else {
            hideFieldError(bulkEndTimeInput, bulkEndTimeError);
        }
    }
    
    if (!isValid) {
        hideBulkError();
    }
    
    return isValid;
}

// Date Generation Functions
function generateDateRange(startDate, endDate) {
    const dates = [];
    const currentDate = new Date(startDate);
    const finalDate = new Date(endDate);
    
    while (currentDate <= finalDate) {
        dates.push(new Date(currentDate));
        currentDate.setDate(currentDate.getDate() + 1);
    }
    
    return dates;
}

function generateBulkDates() {
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    const weeklyRecurrence = document.getElementById('weeklyRecurrence').checked;
    const recurrenceWeeks = parseInt(document.getElementById('recurrenceWeeks').value) || 1;
    
    if (!startDate || !endDate) {
        return [];
    }
    
    let dates = generateDateRange(startDate, endDate);
    
    if (weeklyRecurrence && recurrenceWeeks > 1) {
        const additionalDates = [];
        const baseDates = [...dates];
        
        for (let week = 1; week < recurrenceWeeks; week++) {
            baseDates.forEach(date => {
                const newDate = new Date(date);
                newDate.setDate(newDate.getDate() + (week * 7));
                additionalDates.push(newDate);
            });
        }
        
        dates = dates.concat(additionalDates);
    }
    
    // Convert to YYYY-MM-DD format and sort
    return dates
        .map(date => date.toISOString().split('T')[0])
        .sort();
}

// Preview Functions
function updateDatePreview() {
    const dates = generateBulkDates();
    const previewElement = document.getElementById('datePreview');
    const previewList = document.getElementById('previewList');
    const previewCount = document.getElementById('previewCount');
    
    if (dates.length > 0) {
        previewElement.style.display = 'block';
        previewList.innerHTML = dates.map(date => {
            const dateObj = new Date(date + 'T00:00:00');
            return dateObj.toLocaleDateString('en-US', { 
                weekday: 'short', 
                year: 'numeric', 
                month: 'short', 
                day: 'numeric' 
            });
        }).join('<br>');
        previewCount.textContent = `Total: ${dates.length} dates`;
    } else {
        previewElement.style.display = 'none';
    }
}

// Error Display Functions
function showFieldError(input, errorElement, message) {
    input.classList.add('is-invalid');
    errorElement.textContent = message;
    errorElement.style.display = 'block';
}

function hideFieldError(input, errorElement) {
    input.classList.remove('is-invalid');
    errorElement.textContent = '';
    errorElement.style.display = 'none';
}

function showSingleError(message) {
    const errorElement = document.getElementById('singleFormError');
    errorElement.textContent = message;
    errorElement.classList.remove('d-none');
}

function hideSingleError() {
    const errorElement = document.getElementById('singleFormError');
    errorElement.classList.add('d-none');
}

function showBulkError(message) {
    const errorElement = document.getElementById('bulkFormError');
    errorElement.textContent = message;
    errorElement.classList.remove('d-none');
}

function hideBulkError() {
    const errorElement = document.getElementById('bulkFormError');
    errorElement.classList.add('d-none');
}

// Reset forms when modals are hidden
document.getElementById('addAvailabilityModal').addEventListener('hidden.bs.modal', function() {
    const form = document.getElementById('singleAvailabilityForm');
    form.reset();
    form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    form.querySelectorAll('.invalid-feedback').forEach(el => el.style.display = 'none');
    hideSingleError();
});

document.getElementById('bulkAddModal').addEventListener('hidden.bs.modal', function() {
    const form = document.getElementById('bulkAvailabilityForm');
    form.reset();
    form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    form.querySelectorAll('.invalid-feedback').forEach(el => el.style.display = 'none');
    document.getElementById('weeklyOptions').style.display = 'none';
    document.getElementById('datePreview').style.display = 'none';
    hideBulkError();
});