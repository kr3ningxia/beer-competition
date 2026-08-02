package com.beercompetition.service.impl;

import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.BeerEntryExtraFieldMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.EntryDeliveryMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.vo.FileDownloadVO;
import com.beercompetition.service.EntryScanLabelService;
import com.beercompetition.service.support.EntryLabelFileGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminExportServiceImplTest {

    @Mock
    private CompetitionMapper competitionMapper;
    @Mock
    private CompetitionCategoryMapper competitionCategoryMapper;
    @Mock
    private BeerEntryMapper beerEntryMapper;
    @Mock
    private BeerEntryExtraFieldMapper beerEntryExtraFieldMapper;
    @Mock
    private BreweryMapper breweryMapper;
    @Mock
    private EntryPaymentMapper entryPaymentMapper;
    @Mock
    private EntryDeliveryMapper entryDeliveryMapper;
    @Mock
    private AdminOperationLogMapper adminOperationLogMapper;
    @Mock
    private EntryScanLabelService entryScanLabelService;
    @Mock
    private EntryLabelFileGenerator entryLabelFileGenerator;

    @InjectMocks
    private AdminExportServiceImpl adminExportService;

    @Test
    void exportEntriesIncludesBreweryPhone() throws IOException {
        Competition competition = new Competition();
        competition.setId(1L);
        competition.setName("测试比赛");
        competition.setCode("TEST-2026");

        BeerEntry entry = new BeerEntry();
        entry.setId(10L);
        entry.setCompetitionId(1L);
        entry.setBreweryId(20L);
        entry.setCategoryId(30L);
        entry.setUuid("entry-uuid");
        entry.setName("测试酒款");
        entry.setStyle("Pilsner");
        entry.setStatus("REGISTERED");

        Brewery brewery = new Brewery();
        brewery.setId(20L);
        brewery.setCompanyName("测试厂牌");
        brewery.setContactName("测试联系人");
        brewery.setPhone("13800000001");

        when(competitionMapper.selectById(1L)).thenReturn(competition);
        when(beerEntryMapper.selectList(any())).thenReturn(List.of(entry));
        when(entryPaymentMapper.selectList(any())).thenReturn(List.of());
        when(entryDeliveryMapper.selectList(any())).thenReturn(List.of());
        when(entryScanLabelService.listActiveLabels(anyCollection())).thenReturn(Map.of());
        when(competitionCategoryMapper.selectList(any())).thenReturn(List.of());
        when(breweryMapper.selectBatchIds(anyCollection())).thenReturn(List.of(brewery));
        when(beerEntryExtraFieldMapper.selectList(any())).thenReturn(List.of());

        FileDownloadVO download = adminExportService.exportEntries(1L, null, null, null, null, null);
        String worksheet = readWorksheet(download.getContent());

        assertThat(worksheet).contains("<c r=\"I1\" t=\"inlineStr\" s=\"1\"><is><t>手机号</t>");
        assertThat(worksheet).contains("<c r=\"I2\" t=\"inlineStr\"><is><t>13800000001</t>");
    }

    private String readWorksheet(byte[] xlsx) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx), StandardCharsets.UTF_8)) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                if ("xl/worksheets/sheet1.xml".equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
                entry = zip.getNextEntry();
            }
        }
        throw new IOException("工作簿中未找到报名台账工作表");
    }
}
