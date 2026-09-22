package com.sparta.link;

import com.sparta.tag.Tag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "link_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LinkTag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    private Link link;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public static LinkTag of(Link link, Tag tag) {
        LinkTag linkTag = new LinkTag();
        linkTag.link = link;
        linkTag.tag = tag;
        return linkTag;
    }
}
