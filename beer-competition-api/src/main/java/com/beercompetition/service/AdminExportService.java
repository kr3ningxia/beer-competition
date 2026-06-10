package com.beercompetition.service;

import com.beercompetition.pojo.vo.FileDownloadVO;

public interface AdminExportService {

    FileDownloadVO exportEntries(Long competitionId, Long categoryId, String entryStatus, String paymentStatus,
                                 String deliveryStatus, String keyword);

    FileDownloadVO exportDelivery(Long competitionId, Long categoryId, String entryStatus, String paymentStatus,
                                  String deliveryStatus, String keyword);

    FileDownloadVO exportLabels(Long competitionId, Long categoryId, String entryStatus, String paymentStatus,
                                String deliveryStatus, String keyword, Integer copies);

    void logScoringExport(Long competitionId);
}
