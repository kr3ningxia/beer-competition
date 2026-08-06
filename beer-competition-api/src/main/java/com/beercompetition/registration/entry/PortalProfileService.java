package com.beercompetition.registration.entry;

import com.beercompetition.pojo.dto.PortalProfileUpdateRequest;
import com.beercompetition.pojo.vo.PortalProfileVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 维护当前厂商账号及厂牌资料。
 */
public interface PortalProfileService {

    PortalProfileVO getPortalProfile();

    PortalProfileVO updatePortalProfile(PortalProfileUpdateRequest request);

    PortalProfileVO uploadPortalAvatar(MultipartFile file);
}
