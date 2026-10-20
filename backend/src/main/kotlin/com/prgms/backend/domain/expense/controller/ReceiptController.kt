package com.prgms.backend.domain.expense.controller

import com.prgms.backend.domain.expense.service.ReceiptService
import com.prgms.backend.domain.expense.service.ReceiptStorage
import com.prgms.backend.global.ApiResponse
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MaxUploadSizeExceededException
import java.security.Principal

@RestController
@RequestMapping("/api/meetings/{meetingId}/expenses/{expenseId}/receipt")
class ReceiptController(private val receipts: ReceiptService, private val storage: ReceiptStorage) {
    @PutMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(@PathVariable meetingId: Long, @PathVariable expenseId: Long, principal: Principal?,
               @RequestPart("file") file: MultipartFile): ApiResponse<Void> {
        receipts.checkUpload(meetingId, expenseId, principal)
        val key = storage.store(file)
        try {
            receipts.replace(meetingId, expenseId, principal, key)
        } catch (exception: RuntimeException) {
            storage.delete(key)
            throw exception
        }
        return ApiResponse.success(200, null)
    }

    @GetMapping(produces = [MediaType.IMAGE_PNG_VALUE])
    fun read(@PathVariable meetingId: Long, @PathVariable expenseId: Long, principal: Principal?): ResponseEntity<ByteArray> =
        ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
            .header("X-Content-Type-Options", "nosniff").body(receipts.read(meetingId, expenseId, principal))

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable meetingId: Long, @PathVariable expenseId: Long, principal: Principal?) {
        receipts.replace(meetingId, expenseId, principal, null)
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun tooLarge(): ResponseEntity<ApiResponse<Void>> =
        ResponseEntity.status(413).body(ApiResponse.error(413, "영수증은 5MB 이하로 첨부해주세요."))
}
