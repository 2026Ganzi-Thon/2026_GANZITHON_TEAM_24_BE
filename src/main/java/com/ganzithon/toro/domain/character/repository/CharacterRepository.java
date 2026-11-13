package com.ganzithon.toro.domain.character.repository;

import com.ganzithon.toro.domain.character.entity.UserCharacter;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CharacterRepository extends JpaRepository<UserCharacter, Long> {

    /** 최근 사용순. 같은 시각이면 나중에 만든 것 먼저 */
    List<UserCharacter> findAllByUserIdOrderByLastUsedAtDescIdDesc(Long userId);

    long countByUserId(Long userId);

    boolean existsByUserId(Long userId);

    /** 내 캐릭터만 찾는다. 남의 것이면 빈 값 → CHAR_001 */
    Optional<UserCharacter> findByIdAndUserId(Long id, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserCharacter c where c.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
