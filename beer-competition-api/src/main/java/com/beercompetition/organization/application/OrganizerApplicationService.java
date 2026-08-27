package com.beercompetition.organization.application;

import com.beercompetition.pojo.dto.OrganizerApplicationReviewRequest;
import com.beercompetition.pojo.dto.OrganizerApplicationSubmitRequest;
import com.beercompetition.pojo.vo.OrganizerApplicationAdminVO;
import com.beercompetition.pojo.vo.OrganizerApplicationStatusVO;
import com.beercompetition.pojo.vo.OrganizerApplicationSubmitVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OrganizerApplicationService {

    OrganizerApplicationSubmitVO submit(OrganizerApplicationSubmitRequest request);

    OrganizerApplicationSubmitVO submit(OrganizerApplicationSubmitRequest request, MultipartFile material);

    OrganizerApplicationSubmitVO submitForCurrentPortal(OrganizerApplicationSubmitRequest request);

    OrganizerApplicationSubmitVO submitForCurrentPortal(OrganizerApplicationSubmitRequest request,
                                                        MultipartFile material);

    OrganizerApplicationStatusVO queryStatus(String applicationNo, String contactPhone);

    List<OrganizerApplicationStatusVO> listForCurrentPortal();

    OrganizerApplicationStatusVO resubmit(String applicationNo,
                                           String contactPhone,
                                           OrganizerApplicationSubmitRequest request);

    OrganizerApplicationStatusVO resubmit(String applicationNo,
                                           String contactPhone,
                                           OrganizerApplicationSubmitRequest request,
                                           MultipartFile material);

    OrganizerApplicationStatusVO resubmitForCurrentPortal(String applicationNo,
                                                           OrganizerApplicationSubmitRequest request);

    OrganizerApplicationStatusVO resubmitForCurrentPortal(String applicationNo,
                                                           OrganizerApplicationSubmitRequest request,
                                                           MultipartFile material);

    List<OrganizerApplicationAdminVO> listForPlatform(String status);

    OrganizerApplicationAdminVO review(Long applicationId, OrganizerApplicationReviewRequest request);
}
