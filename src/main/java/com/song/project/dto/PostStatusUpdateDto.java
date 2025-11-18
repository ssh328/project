package com.song.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostStatusUpdateDto {
    @NotBlank(message = "상태 값이 필요합니다.")
    @Pattern(regexp = "ON_SALE|RESERVED|SOLD", 
             message = "상태는 ON_SALE, RESERVED, SOLD 중 하나여야 합니다.")
    private String status;
}
