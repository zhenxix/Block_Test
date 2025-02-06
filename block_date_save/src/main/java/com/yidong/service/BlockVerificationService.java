package com.yidong.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.yidong.model.VerificationProgress;
import com.yidong.model.VerificationResult;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

@Service
public class BlockVerificationService {
    private static final Logger logger = LoggerFactory.getLogger(BlockVerificationService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    
    // 存储验证进度和结果
    private VerificationProgress currentProgress;
    private List<VerificationResult> verificationResults;
    private ExecutorService executorService;
    private boolean isVerifying = false;

    public BlockVerificationService() {
        this.executorService = Executors.newSingleThreadExecutor();
        this.verificationResults = new ArrayList<>();
        this.currentProgress = new VerificationProgress();
    }

    public void startVerification(String content, String apiUrl) {
        if (isVerifying) {
            throw new IllegalStateException("验证任务正在进行中");
        }

        isVerifying = true;
        verificationResults.clear();
        
        executorService.submit(() -> {
            try {
                List<VerificationResult> results = verifyBlocks(content, apiUrl);
                synchronized (this) {
                    verificationResults.addAll(results);
                    isVerifying = false;
                    updateProgress(results.size(), results.size(), null, results);
                }
            } catch (Exception e) {
                logger.error("验证过程发生错误", e);
                isVerifying = false;
                updateProgress(0, 0, null, Collections.emptyList());
            }
        });
    }

    private List<VerificationResult> verifyBlocks(String content, String apiUrl) {
        try {
            // 解析上传的文件内容
            JsonNode rootNode = objectMapper.readTree(content);
            List<Map<String, Object>> blocks = new ArrayList<>();
            
            // 处理不同的文件格式
            if (rootNode.isArray()) {
                // 如果是数组格式
                rootNode.forEach(node -> blocks.add(objectMapper.convertValue(node, Map.class)));
            } else if (rootNode.has("blocks")) {
                // 如果是 {blocks: [...]} 格式
                rootNode.get("blocks").forEach(node -> blocks.add(objectMapper.convertValue(node, Map.class)));
            } else if (rootNode.has("block_num")) {
                // 如果是单个区块
                blocks.add(objectMapper.convertValue(rootNode, Map.class));
            } else {
                throw new RuntimeException("无效的文件格式：需要区块数组或包含blocks字段的对象");
            }

            if (blocks.isEmpty()) {
                throw new RuntimeException("未找到任何区块数据");
            }
            
            List<VerificationResult> results = new ArrayList<>();
            int totalBlocks = blocks.size();
            
            for (int i = 0; i < blocks.size(); i++) {
                Map<String, Object> localBlock = blocks.get(i);
                Long blockNum = Long.valueOf(localBlock.get("block_num").toString());
                
                // 验证本地区块连续性
                boolean isLocalContinuous = true;
                if (i > 0) {
                    // 获取前一个区块进行连续性验证
                    Map<String, Object> previousLocalBlock = blocks.get(i - 1);
                    isLocalContinuous = verifyLocalContinuity(localBlock, previousLocalBlock);
                } else {
                    // 对于第一个区块，检查其 previous 是否为全0哈希（如果不是第一个区块号，则标记为不连续）
                    String firstBlockPrevious = (String) localBlock.get("previous");
                    if (blockNum == 1) {
                        isLocalContinuous = firstBlockPrevious.matches("^0{64}$");
                        if (!isLocalContinuous) {
                            logger.error("第一个区块的 previous 不是全0哈希: {}", firstBlockPrevious);
                        }
                    } else {
                        isLocalContinuous = false;
                        logger.error("文件中的第一个区块号不是1: {}", blockNum);
                    }
                }
                
                try {
                    // 从链上获取当前区块数据
                    Map<String, Object> requestBody = new HashMap<>();
                    requestBody.put("block_num_or_id", blockNum);
                    Map chainBlock = restTemplate.postForObject(apiUrl, requestBody, Map.class);

                    // 验证链上区块的连续性
                    boolean currentChainContinuous = true;
                    // 获取当前区块的 previous 哈希
                    String currentPrevious = chainBlock.get("previous").toString();
                    
                    // 通过 API 获取前一个区块的信息
                    Map<String, Object> previousRequestBody = new HashMap<>();
                    previousRequestBody.put("block_num_or_id", blockNum - 1);
                    try {
                        Map<String, Object> prevChainBlock = restTemplate.postForObject(apiUrl, previousRequestBody, Map.class);
                        // 验证区块号连续性
                        Long currentBlockNum = Long.valueOf(chainBlock.get("block_num").toString());
                        Long previousBlockNum = Long.valueOf(prevChainBlock.get("block_num").toString());
                        boolean isBlockNumContinuous = (currentBlockNum - previousBlockNum == 1);
                        
                        // 验证区块哈希连续性
                        String previousId = prevChainBlock.get("id").toString();
                        boolean isHashContinuous = currentPrevious.equals(previousId);
                        
                        currentChainContinuous = isBlockNumContinuous && isHashContinuous;
                        
                        if (!currentChainContinuous) {
                            if (!isBlockNumContinuous) {
                                logger.error("链上区块号不连续: 当前区块号 {} 与前一区块号 {} 不连续",
                                    currentBlockNum, previousBlockNum);
                            }
                            if (!isHashContinuous) {
                                logger.error("链上区块哈希不连续: 区块 {} 的 previous ({}) 与前一个区块的 id ({}) 不匹配",
                                    currentBlockNum, currentPrevious, previousId);
                            }
                        }
                    } catch (Exception e) {
                        // 如果是第一个区块，previous 应该是全0的哈希
                        if (blockNum > 1) {
                            logger.error("获取链上前一个区块时发生错误: {}", e.getMessage());
                            currentChainContinuous = false;
                        } else {
                            // 对于第一个区块，检查 previous 是否为全0哈希
                            currentChainContinuous = currentPrevious.matches("^0{64}$");
                        }
                    }

                    // 获取哈希值
                    String chainHash = chainBlock.get("id").toString();
                    String localHash = localBlock.get("id").toString();
                    
                    // 比较数据
                    boolean dataMatch = compareBlockData(localBlock, chainBlock);
                    boolean hashMatch = chainHash.equals(localHash);

                    // 创建验证结果
                    VerificationResult result = VerificationResult.builder()
                        .blockNum(blockNum)
                        .dataMatch(dataMatch)
                        .hashMatch(hashMatch)
                        .chainHash(chainHash)
                        .localHash(localHash)
                        .message(createVerificationMessage(dataMatch, hashMatch, new ArrayList<>()))
                        .isLocalContinuous(isLocalContinuous)
                        .isChainContinuous(currentChainContinuous)
                        .build();
                    
                    results.add(result);
                    updateProgress(i + 1, totalBlocks, blockNum, results);
                    
                    logger.info("验证区块 {}: 数据匹配={}, 哈希匹配={}, 本地连续={}, 链上连续={}", 
                        blockNum, dataMatch, hashMatch, isLocalContinuous, currentChainContinuous);
                    
                } catch (Exception e) {
                    logger.error("验证区块 {} 时发生错误: {}", blockNum, e.getMessage());
                    VerificationResult result = VerificationResult.builder()
                        .blockNum(blockNum)
                        .dataMatch(false)
                        .hashMatch(false)
                        .chainHash("获取失败")
                        .localHash(localBlock.get("id").toString())
                        .message("验证失败: " + e.getMessage())
                        .isLocalContinuous(isLocalContinuous)
                        .isChainContinuous(false)  // 出错时设置为 false
                        .build();
                    results.add(result);
                    updateProgress(i + 1, totalBlocks, blockNum, results);
                }
            }
            
            return results;
        } catch (Exception e) {
            logger.error("验证过程发生错误", e);
            throw new RuntimeException("验证过程发生错误: " + e.getMessage());
        }
    }

    private void updateProgress(int verified, int total, Long currentBlock, List<VerificationResult> results) {
        synchronized (this) {
            currentProgress.setVerifiedCount(verified);
            currentProgress.setTotalBlocks(total);
            currentProgress.setCurrentBlock(currentBlock);
            currentProgress.setVerifying(isVerifying);
            currentProgress.setVerificationResults(new ArrayList<>(results));
        }
    }

    public VerificationProgress getVerificationProgress() {
        synchronized (this) {
            return currentProgress;
        }
    }

    public List<VerificationResult> getVerificationResults() {
        synchronized (this) {
            return new ArrayList<>(verificationResults);
        }
    }

    private boolean compareBlockData(Map<String, Object> localBlock, Map<String, Object> chainBlock) {
        try {
            List<String> mismatchFields = new ArrayList<>();
            
            // 检查基本字段
            if (!Objects.equals(localBlock.get("block_num").toString(), chainBlock.get("block_num").toString())) {
                mismatchFields.add("block_num");
            }
            if (!Objects.equals(localBlock.get("timestamp"), chainBlock.get("timestamp"))) {
                mismatchFields.add("timestamp");
            }
            if (!Objects.equals(localBlock.get("producer"), chainBlock.get("producer"))) {
                mismatchFields.add("producer");
            }
            if (!Objects.equals(localBlock.get("previous"), chainBlock.get("previous"))) {
                mismatchFields.add("previous");
            }
            if (!Objects.equals(localBlock.get("transaction_mroot"), chainBlock.get("transaction_mroot"))) {
                mismatchFields.add("transaction_mroot");
            }
            if (!Objects.equals(localBlock.get("action_mroot"), chainBlock.get("action_mroot"))) {
                mismatchFields.add("action_mroot");
            }
            if (!Objects.equals(localBlock.get("schedule_version"), chainBlock.get("schedule_version"))) {
                mismatchFields.add("schedule_version");
            }

            boolean dataMatch = mismatchFields.isEmpty();
            
            if (!dataMatch) {
                logger.error("区块 {} 数据不匹配的字段: {}", 
                    localBlock.get("block_num"), 
                    String.join(", ", mismatchFields));
                
                // 记录不匹配字段的具体值
                for (String field : mismatchFields) {
                    logger.error("区块 {} 字段 {} 不匹配: 本地值={}, 链上值={}", 
                        localBlock.get("block_num"),
                        field,
                        localBlock.get(field),
                        chainBlock.get(field));
                }
            }

            return dataMatch;
        } catch (Exception e) {
            logger.error("比较区块数据时发生错误: {}", e.getMessage());
            return false;
        }
    }

    private String createVerificationMessage(boolean dataMatch, boolean hashMatch, List<String> mismatchFields) {
        if (dataMatch && hashMatch) {
            return "验证通过";
        } else if (!dataMatch && !hashMatch) {
            return "数据和哈希不匹配，不匹配字段: " + String.join(", ", mismatchFields);
        } else if (!dataMatch) {
            return "数据不匹配，不匹配字段: " + String.join(", ", mismatchFields);
        } else {
            return "哈希不匹配";
        }
    }

    private boolean verifyLocalContinuity(Map<String, Object> currentBlock, Map<String, Object> previousBlock) {
        try {
            // 获取区块号
            Long currentBlockNum = Long.valueOf(currentBlock.get("block_num").toString());
            Long previousBlockNum = Long.valueOf(previousBlock.get("block_num").toString());
            
            // 验证区块号连续性
            boolean isBlockNumContinuous = (currentBlockNum - previousBlockNum == 1);
            if (!isBlockNumContinuous) {
                logger.error("本地区块号不连续: 区块 {} 与区块 {} 之间存在间隔",
                    currentBlockNum, previousBlockNum);
                return false;
            }
            
            // 验证区块哈希连续性
            String currentPrevious = (String) currentBlock.get("previous");
            String previousId = (String) previousBlock.get("id");
            boolean isHashContinuous = Objects.equals(currentPrevious, previousId);
            if (!isHashContinuous) {
                logger.error("本地区块哈希不连续: 区块 {} 的 previous ({}) 与区块 {} 的 id ({}) 不匹配",
                    currentBlockNum, currentPrevious, previousBlockNum, previousId);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.error("验证本地区块连续性时发生错误: {}", e.getMessage());
            return false;
        }
    }
} 