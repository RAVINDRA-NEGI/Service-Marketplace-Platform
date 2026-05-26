# Repository Overview

This project is a Spring Boot 3.5.x monolith for a two-sided service marketplace where clients discover professionals, book time slots, and leave reviews.

## Stack
- Java 21, Spring Boot (Web, Thymeleaf, Security, Validation, JPA)
- MySQL + Hibernate ORM
- Server-rendered Thymeleaf templates + static JS/CSS assets

## Core Domain
- `User` with roles (`CLIENT` and `PROFESSIONAL`)
- `ProfessionalProfile` and `ClientProfile`
- `Availability` slots owned by professionals
- `Booking` entities tied to a client, professional, and slot
- `Review` attached to completed bookings

## Architecture
- MVC controllers for auth, client flows, professional flows, bookings, availability, and reviews
- Service layer with business rules (booking validation, slot reservation/release, review constraints)
- Repository layer using Spring Data JPA
- Spring Security with form login and role-based URL protection

## Key User Journeys
1. Register as client or professional.
2. Complete role-specific profile.
3. Professional creates availability slots.
4. Client searches professionals and books a slot.
5. Professional manages booking statuses.
6. Client leaves a review after completion.

## Notable Operational Notes
- Database config and upload directory are in `application.yml`.
- Uploaded profile/client photos and certificates are stored under `uploads/`.
- One suspicious file exists in templates: `professional/bash.exe.stackdump`.
