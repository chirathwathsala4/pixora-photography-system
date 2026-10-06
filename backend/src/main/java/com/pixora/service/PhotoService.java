package com.pixora.service;

import com.pixora.dto.PhotoResponse;
import com.pixora.entity.Booking;
import com.pixora.entity.Photo;
import com.pixora.entity.User;
import com.pixora.exception.ResourceNotFoundException;
import com.pixora.repository.BookingRepository;
import com.pixora.repository.PhotoRepository;
import com.pixora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhotoService {
    
     // Repository used to perform database operations on photos
    private final PhotoRepository photoRepository;
    // Repository used to find the related booking
    private final BookingRepository bookingRepository;
    // Repository used to find the photographer
    private final UserRepository userRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${server.port:8080}")
    private String serverPort;


    // CREATE - Upload and save a new photo
    @Transactional
    public PhotoResponse uploadPhoto(Long bookingId, Long photographerId, MultipartFile file) throws IOException {
        // Check whether the booking exists
        Booking booking = bookingRepository.findById(bookingId)
          .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        
         // Check whether the photographer exists  
        User photographer = userRepository.findById(photographerId)
                .orElseThrow(() -> new ResourceNotFoundException("Photographer not found"));

        // Create the upload directory if it does not exist
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate a unique filename for the uploaded photo
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        // Save the uploaded file to the server
        Files.copy(file.getInputStream(), filePath);

        // Create the URL used to access the uploaded photo
        String photoUrl = "http://localhost:" + serverPort + "/uploads/" + filename;

        // Create a new Photo object
        Photo photo = Photo.builder()
                .booking(booking)
                .photographer(photographer)
                .photoUrl(photoUrl)
                .isPublishedPortfolio(false)
                .build();

                
        return toResponse(photoRepository.save(photo)); // Save the photo to the database
    }


    // UPDATE - Change the portfolio publishing status
    @Transactional
    public PhotoResponse togglePublishPortfolio(Long photoId) {
        // Find the photo and check whether it exists
        Photo photo = findPhoto(photoId);
        // Toggle the portfolio status
        photo.setIsPublishedPortfolio(!photo.getIsPublishedPortfolio());
        // Save the updated photo
        return toResponse(photoRepository.save(photo));
    }


    // READ - Get all photos belonging to a booking
    @Transactional(readOnly = true)
    public List<PhotoResponse> getPhotosByBooking(Long bookingId) {
        return photoRepository.findByBookingBookingId(bookingId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // READ - Get photos published in the portfolio
    @Transactional(readOnly = true)
    public List<PhotoResponse> getPortfolioPhotos() {
        return photoRepository.findByIsPublishedPortfolioTrue()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // READ - Get all photos
    @Transactional(readOnly = true)
    public List<PhotoResponse> getAllPhotos() {
        return photoRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // DELETE - Delete a photo from the server and database
    @Transactional
    public void deletePhoto(Long photoId) {
        Photo photo = findPhoto(photoId);// Find the photo and check whether it exists
        try {

            // Get the filename from the photo URL
            String url = photo.getPhotoUrl();
            String filename = url.substring(url.lastIndexOf("/") + 1);
            // Find and delete the physical photo file
            Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (Exception ignored) {}

        photoRepository.delete(photo);// Delete the photo record from the database
    }
    
    // UPDATE - Toggle the favorite status of a photo
    @Transactional
    public PhotoResponse toggleFavorite(Long photoId, Long clientId) {
      
        // Find the photo and check whether it exists
        Photo photo = findPhoto(photoId);
        // Check whether the client owns the related booking
        if (!photo.getBooking().getClient().getUserId().equals(clientId)) {
            throw new RuntimeException("Unauthorized to favorite photos from other clients' events");
        }
         // Toggle the favorite status
        photo.setIsFavorite(photo.getIsFavorite() == null ? true : !photo.getIsFavorite());
       
        // Save the updated photo
        return toResponse(photoRepository.save(photo));
    }

    // Find a photo by ID and validate that it exists
    private Photo findPhoto(Long id) {
        return photoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Photo not found with id: " + id));
    }

    // Convert Photo entity into PhotoResponse DTO
    public PhotoResponse toResponse(Photo p) {
        String normalizedUrl = p.getPhotoUrl();
        // Update the URL when the server port is different
        if (normalizedUrl != null && normalizedUrl.contains(":8080/")) {
            normalizedUrl = normalizedUrl.replace(":8080/", ":" + serverPort + "/");
        }
        
        // Build the response object
        return PhotoResponse.builder()
                .photoId(p.getPhotoId())
                .bookingId(p.getBooking().getBookingId())
                .photographerId(p.getPhotographer().getUserId())
                .photographerName(p.getPhotographer().getFullName())
                .photoUrl(normalizedUrl)
                .isPublishedPortfolio(p.getIsPublishedPortfolio())
                .isFavorite(p.getIsFavorite() != null && p.getIsFavorite())
                .build();
    }
}