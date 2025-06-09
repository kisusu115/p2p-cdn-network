package com.p2pnetwork.message.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class FileDownloadContent {
    boolean exist;
    String fileHash;
    String fileData;
}
