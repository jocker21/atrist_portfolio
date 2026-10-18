package com.example.artist.repository;
import com.example.artist.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {}
