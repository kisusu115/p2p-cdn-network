package com.p2pnetwork.message;

public enum MessageType {
    INTRODUCE,                  // Peer → Bootstrap: 자기 정보 소개
    ASSIGN_SUPERNODE,           // Bootstrap → Peer: 슈퍼노드 할당/승격
    JOIN_REQUEST,               // Peer → SuperNode: 그룹 가입 요청
    JOIN_RESPONSE,              // SuperNode → Peer: 로컬 라우팅 테이블 전달
    NEW_PEER_BROADCAST,         // SuperNode → Group: 새 로컬 Peer 등장 알림
    NEW_SUPERNODE_BROADCAST,    // Bootstrap → SuperNode들: 새 슈퍼노드 등장 알림
    PROMOTE_REDUNDANCY,         // SuperNode → Peer: 레둔던시 승격
    NEW_REDUNDANCY_BROADCAST,   // SuperNode → 다른 SuperNode들: 새 레둔던시 등장 알림
    TCP_CONNECT,                // SuperNode → Redundancy: TCP 연결 수립
    REQUEST_TCP_CONNECT,        // Redundancy → SuperNode: TCP 연결 재수립 요청
    SERVERSOCKET_DEAD,          // SuperNode → Redundancy: TCP 연결에 ServerSocket이 죽었다고 알림
    REQUEST_TEMP_PROMOTE,       // Redundancy → SuperNode들: Bootstrap이 죽어 일시적 승격을 요청
    ACCEPT_PROMOTE,             // SuperNode → Redundancy: 승격 허가
    DENY_PROMOTE,               // SuperNode → Redundancy: 승격 불허
    BOOTSTRAP_REPLACEMENT,      // Redundancy → SuperNode들과 Peer들: Bootstrap 대신 Redundancy를 사용하라 알림
    BOOTSTRAP_REVIVED,          // Bootstrap → Redundancy: Bootstrap이 복구됨을 알림
    BOOTSTRAP_TABLE_SYNC,       // Redundancy → Bootstrap: Bootstrap이 복구되어 SuperNodeTable과 LocalRoutingTable을 전송해줌
    BOOTSTRAP_WORKING,          // Redundancy → SuperNode들과 Peer들: 다시 Bootstrap을 사용하라 알림
    REQUEST_PROMOTE,            // Redundancy → SuperNode들: SuperNode가 죽어 자신의 승격을 요청
    PROMOTED_SUPERNODE_BROADCAST,       // SuperNode → SuperNode들과 Peer들: 자신이 승격되어 table을 변경하게 만듦
    PROMOTED_REDUNDANCY_BROADCAST,      // SuperNode → SuperNode들과 Peer들: 새로 Peer를 Redundancy로 승격시켜 table을 변경하게 만듦
    //SUPERNODE_REVIVED,          // (이전) SuperNode → SuperNode 또는 Peer: SuperNode가 복구됨을 알림
    //OLD_SUPERNODE_REVIVED,      // Peer → SuperNode: 이전에 SuperNode였던 노드가 복구됨을 알림
    //DEMOTE_PEER,                // SuperNode → (이전) SuperNode: Peer로 강등
    CHECK_SUPERNODE,            // Peer → Redundancy: Redundancy에게 SuperNode가 죽은 것 같다고 알림
    UPDATE_SUPERNODE_TABLE_SUPER,       // SuperNode → Redundancy: SuperNodeTable을 update하게 만듦
    UPDATE_SUPERNODE_TABLE_BOOTSTRAP    // SuperNode → Redundancy: SuperNodeTable을 update하게 만듦
}