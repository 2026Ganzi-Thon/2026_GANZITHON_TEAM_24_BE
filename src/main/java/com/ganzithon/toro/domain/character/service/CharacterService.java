package com.ganzithon.toro.domain.character.service;

import com.ganzithon.toro.domain.character.dto.CharacterCreateRequest;
import com.ganzithon.toro.domain.character.dto.CharacterCreateResponse;
import com.ganzithon.toro.domain.character.dto.CharacterListResponse;
import com.ganzithon.toro.domain.character.dto.CharacterResponse;
import com.ganzithon.toro.domain.character.dto.CharacterUpdateRequest;
import com.ganzithon.toro.domain.character.dto.CharacterUseResponse;
import com.ganzithon.toro.domain.character.entity.UserCharacter;
import com.ganzithon.toro.domain.character.repository.CharacterRepository;
import com.ganzithon.toro.domain.user.entity.User;
import com.ganzithon.toro.domain.user.repository.UserRepository;
import com.ganzithon.toro.global.exception.BusinessException;
import com.ganzithon.toro.global.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기본 캐릭터(말랑이)는 DB에 없고 프론트가 표시한다. 여기서는 사용자가 꾸민 캐릭터만 다룬다.
 */
@Service
@RequiredArgsConstructor
public class CharacterService {

    /** 한 사람당 저장할 수 있는 캐릭터 수 */
    public static final int MAX_CHARACTERS = 3;

    private final CharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final CharacterNameValidator nameValidator;

    @Transactional(readOnly = true)
    public CharacterListResponse list(Long userId) {
        return new CharacterListResponse(characterRepository.findAllByUserIdOrderByLastUsedAtDescIdDesc(userId)
                .stream()
                .map(CharacterResponse::from)
                .toList());
    }

    /**
     * '이 캐릭터로 하소연 시작'을 누를 때 호출. 사용자 행을 잠근 뒤 개수를 세서 동시에 눌러도 3개를 넘지 않게 한다.
     * 이미 3개면 replaceId 캐릭터를 지우고 새로 저장한다(한 트랜잭션). replaceId가 없으면 CHAR_003.
     */
    @Transactional
    public CharacterCreateResponse create(Long userId, CharacterCreateRequest request) {
        nameValidator.validate(request.name());
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (characterRepository.countByUserId(userId) >= MAX_CHARACTERS) {
            if (request.replaceId() == null) {
                throw new BusinessException(ErrorCode.CHARACTER_LIMIT_EXCEEDED);
            }
            characterRepository.delete(findMine(request.replaceId(), userId));
            characterRepository.flush();
        }

        UserCharacter saved = characterRepository.save(UserCharacter.create(user, request, LocalDateTime.now()));
        return new CharacterCreateResponse(CharacterResponse.from(saved));
    }

    /** 다시 꾸미기. 목록 순서(lastUsedAt)는 바꾸지 않는다 */
    @Transactional
    public CharacterResponse update(Long userId, Long characterId, CharacterUpdateRequest request) {
        nameValidator.validate(request.name());
        UserCharacter character = findMine(characterId, userId);
        character.apply(request);
        characterRepository.flush(); // updatedAt을 응답에 담으려고 먼저 반영
        return CharacterResponse.from(character);
    }

    @Transactional
    public void delete(Long userId, Long characterId) {
        characterRepository.delete(findMine(characterId, userId));
    }

    /** 저장된 캐릭터로 하소연을 시작할 때 호출. 홈 목록 맨 앞으로 온다 */
    @Transactional
    public CharacterUseResponse markUsed(Long userId, Long characterId) {
        UserCharacter character = findMine(characterId, userId);
        character.markUsed(LocalDateTime.now());
        return new CharacterUseResponse(character.getId(), character.getLastUsedAt());
    }

    /** 내 캐릭터가 아니거나 없으면 CHAR_001 (남의 것인지 구분해서 알려 주지 않는다) */
    private UserCharacter findMine(Long characterId, Long userId) {
        return characterRepository.findByIdAndUserId(characterId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHARACTER_NOT_FOUND));
    }
}
