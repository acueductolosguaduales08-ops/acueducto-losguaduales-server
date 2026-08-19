package com.acueducto.backend.repository;

import com.acueducto.backend.entity.ReaccionPublicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReaccionPublicacionRepository extends JpaRepository<ReaccionPublicacion, Long> {

    List<ReaccionPublicacion> findByPublicacionIdOrderByContadorDesc(Long publicacionId);

    Optional<ReaccionPublicacion> findByPublicacionIdAndEmoji(Long publicacionId, String emoji);

    /** Incremento atomico en una sola sentencia SQL, seguro ante reacciones concurrentes. */
    @Modifying
    @Query("update ReaccionPublicacion r set r.contador = r.contador + 1 " +
            "where r.publicacion.id = :publicacionId and r.emoji = :emoji")
    int incrementar(@Param("publicacionId") Long publicacionId, @Param("emoji") String emoji);

    /** Decremento atomico, nunca deja el contador por debajo de cero. */
    @Modifying
    @Query("update ReaccionPublicacion r set r.contador = r.contador - 1 " +
            "where r.publicacion.id = :publicacionId and r.emoji = :emoji and r.contador > 0")
    int decrementar(@Param("publicacionId") Long publicacionId, @Param("emoji") String emoji);
}
