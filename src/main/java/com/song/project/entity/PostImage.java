package com.song.project.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class PostImage {
    @Id @GeneratedValue
    private Long id;

    private String imgUrl;

    @ManyToOne
    @JoinColumn(name = "post_id")
    @JsonBackReference
    private Post post;    // 이미지가 속한 게시물
}