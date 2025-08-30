package com.marketplace.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.dto.BookingDto;
import com.marketplace.exception.AvailabilityNotFoundException;
import com.marketplace.exception.BookingException;
import com.marketplace.exception.ProfessionalNotFoundException;
import com.marketplace.exception.SlotNotAvailableException;
import com.marketplace.exception.UnauthorizedAccessException;
import com.marketplace.model.Availability;
import com.marketplace.model.Booking;
import com.marketplace.model.Booking.BookingStatus;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.User;
import com.marketplace.repository.AvailabilityRepository;
import com.marketplace.repository.BookingRepository;
import com.marketplace.repository.ProfessionalProfileRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.BookingService;
import com.marketplace.util.Constants;

@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ProfessionalProfileRepository profileRepository;
    private final AvailabilityRepository availabilityRepository;

    public BookingServiceImpl(BookingRepository bookingRepository,
                            UserRepository userRepository,
                            ProfessionalProfileRepository profileRepository,
                            AvailabilityRepository availabilityRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.availabilityRepository = availabilityRepository;
    }

    @Override
    @Transactional
    public Booking createBooking(User client, BookingDto bookingDto) {
        logger.info("Creating booking for client ID: {}", client.getId());

        // Validate input
        if (bookingDto.getProfessionalId() == null) {
            throw new BookingException("Professional ID is required");
        }
        if (bookingDto.getAvailabilityId() == null) {
            throw new BookingException("Availability ID is required");
        }

        // Get professional
        ProfessionalProfile professional = profileRepository.findById(bookingDto.getProfessionalId())
                .orElseThrow(() -> new ProfessionalNotFoundException("Professional not found"));

        // Get availability slot
        Availability availability = availabilityRepository.findById(bookingDto.getAvailabilityId())
                .orElseThrow(() -> new AvailabilityNotFoundException("Availability slot not found"));

        // Validate business rules
        validateBookingRules(client, professional, availability);

        // Atomically reserve the slot
        if (!reserveSlot(availability.getId())) {
            throw new SlotNotAvailableException("This time slot is no longer available");
        }

        try {
            // Create booking
            Booking booking = new Booking(client, professional, availability, bookingDto.getServiceDetails());
            Booking savedBooking = bookingRepository.save(booking);

            logger.info("Booking created successfully with ID: {}", savedBooking.getId());
            return savedBooking;
        } catch (Exception e) {
            // Rollback slot reservation if booking creation fails
            releaseSlot(availability.getId());
            throw new BookingException("Failed to create booking", e);
        }
    }

    private void validateBookingRules(User client, ProfessionalProfile professional, Availability availability) {
        // Check if client is trying to book their own service
        if (professional.getUser().getId().equals(client.getId())) {
            throw new BookingException("You cannot book your own service");
        }

        // Validate that availability belongs to the professional
        if (!availability.getProfessional().getId().equals(professional.getId())) {
            throw new BookingException("This availability slot does not belong to the selected professional");
        }

        // Validate that the booking is for a future time
        LocalDateTime slotDateTime = LocalDateTime.of(availability.getDate(), availability.getStartTime());
        if (slotDateTime.isBefore(LocalDateTime.now())) {
            throw new BookingException("Cannot book past time slots");
        }

        // Check if the slot is already booked
        if (availability.isBooked()) {
            throw new SlotNotAvailableException("This availability slot is already booked");
        }
    }

    @Transactional
    private boolean reserveSlot(Long availabilityId) {
        // Use atomic query to ensure thread safety
        int rowsUpdated = availabilityRepository.markAsBookedIfAvailable(availabilityId);
        return rowsUpdated > 0;
    }


    @Transactional
    private void releaseSlot(Long availabilityId) {
        // Use atomic query for releasing slots
        int rowsUpdated = availabilityRepository.releaseSlot(availabilityId);
        if (rowsUpdated == 0) {
            logger.warn("No rows updated when releasing slot {}", availabilityId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getClientBookings(User client) {
        if (client == null) {
            throw new BookingException("Client cannot be null");
        }
        return bookingRepository.findByClientOrderByCreatedAtDesc(client);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getProfessionalBookings(User user) {
        if (user == null) {
            throw new BookingException("User cannot be null");
        }
        
        ProfessionalProfile professional = profileRepository.findByUser(user)
                .orElseThrow(() -> new ProfessionalNotFoundException(Constants.PROFILE_NOT_FOUND));
        return bookingRepository.findByProfessionalOrderByCreatedAtDesc(professional);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getClientBookingsByStatus(User client, BookingStatus status) {
        if (client == null) {
            throw new BookingException("Client cannot be null");
        }
        if (status == null) {
            throw new BookingException("Status cannot be null");
        }
        return bookingRepository.findByClientAndStatusOrderByCreatedAtDesc(client, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getProfessionalBookingsByStatus(User user, BookingStatus status) {
        if (user == null) {
            throw new BookingException("User cannot be null");
        }
        if (status == null) {
            throw new BookingException("Status cannot be null");
        }
        
        ProfessionalProfile professional = profileRepository.findByUser(user)
                .orElseThrow(() -> new ProfessionalNotFoundException(Constants.PROFILE_NOT_FOUND));
        return bookingRepository.findByProfessionalAndStatusOrderByCreatedAtDesc(professional, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByDateRange(User user, LocalDate startDate, LocalDate endDate, boolean isClient) {
        if (user == null) {
            throw new BookingException("User cannot be null");
        }
        if (startDate == null || endDate == null) {
            throw new BookingException("Start date and end date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new BookingException("Start date cannot be after end date");
        }
        
        if (isClient) {
            return bookingRepository.findBookingsByClientAndDateRange(user, startDate, endDate);
        } else {
            ProfessionalProfile professional = profileRepository.findByUser(user)
                    .orElseThrow(() -> new ProfessionalNotFoundException(Constants.PROFILE_NOT_FOUND));
            return bookingRepository.findBookingsByProfessionalAndDateRange(professional, startDate, endDate);
        }
    }

    @Override
    @Transactional
    public Booking updateBookingStatus(Long bookingId, User user, BookingStatus status) {
        if (bookingId == null) {
            throw new BookingException("Booking ID cannot be null");
        }
        if (status == null) {
            throw new BookingException("Status cannot be null");
        }
        
        Booking booking = getBookingByIdAndUser(bookingId, user);
        
        // Validate status transition
        if (!isValidStatusTransition(booking.getStatus(), status)) {
            throw new BookingException("Invalid status transition from " + booking.getStatus() + " to " + status);
        }
        
        // If cancelling, atomically free up the availability slot
        if (status == BookingStatus.CANCELLED && booking.getStatus() != BookingStatus.CANCELLED) {
            if (booking.getAvailability() != null) {
                int rowsUpdated = availabilityRepository.releaseSlot(booking.getAvailability().getId());
                if (rowsUpdated == 0) {
                    logger.warn("Failed to release slot {} for booking {}", 
                               booking.getAvailability().getId(), bookingId);
                }
            }
        }
        
        booking.setStatus(status);
        Booking savedBooking = bookingRepository.save(booking);
        
        logger.info("Booking status updated to {} for booking ID: {}", status, bookingId);
        return savedBooking;
    }

    private boolean isValidStatusTransition(BookingStatus currentStatus, BookingStatus newStatus) {
        // Define valid transitions based on your business rules
        switch (currentStatus) {
            case PENDING:
                return newStatus == BookingStatus.CONFIRMED || newStatus == BookingStatus.CANCELLED;
            case CONFIRMED:
                return newStatus == BookingStatus.COMPLETED || newStatus == BookingStatus.CANCELLED;
            case CANCELLED:
                return false; // Cannot change from cancelled
            case COMPLETED:
                return false; // Cannot change from completed
            default:
                return false;
        }
    }

    @Override
    @Transactional
    public Booking updateBookingDetails(Long bookingId, User user, String serviceDetails) {
        if (bookingId == null) {
            throw new BookingException("Booking ID cannot be null");
        }
        
        Booking booking = getBookingByIdAndUser(bookingId, user);
        
        // Only allow updates for pending or confirmed bookings
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BookingException("Cannot update details for " + booking.getStatus().toString().toLowerCase() + " bookings");
        }
        
        booking.setServiceDetails(serviceDetails);
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId, User user) {
        if (bookingId == null) {
            throw new BookingException("Booking ID cannot be null");
        }
        
        Booking booking = getBookingByIdAndUser(bookingId, user);
        
        // Check if booking can be cancelled
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BookingException("Cannot cancel completed booking");
        }
        
        // Atomically free up the availability slot
        if (booking.getAvailability() != null) {
            int rowsUpdated = availabilityRepository.releaseSlot(booking.getAvailability().getId());
            if (rowsUpdated == 0) {
                logger.warn("Failed to release slot {} for booking {}", 
                           booking.getAvailability().getId(), bookingId);
            }
        }
        
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        
        logger.info("Booking cancelled successfully with ID: {}", bookingId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSlotBooked(Long availabilityId) {
        if (availabilityId == null) {
            throw new BookingException("Availability ID cannot be null");
        }
        return bookingRepository.existsByAvailabilityIdAndStatusNot(availabilityId, BookingStatus.CANCELLED);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canBookSlot(Long availabilityId, User client) {
        if (availabilityId == null) {
            throw new BookingException("Availability ID cannot be null");
        }
        if (client == null) {
            throw new BookingException("Client cannot be null");
        }
        return !isSlotBooked(availabilityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        if (id == null) {
            throw new BookingException("Booking ID cannot be null");
        }
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BookingException("Booking not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBookingByIdAndUser(Long id, User user) {
        if (user == null) {
            throw new BookingException("User cannot be null");
        }
        
        Booking booking = getBookingById(id);
        
        // Check if user has access to this booking
        if (!booking.getClient().getId().equals(user.getId()) && 
            !booking.getProfessional().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("Access denied to booking");
        }
        
        return booking;
    }
}