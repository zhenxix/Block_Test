package com.yidong.model;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class ProgressStatus {
    private long processedBlocks;
    private boolean isProcessing;
    private boolean hasError;
    private long totalBlocks;
    private Long currentBlockNum;
} 