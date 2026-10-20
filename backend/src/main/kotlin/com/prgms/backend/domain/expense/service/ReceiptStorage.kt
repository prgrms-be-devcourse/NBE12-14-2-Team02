package com.prgms.backend.domain.expense.service

import com.prgms.backend.domain.expense.exception.ExpenseRequestException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import javax.imageio.IIOException
import javax.imageio.ImageIO

@Component
class ReceiptStorage(@Value("\${app.receipts.directory:./uploads/receipts}") directory: String) {
    private val root = Path.of(directory).toAbsolutePath().normalize()

    // 디코딩과 파일 저장은 모임의 DB 잠금을 잡기 전에 처리합니다.
    fun store(file: MultipartFile?): String {
        if (file == null || file.isEmpty || file.size > 5 * 1024 * 1024)
            throw ExpenseRequestException(400, "영수증은 5MB 이하의 JPG 또는 PNG 1장이어야 합니다.")
        try {
            file.inputStream.use { source ->
                ImageIO.createImageInputStream(source).use { input ->
                    val readers = ImageIO.getImageReaders(input)
                    if (!readers.hasNext()) throw invalidImage()
                    val reader = readers.next()
                    try {
                        val format = reader.formatName
                        if (!format.equals("JPEG", ignoreCase = true) && !format.equals("PNG", ignoreCase = true)) throw invalidImage()
                        reader.input = input
                        val pixels = reader.getWidth(0).toLong() * reader.getHeight(0)
                        if (pixels > 20_000_000) throw ExpenseRequestException(400, "영수증은 2,000만 화소 이하로 첨부해주세요.")
                        val image = reader.read(0)
                        val key = "${UUID.randomUUID()}.png"
                        Files.createDirectories(root)
                        val target = resolve(key)
                        try {
                            if (!ImageIO.write(image, "png", target.toFile())) throw IOException("Image writer unavailable")
                        } catch (exception: IOException) {
                            Files.deleteIfExists(target)
                            throw exception
                        }
                        return key
                    } finally {
                        reader.dispose()
                    }
                }
            }
        } catch (exception: IIOException) {
            throw invalidImage()
        } catch (exception: IOException) {
            throw ExpenseRequestException(500, "영수증 파일을 저장하지 못했습니다.")
        }
    }

    fun read(key: String?): ByteArray = try {
        Files.readAllBytes(resolve(key))
    } catch (exception: IOException) {
        throw ExpenseRequestException(404, "영수증 파일을 찾을 수 없습니다.")
    }

    fun delete(key: String?) {
        if (key == null || !key.matches(KEY_PATTERN)) return
        try {
            Files.deleteIfExists(resolve(key))
        } catch (exception: IOException) {
            logger.warn("영수증 파일 정리가 지연되었습니다.")
        }
    }

    fun deleteAfterCommit(key: String?) {
        if (key == null) return
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() { delete(key) }
        })
    }

    private fun resolve(key: String?): Path {
        if (key == null || !key.matches(KEY_PATTERN)) throw ExpenseRequestException(404, "영수증 파일을 찾을 수 없습니다.")
        return root.resolve(key)
    }

    private fun invalidImage() = ExpenseRequestException(400, "정상적인 JPG 또는 PNG 이미지를 첨부해주세요.")

    companion object {
        private val KEY_PATTERN = Regex("[a-f0-9-]{36}\\.png")
        private val logger = LoggerFactory.getLogger(ReceiptStorage::class.java)
    }
}
