package com.beercompetition.registration.entry;

import com.beercompetition.pojo.vo.FileDownloadVO;
import com.beercompetition.pojo.vo.PortalEntryLabelVO;

/**
 * 生成和下载报名标签，所有入口先验证当前厂商对报名的所有权。
 */
public interface EntryDocumentService {

    PortalEntryLabelVO getPortalEntryLabel(Long entryId);

    FileDownloadVO downloadPortalEntryLabelPdf(Long entryId);

    FileDownloadVO downloadPortalEntryLabelPng(Long entryId);
}
