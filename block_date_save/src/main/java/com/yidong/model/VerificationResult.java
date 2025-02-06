package com.yidong.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class VerificationResult {
    private Long blockNum;
    private boolean dataMatch;
    private boolean hashMatch;
    private String chainHash;
    private String localHash;
    private String message;
    private boolean isLocalContinuous;
    private boolean isChainContinuous;
    private List<String> mismatchFields;
    private Map<String, Object> fieldDetails;
} 