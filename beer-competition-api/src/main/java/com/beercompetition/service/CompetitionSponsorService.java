package com.beercompetition.service;

import com.beercompetition.pojo.dto.CompetitionSponsorBatchUpdateRequest;
import com.beercompetition.pojo.vo.CompetitionSponsorLogoVO;
import com.beercompetition.pojo.vo.CompetitionSponsorVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CompetitionSponsorService {

    List<CompetitionSponsorVO> listSponsors(Long competitionId);

    List<CompetitionSponsorVO> updateSponsors(Long competitionId, CompetitionSponsorBatchUpdateRequest request);

    CompetitionSponsorLogoVO uploadSponsorLogo(Long competitionId, MultipartFile file);
}
