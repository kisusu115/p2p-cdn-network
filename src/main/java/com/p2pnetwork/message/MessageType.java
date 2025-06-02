package com.p2pnetwork.message;

public enum MessageType {
    INTRODUCE,                  // Peer → Bootstrap: 자기 정보 소개
    ASSIGN_SUPERNODE,           // Bootstrap → Peer: 슈퍼노드 할당/승격
    JOIN_REQUEST,               // Peer → SuperNode: 그룹 가입 요청
    JOIN_RESPONSE,              // SuperNode → Peer: 로컬 라우팅 테이블 전달
    NEW_PEER_BROADCAST,         // SuperNode → Group: 새 로컬 Peer 등장 알림
    NEW_SUPERNODE_BROADCAST,    // Bootstrap → SuperNode들: 새 슈퍼노드 등장 알림
    PROMOTE_REDUNDANCY,         // Bootstrap → Peer: 레둔던시 승격
    NEW_REDUNDANCY_BROADCAST,   // SuperNode → 다른 SuperNode들: 새 레둔던시 등장 알림
}