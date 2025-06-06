package com.p2pnetwork.message.dto;

import com.p2pnetwork.routing.RoutingEntry;
import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class AssignSuperNodeContent {
    private boolean promote;                 // true: 승격, false: 할당
    private RoutingEntry superNodeEntry;     // 할당된 슈퍼노드 정보 (할당 시 필요)
    private RoutingEntry[] superNodeTable;   // 슈퍼노드 테이블 전체 (승격 시 필요)
    private RoutingEntry[] redundancyTable;  // 레둔던시 테이블 전체 (승격 시 필요)
}
