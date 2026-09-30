package com.prgms.backend.domain.content.ENUM;

public enum ContentPreference {
    PREFER, // 3점
    AVAILABLE, // 2점
    DISLIKE; // 1점

    public int score(){
        return switch (this){
            case PREFER -> 3;
            case AVAILABLE -> 2;
            case DISLIKE -> 1;
        };
    }
}
