# docs/project-overview.md

# Project Overview

## Purpose

RoomKH is a property marketplace platform for Cambodia where users can search for properties to rent or buy, property owners can list their properties for sale or rent, and administrators oversee the platform's content quality. This backend is the Spring Boot REST API that powers the React frontend.

## System Roles

| Role | Description |
|---|---|
| USER | A property seeker who can browse listings, save favorites, and send inquiries (favorites and inquiries not yet implemented) |
| SELLER | An approved property owner who can create and manage property listings (seller approval flow not yet implemented) |
| ADMIN | A system administrator who reviews seller requests and property listings (admin dashboard not yet implemented) |

## Current Completed Features

- Project setup: Spring Boot, Maven, PostgreSQL via Docker Compose, Flyway, environment-based configuration
- User and role database foundation with USER, SELLER, and ADMIN roles
- Standardized API response format and centralized exception handling
- User registration with BCrypt password hashing
- JWT-based stateless authentication with protected endpoints
- Refresh token system with remember-me support and logout
- Email-or-phone identifier authentication policy (Cambodia phone number support)

## Future Planned Features (Not Implemented Yet)

- Seller request and approval workflow
- SMS OTP seller activation
- Property module (public browsing, seller property management, image upload)
- Admin dashboard APIs
- Favorites
- Property inquiry
- Forgot password
- Rate limiting and IP blocking
- Payment and revenue features