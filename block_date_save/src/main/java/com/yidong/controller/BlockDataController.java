package com.yidong.controller;

import com.yidong.model.ApiResponse;
import com.yidong.model.BlockDataRequest;
import com.yidong.model.ProgressStatus;
import com.yidong.model.VerificationResult;
import com.yidong.model.VerificationProgress;
import com.yidong.service.BlockDataService;
import com.yidong.service.BlockVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/blocks")
@CrossOrigin(origins = "http://localhost:5173") // Vite默认端口
public class BlockDataController {

    @Autowired
    private BlockDataService blockDataService;

    @Autowired
    private BlockVerificationService blockVerificationService;

    @PostMapping("/fetch")
    public ResponseEntity<?> fetchBlockData(@RequestBody BlockDataRequest request) {
        try {
            blockDataService.fetchAndSaveBlocks(
                request.getApiUrl(),
                request.getStartBlock(),
                request.getEndBlock()
            );
            return ResponseEntity.ok(new ApiResponse(true, "数据获取任务已开始"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "数据获取失败：" + e.getMessage()));
        }
    }

    @GetMapping("/progress")
    public ResponseEntity<ProgressStatus> getProgress() {
        return ResponseEntity.ok(blockDataService.getProgress());
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateApi(@RequestBody BlockDataRequest request) {
        Map<String, Object> result = blockDataService.validateApi(request.getApiUrl());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyBlocks(
        @RequestParam("file") MultipartFile file,
        @RequestParam("apiUrl") String apiUrl
    ) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            blockVerificationService.startVerification(content, apiUrl);
            return ResponseEntity.ok(new ApiResponse(true, "验证任务已开始"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "验证失败: " + e.getMessage()));
        }
    }

    @GetMapping("/verify/progress")
    public ResponseEntity<VerificationProgress> getVerificationProgress() {
        return ResponseEntity.ok(blockVerificationService.getVerificationProgress());
    }

    @GetMapping("/verify/results")
    public ResponseEntity<?> getVerificationResults() {
        try {
            List<VerificationResult> results = blockVerificationService.getVerificationResults();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("results", results);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "获取验证结果失败: " + e.getMessage()));
        }
    }
}
