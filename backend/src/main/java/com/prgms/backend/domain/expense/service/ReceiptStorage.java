package com.prgms.backend.domain.expense.service;

import com.prgms.backend.domain.expense.exception.ExpenseRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;

@Component
public class ReceiptStorage {
    private final Path root;
    public ReceiptStorage(@Value("${app.receipts.directory:./uploads/receipts}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }

    // 이미지 디코딩·파일 저장은 모임의 DB 잠금을 잡기 전에 처리합니다.
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
            throw new ExpenseRequestException(400, "영수증은 5MB 이하의 JPG 또는 PNG 1장이어야 합니다.");
        }
        try (var source = file.getInputStream(); var input = ImageIO.createImageInputStream(source)) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalidImage();
            var reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG")) throw invalidImage();
                reader.setInput(input);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > 20_000_000) throw new ExpenseRequestException(400, "영수증은 2,000만 화소 이하로 첨부해주세요.");
                var image = reader.read(0);
                String key = UUID.randomUUID() + ".png";
                Files.createDirectories(root);
                Path target = resolve(key);
                try {
                    if (!ImageIO.write(image, "png", target.toFile())) throw new IOException("Image writer unavailable");
                } catch (IOException e) {
                    Files.deleteIfExists(target);
                    throw e;
                }
                return key;
            } finally { reader.dispose(); }
        } catch (javax.imageio.IIOException e) {
            throw invalidImage();
        } catch (IOException e) {
            throw new ExpenseRequestException(500, "영수증 파일을 저장하지 못했습니다.");
        }
    }

    public byte[] read(String key) {
        try { return Files.readAllBytes(resolve(key)); }
        catch (IOException e) { throw new ExpenseRequestException(404, "영수증 파일을 찾을 수 없습니다."); }
    }

    public void delete(String key) {
        if (key == null) return;
        // 과거 형식의 키나 저장소 외부 경로는 삭제하지 않습니다.
        if (!key.matches("[a-f0-9-]{36}\\.png")) return;
        try { Files.deleteIfExists(resolve(key)); }
        catch (IOException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("영수증 파일 정리가 지연되었습니다."); }
    }

    public void deleteAfterCommit(String key) {
        if (key == null) return;
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCommit() { delete(key); }
            });
    }

    private Path resolve(String key) {
        if (key == null || !key.matches("[a-f0-9-]{36}\\.png")) throw new ExpenseRequestException(404, "영수증 파일을 찾을 수 없습니다.");
        return root.resolve(key);
    }
    private ExpenseRequestException invalidImage() { return new ExpenseRequestException(400, "정상적인 JPG 또는 PNG 이미지를 첨부해주세요."); }
}
