package com.yidong.model;

import java.util.List;

public class VerificationProgress {
    private Long currentBlock;
    private int verifiedCount;
    private int totalBlocks;
    private boolean isVerifying;
    private List<VerificationResult> verificationResults;

    // Getters and Setters
    public Long getCurrentBlock() {
        return currentBlock;
    }

    public void setCurrentBlock(Long currentBlock) {
        this.currentBlock = currentBlock;
    }

    public int getVerifiedCount() {
        return verifiedCount;
    }

    public void setVerifiedCount(int verifiedCount) {
        this.verifiedCount = verifiedCount;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public void setTotalBlocks(int totalBlocks) {
        this.totalBlocks = totalBlocks;
    }

    public boolean isVerifying() {
        return isVerifying;
    }

    public void setVerifying(boolean verifying) {
        isVerifying = verifying;
    }

    public List<VerificationResult> getVerificationResults() {
        return verificationResults;
    }

    public void setVerificationResults(List<VerificationResult> verificationResults) {
        this.verificationResults = verificationResults;
    }
} 