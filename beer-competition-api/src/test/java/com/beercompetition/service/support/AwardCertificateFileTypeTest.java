package com.beercompetition.service.support;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AwardCertificateFileTypeTest {

    @Test
    void detectsSupportedCertificateContent() {
        assertThat(AwardCertificateFileType.fromContent(bytes("%PDF-1.7")))
                .contains(AwardCertificateFileType.PDF);
        assertThat(AwardCertificateFileType.fromContent(new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00
        })).contains(AwardCertificateFileType.PNG);
        assertThat(AwardCertificateFileType.fromContent(new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0
        })).contains(AwardCertificateFileType.JPEG);
    }

    @Test
    void rejectsUnknownCertificateContent() {
        assertThat(AwardCertificateFileType.fromContent(bytes("plain text"))).isEmpty();
    }

    @Test
    void mapsSupportedFilenameExtensionsToDownloadContentType() {
        assertThat(AwardCertificateFileType.resolveContentType("award.pdf")).isEqualTo("application/pdf");
        assertThat(AwardCertificateFileType.resolveContentType("award.PNG")).isEqualTo("image/png");
        assertThat(AwardCertificateFileType.resolveContentType("award.jpg")).isEqualTo("image/jpeg");
        assertThat(AwardCertificateFileType.resolveContentType("award.jpeg")).isEqualTo("image/jpeg");
        assertThat(AwardCertificateFileType.resolveContentType("award.bin")).isEqualTo("application/octet-stream");
    }

    @Test
    void validatesDeclaredContentType() {
        assertThat(AwardCertificateFileType.PNG.matchesDeclaredContentType("image/png")).isTrue();
        assertThat(AwardCertificateFileType.PNG.matchesDeclaredContentType("application/octet-stream")).isTrue();
        assertThat(AwardCertificateFileType.PNG.matchesDeclaredContentType(null)).isTrue();
        assertThat(AwardCertificateFileType.PNG.matchesDeclaredContentType("image/jpeg")).isFalse();
        assertThat(AwardCertificateFileType.JPEG.matchesDeclaredContentType("image/jpg")).isTrue();
    }

    @Test
    void exposesFilenameAndContentMismatchForUploadValidation() {
        assertThat(AwardCertificateFileType.fromFilename("renamed.jpg"))
                .contains(AwardCertificateFileType.JPEG);
        assertThat(AwardCertificateFileType.fromContent(bytes("%PDF-1.7")))
                .contains(AwardCertificateFileType.PDF);
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }
}
