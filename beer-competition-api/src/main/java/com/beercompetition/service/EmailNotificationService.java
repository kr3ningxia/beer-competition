package com.beercompetition.service;

import com.beercompetition.pojo.dto.NotificationRuleUpdateRequest;
import com.beercompetition.pojo.dto.NotificationTemplateUpdateRequest;
import com.beercompetition.pojo.dto.NotificationTestSendRequest;
import com.beercompetition.pojo.vo.CompetitionNotificationConfigVO;
import com.beercompetition.pojo.vo.EmailDeliveryVO;

import java.time.LocalDateTime;
import java.util.List;

public interface EmailNotificationService {

    CompetitionNotificationConfigVO getConfiguration(Long competitionId);

    CompetitionNotificationConfigVO updateRule(Long competitionId, NotificationRuleUpdateRequest request);

    CompetitionNotificationConfigVO updateTemplate(Long competitionId, String eventCode,
                                                    NotificationTemplateUpdateRequest request);

    String sendTest(Long competitionId, NotificationTestSendRequest request);

    List<EmailDeliveryVO> listDeliveries(Long competitionId, String status);

    void retryDelivery(Long competitionId, Long deliveryId);

    int generateScheduledNotifications(LocalDateTime now);

    int sendPendingEmails(LocalDateTime now);
}
