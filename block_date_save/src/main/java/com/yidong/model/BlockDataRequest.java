package com.yidong.model;

import lombok.Data;

@Data
public class BlockDataRequest {
    private String apiUrl;
    private long startBlock;
    private long endBlock;
} 