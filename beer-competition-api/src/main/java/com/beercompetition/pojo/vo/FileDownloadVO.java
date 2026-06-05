package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileDownloadVO {

    private String fileName;
    private String contentType;
    private byte[] content;
}
