package com.prgms.backend.domain.expense.controller;

import com.prgms.backend.domain.expense.service.*;
import com.prgms.backend.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings/{meetingId}/expenses/{expenseId}/receipt")
public class ReceiptController {
    private final ReceiptService receipts;
    private final ReceiptStorage storage;

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> upload(@PathVariable long meetingId, @PathVariable long expenseId, Principal principal,
                                   @RequestPart("file") MultipartFile file) {
        receipts.checkUpload(meetingId, expenseId, principal);
        String key = storage.store(file);
        try { receipts.replace(meetingId, expenseId, principal, key); }
        catch (RuntimeException e) { storage.delete(key); throw e; }
        return ApiResponse.success(200, null);
    }

    @GetMapping(produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> read(@PathVariable long meetingId, @PathVariable long expenseId, Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options", "nosniff")
                .body(receipts.read(meetingId, expenseId, principal));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long meetingId, @PathVariable long expenseId, Principal principal) {
        receipts.replace(meetingId, expenseId, principal, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> tooLarge() {
        return ResponseEntity.status(413).body(ApiResponse.error(413, "영수증은 5MB 이하로 첨부해주세요."));
    }
}
