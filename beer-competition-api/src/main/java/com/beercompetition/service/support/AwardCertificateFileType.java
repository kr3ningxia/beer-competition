package com.beercompetition.service.support;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum AwardCertificateFileType {

    PDF("application/pdf", Set.of("pdf"), new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}),
    PNG("image/png", Set.of("png"), new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
    JPEG("image/jpeg", Set.of("jpg", "jpeg"), new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

    private static final String GENERIC_CONTENT_TYPE = "application/octet-stream";

    private final String contentType;
    private final Set<String> extensions;
    private final byte[] signature;

    AwardCertificateFileType(String contentType, Set<String> extensions, byte[] signature) {
        this.contentType = contentType;
        this.extensions = extensions;
        this.signature = signature;
    }

    public String contentType() {
        return contentType;
    }

    public boolean matchesDeclaredContentType(String declaredContentType) {
        if (!StringUtils.hasText(declaredContentType)) {
            return true;
        }
        String normalized = declaredContentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return GENERIC_CONTENT_TYPE.equals(normalized)
                || contentType.equals(normalized)
                || (this == JPEG && "image/jpg".equals(normalized));
    }

    public static Optional<AwardCertificateFileType> fromFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return Optional.empty();
        }
        String normalized = filename.trim().toLowerCase(Locale.ROOT);
        int extensionIndex = normalized.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == normalized.length() - 1) {
            return Optional.empty();
        }
        String extension = normalized.substring(extensionIndex + 1);
        return Arrays.stream(values())
                .filter(type -> type.extensions.contains(extension))
                .findFirst();
    }

    public static Optional<AwardCertificateFileType> fromContent(byte[] content) {
        if (content == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(type -> startsWith(content, type.signature))
                .findFirst();
    }

    public static String resolveContentType(String filename) {
        return fromFilename(filename)
                .map(AwardCertificateFileType::contentType)
                .orElse(GENERIC_CONTENT_TYPE);
    }

    private static boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }
}
