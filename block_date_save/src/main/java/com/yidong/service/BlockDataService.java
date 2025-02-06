package com.yidong.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yidong.model.ProgressStatus;
import org.springframework.scheduling.annotation.Async;
import java.util.List;
import java.util.ArrayList;
import java.lang.StringBuilder;

@Service
public class BlockDataService {
    
    private static final String OUTPUT_DIR = "block_data";
    private static final Logger log = LoggerFactory.getLogger(BlockDataService.class);
    private static final long RETRY_DELAY_MS = 500;
    private static final long MAX_RETRY_DELAY_MS = 2000;
    
    private volatile long totalBlocks = 0;
    private volatile long processedBlockCount = 0;
    private volatile boolean isProcessing = false;
    private volatile boolean hasError = false;
    private volatile Long currentBlockNum = null;
    
    public ProgressStatus getProgress() {
        return new ProgressStatus(
            processedBlockCount,
            isProcessing,
            hasError,
            totalBlocks,
            currentBlockNum
        );
    }
    
    /**
     * 验证API是否可用
     * @param apiUrl API地址
     * @return 验证结果
     */
    public Map<String, Object> validateApi(String apiUrl) {
        Map<String, Object> result = new HashMap<>();
        RestTemplate restTemplate = new RestTemplate();
        
        try {
            // 使用区块号 1000000 作为测试
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("block_num_or_id", 1000000);
            
            String response = restTemplate.postForObject(
                apiUrl,
                requestBody,
                String.class
            );
            
            if (response != null) {
                result.put("success", true);
                result.put("message", "API连接成功");
            } else {
                result.put("success", false);
                result.put("message", "API返回空数据");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "API连接失败: " + e.getMessage());
            log.error("API验证失败: {}", e.getMessage());
        }
        
        return result;
    }
    
    @Async
    public void fetchAndSaveBlocks(String apiUrl, long startBlock, long endBlock) {
        try {
            // 先验证API
            Map<String, Object> validationResult = validateApi(apiUrl);
            if (!(Boolean)validationResult.get("success")) {
                throw new RuntimeException((String)validationResult.get("message"));
            }
            
            processedBlockCount = 0;
            isProcessing = true;
            hasError = false;
            totalBlocks = endBlock - startBlock + 1;
            
            File dir = new File(OUTPUT_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 创建一个列表来存储所有区块数据
            List<String> blockDataList = new ArrayList<>();
            RestTemplate restTemplate = new RestTemplate();
            
            for (long blockNum = startBlock; blockNum <= endBlock; blockNum++) {
                currentBlockNum = blockNum;
                boolean blockSuccess = false;
                int retryCount = 0;
                
                while (!blockSuccess) {
                    try {
                        Map<String, Object> requestBody = new HashMap<>();
                        requestBody.put("block_num_or_id", blockNum);
                        
                        String blockData = restTemplate.postForObject(
                            apiUrl,
                            requestBody,
                            String.class
                        );
                        
                        // 将区块数据添加到列表中
                        blockDataList.add(blockData);
                        
                        blockSuccess = true;
                        processedBlockCount++;
                        log.info("成功获取区块 {} 的数据 ({}/{})", 
                            blockNum, processedBlockCount, totalBlocks);
                        
                    } catch (Exception e) {
                        retryCount++;
                        hasError = true;
                        log.error("获取区块 {} 数据失败 (尝试 {}): {}", 
                            blockNum, retryCount, e.getMessage());
                        
                        try {
                            long waitTime = Math.min(RETRY_DELAY_MS * retryCount, MAX_RETRY_DELAY_MS);
                            log.info("等待 {} 毫秒后重试区块 {}", waitTime, blockNum);
                            Thread.sleep(waitTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("线程被中断", ie);
                        }
                    }
                }
            }
            
            // 将所有区块数据写入单个文件
            if (!blockDataList.isEmpty()) {
                // 构建包含所有区块的 JSON 数组
                StringBuilder jsonContent = new StringBuilder();
                jsonContent.append("{\n  \"blocks\": [\n");
                
                for (int i = 0; i < blockDataList.size(); i++) {
                    jsonContent.append(blockDataList.get(i));
                    if (i < blockDataList.size() - 1) {
                        jsonContent.append(",\n");
                    }
                }
                
                jsonContent.append("\n  ]\n}");
                
                // 生成文件名，包含区块范围
                String fileName = String.format("blocks_%d_%d.json", startBlock, endBlock);
                File outputFile = new File(dir, fileName);
                
                Files.write(
                    outputFile.toPath(),
                    jsonContent.toString().getBytes(StandardCharsets.UTF_8)
                );
                
                log.info("成功将所有区块数据保存到文件: {}", fileName);
            }
            
            // 所有区块都处理完成后的检查
            if (processedBlockCount == totalBlocks) {
                hasError = false;
                log.info("所有区块数据获取成功. 总计: {}", totalBlocks);
            } else {
                hasError = true;
                log.error("部分区块数据获取失败. 总计: {}, 成功: {}", totalBlocks, processedBlockCount);
            }
            
        } catch (Exception e) {
            log.error("获取过程中发生错误: {}", e.getMessage());
            hasError = true;
        } finally {
            currentBlockNum = null;
            isProcessing = false;
            log.info("数据获取任务结束. 总计: {}, 成功: {}, 是否有错误: {}", 
                totalBlocks, processedBlockCount, hasError);
        }
    }
} 