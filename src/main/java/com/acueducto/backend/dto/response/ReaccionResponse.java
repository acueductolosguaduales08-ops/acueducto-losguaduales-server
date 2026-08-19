package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.ReaccionPublicacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReaccionResponse {
    private String emoji;
    private long contador;

    public static ReaccionResponse fromEntity(ReaccionPublicacion r) {
        return ReaccionResponse.builder().emoji(r.getEmoji()).contador(r.getContador()).build();
    }
}
