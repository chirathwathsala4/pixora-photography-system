package com.pixora.controller;

import com.pixora.dto.PackageDto;
import com.pixora.dto.PhotoResponse;
import com.pixora.dto.ReviewResponse;
import com.pixora.service.PackageService;
import com.pixora.service.PhotoService;
import com.pixora.service.ReviewService;
import com.pixora.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final PackageService packageService;
    private final PhotoService photoService;
    private final UserService userService;
    private final ReviewService reviewService;

    @GetMapping("/packages")
    public ResponseEntity<List<PackageDto>> getActivePackages() {
        return ResponseEntity.ok(packageService.getActivePackages());
    }

    //Shows photographers that are active within the studio
    @GetMapping("/photographers")
    public ResponseEntity<?> getActivePhotographers() {
        return ResponseEntity.ok(userService.getActivePhotographers());
    }

    @GetMapping("/portfolio")
    public ResponseEntity<List<PhotoResponse>> getPortfolio() {
        return ResponseEntity.ok(photoService.getPortfolioPhotos());
    }

    @GetMapping("/photographers/{photographerId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getPhotographerReviews(@PathVariable Long photographerId) {
        return ResponseEntity.ok(reviewService.getReviewsByPhotographer(photographerId));
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<ReviewResponse>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }
}