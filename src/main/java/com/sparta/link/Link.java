package com.sparta.link;

import com.sparta.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Entity
@Table(name = "links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String url;

    private String title;
    private String description;
    private String faviconUrl;
    private String summary;

    @Column(nullable = false)
    private boolean isEmbedded = false;

    @OneToMany(mappedBy = "link", cascade = CascadeType.ALL, orphanRemoval = true)
    private Link<LinkTag> linkTags = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    public static Link of(User user, String url) {
        Link link = new Link();
        link.user = user;
        link.url = url;
        return link;
    }

    public void updateMetadata(String title, String description, String faviconUrl) {
        this.title = title;
        this.description = description;
        this.faviconUrl = faviconUrl;
    }

    public void updateSummary(String summary) {
        this.summary = summary;
    }

    public void markEmbedded() {
        this.isEmbedded = true;
    }
}
