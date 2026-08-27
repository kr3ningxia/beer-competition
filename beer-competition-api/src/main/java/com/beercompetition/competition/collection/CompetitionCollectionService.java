package com.beercompetition.competition.collection;

import com.beercompetition.pojo.dto.CompetitionCollectionConfigUpdateRequest;
import com.beercompetition.pojo.vo.CompetitionCollectionConfigVO;
import com.beercompetition.pojo.vo.CompetitionCollectionQrVO;
import org.springframework.web.multipart.MultipartFile;

public interface CompetitionCollectionService {

    CompetitionCollectionConfigVO getAdminConfig(Long competitionId);

    CompetitionCollectionConfigVO updateAdminConfig(Long competitionId,
                                                     CompetitionCollectionConfigUpdateRequest request);

    CompetitionCollectionQrVO uploadWechatQr(Long competitionId, MultipartFile file);

    CompetitionCollectionConfigVO getPortalConfig(Long competitionId);
}
