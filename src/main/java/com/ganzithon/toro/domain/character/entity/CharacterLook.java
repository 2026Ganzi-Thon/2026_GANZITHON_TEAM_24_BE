package com.ganzithon.toro.domain.character.entity;

/**
 * 캐릭터 이름과 꾸미기 값. 만들기·다시 꾸미기 요청이 같이 쓴다.
 */
public interface CharacterLook {

    String name();

    String color();

    String eyes();

    String nose();

    String mouth();

    String eyebrows();

    String accessory();
}
