package com.sparta.link;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LinkRepository extends JpaRepository<Link, UUID> {
    List<Link> findByUserIdAndIdLessThanOrderByIdDesc(UUID userId, Long cursorId, Pageable pageable);
    List<Link> findByUserIdOrderByIdDesc(UUID userId, Pageable pageable);
}
