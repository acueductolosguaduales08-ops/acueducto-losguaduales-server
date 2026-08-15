package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.HeroLink;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeroLinkResponse {
    private Long id;
    private String link;
    private boolean principal;

    public static HeroLinkResponse fromEntity(HeroLink h) {
        return HeroLinkResponse.builder()
                .id(h.getId())
                .link(h.getLink())
                .principal(h.isPrincipal())
                .build();
    }
}
