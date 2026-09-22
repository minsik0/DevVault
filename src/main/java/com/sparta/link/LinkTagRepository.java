package com.sparta.link;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LinkTagRepository extends JpaRepository<LinkTag, UUID> {
    void deleteByLinkIdAndTagId(UUID linkId, UUID tagId);
}
