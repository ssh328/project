package com.song.project.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostCreateDto {
    @NotBlank(message = "제목을 입력해주세요.")
    @Size(min = 2, max = 250, message = "제목은 2자 이상 250자 이하여야 합니다.")
    private String title;

    @NotNull(message = "가격을 입력해주세요.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    @Max(value = 999999999, message = "가격은 999,999,999원 이하여야 합니다.")
    private Integer price;

    @NotBlank(message = "카테고리를 선택해주세요.")
    private String category;

    @NotBlank(message = "내용을 입력해주세요.")
    @Size(min = 10, max = 10000, message = "내용은 10자 이상 10,000자 이하여야 합니다.")
    private String body;

    private String image; // 선택적 필드
}
