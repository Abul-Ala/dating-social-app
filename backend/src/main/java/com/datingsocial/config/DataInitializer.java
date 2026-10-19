package com.datingsocial.config;

import com.datingsocial.model.Interest;
import com.datingsocial.repository.InterestRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final InterestRepository interestRepository;

    public DataInitializer(InterestRepository interestRepository) {
        this.interestRepository = interestRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Only seed predefined interest tags if they don't already exist.
        // No default or dummy users are created - only real registered users will exist in the system.
        if (interestRepository.count() == 0) {
            List<Interest> interests = List.of(
                new Interest("Coffee", "Lifestyle", "\u2615"),
                new Interest("Hiking", "Outdoor", "\uD83E\uDD7E"),
                new Interest("Photography", "Creative", "\uD83D\uDCF8"),
                new Interest("Fitness & Gym", "Lifestyle", "\uD83C\uDFCB\uFE0F"),
                new Interest("Coding", "Tech", "\uD83D\uDCBB"),
                new Interest("Indie Music", "Music", "\uD83C\uDFB8"),
                new Interest("Yoga", "Lifestyle", "\uD83E\uDDD8"),
                new Interest("Travel", "Outdoor", "\u2708\uFE0F"),
                new Interest("Art & Design", "Creative", "\uD83C\uDFA8"),
                new Interest("Foodie", "Lifestyle", "\uD83C\uDF5C"),
                new Interest("Dogs", "Lifestyle", "\uD83D\uDC36"),
                new Interest("Sci-Fi & Anime", "Tech", "\uD83E\uDE90"),
                new Interest("Concerts", "Music", "\uD83C\uDFB6")
            );
            interestRepository.saveAll(interests);
        }
    }
}
