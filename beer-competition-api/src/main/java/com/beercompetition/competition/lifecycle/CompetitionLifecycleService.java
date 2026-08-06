package com.beercompetition.competition.lifecycle;

import com.beercompetition.pojo.dto.CompetitionReopenRegistrationRequest;
import com.beercompetition.pojo.dto.CompetitionReturnToSampleCheckRequest;
import com.beercompetition.pojo.vo.CompetitionDetailVO;

/**
 * 统一推进赛事生命周期并记录阶段变更。
 *
 * <p>赛事状态修改必须从该服务进入，以保证前置检查、数据修正和审计行为一致。</p>
 */
public interface CompetitionLifecycleService {

    int closeExpiredRegistrations();

    CompetitionDetailVO openRegistration(Long id);

    CompetitionDetailVO closeRegistration(Long id);

    CompetitionDetailVO prepareJudging(Long id);

    CompetitionDetailVO reopenRegistration(Long id, CompetitionReopenRegistrationRequest request);

    CompetitionDetailVO returnToSampleCheck(Long id, CompetitionReturnToSampleCheckRequest request);
}
