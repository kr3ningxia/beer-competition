<template>
  <div class="competition-detail">
    <section v-if="competition" class="detail-head">
      <button class="breadcrumb-link" type="button" @click="router.push('/admin/competitions')">
        <Back />
        比赛管理
      </button>
      <div class="head-main">
        <div class="title-block">
          <div class="title-line">
            <h1>{{ competition.name }}</h1>
          </div>
          <div class="meta-line">
            <span :class="['state-badge', statusInfo.tone]">{{ statusInfo.label }}</span>
            <span v-for="item in headerMetaItems" :key="item.key">{{ item.label }}</span>
          </div>
        </div>
        <div class="head-action-group">
          <span
            class="disabled-action-tip"
            :data-disabled-reason="stagePrimaryActionDisabledReason || null"
            :title="stagePrimaryActionDisabledReason"
          >
            <button
              class="tool-button primary"
              type="button"
              :disabled="!stagePrimaryAction.enabled"
              @click="handlePrimaryAction"
            >
              {{ stagePrimaryAction.text }}
              <Right />
            </button>
          </span>
          <details class="more-actions">
            <summary>更多</summary>
            <div class="more-actions-menu">
              <button
                v-for="action in stageSecondaryActions"
                :key="action.key"
                :class="{ danger: action.danger }"
                type="button"
                @click="action.handler"
              >
                {{ action.label }}
              </button>
            </div>
          </details>
        </div>
      </div>
    </section>

    <section v-if="competition" class="detail-shell">
      <div class="detail-tabbar">
        <nav class="detail-tabs" aria-label="比赛详情导航">
          <button
            v-for="tab in detailTabs"
            :key="tab.key"
            :class="{ active: activeTab === tab.key, disabled: !tab.enabled }"
            :aria-disabled="!tab.enabled"
            :title="tab.disabledReason || tab.label"
            type="button"
            @click="handleDetailTabChange(tab.key)"
          >
            <component :is="tab.icon" />
            {{ tab.label }}
          </button>
        </nav>
        <div v-if="tabSaveAction" class="tab-save-actions">
          <button class="tool-button primary" type="button" @click="tabSaveAction.handler">
            <Check />
            {{ tabSaveAction.label }}
          </button>
        </div>
      </div>

      <main class="tab-content">
        <section v-if="activeTab === 'overview'" class="tab-panel">
          <div class="overview-grid">
            <article class="metric-card">
              <small>开放报名准备</small>
              <strong>{{ registrationReadyCount }} / {{ registrationRequiredChecks.length }}</strong>
              <p>{{ registrationBlockText }}</p>
            </article>
            <article class="metric-card">
              <small>当前轮次</small>
              <strong>{{ currentRound?.name || '-' }}</strong>
              <p>{{ currentRoundTypeLabel }} · {{ currentRoundStatusText }}</p>
            </article>
            <article class="metric-card">
              <small>当前轮次桌数</small>
              <strong>{{ currentRoundTables.length }} 桌</strong>
              <p>{{ isFeedbackOnlyCompetition ? `${currentRoundEntryCount} 款酒` : `${currentRoundEntryCount} 款酒 · 目标 ${currentRoundTargetCount} 款` }}</p>
            </article>
            <article class="metric-card">
              <small>{{ isFeedbackOnlyCompetition ? '诊断发布' : (currentRoundIsTerminal ? '结果确认' : '晋级名单') }}</small>
              <strong>{{ isFeedbackOnlyCompetition ? (canPublishResults ? '可发布' : feedbackFinalizedCount) : (currentRoundIsTerminal ? (canPublishResults ? '可发布' : '待确认') : advancedPool.length) }}</strong>
              <p>{{ isFeedbackOnlyCompetition ? '首轮锁定后发布诊断结果' : (currentRoundIsTerminal ? '确认奖项后发布结果' : '锁定上一轮后用于创建下一轮') }}</p>
            </article>
          </div>

          <div class="two-column">
            <article class="panel-card">
              <div class="panel-heading">
                <h2>当前需要处理</h2>
                <span>{{ overviewActionItems.length }} 项</span>
              </div>
              <div class="alert-list">
                <p v-if="overviewActionItems.length === 0" class="success">
                  <CircleCheck />
                  <span>当前阶段没有阻塞项，可以继续推进</span>
                </p>
                <p v-for="item in overviewActionItems" v-else :key="item.key" :class="item.level">
                  <component :is="item.level === 'danger' ? Warning : Clock" />
                  <span>{{ item.text }}</span>
                  <button v-if="item.targetTab" class="link-action" type="button" @click="handleDetailTabChange(item.targetTab)">去处理</button>
                </p>
              </div>
            </article>

            <article class="panel-card">
              <div class="panel-heading">
                <h2>轮次重组摘要</h2>
                <span>{{ rounds.length }} 个节点</span>
              </div>
              <div class="round-path">
                <span v-for="round in rounds" :key="round.id" :class="{ active: round.id === activeRoundId }">
                  <strong>{{ round.name }}</strong>
                  <small>{{ round.tables.length }} 桌 · {{ countRoundEntries(round) }} 款</small>
                </span>
              </div>
            </article>
          </div>

          <article class="panel-card">
            <div class="panel-heading">
              <h2>阶段任务</h2>
              <span>随比赛阶段推进处理</span>
            </div>
            <div class="future-task-list">
              <button
                v-for="task in futureStageTasks"
                :key="task.key"
                :class="['future-task', task.state]"
                type="button"
                @click="handleFutureTask(task)"
              >
                <span>
                  <strong>{{ task.label }}</strong>
                  <small>{{ task.detail }}</small>
                </span>
                <em>{{ task.statusText }}</em>
              </button>
            </div>
          </article>
        </section>

        <section v-if="activeTab === 'analysis'" class="tab-panel analysis-panel">
          <CompetitionAnalyticsPanel
            :analytics="competitionAnalytics"
            :competition-name="competition.name"
            :loading="competitionAnalyticsLoading"
          />
        </section>

        <section v-if="activeTab === 'baseInfo'" class="tab-panel base-info-panel">
          <div v-if="!editable.baseInfo" class="edit-banner locked">
            <Lock />
            {{ editable.description ? '当前阶段仅允许维护赛事简介，价格和报名时间已锁定' : '当前阶段基础信息已锁定' }}
          </div>

          <article class="panel-card base-info-card">
            <div class="panel-heading">
              <div>
                <h2>基础信息</h2>
                <span>厂牌端会展示赛事简介、报名窗口和当前应付金额</span>
              </div>
            </div>
            <div class="base-info-groups">
              <label class="wide-field">
                <span>比赛名称</span>
                <input v-model.trim="baseForm.name" :disabled="!editable.baseInfo" />
              </label>

              <section class="form-subgroup">
                <h3>赛事属性</h3>
                <div class="base-form-grid">
                  <label>
                    <span>比赛日期</span>
                    <input v-model="baseForm.competitionDate" type="date" :disabled="!editable.baseInfo" />
                  </label>
                  <label>
                    <span>比赛编号</span>
                    <input v-model.trim="baseForm.code" :disabled="!editable.baseInfo" />
                  </label>
                </div>
              </section>

              <section class="form-subgroup">
                <h3>报名时间</h3>
                <div class="base-form-grid">
                  <label>
                    <span>报名开始时间</span>
                    <input v-model="baseForm.registrationStart" type="datetime-local" :disabled="!editable.baseInfo" />
                  </label>
                  <label>
                    <span>报名截止时间</span>
                    <input v-model="baseForm.registrationDeadline" type="datetime-local" :disabled="!editable.baseInfo" />
                  </label>
                </div>
              </section>

              <section class="form-subgroup">
                <h3>费用规则</h3>
                <div class="base-form-grid">
                  <label>
                    <span>普通报名费</span>
                    <input v-model.number="baseForm.entryFee" min="0" type="number" :disabled="!editable.basePrice" />
                  </label>
                  <label>
                    <span>早鸟价</span>
                    <input v-model.number="baseForm.earlyBirdFee" min="0" type="number" :disabled="!editable.basePrice" />
                  </label>
                  <label>
                    <span>早鸟截止时间</span>
                    <input v-model="baseForm.earlyBirdDeadline" type="datetime-local" :disabled="!editable.basePrice" />
                  </label>
                </div>
              </section>

              <section class="form-subgroup">
                <h3>赛事展示</h3>
                <label class="wide-field">
                  <span>赛事简介</span>
                  <textarea v-model.trim="baseForm.description" maxlength="1000" rows="4" :disabled="!editable.description" />
                </label>
                <label class="wide-field">
                  <span>参赛细则链接</span>
                  <input v-model.trim="baseForm.rulesUrl" placeholder="例如 https://mp.weixin.qq.com/s/..." :disabled="!editable.description" />
                </label>
              </section>
            </div>
          </article>

          <article class="panel-card base-info-card">
            <div class="panel-heading">
              <div>
                <h2>送样信息</h2>
              </div>
            </div>
            <div class="base-form-grid">
              <label>
                <span>送样方式</span>
                <select v-model="baseForm.deliveryMethod" :disabled="!editable.baseInfo">
                  <option value="BOTH">快递寄送 / 现场送样</option>
                  <option value="EXPRESS">仅快递寄送</option>
                  <option value="ONSITE">仅现场送样</option>
                </select>
              </label>
              <label>
                <span>地址展示</span>
                <select v-model="baseForm.logisticsVisibility" :disabled="!editable.baseInfo">
                  <option value="PAYMENT_CONFIRMED">支付成功后显示完整地址</option>
                  <option value="LOGIN_REQUIRED">登录后显示完整地址</option>
                  <option value="PUBLIC">公开显示完整地址</option>
                </select>
              </label>
              <label>
                <span>送达开始</span>
                <input v-model="baseForm.sampleArrivalStart" type="datetime-local" :disabled="!editable.baseInfo" />
              </label>
              <label>
                <span>送达截止</span>
                <input v-model="baseForm.sampleArrivalDeadline" type="datetime-local" :disabled="!editable.baseInfo" />
              </label>
              <label>
                <span>收件人</span>
                <input v-model.trim="baseForm.deliveryRecipient" :disabled="!editable.baseInfo" />
              </label>
              <label>
                <span>收件联系电话</span>
                <input v-model.trim="baseForm.deliveryPhone" :disabled="!editable.baseInfo" />
              </label>
              <label class="wide-field">
                <span>收件地址</span>
                <input v-model.trim="baseForm.deliveryAddress" :disabled="!editable.baseInfo" />
              </label>
              <label>
                <span>样品数量要求</span>
                <input v-model.trim="baseForm.sampleQuantityNote" :disabled="!editable.baseInfo" />
              </label>
              <label class="wide-field">
                <span>送样说明</span>
                <textarea v-model.trim="baseForm.deliveryNote" rows="3" :disabled="!editable.baseInfo" />
              </label>
            </div>
          </article>
        </section>

        <section v-if="activeTab === 'sponsors'" class="tab-panel sponsor-panel">
          <article class="panel-card sponsor-config-card">
            <div class="panel-heading">
              <div>
                <h2>投屏赞助商</h2>
                <span>公开看板只展示赞助等级、品牌名称和 Logo</span>
              </div>
              <button class="tool-button" type="button" @click="addSponsor">
                <Plus />
                添加赞助商
              </button>
            </div>

            <div v-if="sponsorForm.length" class="sponsor-editor-list">
              <article v-for="(sponsor, index) in sponsorForm" :key="sponsor.localId" class="sponsor-editor-row">
                <div class="sponsor-logo-box">
                  <img v-if="sponsor.logoUrl" :src="sponsor.logoUrl" :alt="sponsor.sponsorName || '赞助商 Logo'" />
                  <span v-else>{{ sponsor.sponsorName?.slice(0, 2) || 'Logo' }}</span>
                </div>
                <div class="sponsor-fields">
                  <label>
                    <span>赞助等级</span>
                    <input v-model.trim="sponsor.tierLabel" placeholder="例如 主办方、首席赞助、行业赞助" />
                  </label>
                  <label>
                    <span>品牌名称</span>
                    <input v-model.trim="sponsor.sponsorName" placeholder="公开展示名称" />
                  </label>
                  <label>
                    <span>排序</span>
                    <input v-model.number="sponsor.sortOrder" min="0" type="number" />
                  </label>
                </div>
                <div class="sponsor-controls">
                  <label class="inline-check">
                    <input v-model="sponsor.featured" type="checkbox" />
                    重点展示
                  </label>
                  <label class="inline-check">
                    <input v-model="sponsor.enabled" type="checkbox" />
                    启用
                  </label>
                  <input
                    :ref="(el) => setSponsorLogoInputRef(el, sponsor.localId)"
                    accept="image/jpeg,image/png,image/webp"
                    class="hidden-file-input"
                    type="file"
                    @change="handleSponsorLogoChange($event, sponsor)"
                  />
                  <button class="tool-button" type="button" :disabled="sponsor.uploading" @click="triggerSponsorLogoInput(sponsor.localId)">
                    {{ sponsor.uploading ? '上传中' : '上传 Logo' }}
                  </button>
                  <button class="icon-button danger" type="button" @click="removeSponsor(index)">
                    <Delete />
                  </button>
                </div>
              </article>
            </div>
            <div v-else class="empty-state sponsor-empty">
              <strong>还没有赞助商配置</strong>
              <span>添加后会显示在公开投屏看板顶部区域</span>
            </div>
          </article>

          <div v-if="sponsorLogoCrop.open" class="sponsor-logo-crop-mask" @click.self="cancelSponsorLogoCrop">
            <section class="sponsor-logo-crop-dialog" role="dialog" aria-modal="true" aria-label="调整 Logo">
              <header>
                <div>
                  <h3>调整 Logo</h3>
                  <span>拖动图片调整位置，缩放后确认上传</span>
                </div>
                <button class="icon-button" type="button" @click="cancelSponsorLogoCrop">
                  <Close />
                </button>
              </header>

              <div
                class="sponsor-logo-crop-stage"
                @pointerdown="startSponsorLogoDrag"
                @pointermove="moveSponsorLogoDrag"
                @pointerup="endSponsorLogoDrag"
                @pointercancel="endSponsorLogoDrag"
                @pointerleave="endSponsorLogoDrag"
                @wheel.prevent="zoomSponsorLogoCrop"
              >
                <img
                  v-if="sponsorLogoCrop.previewUrl"
                  :src="sponsorLogoCrop.previewUrl"
                  alt="Logo 预览"
                  draggable="false"
                  :style="sponsorLogoCropImageStyle"
                  @load="handleSponsorLogoPreviewLoad"
                />
                <div class="sponsor-logo-crop-frame" aria-hidden="true" />
              </div>

              <div class="sponsor-logo-crop-tools">
                <label>
                  <span>缩放</span>
                  <input v-model.number="sponsorLogoCrop.scale" min="1" max="3" step="0.01" type="range" />
                </label>
                <button class="tool-button" type="button" @click="resetSponsorLogoCrop">重置</button>
                <button class="tool-button primary" type="button" :disabled="sponsorLogoCrop.uploading" @click="confirmSponsorLogoCrop">
                  {{ sponsorLogoCrop.uploading ? '上传中' : '确认上传' }}
                </button>
              </div>
            </section>
          </div>
        </section>

        <section v-if="activeTab === 'entryConfig'" class="tab-panel entry-config-panel">
          <div v-if="!editable.entryStructure" class="edit-banner locked">
            <Lock />
            比赛已进入后续流程，报名结构已锁定，投递组别和补充字段不再直接编辑
          </div>

          <article class="panel-card category-card">
            <div class="panel-heading">
              <div>
                <h2>投递组别</h2>
              </div>
              <div v-if="editable.entryStructure" class="panel-actions">
                <button class="tool-button" type="button" @click="syncCategoryFormFromStyleLibrary">
                  <CircleCheck />
                  同步当前风格库
                </button>
                <button class="tool-button" type="button" @click="categoryForm.push('')">
                  <Plus />
                  添加组别
                </button>
              </div>
            </div>
            <div v-if="editable.entryStructure" class="category-editor-list">
              <label v-for="(_, index) in categoryForm" :key="`category-${index}`">
                <input v-model.trim="categoryForm[index]" placeholder="例如 IPA" />
                <button class="icon-button" type="button" @click="removeItem(categoryForm, index)">
                  <Delete />
                </button>
              </label>
            </div>
            <div v-else class="pill-list">
              <span v-for="category in categoryForm" :key="category">{{ category }}</span>
              <p v-if="categoryForm.length === 0" class="empty-line">当前没有投递组别数据，请管理员修正</p>
            </div>
          </article>

          <article class="panel-card library-card">
            <div class="panel-heading library-heading">
              <div>
                <h2>报名风格</h2>
              </div>
            </div>
            <div class="library-config-block">
              <div class="library-control-row">
                <select v-model="selectedStyleLibraryVersion" :disabled="!editable.styleLibrary" aria-label="当前风格库">
                  <option v-for="library in styleLibraryOptions" :key="library.value" :value="library.value">
                    {{ library.label }}
                  </option>
                </select>
                <button class="tool-button" type="button" @click="openSelectedStyleLibrary">
                  查看风格库
                </button>
              </div>
            </div>
            <div class="style-distribution">
              <div class="style-distribution-head">
                <strong>分类分布</strong>
              </div>
              <div
                ref="styleDistributionListRef"
                :class="['style-distribution-list', { 'has-more': styleDistributionHasOverflow }]"
              >
                <span v-for="category in styleCategorySummary" :key="category.name">
                  <b>{{ category.name }}</b>
                  <small>{{ category.count }} 个风格</small>
                </span>
                <button
                  v-if="styleDistributionHasOverflow"
                  class="style-distribution-more"
                  type="button"
                  @click="openSelectedStyleLibrary"
                >
                  ...更多
                </button>
                <p v-if="styleCategorySummary.length === 0" class="empty-line">当前风格库还没有可用分类</p>
              </div>
            </div>
          </article>

          <article class="panel-card field-config-card">
            <div class="panel-heading">
              <div>
                <h2>报名补充信息</h2>
              </div>
              <button v-if="editable.entryStructure" class="tool-button" type="button" @click="addEntryField">
                <Plus />
                添加字段
              </button>
            </div>
            <div class="data-table field-table">
              <div class="table-head">
                <span>字段名称</span>
                <span>类型</span>
                <span>提示文案</span>
                <span>必填</span>
                <span>评审可见</span>
                <span></span>
              </div>
              <div v-for="(field, index) in entryFieldForm" :key="field.localId" class="table-row">
                <input v-model.trim="field.fieldLabel" :disabled="!editable.entryStructure" />
                <select v-model="field.fieldType" :disabled="!editable.entryStructure">
                  <option value="text">短文本</option>
                  <option value="textarea">长文本</option>
                  <option value="number">数字</option>
                  <option value="select">选项</option>
                  <option value="multi_select">多选</option>
                </select>
                <input v-model.trim="field.helpText" :disabled="!editable.entryStructure" placeholder="给厂牌看的填写说明" />
                <label class="check-control"><input v-model="field.required" type="checkbox" :disabled="!editable.entryStructure" /> 必填</label>
                <label class="check-control"><input v-model="field.visibleToJudges" type="checkbox" :disabled="!editable.entryStructure" /> 评审可见</label>
                <button class="icon-button" type="button" :disabled="!editable.entryStructure" @click="removeItem(entryFieldForm, index)">
                  <Delete />
                </button>
              </div>
              <p v-if="entryFieldForm.length === 0" class="empty-line">当前没有报名补充字段</p>
            </div>
          </article>
        </section>

        <section v-if="activeTab === 'entries'" class="tab-panel">
          <div class="overview-grid">
            <article class="metric-card"><small>全部</small><strong>{{ competition.entriesSummary.total }}</strong><p>已报名酒款</p></article>
            <article class="metric-card"><small>待支付</small><strong>{{ competition.entriesSummary.pendingPayment }}</strong><p>需跟进支付</p></article>
            <article class="metric-card"><small>已入库</small><strong>{{ competition.entriesSummary.stored }}</strong><p>现场已确认</p></article>
            <article class="metric-card"><small>{{ isFeedbackOnlyCompetition ? '评审轨迹' : '晋级轨迹' }}</small><strong>{{ roundEntryPool.length }}</strong><p>真实轮次分配</p></article>
          </div>
          <article class="panel-card">
            <div class="panel-heading">
              <div>
                <h2>参赛酒款</h2>
                <span>短编号、准备状态和轮次轨迹</span>
              </div>
              <button class="tool-button" type="button" @click="openAdminEntries">
                打开酒款管理
                <Right />
              </button>
            </div>
            <div class="data-table entries-table">
              <div class="table-head">
                <span>参赛酒款</span>
                <span>编号</span>
                <span>风格</span>
                <span>运单号</span>
                <span>进度</span>
                <span>操作</span>
              </div>
              <div
                v-for="entry in roundEntryPool"
                :key="entry.uuid"
                :class="['table-row', { refunded: isRefundedEntry(entry), 'refund-priority': hasRefundPriority(entry) }]"
              >
                <div class="entry-main">
                  <strong>{{ entry.name || '-' }}</strong>
                  <small>{{ entry.breweryCompanyName || '未关联厂牌' }}</small>
                </div>
                <div class="entry-code-cell">
                  <strong>{{ entry.shortCode }}</strong>
                  <small>{{ entry.uuid }}</small>
                </div>
                <div class="entry-style-cell">
                  <strong>{{ entry.categoryName }}</strong>
                  <small>{{ entry.style }}</small>
                </div>
                <div class="entry-tracking-cell">
                  <strong>{{ entry.trackingNo || '-' }}</strong>
                  <small>{{ entry.carrier || '未填写快递公司' }}</small>
                </div>
                <div class="entry-followup">
                  <span :class="['entry-state-pill', entryLatestProgress(entry).tone]">{{ entryLatestProgress(entry).label }}</span>
                </div>
                <div class="entry-actions">
                  <button class="mini-action" type="button" @click="openAdminEntry(entry)">查看</button>
                  <button v-if="!isRefundedEntry(entry)" class="mini-action" type="button" :disabled="!entry.canMarkStored" @click="markEntryStoredAction(entry)">确认入库</button>
                </div>
              </div>
            </div>
          </article>
        </section>

        <section v-if="activeTab === 'judges'" class="tab-panel">
          <div v-if="preplanningNotice" class="edit-banner preplanning-banner">
            <span>{{ preplanningNotice }}</span>
          </div>
          <TableAllocationWorkbench
            :allocation-mode="allocationMode"
            :rounds="rounds"
            :active-round-id="activeRoundId"
            :current-round="currentRound"
            :current-round-tables="currentRoundTables"
            :selected-round-table="selectedRoundTable"
            :selected-round-table-id="selectedRoundTableId"
            :editable-judges="canEditBaseJudgeTables"
            :judge-table-form="judgeTableForm"
            :role-options="roleOptions"
            :role-filters="roleFilters"
            :judge-keyword="judgeKeyword"
            :judge-role-filter="judgeRoleFilter"
            :selected-table-local-id="selectedTableLocalId"
            :selected-role="selectedRole"
            :judge-metrics="judgeMetrics"
            :judge-filter-counts="judgeFilterCounts"
            :validation-issues="validationIssues"
            :filtered-judge-pool="filteredJudgePool"
            :assignments-for-table="assignmentsForTable"
            :table-validation-issues="tableValidationIssues"
            :filtered-round-pool="filteredRoundPool"
            :current-pool-entries="currentPoolEntries"
            :round-category-filters="roundCategoryFilters"
            :round-category-filter="roundCategoryFilter"
            :round-keyword="roundKeyword"
            :selected-entry-uuids="selectedEntryUuids"
            :round-validation-issues="roundValidationIssues"
            :publish-disabled-reason="roundPublishDisabledReason"
            :can-publish="canPublishCurrentRound"
            :competition-type="competition?.competitionType || 'AWARD'"
            :get-judge="getJudge"
            :get-judge-initial="getJudgeInitial"
            :get-judge-assignment-summary="getJudgeAssignmentSummary"
            :is-assigned="isAssigned"
            :is-judge-active="isJudgeActive"
            :get-round-entry-assignment="getRoundEntryAssignment"
            :get-round-table-issues="getRoundTableIssues"
            :get-round-table-conflict-warnings="getRoundTableConflictWarnings"
            @update:allocation-mode="handleAllocationModeChange"
            @select-round="selectRound"
            @update:judge-keyword="judgeKeyword = $event"
            @update:judge-role-filter="judgeRoleFilter = $event"
            @update:round-keyword="roundKeyword = $event"
            @update:round-category-filter="roundCategoryFilter = $event"
            @select-assignment-target="selectAssignmentTarget"
            @add-judge-to-target="addJudgeToTarget"
            @add-judge-table="addJudgeTable"
            @remove-judge-table="removeJudgeTable"
            @remove-assignment="removeAssignment"
            @save-judge-draft="saveJudgeDraft"
            @start-judge-drag="startJudgeDrag"
            @start-assignment-drag="startAssignmentDrag"
            @drop-on-role="dropOnRole"
            @clear-drag="clearDrag"
            @toggle-entry-selection="toggleEntrySelection"
            @add-selected-to-table="addSelectedEntriesToTable"
            @add-entry-to-selected-table="addEntryToSelectedRoundTable"
            @start-entry-drag="startEntryDrag"
            @open-entry-auto-assign="openEntryAutoAssignDialog"
            @select-round-table="selectedRoundTableId = $event"
            @drop-entry-on-round-table="dropEntryOnRoundTable"
            @remove-entry-from-round-table="removeEntryFromRoundTable"
            @update-table-captain="updateRoundTableCaptain"
            @update-round-table-name="updateRoundTableName"
            @update-table-scope="updateRoundTableScope"
            @update-table-target="updateRoundTableTarget"
            @set-round-captain="setRoundCaptainForSelectedTable"
            @update-round-target-mode="updateRoundTargetMode"
            @add-round-table="addRoundTable"
            @remove-round-table="removeRoundTable"
            @add-round-participant="addRoundParticipantToSelectedTable"
            @remove-round-participant="removeRoundParticipant"
            @drop-round-judge="dropRoundJudge"
            @sync-round-candidates="syncCurrentRoundCandidates"
            @publish-current-round="publishCurrentRound"
          />
        </section>

        <section v-if="activeTab === 'rounds'" class="tab-panel rounds-panel">
          <div v-if="preplanningNotice" class="edit-banner preplanning-banner">
            <span>{{ preplanningNotice }}</span>
          </div>
          <section :class="['round-planning-shell', { 'feedback-only-rounds': isFeedbackOnlyCompetition }]">
            <aside v-if="!isFeedbackOnlyCompetition" class="round-pyramid-panel" aria-label="晋级路径">
              <header class="round-pyramid-head">
                <h2>晋级路径</h2>
              </header>

              <div class="round-pyramid">
                <article
                  v-for="node in roundPyramidNodes"
                  :key="node.key"
                  :class="['pyramid-node', node.state, { active: node.active, actionable: node.actionable }]"
                  :style="{ '--node-width': `${node.width}%` }"
                  :role="node.actionable ? 'button' : undefined"
                  :tabindex="node.actionable ? 0 : undefined"
                  :title="node.hint"
                  @click="handleRoundPyramidNode(node)"
                  @keydown.enter.prevent="handleRoundPyramidNode(node)"
                  @keydown.space.prevent="handleRoundPyramidNode(node)"
                >
                  <span v-if="node.tableChips.length" class="pyramid-table-row">
                    <button
                      v-for="table in node.tableChips"
                      :key="`${node.key}-${table.id}`"
                      :class="{ active: table.active }"
                      type="button"
                      :title="table.label"
                      @click.stop="selectPyramidTable(node, table)"
                    >
                      {{ table.label }}
                      <span class="pyramid-table-popover">
                        <strong>{{ table.label }}</strong>
                        <small v-for="line in table.previewLines" :key="`${table.id}-${line}`">{{ line }}</small>
                      </span>
                    </button>
                    <i v-if="node.extraTableCount">+{{ node.extraTableCount }}</i>
                  </span>
                  <button
                    v-else-if="node.actionable"
                    class="pyramid-placeholder-mark"
                    type="button"
                    @click.stop="handleRoundPyramidNode(node)"
                  >
                    {{ node.placeholderText }}
                  </button>
                  <span v-else class="pyramid-placeholder-mark">{{ node.placeholderText }}</span>

                  <span class="pyramid-layer-caption">
                    <strong>{{ node.label }}</strong>
                    <em>{{ node.statusText }}</em>
                  </span>
                  <b v-if="node.summary">{{ node.summary }}</b>
                </article>
              </div>
            </aside>

            <section class="round-workspace">
              <article class="round-current-card">
                <div class="round-current-main">
                  <small>{{ currentRound?.name || '未创建轮次' }} · {{ currentRoundTypeLabel }} · {{ currentRoundStatusText }}</small>
                  <h2>{{ roundReadinessTitle }}</h2>
                  <p v-if="roundReadinessDetail">{{ roundReadinessDetail }}</p>
                </div>

                <div class="round-console-metrics">
                  <span v-for="metric in currentRoundMetrics" :key="metric.label">
                    {{ metric.label }}
                    <strong>{{ metric.value }}</strong>
                  </span>
                </div>

                <div class="round-primary-actions">
                  <label v-if="currentRound?.type === 'RANKING' && currentRound?.status === 'DRAFT'" class="round-target-control">
                    <span>轮次目标</span>
                    <select :value="currentRoundTargetMode" @change="updateRoundTargetMode($event.target.value)">
                      <option v-for="option in currentRoundTargetModeOptions" :key="option.value" :value="option.value">
                        {{ option.label }}
                      </option>
                    </select>
                  </label>
                  <button
                    v-if="currentRound?.type === 'RANKING' && currentRound?.status === 'DRAFT' && currentRound?.sourceLocked && !currentRound?.candidatesSynced"
                    class="tool-button primary"
                    type="button"
                    @click="syncCurrentRoundCandidates"
                  >
                    同步候选酒款
                  </button>
                  <button
                    v-else-if="currentRound?.status === 'DRAFT'"
                    :class="['tool-button', 'primary', { blocked: !canPublishCurrentRound }]"
                    type="button"
                    :aria-disabled="!canPublishCurrentRound"
                    @click="publishCurrentRound"
                  >
                    {{ currentRound?.type === 'SCORE' ? '发布给评审' : '发布给桌长和参与评审' }}
                  </button>
                  <button
                    v-if="currentRound?.roundNo === 1 && currentRound?.status === 'PUBLISHED'"
                    :class="['tool-button', { primary: firstRoundCompletionStatus.ready }]"
                    type="button"
                    :disabled="!firstRoundCompletionStatus.ready"
                    :title="firstRoundCompletionStatus.hint"
                    @click="completeFirstRoundAction"
                  >
                    确认首轮完成
                  </button>
                  <button v-if="currentRound?.status === 'SUBMITTED'" class="tool-button primary" type="button" @click="lockCurrentRound">
                    确认锁定
                  </button>
                  <button v-else-if="canLockCurrentDraftSourceRound" class="tool-button primary" type="button" @click="lockCurrentDraftSourceRound">
                    确认锁定上一轮
                  </button>
                  <button v-if="currentRoundIsTerminal && currentRound?.status === 'LOCKED'" class="tool-button primary" type="button" @click="goToResults">
                    去确认结果
                  </button>
                  <button v-if="currentRound?.type === 'RANKING' && currentRound?.status === 'DRAFT'" class="tool-button danger" type="button" @click="deleteCurrentDraftRound">
                    删除草稿轮
                  </button>
                  <button v-if="canCreateNextRound" class="tool-button primary" type="button" @click="openCreateRoundDialog">
                    {{ createNextRoundButtonText }}
                  </button>
                  <button class="tool-button" type="button" @click="openLiveDashboard">
                    打开投屏看板
                  </button>
                </div>
              </article>

              <section ref="roundProgressSection" class="round-table-overview">
                <header>
                  <div>
                    <small>当前轮次</small>
                    <h3>桌次进度</h3>
                  </div>
                  <span>{{ currentRoundTables.length }} 桌</span>
                </header>

                <div class="round-table-list">
                  <div
                    v-for="table in currentRoundTableSummaries"
                    :key="`round-summary-${table.id}`"
                    :class="['round-table-row', currentRound?.type === 'SCORE' ? 'score-row' : 'ranking-row', table.tone, { active: selectedRoundTableId === table.id }]"
                    @click="selectedRoundTableId = table.id"
                    @keydown.enter="selectedRoundTableId = table.id"
                    @keydown.space.prevent="selectedRoundTableId = table.id"
                    role="button"
                    tabindex="0"
                  >
                    <strong>{{ table.name }}</strong>
                    <span>{{ table.entryCount }} 款</span>
                    <span>桌长 {{ table.captainName }}</span>
                    <span>{{ table.primaryProgressLabel }} {{ table.primaryProgress }}</span>
                    <span>{{ table.secondaryProgressLabel }} {{ table.secondaryProgress }}</span>
                    <span v-if="currentRound?.type === 'SCORE'">确认 {{ table.confirmationProgress }}</span>
                    <span>{{ table.targetLabel }} {{ table.targetDisplay }}</span>
                    <em>{{ table.statusText }}</em>
                    <button
                      class="round-table-detail-button"
                      type="button"
                      :title="currentRound?.type === 'RANKING' ? '查看排序详情' : '查看评分详情'"
                      @click.stop="openRoundTableScoreDetail(table.id)"
                    >
                      <Search />
                      详情
                    </button>
                  </div>
                  <p v-if="!currentRoundTables.length" class="empty-line">还没有轮次桌，先在分桌分配中安排首轮</p>
                </div>
              </section>

              <article :class="['round-task-hint', roundTodoHint.tone]">
                <div>
                  <strong>{{ roundTodoHint.title }}</strong>
                  <span>{{ roundTodoHint.detail }}</span>
                </div>
                <button v-if="roundTodoHint.action === 'createNextRound'" class="tool-button primary" type="button" @click="openCreateRoundDialog">
                  {{ createNextRoundButtonText }}
                </button>
                <button v-else-if="roundTodoHint.action === 'goToRoundAllocation'" class="tool-button primary" type="button" @click="goToRoundAllocation">
                  去分桌分配
                </button>
                <button v-else-if="roundTodoHint.action === 'focusRoundProgress'" class="tool-button" type="button" @click="focusRoundProgress">
                  查看桌次进度
                </button>
                <button v-else-if="roundTodoHint.action === 'lockSourceRound'" class="tool-button primary" type="button" @click="lockCurrentDraftSourceRound">
                  确认锁定上一轮
                </button>
                <button v-else-if="roundTodoHint.action === 'syncCandidates'" class="tool-button primary" type="button" @click="syncCurrentRoundCandidates">
                  同步候选酒款
                </button>
                <button v-else-if="roundTodoHint.action === 'goToResults'" class="tool-button primary" type="button" @click="goToResults">
                  去确认结果
                </button>
              </article>
            </section>
          </section>
        </section>

        <section v-if="activeTab === 'score'" class="tab-panel score-config-panel">
          <div class="score-panels">
            <article v-for="config in scoreConfigForm" :key="config.role" class="panel-card score-config-card">
              <div class="panel-heading">
                <div>
                  <h2>{{ roleLabels[config.role] }}</h2>
                </div>
                <div class="score-card-actions">
                  <span :class="['score-total-pill', { success: getScoreTotal(config) === 50, danger: getScoreTotal(config) !== 50 }]">
                    {{ getScoreTotal(config) }} / 50
                  </span>
                  <button
                    v-if="editable.scoreConfigs && config.role === 'CROSS'"
                    class="tool-button"
                    type="button"
                    :disabled="config.dimensions.length >= 3"
                    @click="addCrossScoreDimension(config)"
                  >
                    <Plus />
                    添加维度
                  </button>
                </div>
              </div>
              <p v-if="getScoreTotal(config) !== 50" class="score-warning">
                当前总分 {{ getScoreTotal(config) }}，必须调整为 50 后才能保存
              </p>
              <label class="score-comment-limit">
                <span>备注合计字数下限：</span>
                <span class="compact-number-field">
                  <input v-if="editable.scoreConfigs" v-model.number="config.minCommentLength" min="0" type="number" />
                  <strong v-else>{{ config.minCommentLength || 0 }}</strong>
                  <em>字</em>
                </span>
              </label>
              <div class="dimension-list">
                <div class="dimension-head">
                  <span>维度</span>
                  <span>分值</span>
                  <span>备注提示</span>
                  <span></span>
                </div>
                <div v-for="(dimension, index) in config.dimensions" :key="dimension.localId" class="dimension-row">
                  <input v-if="editable.scoreConfigs && config.role === 'CROSS'" v-model.trim="dimension.label" placeholder="维度名称" />
                  <span v-else>{{ dimension.label }}</span>
                  <input v-if="editable.scoreConfigs && config.role === 'CROSS'" v-model.number="dimension.maxScore" min="1" type="number" />
                  <strong v-else>{{ dimension.maxScore }}</strong>
                  <input v-if="editable.scoreConfigs" v-model.trim="dimension.notePrompt" placeholder="这个维度的备注提示" />
                  <span v-else>{{ dimension.notePrompt || '-' }}</span>
                  <button
                    v-if="editable.scoreConfigs && config.role === 'CROSS'"
                    class="icon-button"
                    type="button"
                    :disabled="config.dimensions.length <= 2"
                    @click="removeScoreDimension(config, index)"
                  >
                    <Delete />
                  </button>
                  <span v-else class="dimension-action-placeholder"></span>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section v-if="activeTab === 'feedback'" class="tab-panel feedback-review-panel">
          <div class="feedback-review-shell">
            <div class="feedback-filter-bar">
              <label class="feedback-search-field">
                <Search />
                <input v-model.trim="feedbackFilters.keyword" placeholder="搜索匿名编号 / 短编号 / 风格" type="search" />
              </label>
              <label class="feedback-select-field">
                <span>评审桌</span>
                <select v-model="feedbackFilters.tableName">
                  <option v-for="option in feedbackTableOptions" :key="option" :value="option">{{ option }}</option>
                </select>
              </label>
              <label class="feedback-select-field">
                <span>投递组别</span>
                <select v-model="feedbackFilters.categoryName">
                  <option v-for="option in feedbackCategoryOptions" :key="option" :value="option">{{ option }}</option>
                </select>
              </label>
              <label class="feedback-select-field">
                <span>评审类型</span>
                <select v-model="feedbackFilters.judgeType">
                  <option v-for="option in feedbackJudgeTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                </select>
              </label>
              <div class="feedback-status-segments">
                <button
                  v-for="option in feedbackStatusOptions"
                  :key="option.value"
                  type="button"
                  :class="{ active: feedbackFilters.status === option.value }"
                  @click="feedbackFilters.status = option.value"
                >
                  {{ option.label }}
                </button>
              </div>
            </div>

            <div class="feedback-split-layout">
              <section class="feedback-entry-list">
                <div class="feedback-entry-scroll">
                  <div v-if="feedbackReviewLoading" class="feedback-empty-state">正在加载反馈数据</div>
                  <div v-else-if="!feedbackFilteredEntries.length" class="feedback-empty-state">没有符合条件的酒款</div>
                  <template v-else>
                    <button
                      v-for="entry in feedbackFilteredEntries"
                      :key="feedbackEntryKey(entry)"
                      type="button"
                      :class="['feedback-entry-row', { active: feedbackEntryKey(entry) === feedbackEntryKey(selectedFeedbackEntry) }]"
                      @click="selectedFeedbackEntryKey = feedbackEntryKey(entry)"
                    >
                      <span>
                        <strong>{{ formatFeedbackEntryTitle(entry) }}</strong>
                        <small>{{ [entry.tableName, entry.categoryName, entry.style].filter(Boolean).join(' · ') }}</small>
                      </span>
                      <em>
                        <strong>{{ entry.consensusScore ?? '—' }}</strong>
                        <small :class="{ pending: entry.personalSubmitted < entry.personalTotal }">{{ entry.personalSubmitted }}/{{ entry.personalTotal }}</small>
                      </em>
                    </button>
                  </template>
                </div>
                <footer>首轮共 {{ feedbackFilteredEntries.length }} 款（合计 {{ feedbackReviewEntries.length }} 款）</footer>
              </section>

              <section class="feedback-detail-panel">
                <template v-if="selectedFeedbackEntry">
                  <header class="feedback-detail-head">
                    <strong>{{ formatFeedbackEntryTitle(selectedFeedbackEntry) }}</strong>
                    <small>{{ [selectedFeedbackEntry.roundName || '首轮', selectedFeedbackEntry.tableName, selectedFeedbackEntry.categoryName, selectedFeedbackEntry.style].filter(Boolean).join(' · ') }}</small>
                  </header>

                  <div class="feedback-detail-body">
                    <section class="feedback-block">
                      <div class="feedback-block-title">
                        <h2>桌长最终意见</h2>
                        <button
                          v-if="selectedFeedbackEntry.captainOpinion?.editable"
                          class="feedback-edit-button"
                          type="button"
                          @click="openFeedbackCommentEditor('captain', selectedFeedbackEntry)"
                        >
                          <EditPen />
                          <span>编辑</span>
                        </button>
                      </div>
                      <article v-if="selectedFeedbackEntry.captainOpinion?.submitted" class="captain-opinion-card">
                          <div class="captain-opinion-meta">
                            <span>桌长 <strong>{{ selectedFeedbackEntry.captainOpinion.captainName || '未知桌长' }}</strong></span>
                            <span>共识分 <strong class="score-highlight">{{ selectedFeedbackEntry.captainOpinion.consensusScore }}</strong> / {{ selectedFeedbackEntry.captainOpinion.maxConsensus || 50 }}</span>
                            <em v-if="!isFeedbackOnlyCompetition" :class="{ advanced: selectedFeedbackEntry.captainOpinion.advanced }">{{ selectedFeedbackEntry.captainOpinion.advanced ? '已晋级' : '未晋级' }}</em>
                            <small>提交于 {{ formatFeedbackTime(selectedFeedbackEntry.captainOpinion.submittedAt) }}</small>
                            <small>评语 {{ formatCommentCount(selectedFeedbackEntry.captainOpinion.commentCharCount) }}</small>
                          </div>
                        <p>{{ selectedFeedbackEntry.captainOpinion.comments || '暂无综合评语' }}</p>
                      </article>
                      <article v-else class="captain-missing-card">
                        <Warning />
                        <span>桌长尚未提交最终意见</span>
                      </article>
                    </section>

                    <section class="feedback-block">
                      <div class="feedback-block-title">
                        <h2>评审原始评分</h2>
                        <span>{{ selectedFeedbackEntry.personalSubmitted }} / {{ selectedFeedbackEntry.personalTotal }} 已提交</span>
                      </div>
                      <div class="judge-score-list">
                        <article
                          v-for="judge in selectedFeedbackEntry.judges"
                          :key="`${selectedFeedbackEntry.beerUuid}-${judge.judgePublicId || judge.judgeName}-${judge.role}`"
                          :class="['judge-score-card', { missing: !judge.scored }]"
                        >
                          <header>
                            <span>
                              <strong>{{ judge.judgeName }}</strong>
                              <small :class="{ captain: judge.role === 'CAPTAIN' }">{{ judge.roleLabel || feedbackRoleLabel(judge.role) }}</small>
                              <em v-if="feedbackJudgeAnomalyLabel(judge.anomaly)" :class="['judge-anomaly', judge.anomaly]">{{ feedbackJudgeAnomalyLabel(judge.anomaly) }}</em>
                            </span>
                            <span v-if="judge.scored" class="judge-score-meta">
                              <small>提交于 {{ formatFeedbackTime(judge.submittedAt) }}</small>
                              <small v-if="judge.durationSeconds">用时 {{ formatJudgeDuration(judge.durationSeconds) }}</small>
                              <small>评语 {{ formatCommentCount(judge.commentCharCount) }}</small>
                              <strong>总分 {{ judge.totalScore }} / {{ judge.maxTotal || 50 }}</strong>
                              <button
                                v-if="judge.editable"
                                class="feedback-edit-button compact"
                                type="button"
                                @click="openFeedbackCommentEditor('judge', selectedFeedbackEntry, judge)"
                              >
                                <EditPen />
                                <span>编辑</span>
                              </button>
                            </span>
                          </header>
                          <p v-if="!judge.scored" class="judge-missing-text">该评审尚未提交个人评分</p>
                          <div v-else class="judge-dimension-list">
                            <div v-for="dimension in judge.dimensions" :key="dimension.key || dimension.label" class="judge-dimension-row">
                              <span>{{ dimension.label || dimension.key }}</span>
                              <strong>{{ dimension.score ?? '-' }} / {{ dimension.maxScore ?? '-' }}</strong>
                              <p v-if="dimension.note">{{ dimension.note }}</p>
                            </div>
                          </div>
                        </article>
                      </div>
                    </section>

                    <section v-if="selectedFeedbackIssues.length" class="feedback-block">
                      <h2 class="feedback-issue-title">复核提示</h2>
                      <ul class="feedback-issue-list">
                        <li v-for="issue in selectedFeedbackIssues" :key="issue.message" :class="issue.severity">
                          <Warning />
                          <span>{{ issue.message }}</span>
                        </li>
                      </ul>
                    </section>
                  </div>
                </template>
                <div v-else class="feedback-empty-state">选择左侧酒款查看反馈详情</div>
              </section>
            </div>
          </div>
        </section>

        <section v-if="activeTab === 'results'" class="tab-panel">
          <div class="results-workbench">
            <article v-if="!isFeedbackOnlyCompetition" class="panel-card award-table-card">
              <div class="panel-heading">
                <h2>获奖名单</h2>
                <span>{{ awardDrafts.length }} 项</span>
              </div>
              <div v-if="awardDrafts.length && !championAwards.length" class="award-missing-banner">
                <Warning />
                <span>缺少全场总冠军，请锁定决赛轮后重新生成奖项</span>
              </div>
              <div v-if="awardRows.length" class="award-result-table">
                <div class="award-result-row table-head">
                  <span>奖项</span>
                  <span>组别</span>
                  <span>获奖酒款</span>
                  <span>来源路径</span>
                  <span>状态</span>
                  <span>奖状</span>
                  <span>奖状上传</span>
                </div>
                <div
                  v-for="award in awardRows"
                  :key="`${award.awardType}-${award.categoryId || 'all'}-${award.rankNo}`"
                  :class="['award-result-row', { champion: award.awardType === 'CHAMPION' }]"
                >
                  <div class="award-name-cell">
                    <strong>{{ award.awardName || '奖项' }}</strong>
                    <small v-if="award.awardType === 'CHAMPION'">全场</small>
                  </div>
                  <span>{{ formatAwardScope(award) }}</span>
                  <el-select
                    v-model="award.beerEntryId"
                    class="award-select"
                    clearable
                    filterable
                    teleported
                    popper-class="award-select-popper"
                    :disabled="!canEditAwardSelection(award)"
                  >
                    <el-option
                      v-if="award.awardType !== 'CHAMPION'"
                      label="不设置此奖项"
                      :value="null"
                    />
                    <el-option
                      v-for="entry in awardEntryOptions(award)"
                      :key="entry.id"
                      :label="formatAwardEntryLabel(entry)"
                      :value="entry.id"
                    />
                  </el-select>
                  <span class="award-path-cell" :title="getAwardPath(award)">{{ getAwardPath(award) }}</span>
                  <em :class="['award-status-pill', award.status]">{{ formatAwardStatus(award) }}</em>
                  <div class="award-certificate-cell">
                    <strong :class="{ uploaded: award.certificateUploaded }">
                      {{ award.certificateUploaded ? '已上传' : '未上传' }}
                    </strong>
                    <small v-if="award.certificateFilename" :title="award.certificateFilename">{{ award.certificateFilename }}</small>
                  </div>
                  <div class="award-row-actions">
                    <button
                      v-if="award.certificateUploaded"
                      type="button"
                      :disabled="isCertificateActionBusy(award)"
                      @click="previewAwardCertificate(award)"
                    >
                      预览
                    </button>
                    <button
                      type="button"
                      :disabled="isCertificateActionBusy(award) || !canUploadAwardCertificate(award)"
                      class="award-upload-icon-button"
                      :title="canUploadAwardCertificate(award) ? (award.certificateUploaded ? '更换奖状' : '上传奖状') : '确认奖项后上传奖状'"
                      :aria-label="award.certificateUploaded ? '更换奖状' : '上传奖状'"
                      @click="chooseAwardCertificate(award)"
                    >
                      +
                    </button>
                    <button
                      v-if="award.certificateUploaded"
                      type="button"
                      :disabled="isCertificateActionBusy(award)"
                      @click="deleteAwardCertificateAction(award)"
                    >
                      删除
                    </button>
                  </div>
                </div>
              </div>
              <p v-else class="empty-line">锁定决赛轮后生成奖项</p>
              <input ref="awardCertificateInput" class="file-input" type="file" accept="application/pdf,.pdf" @change="handleAwardCertificateSelected" />
            </article>

            <article v-else class="panel-card result-check-card feedback-result-card">
              <div class="panel-heading">
                <h2>诊断结果</h2>
                <span>{{ feedbackFinalizedCount }} / {{ feedbackEntryCount }} 款</span>
              </div>
              <div class="diagnosis-summary">
                <strong>{{ feedbackPublishSummary }}</strong>
              </div>
            </article>

            <article class="panel-card result-check-card">
              <div class="panel-heading">
                <h2>发布前检查</h2>
                <span>{{ resultChecks.filter((item) => item.done).length }} / {{ resultChecks.length }}</span>
              </div>
              <div class="check-list">
                <div v-for="check in resultChecks" :key="check.label" :class="['check-item', check.done ? 'done' : 'pending']">
                  <CircleCheck v-if="check.done" />
                  <Warning v-else />
                  <span>{{ check.label }}</span>
                  <strong>{{ check.done ? '通过' : '待处理' }}</strong>
                </div>
                <div v-if="!isFeedbackOnlyCompetition" :class="['check-item', certificateCheck.done ? 'done' : 'pending', 'optional']">
                  <CircleCheck v-if="certificateCheck.done" />
                  <Warning v-else />
                  <span>{{ certificateCheck.label }}</span>
                  <strong>{{ certificateCheck.status }}</strong>
                </div>
              </div>
              <div class="result-action-panel">
                <h3>发布管理</h3>
                <button
                  v-if="showGenerateAwardsAction"
                  class="tool-button"
                  type="button"
                  :disabled="!canGenerateAwards"
                  :title="generateAwardsDisabledReason"
                  @click="generateAwardsAction"
                >
                  {{ awardDrafts.length ? '重新生成获奖名单' : '生成获奖名单' }}
                </button>
                <button
                  v-if="showConfirmAwardsAction"
                  class="tool-button primary"
                  type="button"
                  :disabled="!canConfirmAwards"
                  :title="confirmAwardsDisabledReason"
                  @click="confirmAwardsAction"
                >
                  确认获奖名单
                </button>
                <button class="tool-button" type="button" @click="downloadScoringData">导出评分数据</button>
                <button
                  v-if="showPublishResultsAction"
                  class="tool-button primary"
                  type="button"
                  :disabled="!canPublishResults"
                  :title="publishResultsDisabledReason"
                  @click="publishResultsAction"
                >
                  {{ isFeedbackOnlyCompetition ? '发布诊断结果' : '发布到厂牌端' }}
                </button>
                <p v-if="!showGenerateAwardsAction && !showConfirmAwardsAction && !showPublishResultsAction">
                  结果已发布，当前只需要按获奖名单补充奖状
                </p>
              </div>
            </article>
          </div>

          <article v-if="!isFeedbackOnlyCompetition" class="panel-card path-audit-card">
            <div class="panel-heading">
              <div>
                <h2>晋级路径核验</h2>
                <span>{{ roundRestructureText ? `${roundRestructureText} -> 奖项` : '暂无轮次路径' }}</span>
              </div>
              <button class="path-toggle" type="button" @click="pathAuditOpen = !pathAuditOpen">
                {{ pathAuditOpen ? '收起' : `展开 ${resultPathEntries.length} 条` }}
              </button>
            </div>
            <div v-if="pathAuditOpen" class="path-table">
              <div class="table-head">
                <span>匿名编号</span>
                <span>组别</span>
                <span>路径</span>
                <span>奖项</span>
              </div>
              <div v-for="entry in resultPathEntries" :key="entry.uuid" class="table-row">
                <strong>{{ entry.uuid }}</strong>
                <span>{{ entry.categoryName }}</span>
                <span>{{ getEntryPath(entry.uuid) }}</span>
                <span>{{ getEntryAward(entry.uuid) || '候选' }}</span>
              </div>
            </div>
          </article>
        </section>
      </main>
    </section>

    <section v-else class="not-found">
      <h1>{{ loading ? '正在加载比赛' : '没有找到比赛' }}</h1>
      <button class="tool-button primary" type="button" @click="router.push('/admin/competitions')">返回台账</button>
    </section>

    <Teleport to="body">
      <div v-if="publishRegistrationConfirmOpen" class="stage-confirm-backdrop" @click.self="closePublishRegistrationConfirm">
        <section class="stage-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="publish-registration-title">
          <header>
            <span class="confirm-kicker">开放报名</span>
            <h2 id="publish-registration-title">确认发布报名？</h2>
          </header>
          <p class="confirm-copy">
            发布后，厂牌端将展示本场比赛并允许报名参赛，发布前请确认报名时间、投递组别和风格库已经核对完成
          </p>
          <div class="confirm-summary">
            <span>
              <small>比赛</small>
              <strong>{{ competition?.name || '-' }}</strong>
            </span>
            <span>
              <small>报名截止</small>
              <strong>{{ competition?.registrationDeadline ? formatDateTime(competition.registrationDeadline) : '-' }}</strong>
            </span>
            <span>
              <small>开放准备</small>
              <strong>{{ registrationReadyCount }} / {{ registrationRequiredChecks.length }}</strong>
            </span>
          </div>
          <footer>
            <button class="confirm-button ghost" type="button" :disabled="publishRegistrationLoading" @click="closePublishRegistrationConfirm">取消</button>
            <button class="confirm-button primary" type="button" :disabled="publishRegistrationLoading" @click="confirmPublishRegistration">
              {{ publishRegistrationLoading ? '发布中' : '确认发布' }}
              <Right />
            </button>
          </footer>
        </section>
      </div>

      <div v-if="closeRegistrationConfirmOpen" class="stage-confirm-backdrop" @click.self="closeRegistrationConfirm">
        <section class="stage-confirm-dialog warning" role="dialog" aria-modal="true" aria-labelledby="close-registration-title">
          <header>
            <span class="confirm-kicker">截止报名</span>
            <h2 id="close-registration-title">确认截止报名？</h2>
          </header>
          <p class="confirm-copy">
            截止后，厂牌端将停止接收新的报名提交，已报名酒款仍会保留在后台，后续可继续处理支付、入库和评审准备
          </p>
          <div class="confirm-summary">
            <span>
              <small>比赛</small>
              <strong>{{ competition?.name || '-' }}</strong>
            </span>
            <span>
              <small>已报名</small>
              <strong>{{ competition?.entriesSummary?.registered ?? competition?.entriesSummary?.total ?? 0 }} 款</strong>
            </span>
            <span>
              <small>原截止时间</small>
              <strong>{{ competition?.registrationDeadline ? formatDateTime(competition.registrationDeadline) : '-' }}</strong>
            </span>
          </div>
          <footer>
            <button class="confirm-button ghost" type="button" :disabled="closeRegistrationLoading" @click="closeRegistrationConfirm">取消</button>
            <button class="confirm-button primary" type="button" :disabled="closeRegistrationLoading" @click="confirmCloseRegistration">
              {{ closeRegistrationLoading ? '截止中' : '截止报名' }}
              <Right />
            </button>
          </footer>
        </section>
      </div>

      <div v-if="roundPublishConfirmOpen" class="stage-confirm-backdrop" @click.self="closeRoundPublishConfirm">
        <section class="stage-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="round-publish-confirm-title">
          <header>
            <span class="confirm-kicker">轮次发布</span>
            <h2 id="round-publish-confirm-title">确认发布当前轮次？</h2>
          </header>
          <p class="confirm-copy">
            发布后，{{ currentRoundPublishTarget }}会在对应端看到本轮任务，发布前请确认桌次、人员和酒款都已经核对完成
          </p>
          <div class="confirm-summary">
            <span>
              <small>当前轮次</small>
              <strong>{{ currentRound?.name || '-' }}</strong>
            </span>
            <span>
              <small>发布对象</small>
              <strong>{{ currentRoundPublishTarget }}</strong>
            </span>
            <span>
              <small>桌数/酒款</small>
              <strong>{{ currentRoundTables.length }} 桌 / {{ currentRoundEntryCount }} 款</strong>
            </span>
          </div>
          <footer>
            <button class="confirm-button ghost" type="button" :disabled="roundPublishLoading" @click="closeRoundPublishConfirm">取消</button>
            <button class="confirm-button primary" type="button" :disabled="roundPublishLoading" @click="confirmPublishCurrentRound">
              {{ roundPublishLoading ? '发布中' : '确认发布' }}
              <Right />
            </button>
          </footer>
        </section>
      </div>

      <div v-if="roundPublishBlockedDialogOpen" class="stage-confirm-backdrop" @click.self="closeRoundPublishBlockedDialog">
        <section class="stage-confirm-dialog warning" role="dialog" aria-modal="true" aria-labelledby="round-publish-blocked-title">
          <header>
            <span class="confirm-kicker">发布检查</span>
            <h2 id="round-publish-blocked-title">暂不能发布当前轮次</h2>
          </header>
          <p class="confirm-copy">
            {{ roundPublishBlockedMessage }}
          </p>
          <div class="confirm-summary">
            <span>
              <small>当前轮次</small>
              <strong>{{ currentRound?.name || '-' }}</strong>
            </span>
            <span>
              <small>发布对象</small>
              <strong>{{ currentRoundPublishTarget }}</strong>
            </span>
            <span>
              <small>处理项</small>
              <strong>{{ roundPublishBlockedMessage }}</strong>
            </span>
          </div>
          <footer>
            <button class="confirm-button primary" type="button" @click="closeRoundPublishBlockedDialog">
              我知道了
            </button>
          </footer>
        </section>
      </div>

      <div v-if="roundLockConfirmOpen" class="stage-confirm-backdrop" @click.self="closeRoundLockConfirm">
        <section class="stage-confirm-dialog warning" role="dialog" aria-modal="true" aria-labelledby="round-lock-confirm-title">
          <header>
            <span class="confirm-kicker">轮次锁定</span>
            <h2 id="round-lock-confirm-title">{{ roundLockConfirmTitle }}</h2>
          </header>
          <p class="confirm-copy">
            {{ roundLockConfirmCopy }}
          </p>
          <div class="confirm-summary">
            <span>
              <small>锁定轮次</small>
              <strong>{{ roundLockTargetRound?.name || '-' }}</strong>
            </span>
            <span>
              <small>轮次类型</small>
              <strong>{{ roundLockTargetIsTerminal ? '决赛轮' : (roundLockTargetRound?.type === 'SCORE' ? '评分轮' : '排序轮') }}</strong>
            </span>
            <span>
              <small>桌数/酒款</small>
              <strong>{{ roundLockTargetTables.length }} 桌 / {{ roundLockTargetEntryCount }} 款</strong>
            </span>
          </div>
          <footer>
            <button class="confirm-button ghost" type="button" :disabled="roundLockLoading" @click="closeRoundLockConfirm">取消</button>
            <button class="confirm-button primary" type="button" :disabled="roundLockLoading" @click="confirmLockCurrentRound">
              {{ roundLockLoading ? '锁定中' : '确认锁定' }}
              <Right />
            </button>
          </footer>
        </section>
      </div>

      <div v-if="firstRoundCompleteConfirmOpen" class="stage-confirm-backdrop" @click.self="closeFirstRoundCompleteConfirm">
        <section class="stage-confirm-dialog warning" role="dialog" aria-modal="true" aria-labelledby="first-round-complete-title">
          <header>
            <span class="confirm-kicker">首轮锁定</span>
            <h2 id="first-round-complete-title">确认首轮完成？</h2>
          </header>
          <p class="confirm-copy">
            {{ isFeedbackOnlyCompetition ? '确认后将锁定首轮诊断结果，请确认所有评分和桌长综合评语已经核对无误' : '确认后将锁定首轮，并生成晋级酒款，请确认所有评分和桌长汇总已经核对无误' }}
          </p>
          <div class="confirm-summary">
            <span>
              <small>当前轮次</small>
              <strong>{{ currentRound?.name || '首轮' }}</strong>
            </span>
            <span v-if="!isFeedbackOnlyCompetition">
              <small>晋级酒款</small>
              <strong>{{ firstRoundCompletionStatus.advancedCount }} 款</strong>
            </span>
            <span>
              <small>桌数/酒款</small>
              <strong>{{ currentRoundTables.length }} 桌 / {{ currentRoundEntryCount }} 款</strong>
            </span>
          </div>
          <footer>
            <button class="confirm-button ghost" type="button" :disabled="firstRoundCompleteLoading" @click="closeFirstRoundCompleteConfirm">再核对</button>
            <button class="confirm-button primary" type="button" :disabled="firstRoundCompleteLoading" @click="confirmCompleteFirstRound">
              {{ firstRoundCompleteLoading ? '锁定中' : '确认完成' }}
              <Right />
            </button>
          </footer>
        </section>
      </div>

      <div v-if="businessConfirm.open" class="stage-confirm-backdrop" @click.self="closeBusinessConfirm">
        <section :class="['stage-confirm-dialog', businessConfirm.tone]" role="dialog" aria-modal="true" aria-labelledby="business-confirm-title">
          <header>
            <span class="confirm-kicker">{{ businessConfirm.kicker }}</span>
            <h2 id="business-confirm-title">{{ businessConfirm.title }}</h2>
          </header>
          <p class="confirm-copy">{{ businessConfirm.copy }}</p>
          <div class="confirm-summary">
            <span v-for="item in businessConfirm.summary" :key="item.label">
              <small>{{ item.label }}</small>
              <strong>{{ item.value }}</strong>
            </span>
          </div>
          <label v-if="businessConfirm.reasonLabel" class="confirm-reason">
            <span>{{ businessConfirm.reasonLabel }}</span>
            <textarea v-model.trim="businessConfirm.reason" :placeholder="businessConfirm.reasonPlaceholder" :maxlength="businessConfirm.reasonMaxLength"></textarea>
          </label>
          <label v-if="businessConfirm.deadlineLabel" class="confirm-reason confirm-date-field">
            <span>{{ businessConfirm.deadlineLabel }}</span>
            <input v-model="businessConfirm.deadlineValue" type="datetime-local" />
          </label>
          <footer>
            <button class="confirm-button ghost" type="button" :disabled="businessConfirm.loading" @click="closeBusinessConfirm">
              {{ businessConfirm.cancelText }}
            </button>
            <button class="confirm-button primary" type="button" :disabled="businessConfirm.loading || businessConfirmReasonInvalid || businessConfirmDeadlineInvalid" @click="confirmBusinessAction">
              {{ businessConfirm.loading ? businessConfirm.loadingText : businessConfirm.confirmText }}
              <Right />
            </button>
          </footer>
        </section>
      </div>
    </Teleport>

    <CreateRoundWizard
      :open="createRoundDialogOpen"
      :next-round-name="nextRoundName"
      :advanced-pool="advancedPool"
      :advanced-category-stats="advancedCategoryStats"
      :target-mode="createRoundForm.targetMode"
      :target-count="createRoundForm.targetCount"
      :table-count="createRoundForm.tableCount"
      :target-options="createRoundTargetOptions"
      :early-draft="createNextRoundIsEarlyDraft"
      @update:target-mode="updateCreateRoundTargetMode"
      @update:target-count="createRoundForm.targetCount = $event"
      @update:table-count="createRoundForm.tableCount = $event"
      @close="closeCreateRoundDialog"
      @finish="finishCreateRound"
    />

    <el-dialog
      v-model="entryAutoAssignDialogOpen"
      class="entry-auto-assign-dialog-shell"
      width="420px"
      :title="entryAutoAssignTable ? `${entryAutoAssignTable.name} · 自动分配` : '自动分配'"
      align-center
      destroy-on-close
    >
      <div class="entry-auto-assign-dialog">
        <label class="entry-auto-assign-field">
          <span>范围</span>
          <el-select
            v-model="entryAutoAssignForm.categoryId"
            placeholder="选择范围"
            popper-class="entry-auto-assign-popper"
            style="width: 100%"
          >
            <el-option
              v-for="option in entryAutoAssignCategoryOptions"
              :key="option.id"
              :label="option.name"
              :value="option.id"
            >
              <div class="entry-auto-assign-option">
                <span>{{ option.name }}</span>
                <small>{{ option.unassigned }} 款可用</small>
              </div>
            </el-option>
          </el-select>
        </label>

        <label class="entry-auto-assign-field">
          <span>数量</span>
          <el-input-number
            v-model="entryAutoAssignForm.quantity"
            :min="1"
            :max="Math.max(entryAutoAssignStats.unassigned, 1)"
            controls-position="right"
          />
        </label>

        <div class="entry-auto-assign-stats">
          <span>
            <strong>{{ entryAutoAssignStats.total }}</strong>
            总计
          </span>
          <span>
            <strong>{{ entryAutoAssignStats.unassigned }}</strong>
            未分配
          </span>
          <span class="highlight">
            <strong>{{ entryAutoAssignStats.adding }}</strong>
            本次加入
          </span>
        </div>
      </div>

      <template #footer>
        <div class="dialog-actions">
          <button class="tool-button" type="button" @click="closeEntryAutoAssignDialog">取消</button>
          <button
            class="tool-button primary"
            type="button"
            :disabled="!entryAutoAssignForm.categoryId || entryAutoAssignStats.unassigned === 0"
            @click="confirmEntryAutoAssign"
          >
            加入
          </button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="roundScoreDetailDialogOpen"
      class="round-score-detail-dialog-shell"
      width="980px"
      :title="roundDetailDialogTitle"
      align-center
      destroy-on-close
    >
      <div v-if="roundScoreDetailTable" class="round-score-detail-dialog">
        <section v-if="isRoundScoreDetailRanking" class="score-detail-summary ranking-detail-summary">
          <span>
            <small>桌长</small>
            <strong>{{ roundRankingDetailStats.captainName }}</strong>
          </span>
          <span>
            <small>候选酒款</small>
            <strong>{{ roundRankingDetailStats.candidateCount }} 款</strong>
          </span>
          <span>
            <small>{{ roundRankingDetailStats.targetLabel }}</small>
            <strong>{{ roundRankingDetailStats.filledCount }} / {{ roundRankingDetailStats.targetCount }}</strong>
          </span>
          <span>
            <small>轮次状态</small>
            <strong>{{ roundRankingDetailStats.statusText }}</strong>
          </span>
        </section>

        <template v-if="isRoundScoreDetailRanking">
          <section class="ranking-detail-slots">
            <header>
              <h3>排序结果</h3>
              <span>{{ roundRankingDetailStats.resultStatusText }}</span>
            </header>
            <article
              v-for="slot in roundRankingDetailSlots"
              :key="slot.rank"
              :class="{ filled: slot.uuid }"
            >
              <strong>{{ slot.label }}</strong>
              <span>{{ slot.entryName }}</span>
              <small>{{ slot.entryMeta }}</small>
            </article>
          </section>

          <section class="ranking-detail-candidates">
            <header>
              <h3>本桌候选酒款</h3>
              <span>{{ roundScoreDetailEntries.length }} 款</span>
            </header>
            <article v-for="entry in roundScoreDetailEntries" :key="entry.uuid">
              <strong>{{ entry.name }}</strong>
              <span>{{ entry.shortCode || entry.uuid }}</span>
              <small>{{ entry.categoryName }}</small>
            </article>
          </section>
        </template>

        <section v-else class="score-detail-summary">
          <span>
            <small>个人评分</small>
            <strong>{{ roundScoreDetailStats.submitted }} / {{ roundScoreDetailStats.total }}</strong>
          </span>
          <span>
            <small>桌长共识</small>
            <strong>{{ roundScoreDetailCaptainStats.consensus.submitted }} / {{ roundScoreDetailCaptainStats.consensus.total }}</strong>
          </span>
          <span v-if="!isFeedbackOnlyCompetition">
            <small>选择晋级</small>
            <strong>{{ roundScoreDetailCaptainStats.advance.submitted }} / {{ roundScoreDetailCaptainStats.advance.total }}</strong>
          </span>
          <span>
            <small>同桌确认</small>
            <strong>{{ roundScoreDetailConfirmationText }}</strong>
          </span>
          <span v-if="roundScoreDetailTable?.confirmationOverrideFlag" class="pending-judge-summary">
            <small>现场确认</small>
            <strong>{{ roundScoreDetailTable.confirmationOverrideReason || '已处理' }}</strong>
          </span>
          <span class="pending-judge-summary">
            <small>未完成评分评审</small>
            <strong>{{ roundScoreDetailStats.pendingJudgeNames || '无' }}</strong>
          </span>
          <span>
            <small>酒款</small>
            <strong>{{ roundScoreDetailEntries.length }} 款</strong>
          </span>
        </section>
        <button
          v-if="canOverrideRoundScoreConfirmation"
          class="tool-button primary confirmation-override-button"
          type="button"
          @click="overrideRoundScoreConfirmation"
        >
          现场确认
        </button>

        <section v-if="!isRoundScoreDetailRanking" class="score-detail-progress-list">
          <article
            v-for="judge in roundScoreDetailJudges"
            :key="judge.judgePublicId || judge.judgeName"
            :class="['score-detail-progress-row', { pending: hasJudgeDetailPendingTasks(judge), captain: isCaptainDetailJudge(judge) }]"
          >
            <div class="score-detail-judge-name">
              <strong>{{ judge.judgeName }}</strong>
              <small>{{ judge.roleLabel }}</small>
            </div>
            <div class="score-detail-task-list">
              <div
                v-for="task in buildJudgeDetailTasks(judge)"
                :key="task.key"
                :class="['score-detail-task', { pending: task.remaining > 0 }]"
              >
                <small>{{ task.label }}</small>
                <div class="score-detail-progress-track" :aria-label="`${judge.judgeName}${task.label}进度`">
                  <i :style="{ width: `${task.progress}%` }"></i>
                </div>
                <span>{{ task.submitted }} / {{ task.total }}</span>
                <em>{{ task.statusText }}</em>
              </div>
            </div>
          </article>
        </section>
      </div>
    </el-dialog>

    <el-dialog
      v-model="feedbackEditor.visible"
      :close-on-click-modal="false"
      class="feedback-editor-dialog"
      width="760px"
      destroy-on-close
    >
      <template #header>
        <div class="feedback-editor-title">
          <strong>{{ feedbackEditorTitle }}</strong>
          <small>{{ feedbackEditorSubtitle }}</small>
        </div>
      </template>

      <div class="feedback-editor">
        <div class="feedback-editor-context">
          <span>{{ feedbackEditor.entryTitle || '-' }}</span>
          <span>{{ feedbackEditor.entryMeta || '-' }}</span>
          <span v-if="feedbackEditor.targetLabel">{{ feedbackEditor.targetLabel }}</span>
        </div>

        <label v-if="feedbackEditor.type === 'captain'" class="feedback-editor-field">
          <span>综合评语</span>
          <textarea v-model="feedbackEditor.comments" rows="8" maxlength="1000" />
        </label>

        <div v-else class="feedback-editor-dimensions">
          <article v-for="dimension in feedbackEditor.dimensions" :key="dimension.key || dimension.label" class="feedback-editor-dimension">
            <div>
              <strong>{{ dimension.label || dimension.key }}</strong>
              <small>{{ dimension.score ?? '-' }} / {{ dimension.maxScore ?? '-' }}</small>
            </div>
            <textarea v-model="dimension.note" rows="3" maxlength="500" />
          </article>
        </div>

        <label class="feedback-editor-field">
          <span>修改原因</span>
          <input v-model.trim="feedbackEditor.reason" maxlength="120" />
        </label>

        <div class="feedback-editor-footer">
          <span :class="{ warning: feedbackEditorEffectiveLength === 0 }">有效字数 {{ feedbackEditorEffectiveLength }}</span>
          <div>
            <button class="tool-button" type="button" :disabled="feedbackEditor.saving" @click="closeFeedbackCommentEditor">取消</button>
            <button class="tool-button primary" type="button" :disabled="!canSubmitFeedbackEditor" @click="submitFeedbackCommentEdit">
              {{ feedbackEditor.saving ? '保存中' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Back,
  Calendar,
  Check,
  CircleCheck,
  Clock,
  Close,
  DataAnalysis,
  Delete,
  EditPen,
  Files,
  Finished,
  Lock,
  Medal,
  Plus,
  Right,
  Search,
  Setting,
  Tickets,
  TrendCharts,
  Warning,
} from '@element-plus/icons-vue'
import {
  defaultScoreConfigs,
  fieldTypeLabels,
  formatDate,
  formatDateTime,
  getScoreTotal,
  roleLabels,
  statusMeta,
} from './competitionStore'
import {
  closeCompetitionRegistration,
  completeFirstRound,
  confirmCompetitionAwards,
  createFirstRound,
  createNextRound,
  deleteAwardCertificate,
  deleteDraftRound,
  deleteCompetition,
  downloadAwardCertificate,
  exportCompetitionScoringData,
  fetchCompetitionAnalytics,
  fetchCompetitionFeedbackReview,
  fetchCompetitionDetail,
  fetchCompetitionSponsors,
  fetchJudges,
  fetchStyleLibraries,
  generateCompetitionAwards,
  lockRound,
  markEntryStored,
  openCompetitionRegistration,
  overrideRoundTableConfirmation,
  prepareCompetitionJudging,
  publishCompetitionResults,
  publishRound,
  reopenCompetitionRegistration,
  returnCompetitionToSampleCheck,
  saveRoundAllocation,
  syncRoundCandidates,
  updateCompetitionFeedbackComment,
  updateCompetitionBaseInfo,
  updateCompetitionCategories,
  updateCompetitionEntryFields,
  updateCompetitionJudgeAssignments,
  updateCompetitionJudgeTables,
  updateCompetitionSponsors,
  updateCompetitionStyles,
  updateScoreConfigs,
  uploadAwardCertificate,
  uploadCompetitionSponsorLogo,
} from '@/api/admin'
import { fallbackStyleLibraries, formatStyleItemName, getStyleLibrary, normalizeStyleLibraries } from './styleLibraries'
import CreateRoundWizard from './components/competition-detail/CreateRoundWizard.vue'
import CompetitionAnalyticsPanel from './components/competition-detail/CompetitionAnalyticsPanel.vue'
import TableAllocationWorkbench from './components/competition-detail/TableAllocationWorkbench.vue'

const route = useRoute()
const router = useRouter()
const detailTabKeys = new Set(['overview', 'analysis', 'baseInfo', 'sponsors', 'entryConfig', 'entries', 'judges', 'rounds', 'score', 'feedback', 'results'])
const roundProgressPollIntervalMs = 8000
const activeTab = ref(normalizeDetailTab(route.query.tab))
const loading = ref(false)
const competition = ref(null)
const competitionAnalytics = ref(null)
const competitionAnalyticsLoading = ref(false)
const competitionAnalyticsCompetitionId = ref(null)
let roundProgressPollTimer = null
let roundProgressRefreshing = false
let roundTableNameAutoSaveTimer = null
const categoryForm = reactive([])
const baseForm = reactive({
  name: '',
  code: '',
  competitionDate: '',
  registrationStart: '',
  registrationDeadline: '',
  entryFee: 0,
  earlyBirdFee: '',
  earlyBirdDeadline: '',
  description: '',
  rulesUrl: '',
  deliveryMethod: 'BOTH',
  sampleArrivalStart: '',
  sampleArrivalDeadline: '',
  sampleQuantityNote: '',
  deliveryRecipient: '',
  deliveryPhone: '',
  deliveryAddress: '',
  deliveryNote: '',
  logisticsVisibility: 'PAYMENT_CONFIRMED',
})
const sponsorForm = reactive([])
const sponsorLogoInputRefs = new Map()
const sponsorLogoCropOutputWidth = 720
const sponsorLogoCropOutputHeight = 720
const sponsorLogoCrop = reactive({
  open: false,
  sponsor: null,
  file: null,
  previewUrl: '',
  imageWidth: 0,
  imageHeight: 0,
  scale: 1,
  x: 0,
  y: 0,
  dragging: false,
  dragStartX: 0,
  dragStartY: 0,
  startX: 0,
  startY: 0,
  uploading: false,
})
const entryFieldForm = reactive([])
const judgeTableForm = reactive([])
const judgeAssignmentForm = reactive([])
const scoreConfigForm = reactive([])
const judgePool = ref([])
const feedbackReviewEntries = ref([])
const feedbackReviewLoading = ref(false)
const feedbackReviewCompetitionId = ref(null)
const selectedFeedbackEntryKey = ref('')
const feedbackFilters = reactive({
  keyword: '',
  tableName: '全部',
  categoryName: '全部',
  judgeType: '全部',
  status: 'all',
})
const feedbackEditor = reactive({
  visible: false,
  saving: false,
  type: '',
  scoreRecordId: null,
  expectedUpdatedAt: '',
  entryTitle: '',
  entryMeta: '',
  targetLabel: '',
  comments: '',
  reason: '',
  dimensions: [],
})
const judgeKeyword = ref('')
const judgeRoleFilter = ref('UNASSIGNED')
const allocationMode = ref('judges')
const allocationDraftSaving = ref(false)
const selectedTableLocalId = ref(null)
const selectedRole = ref('CAPTAIN')
const draggingItem = ref(null)
const awardCertificateInput = ref(null)
const certificateTargetAward = ref(null)
const certificateActionIds = ref(new Set())
const pathAuditOpen = ref(false)
const publishRegistrationConfirmOpen = ref(false)
const publishRegistrationLoading = ref(false)
const closeRegistrationConfirmOpen = ref(false)
const closeRegistrationLoading = ref(false)
const roundPublishConfirmOpen = ref(false)
const roundPublishLoading = ref(false)
const roundPublishBlockedDialogOpen = ref(false)
const roundPublishBlockedMessage = ref('')
const roundLockConfirmOpen = ref(false)
const roundLockLoading = ref(false)
const roundLockTargetRoundId = ref('')
const firstRoundCompleteConfirmOpen = ref(false)
const firstRoundCompleteLoading = ref(false)
const businessConfirm = reactive({
  open: false,
  loading: false,
  action: '',
  kicker: '',
  title: '',
  copy: '',
  summary: [],
  confirmText: '确认',
  cancelText: '取消',
  loadingText: '处理中',
  tone: 'warning',
  reasonLabel: '',
  reasonPlaceholder: '',
  reason: '',
  reasonMaxLength: 255,
  deadlineLabel: '',
  deadlineValue: '',
  deadlineRequired: false,
  payload: null,
})
const businessConfirmReasonInvalid = computed(() => Boolean(businessConfirm.reasonLabel && !businessConfirm.reason.trim()))
const businessConfirmDeadlineInvalid = computed(() => Boolean(businessConfirm.deadlineRequired && !businessConfirm.deadlineValue))

const rounds = ref([])
const roundEntryPool = ref([])
const firstRoundDraft = reactive({
  id: 'first-round-draft',
  roundNo: 1,
  name: '首轮草稿',
  type: 'SCORE',
  status: 'DRAFT',
  tables: [],
  isPreparationDraft: true,
})
const activeRoundId = ref('')
const selectedRoundTableId = ref('')
const roundProgressSection = ref(null)
const roundScoreDetailDialogOpen = ref(false)
const roundScoreDetailTableId = ref('')
const roundKeyword = ref('')
const roundCategoryFilter = ref('全部')
const selectedEntryUuids = ref([])
const createRoundDialogOpen = ref(false)
const createRoundForm = reactive({
  targetMode: 'MEDALS',
  targetCount: 3,
  tableCount: 1,
})
const entryAutoAssignDialogOpen = ref(false)
const entryAutoAssignTableId = ref('')
const entryAutoAssignForm = reactive({
  categoryId: null,
  quantity: 1,
})
const selectedStyleLibraryVersion = ref('')
const styleLibraryOptions = ref(normalizeStyleLibraries(fallbackStyleLibraries))
const styleDistributionListRef = ref(null)
const styleDistributionHasOverflow = ref(false)
let styleDistributionResizeObserver = null

const tabs = [
  { key: 'overview', label: '概览', icon: DataAnalysis },
  { key: 'analysis', label: '数据分析', icon: TrendCharts },
  { key: 'baseInfo', label: '基础信息', icon: Setting },
  { key: 'sponsors', label: '赞助商', icon: Medal },
  { key: 'entryConfig', label: '报名配置', icon: Setting },
  { key: 'score', label: '评分表', icon: Finished },
  { key: 'entries', label: '参赛酒款', icon: Tickets },
  { key: 'judges', label: '分桌分配', icon: Files },
  { key: 'rounds', label: '轮次编排', icon: Calendar },
  { key: 'feedback', label: '评价查看', icon: CircleCheck },
  { key: 'results', label: '结果发布', icon: Medal },
]

const detailTabs = computed(() => tabs.map((tab) => {
  const disabledReason = resolveDetailTabDisabledReason(tab.key)
  return {
    ...tab,
    label: tab.key === 'results' && isFeedbackOnlyCompetition.value ? '诊断发布' : tab.label,
    enabled: !disabledReason,
    disabledReason,
  }
}))

const entryStatusLabels = {
  PENDING_PAYMENT: '待支付',
  REGISTERED: '已报名',
  STORED: '已入库',
  CANCELED: '已取消',
  RESULT_PUBLISHED: '结果已出',
}

const roleOptions = [
  { label: '桌长', value: 'CAPTAIN' },
  { label: '专业评审', value: 'PROFESSIONAL' },
  { label: '跨界评审', value: 'CROSS' },
]

const roleFilters = [
  { label: '未分配', value: 'UNASSIGNED' },
  { label: '全部', value: 'ALL' },
]

const roundStatusLabels = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  IN_PROGRESS: '处理中',
  SUBMITTED: '已提交',
  LOCKED: '已锁定',
}

const statusInfo = computed(() => statusMeta[competition.value?.status] || statusMeta.DRAFT)
const editable = computed(() => competition.value?.editableScopes || {})
const hasDataIssues = computed(() => Boolean(competition.value?.dataIntegrityIssues?.length))
const registrationRequiredKeys = ['baseInfo', 'categories', 'styleLibrary']
const registrationRequiredChecks = computed(() => (competition.value?.checks || []).filter((check) => registrationRequiredKeys.includes(check.key)))
const registrationReadyCount = computed(() => registrationRequiredChecks.value.filter((check) => check.state === 'done').length)
const registrationBlockText = computed(() => {
  if (hasDataIssues.value) return '系统数据需修正'
  const pending = registrationRequiredChecks.value.filter((check) => check.state !== 'done')
  return pending.length ? `还差 ${pending.map((check) => check.label).join('、')}` : '可开放报名'
})
const selectedStyleLibrary = computed(() => getStyleLibrary(selectedStyleLibraryVersion.value || competition.value?.styleLibraryVersion, styleLibraryOptions.value))
const styleSnapshot = computed(() => competition.value?.styles || [])
const selectedStyleItems = computed(() => {
  const libraryItems = selectedStyleLibrary.value?.styleItems || []
  const items = libraryItems.length ? libraryItems : styleSnapshot.value
  return items.filter((style) => style.status !== 0)
})
const styleCategorySummary = computed(() => {
  const counts = new Map()
  selectedStyleItems.value.forEach((style) => {
    const name = style.categoryName || '未归类'
    counts.set(name, (counts.get(name) || 0) + 1)
  })
  return [...counts.entries()].map(([name, count]) => ({ name, count }))
})
const registrationWindowInfo = computed(() => resolveRegistrationWindowInfo())
const headerMetaItems = computed(() => buildHeaderMetaItems())
const stagePrimaryAction = computed(() => resolveStagePrimaryAction())
const stagePrimaryActionDisabledReason = computed(() => (
  stagePrimaryAction.value.enabled ? '' : stagePrimaryAction.value.disabledReason || '当前阶段不能执行该操作'
))
const stageSecondaryActions = computed(() => resolveStageSecondaryActions())
const tabSaveAction = computed(() => {
  if (activeTab.value === 'baseInfo' && (editable.value.baseInfo || editable.value.description)) {
    return { label: '保存基础信息', handler: saveBaseInfo }
  }
  if (activeTab.value === 'entryConfig' && editable.value.entryStructure) {
    return { label: '保存报名配置', handler: saveEntryConfig }
  }
  if (activeTab.value === 'score' && editable.value.scoreConfigs) {
    return { label: '保存评分表', handler: saveScoreConfigs }
  }
  if (activeTab.value === 'sponsors') {
    return { label: '保存赞助商', handler: saveSponsors }
  }
  return null
})
const futureStageTasks = computed(() => buildFutureStageTasks())
const currentRound = computed(() => rounds.value.find((round) => round.id === activeRoundId.value) || rounds.value[0] || firstRoundDraft)
const currentRoundTables = computed(() => currentRound.value?.tables || [])
const currentRoundIsTerminal = computed(() => isTerminalRound(currentRound.value))
const currentDraftSourceRound = computed(() => {
  if (!currentRound.value || currentRound.value.type !== 'RANKING' || currentRound.value.status !== 'DRAFT' || !currentRound.value.sourceRoundId) return null
  return rounds.value.find((round) => round.id === currentRound.value.sourceRoundId) || null
})
const canLockCurrentDraftSourceRound = computed(() => currentDraftSourceRound.value?.status === 'SUBMITTED')
const roundLockTargetRound = computed(() => rounds.value.find((round) => round.id === roundLockTargetRoundId.value) || currentRound.value)
const roundLockTargetTables = computed(() => roundLockTargetRound.value?.tables || [])
const roundLockTargetEntryCount = computed(() => new Set(roundLockTargetTables.value.flatMap((table) => table.entryUuids)).size)
const roundLockTargetIsTerminal = computed(() => isTerminalRound(roundLockTargetRound.value))
const firstRoundCreated = computed(() => rounds.value.some((round) => Number(round.roundNo) === 1))
const canReturnToSampleCheck = computed(() => (
  competition.value?.status === 'JUDGING_PREP'
    && !rounds.value.some((round) => Number(round.roundNo) === 1 && round.status !== 'DRAFT')
))
const canEditBaseJudgeTables = computed(() => Boolean(editable.value.judgeTables) && !firstRoundCreated.value)
const selectedRoundTable = computed(() => currentRoundTables.value.find((table) => table.id === selectedRoundTableId.value) || currentRoundTables.value[0])
const roundScoreDetailTable = computed(() => currentRoundTables.value.find((table) => table.id === roundScoreDetailTableId.value) || null)
const entryAutoAssignTable = computed(() => currentRoundTables.value.find((table) => table.id === entryAutoAssignTableId.value) || null)
const currentRoundTypeLabel = computed(() => (currentRound.value?.type === 'SCORE' ? '评分制' : '选择排序制'))
const currentRoundStatusText = computed(() => roundStatusLabels[currentRound.value?.status] || currentRound.value?.status || '-')
const currentRoundEntryCount = computed(() => new Set(currentRoundTables.value.flatMap((table) => table.entryUuids)).size)
const currentRoundTargetCount = computed(() => currentRoundTables.value.reduce((sum, table) => sum + Number(table.targetCount || 0), 0))
const advancedPool = computed(() => roundEntryPool.value.filter((entry) => entry.advanced))
const preplanningNotice = computed(() => {
  if (competition.value?.status === 'REGISTRATION_OPEN') {
    return '报名仍在进行，当前分桌和轮次会保留为预排草稿，不会发布给评审，新增或入库酒款后可继续调整'
  }
  if (competition.value?.status === 'JUDGING_PREP' && currentRound.value?.status === 'DRAFT') {
    return '当前处于评审准备中，退回样品入库核对会保留首轮草稿；发布前请重新核对入库酒款和分桌'
  }
  return ''
})
const overviewActionItems = computed(() => buildOverviewActionItems())
const roundCategoryFilters = computed(() => ['全部', ...new Set(currentPoolEntries.value.map((entry) => entry.categoryName).filter(Boolean))])
const currentPoolEntries = computed(() => getPoolEntriesForRound(currentRound.value))
const entryLookup = computed(() => {
  const entries = [...roundEntryPool.value, ...(competition.value?.entries || [])]
  return new Map(entries.map((entry) => [entry.uuid, entry]))
})
const isRoundScoreDetailRanking = computed(() => currentRound.value?.type === 'RANKING')
const roundDetailDialogTitle = computed(() => {
  if (!roundScoreDetailTable.value) return isRoundScoreDetailRanking.value ? '排序详情' : '评分详情'
  return `${roundScoreDetailTable.value.name} · ${isRoundScoreDetailRanking.value ? '排序详情' : '评分详情'}`
})
const roundScoreDetailJudges = computed(() => {
  const details = roundScoreDetailTable.value?.judgeDetails || []
  const source = details.length ? details : buildFallbackRoundScoreDetailJudges(roundScoreDetailTable.value)
  return mergeCaptainIntoRoundScoreDetailJudges(roundScoreDetailTable.value, source)
    .slice()
    .sort((left, right) => {
      const leftCaptain = isCaptainDetailJudge(left)
      const rightCaptain = isCaptainDetailJudge(right)
      if (leftCaptain !== rightCaptain) return leftCaptain ? -1 : 1
      const leftPending = Number(left.totalCount || 0) - Number(left.submittedCount || 0)
      const rightPending = Number(right.totalCount || 0) - Number(right.submittedCount || 0)
      if (leftPending !== rightPending) return rightPending - leftPending
      return String(left.judgeName || '').localeCompare(String(right.judgeName || ''), 'zh-CN')
    })
})
const roundScoreDetailEntries = computed(() => (roundScoreDetailTable.value?.entryUuids || []).map((uuid) => {
  const entry = entryLookup.value.get(uuid) || {}
  return {
    uuid,
    name: entry.name || '未命名酒款',
    shortCode: entry.shortCode || '',
    categoryName: entry.categoryName || entry.category || '-',
  }
}))
const roundRankingDetailSlots = computed(() => getRankingSlots(roundScoreDetailTable.value || {}).map((slot) => {
  const entry = buildRoundDetailEntryDisplay(slot.uuid)
  return {
    ...slot,
    entryName: entry.name,
    entryMeta: entry.meta,
  }
}))
const roundRankingDetailStats = computed(() => {
  const table = roundScoreDetailTable.value
  const targetCount = Number(table?.targetCount || 0)
  const filledCount = getFilledRankingCount(table || {})
  const captainName = getJudge(table?.captainPublicId)?.name || '未指定'
  const statusText = roundStatusLabels[table?.status] || roundStatusLabels[currentRound.value?.status] || table?.status || currentRound.value?.status || '-'
  return {
    captainName,
    candidateCount: table?.entryUuids?.length || 0,
    filledCount,
    targetCount,
    targetLabel: table?.targetMode === 'CHAMPION' ? '总冠军' : (table?.targetMode === 'MEDALS' ? '奖项名额' : '已选择'),
    statusText,
    resultStatusText: table?.targetMode === 'MEDALS'
      ? (filledCount ? `已选择 ${filledCount} 项，未选名额留空` : '暂未选择奖项')
      : (targetCount > 0 && filledCount >= targetCount ? '已完成' : `待选择 ${Math.max(0, targetCount - filledCount)} 款`),
  }
})
const roundScoreDetailStats = computed(() => roundScoreDetailJudges.value.reduce((summary, judge) => {
  const total = Number(judge.totalCount || 0)
  const submitted = Number(judge.submittedCount || 0)
  summary.total += total
  summary.submitted += submitted
  if (submitted < total) {
    summary.pendingJudges += 1
    if (judge.judgeName) summary.pendingJudgeNamesList.push(judge.judgeName)
  }
  summary.pendingJudgeNames = summary.pendingJudgeNamesList.join('、')
  return summary
}, { submitted: 0, total: 0, pendingJudges: 0, pendingJudgeNamesList: [], pendingJudgeNames: '' }))
const roundScoreDetailCaptainStats = computed(() => {
  const table = roundScoreDetailTable.value
  const entryCount = roundScoreDetailEntries.value.length
  const consensus = buildCountProgressFromPercent(normalizeProgress(table?.captainProgress), entryCount)
  const advanceTotal = Number(table?.targetCount || 0)
  const advanceSubmitted = Math.min(advanceTotal, Math.max(0, Number(table?.advancedCount || 0)))
  return {
    consensus,
    advance: {
      submitted: advanceSubmitted,
      total: advanceTotal,
      progress: calculateCountProgress(advanceSubmitted, advanceTotal),
      remaining: Math.max(0, advanceTotal - advanceSubmitted),
    },
  }
})
const roundScoreDetailConfirmationText = computed(() => {
  const table = roundScoreDetailTable.value
  return `${Number(table?.confirmationConfirmedCount || 0)} / ${Number(table?.confirmationRequiredCount || 0)}`
})
const canOverrideRoundScoreConfirmation = computed(() => {
  const table = roundScoreDetailTable.value
  return Boolean(
    currentRound.value?.type === 'SCORE'
      && currentRound.value?.status === 'PUBLISHED'
      && table
      && table.status === 'PUBLISHED'
      && normalizeProgress(table.captainProgress) >= 100
      && !table.confirmationReady
      && !table.confirmationOverrideFlag,
  )
})
const filteredRoundPool = computed(() => {
  const query = roundKeyword.value.toLowerCase()
  return currentPoolEntries.value
    .filter((entry) => {
      const matchesCategory = roundCategoryFilter.value === '全部' || entry.categoryName === roundCategoryFilter.value
      const matchesQuery = !query || [entry.name, entry.uuid, entry.shortCode, entry.categoryName, entry.style, entry.sourceTable]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(query))
      return matchesCategory && matchesQuery
    })
    .sort((left, right) => {
      const leftAssigned = getRoundEntryAssignment(left.uuid) ? 1 : 0
      const rightAssigned = getRoundEntryAssignment(right.uuid) ? 1 : 0
      if (leftAssigned !== rightAssigned) return leftAssigned - rightAssigned
      return String(left.shortCode || left.uuid).localeCompare(String(right.shortCode || right.uuid), 'zh-CN')
    })
})
const roundValidationIssues = computed(() => buildRoundValidationIssues(currentRound.value))
const roundPublishDisabledReason = computed(() => getRoundPublishStageIssue(currentRound.value))
const canPublishCurrentRound = computed(() => currentRound.value?.status === 'DRAFT' && !roundPublishDisabledReason.value && roundValidationIssues.value.length === 0)
const roundPublishActionTitle = computed(() => (canPublishCurrentRound.value ? '' : roundPublishDisabledReason.value || roundValidationIssues.value[0] || '请先处理发布前检查'))
const roundReadinessChecks = computed(() => buildRoundReadinessChecks())
const currentRoundTargetLabel = computed(() => resolveRoundTargetLabel(currentRound.value))
const currentRoundTargetDisplay = computed(() => {
  if (!currentRound.value) return '-'
  if (currentRound.value.type !== 'SCORE') return formatRoundTargetDisplay(currentRound.value)
  const targets = [...new Set(currentRoundTables.value.map((table) => Number(table.targetCount || 0)).filter(Boolean))]
  return targets.length === 1 ? `每桌 ${targets[0]}，共 ${currentRoundTargetCount.value}` : `共 ${currentRoundTargetCount.value}`
})
const currentRoundTargetMode = computed(() => {
  const modes = new Set(currentRoundTables.value.map((table) => table.targetMode).filter(Boolean))
  if (modes.size === 1) return [...modes][0]
  return currentRound.value?.type === 'SCORE' ? 'ADVANCE_COUNT' : 'TOP_N'
})
const currentRoundTargetModeOptions = computed(() => {
  if (currentRoundTargetMode.value === 'CHAMPION' || canUseChampionTarget(currentRound.value)) {
    return [
      { value: 'TOP_N', label: '普通排序轮' },
      { value: 'MEDALS', label: '组别金银铜轮' },
      { value: 'CHAMPION', label: '决赛轮' },
    ]
  }
  return [
    { value: 'TOP_N', label: '普通排序轮' },
    { value: 'MEDALS', label: '组别金银铜轮' },
  ]
})
const currentRoundPublishTarget = computed(() => (currentRound.value?.type === 'RANKING' ? '桌长和参与评审' : '评审'))
const roundLockConfirmTitle = computed(() => {
  const round = roundLockTargetRound.value
  if (!round) return '确认锁定当前轮次？'
  if (isTerminalRound(round)) return '确认锁定决赛轮？'
  return `确认锁定${round.name || '当前轮次'}？`
})
const roundLockConfirmCopy = computed(() => {
  const round = roundLockTargetRound.value
  if (isFeedbackOnlyCompetition.value) {
    return '锁定后，本轮评分、桌长汇总和综合评语将固定，请确认所有评审桌结果已经核对无误'
  }
  if (isTerminalRound(round)) {
    return '锁定后，总冠军排序将固定，后续进入奖项确认和结果发布，请确认决赛桌提交内容已经核对无误'
  }
  if (round?.type === 'SCORE') {
    return '锁定后，本轮评分、桌长汇总和晋级结果将固定，请确认所有评审桌结果已经核对无误'
  }
  return '锁定后，本轮排序结果将固定，并作为后续轮次或奖项确认依据，请确认桌长提交内容已经核对无误'
})
const firstRoundCompletionStatus = computed(() => buildFirstRoundCompletionStatus())
const roundReadinessTitle = computed(() => {
  if (!currentRound.value) return '还没有轮次'
  if (currentRound.value.status === 'DRAFT') {
    if (currentRound.value.isPreparationDraft) return roundValidationIssues.value.length ? `${currentRound.value.name}还需补齐` : '首轮可发布前调整'
    if (canPublishCurrentRound.value) return `${currentRound.value.name}已准备好，可以发布给${currentRoundPublishTarget.value}`
    if (roundPublishDisabledReason.value) return `${currentRound.value.name}已保存为预排草稿`
    return `${currentRound.value.name}发布前还有问题`
  }
  if (currentRound.value.status === 'PUBLISHED') return `${currentRound.value.name}已发布给${currentRoundPublishTarget.value}`
  if (currentRound.value.status === 'IN_PROGRESS') return '本轮排序进行中'
  if (currentRound.value.status === 'SUBMITTED') return currentRoundIsTerminal.value ? '决赛轮已提交，等待确认' : (currentRound.value.type === 'SCORE' ? '首轮已提交，等待确认' : '排序已提交，等待确认')
  if (currentRound.value.status === 'LOCKED') return isFeedbackOnlyCompetition.value ? '首轮诊断已锁定' : (currentRoundIsTerminal.value ? '决赛轮已锁定' : `${currentRound.value.name}已锁定`)
  return currentRoundStatusText.value
})
const roundReadinessDetail = computed(() => {
  if (!currentRound.value) return '请先创建轮次'
  if (roundValidationIssues.value.length) return roundValidationIssues.value[0]
  if (currentRound.value.status === 'DRAFT' && currentRound.value.isPreparationDraft) return roundPublishDisabledReason.value || '点击发布后会保存当前分桌，并发布给评审端'
  if (currentRound.value.status === 'DRAFT' && roundPublishDisabledReason.value) return roundPublishDisabledReason.value
  if (currentRound.value.status === 'DRAFT') return currentRound.value.type === 'SCORE'
    ? '发布后，评审开始评分，桌长可查看本桌酒款并汇总结果'
    : '发布后，桌长提交排序，参与评审查看本桌候选酒款'
  if (currentRound.value.status === 'PUBLISHED') return currentRound.value.type === 'SCORE'
    ? firstRoundCompletionStatus.value.hint
    : '等待桌长提交排序结果'
  if (currentRound.value.status === 'IN_PROGRESS') return '各桌正在处理排序任务'
  if (currentRound.value.status === 'SUBMITTED') return currentRoundIsTerminal.value
    ? '确认总冠军结果无误后锁定，锁定后确认奖项'
    : (currentRound.value.type === 'SCORE' ? '桌长汇总已提交，确认无误后可以锁定本轮' : '排序结果已提交，确认无误后可以锁定本轮')
  if (currentRound.value.status === 'LOCKED') return isFeedbackOnlyCompetition.value
    ? (canPublishResults.value ? '可以发布诊断结果' : '诊断结果已固定')
    : currentRoundIsTerminal.value
    ? '总冠军结果已固定，请确认奖项并发布结果'
    : (isMedalRound(currentRound.value) ? '组别奖项已生成，下一步创建决赛轮' : (canCreateNextRound.value ? `可以创建${nextRoundName.value}` : '本轮结果已固定'))
  return roundNextStepText.value
})
const roundNextStepText = computed(() => {
  if (!currentRound.value) return '请先创建并配置首轮'
  if (currentRound.value.status === 'DRAFT') {
    if (currentRound.value.isPreparationDraft) return roundValidationIssues.value.length ? '先处理发布前检查里的问题' : (roundPublishDisabledReason.value || '点击发布，让评审开始首轮评分')
    if (roundPublishDisabledReason.value) return roundPublishDisabledReason.value
    return canPublishCurrentRound.value ? `点击发布，让${currentRoundPublishTarget.value}开始${currentRound.value.name}` : '先处理发布前检查里的问题'
  }
  if (currentRound.value.status === 'PUBLISHED') return '等待评审评分完成，再由桌长汇总首轮结果'
  if (currentRound.value.status === 'IN_PROGRESS') return '等待桌长提交排序'
  if (currentRound.value.status === 'SUBMITTED') return currentRoundIsTerminal.value ? '确认总冠军后锁定决赛轮' : (canCreateNextRound.value ? `可先创建${nextRoundName.value}草稿安排桌次和人员` : '确认本轮无误后锁定')
  if (currentRound.value.status === 'LOCKED') return isFeedbackOnlyCompetition.value
    ? (canPublishResults.value ? '进入结果发布页发布诊断结果' : '本轮诊断已锁定')
    : currentRoundIsTerminal.value
    ? '进入结果发布页确认奖项'
    : (advancedPool.value.length ? '晋级酒款已准备好，可创建下一轮' : '本轮已锁定')
  return '继续推进当前轮次'
})
const canCreateNextRound = computed(() => {
  if (isFeedbackOnlyCompetition.value) return false
  const lastRound = rounds.value[rounds.value.length - 1]
  if (!lastRound || isTerminalRound(lastRound)) return false
  if (['PUBLISHED', 'IN_PROGRESS', 'SUBMITTED'].includes(lastRound.status)) return true
  if (lastRound.status !== 'LOCKED') return false
  const hasPendingRound = rounds.value.some((round) => ['DRAFT', 'PUBLISHED', 'IN_PROGRESS', 'SUBMITTED'].includes(round.status))
  return !hasPendingRound
})
const createNextRoundIsEarlyDraft = computed(() => {
  const lastRound = rounds.value[rounds.value.length - 1]
  return Boolean(lastRound && lastRound.status !== 'LOCKED')
})
const createNextRoundButtonText = computed(() => (createNextRoundIsEarlyDraft.value ? `提前创建${nextRoundName.value}草稿` : `创建${nextRoundName.value}`))
const nextRoundNumber = computed(() => rounds.value.length + 1)
const nextRoundName = computed(() => {
  const lastRound = rounds.value[rounds.value.length - 1]
  return isMedalRound(lastRound) ? '决赛轮' : `第${toChineseNumber(nextRoundNumber.value)}轮`
})
const createRoundTargetOptions = computed(() => {
  const options = []
  if (competition.value?.status === 'RESULT_CONFIRMING') {
    options.push(buildChampionTargetOption())
    return options
  }
  options.push(
    {
      value: 'TOP_N',
      label: '普通排序轮',
      description: '从晋级名单继续筛选，可按现场需要设置排序数量',
      defaultTargetCount: 3,
    },
    {
      value: 'MEDALS',
      label: '组别金银铜轮',
      description: '每张桌只放同一投递组别，金、银、铜可按评审结果留空',
      fixedTargetCount: 3,
    },
  )
  if (canUseChampionTargetForNextRound()) {
    options.push(buildChampionTargetOption())
  }
  return options
})
const canSubmitRankingRound = computed(() => {
  if (!currentRound.value || currentRound.value.type !== 'RANKING') return false
  if (!['IN_PROGRESS', 'DRAFT'].includes(currentRound.value.status)) return false
  return currentRound.value.tables.every((table) => table.targetMode === 'MEDALS'
    ? getFilledRankingCount(table) > 0
    : getFilledRankingCount(table) === Number(table.targetCount || 0))
})
const currentRoundMetrics = computed(() => buildCurrentRoundMetrics())
const roundPyramidNodes = computed(() => buildRoundPyramidNodes())
const currentRoundTableSummaries = computed(() => currentRoundTables.value.map((table) => buildRoundTableSummary(table)))
const roundTodoHint = computed(() => buildRoundTodoHint())
const advancedCategoryStats = computed(() => {
  const map = new Map()
  advancedPool.value.forEach((entry) => map.set(entry.categoryName, (map.get(entry.categoryName) || 0) + 1))
  return [...map.entries()].map(([category, count]) => ({ category, count }))
})
const entryAutoAssignCategoryOptions = computed(() => {
  const map = new Map()
  currentPoolEntries.value.forEach((entry) => {
    const key = entry.categoryId
    if (key == null) return
    const current = map.get(key) || { id: key, name: entry.categoryName || '-', total: 0, unassigned: 0 }
    current.total += 1
    if (!getRoundEntryAssignment(entry.uuid)) current.unassigned += 1
    map.set(key, current)
  })
  return [...map.values()].sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
})
const entryAutoAssignSelectedCategory = computed(() => entryAutoAssignCategoryOptions.value
  .find((option) => option.id === entryAutoAssignForm.categoryId) || null)
const entryAutoAssignStats = computed(() => {
  const selected = entryAutoAssignSelectedCategory.value
  const total = selected?.total ?? currentPoolEntries.value.length
  const unassigned = selected?.unassigned ?? currentPoolEntries.value.filter((entry) => !getRoundEntryAssignment(entry.uuid)).length
  const quantity = Math.max(Number(entryAutoAssignForm.quantity || 1), 1)
  return {
    total,
    unassigned,
    adding: Math.min(quantity, unassigned),
  }
})
const roundRestructureText = computed(() => rounds.value.map((round) => `${round.name} ${round.tables.length}桌`).join(' -> '))
const isFeedbackOnlyCompetition = computed(() => competition.value?.competitionType === 'FEEDBACK_ONLY')
const awardDrafts = computed(() => competition.value?.awardResults || [])
const championAwards = computed(() => awardDrafts.value.filter((award) => award.awardType === 'CHAMPION'))
const medalAwards = computed(() => awardDrafts.value.filter((award) => award.awardType !== 'CHAMPION'))
const awardRows = computed(() => [...championAwards.value, ...medalAwards.value])
const confirmableAwardDrafts = computed(() => awardDrafts.value.filter((award) => award.beerEntryId))
const terminalRoundLocked = computed(() => rounds.value.some((round) => isTerminalRound(round) && round.status === 'LOCKED'))
const resultsPublished = computed(() => Boolean(competition.value?.resultSetup?.published) || ['PUBLISHED', 'ARCHIVED'].includes(competition.value?.status))
const hasConfirmedAwardResults = computed(() => awardDrafts.value.some((award) => isAwardConfirmedOrPublished(award)))
const awardsConfirmed = computed(() => awardDrafts.value.length > 0 && awardDrafts.value.every((award) => isAwardConfirmedOrPublished(award)))
const hasEditableAwardResults = computed(() => awardDrafts.value.some((award) => canEditAwardSelection(award)))
const showGenerateAwardsAction = computed(() => !isFeedbackOnlyCompetition.value && !awardsConfirmed.value && !resultsPublished.value)
const showConfirmAwardsAction = computed(() => !isFeedbackOnlyCompetition.value && !awardsConfirmed.value && !resultsPublished.value)
const showPublishResultsAction = computed(() => !resultsPublished.value)
const canGenerateAwards = computed(() => !isFeedbackOnlyCompetition.value && terminalRoundLocked.value && !hasConfirmedAwardResults.value && !resultsPublished.value)
const generateAwardsDisabledReason = computed(() => {
  if (!terminalRoundLocked.value) return '请先锁定决赛轮'
  if (resultsPublished.value) return '结果已发布，不能重新生成奖项'
  if (hasConfirmedAwardResults.value) return '奖项已确认，不能重新生成'
  return ''
})
const canConfirmAwards = computed(() => (
  !isFeedbackOnlyCompetition.value
  && competition.value?.status === 'RESULT_CONFIRMING'
  && !resultsPublished.value
  && awardDrafts.value.length > 0
  && confirmableAwardDrafts.value.some((award) => award.awardType === 'MEDAL' && Number(award.rankNo) >= 1 && Number(award.rankNo) <= 3)
  && championAwards.value.some((award) => award.beerEntryId)
  && hasEditableAwardResults.value
  && !hasConfirmedAwardResults.value
))
const confirmAwardsDisabledReason = computed(() => {
  if (awardsConfirmed.value) return '奖项已确认，可以上传奖状或发布结果'
  if (hasConfirmedAwardResults.value) return '奖项已确认，请先撤回确认后再调整'
  if (resultsPublished.value) return '结果已发布，不能调整奖项'
  if (competition.value?.status !== 'RESULT_CONFIRMING') return '请先进入结果确认阶段'
  if (!awardDrafts.value.length) return '请先生成奖项'
  if (!confirmableAwardDrafts.value.some((award) => award.awardType === 'MEDAL' && Number(award.rankNo) >= 1 && Number(award.rankNo) <= 3)) return '至少确认 1 个组别奖项'
  if (!championAwards.value.some((award) => award.beerEntryId)) return '请选择全场总冠军'
  return ''
})
const medalsConfirmed = computed(() => Boolean(competition.value?.resultSetup?.medalsReady) || areMedalAwardsConfirmedLocally())
const championConfirmed = computed(() => Boolean(competition.value?.resultSetup?.championReady) || awardDrafts.value.some((award) => award.awardType === 'CHAMPION' && ['CONFIRMED', 'PUBLISHED'].includes(award.status)))
const finalRoundLocked = computed(() => Boolean(competition.value?.resultSetup?.terminalRoundLocked) || terminalRoundLocked.value)
const canPublishResults = computed(() => Boolean(competition.value?.resultSetup?.canPublishResults) && !resultsPublished.value)
const publishResultsDisabledReason = computed(() => {
  if (resultsPublished.value) return '结果已发布'
  const pending = resultChecks.value.find((item) => !item.done)
  return pending ? pending.label : ''
})
const certificateUploadedCount = computed(() => awardDrafts.value.filter((award) => award.certificateUploaded).length)
const certificateTotalCount = computed(() => confirmableAwardDrafts.value.length || awardDrafts.value.length)
const certificateCheck = computed(() => ({
  label: `奖状 PDF ${certificateUploadedCount.value} / ${certificateTotalCount.value}`,
  done: certificateTotalCount.value === 0 || certificateUploadedCount.value === certificateTotalCount.value,
  status: certificateTotalCount.value === 0 || certificateUploadedCount.value === certificateTotalCount.value ? '完成' : '可后补',
}))
const resultPathEntries = computed(() => roundEntryPool.value.filter((entry) => entry.advanced || getEntryAward(entry.uuid)).slice(0, 8))
const feedbackEntryCount = computed(() => {
  const setupCount = Number(competition.value?.resultSetup?.feedbackEntryCount)
  if (Number.isFinite(setupCount) && setupCount > 0) return setupCount
  return currentRound.value?.type === 'SCORE'
    ? currentRoundEntryCount.value
    : roundEntryPool.value.filter((entry) => entry.stored).length
})
const feedbackFinalScoreCount = computed(() => {
  const setupCount = Number(competition.value?.resultSetup?.feedbackFinalizedCount)
  if (Number.isFinite(setupCount)) return Math.min(setupCount, feedbackEntryCount.value)
  if (currentRound.value?.type === 'SCORE') {
    return currentRoundTables.value.reduce((sum, table) => {
      const explicitCount = Number(table.finalCount)
      if (Number.isFinite(explicitCount)) return sum + Math.min(explicitCount, table.entryUuids?.length || 0)
      return sum + buildCountProgressFromPercent(table.captainProgress, table.entryUuids?.length || 0).submitted
    }, 0)
  }
  return Number(competition.value?.progressSummary?.finalized || 0)
})
const feedbackEvaluatedCount = computed(() => {
  const setupCount = Number(competition.value?.resultSetup?.feedbackEvaluatedCount)
  if (Number.isFinite(setupCount)) return Math.min(setupCount, feedbackEntryCount.value)
  if (currentRound.value?.type === 'SCORE') {
    return currentRoundTables.value.reduce((sum, table) => sum + Math.min(Number(table.evaluatedCount || 0), table.entryUuids?.length || 0), 0)
  }
  return 0
})
const feedbackFinalizedCount = computed(() => (
  finalRoundLocked.value || resultsPublished.value
    ? feedbackEvaluatedCount.value
    : feedbackFinalScoreCount.value
))
const feedbackPublishSummary = computed(() => {
  if (resultsPublished.value) return '诊断结果已发布'
  if (!finalRoundLocked.value) return '等待首轮评审锁定'
  return canPublishResults.value ? '可以发布给厂商和公开结果页' : '还有诊断记录待生成'
})
const resultChecks = computed(() => {
  if (isFeedbackOnlyCompetition.value) {
    const diagnosisGenerated = feedbackEntryCount.value > 0 && feedbackEvaluatedCount.value >= feedbackEntryCount.value
    return [
      { label: '首轮评审已锁定', done: finalRoundLocked.value },
      { label: '诊断记录已生成', done: diagnosisGenerated },
      { label: '可发布给厂商和公开结果页', done: (finalRoundLocked.value && diagnosisGenerated && canPublishResults.value) || resultsPublished.value },
    ]
  }
  return [
    { label: '组别奖项已确认', done: medalsConfirmed.value },
    { label: '总冠军已确认', done: championConfirmed.value },
    { label: '决赛轮已锁定', done: finalRoundLocked.value },
  ]
})

const selectedTable = computed(() => judgeTableForm.find((table) => table.localId === selectedTableLocalId.value))
const selectedRoleLabel = computed(() => roleOptions.find((role) => role.value === selectedRole.value)?.label || '角色')
const selectedTargetLabel = computed(() => selectedTable.value ? `${selectedTable.value.tableName || '未命名评审桌'} · ${selectedRoleLabel.value}` : '请先选择评审桌')
const judgeMetrics = computed(() => ({
  assigned: judgeAssignmentForm.length,
  captain: countAssignedRole('CAPTAIN'),
  professional: countAssignedRole('PROFESSIONAL'),
  cross: countAssignedRole('CROSS'),
}))
const judgeFilterCounts = computed(() => ({
  ALL: judgePool.value.length,
  UNASSIGNED: judgePool.value.filter((judge) => !isAssigned(judge.publicId)).length,
}))
const validationIssues = computed(() => {
  const issues = []
  if (!judgeTableForm.length) issues.push('至少需要创建 1 张基础桌')
  judgeTableForm.forEach((table) => issues.push(...tableValidationIssues(table)))
  const duplicateJudgeIds = judgeAssignmentForm
    .map((assignment) => assignment.judgePublicId)
    .filter((judgePublicId, index, list) => list.indexOf(judgePublicId) !== index)
  new Set(duplicateJudgeIds).forEach((judgePublicId) => {
    issues.push(`${getJudge(judgePublicId)?.name || '某位评审'}在本场比赛中被重复分配`)
  })
  return issues
})
const filteredJudgePool = computed(() => {
  const query = judgeKeyword.value.toLowerCase()
  const pool = judgePool.value.filter((judge) => {
    const matchesKeyword = !query || [
      judge.name,
      judge.maskedPhone,
      judge.qualification,
      judge.breweryConflictText,
      judge.phoneConflictText,
      judge.phoneConflictBreweryName,
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query))
    if (!matchesKeyword) return false

    if (usesRoundJudgePool.value) return true
    return judgeRoleFilter.value === 'ALL'
      || (judgeRoleFilter.value === 'UNASSIGNED' && !isAssigned(judge.publicId))
  })
  if (!usesRoundJudgePool.value) return pool
  return [...pool].sort(compareRoundJudgePoolOrder)
})
const feedbackStatusOptions = [
  { value: 'all', label: '全部' },
  { value: 'comment_missing', label: '待评价' },
  { value: 'awaiting_captain', label: '待汇总' },
]
const feedbackJudgeTypeOptions = [
  { value: '全部', label: '全部' },
  { value: 'PROFESSIONAL', label: '专业评审' },
  { value: 'CROSS', label: '跨界评审' },
]
const feedbackTableOptions = computed(() => buildFeedbackOptions(feedbackReviewEntries.value.map((entry) => entry.tableName)))
const feedbackCategoryOptions = computed(() => buildFeedbackOptions(feedbackReviewEntries.value.map((entry) => entry.categoryName)))
const feedbackFilteredEntries = computed(() => {
  const keyword = feedbackFilters.keyword.trim().toLowerCase()
  return feedbackReviewEntries.value.filter((entry) => {
    if (feedbackFilters.tableName !== '全部' && entry.tableName !== feedbackFilters.tableName) return false
    if (feedbackFilters.categoryName !== '全部' && entry.categoryName !== feedbackFilters.categoryName) return false
    if (feedbackFilters.status !== 'all' && entry.status !== feedbackFilters.status) return false
    if (feedbackFilters.judgeType !== '全部' && !(entry.judges || []).some((judge) => judge.role === feedbackFilters.judgeType)) return false
    if (!keyword) return true
    return [
      entry.entryName,
      entry.labelCode,
      entry.shortCode,
      entry.beerUuid,
      entry.categoryName,
      entry.style,
    ].filter(Boolean).some((value) => String(value).toLowerCase().includes(keyword))
  })
})
const selectedFeedbackEntry = computed(() => {
  const selected = feedbackFilteredEntries.value.find((entry) => feedbackEntryKey(entry) === selectedFeedbackEntryKey.value)
  return selected || feedbackFilteredEntries.value[0] || null
})
const selectedFeedbackIssues = computed(() => buildFeedbackIssues(selectedFeedbackEntry.value))
const feedbackEditorTitle = computed(() => (feedbackEditor.type === 'captain' ? '编辑桌长综合评语' : '编辑评审评语'))
const feedbackEditorSubtitle = computed(() => {
  if (feedbackEditor.type === 'captain') return isFeedbackOnlyCompetition.value ? '只修改文字，不调整共识分' : '只修改文字，不调整共识分和晋级状态'
  return '只修改维度备注，不调整评分'
})
const feedbackEditorEffectiveText = computed(() => {
  if (feedbackEditor.type === 'captain') return feedbackEditor.comments
  return feedbackEditor.dimensions.map((item) => item.note || '').join('')
})
const feedbackEditorEffectiveLength = computed(() => countEffectiveChars(feedbackEditorEffectiveText.value))
const canSubmitFeedbackEditor = computed(() => (
  feedbackEditor.visible
  && !feedbackEditor.saving
  && feedbackEditor.scoreRecordId
  && feedbackEditorEffectiveLength.value > 0
  && String(feedbackEditor.reason || '').trim().length > 0
))
const sponsorLogoCropGeometry = computed(() => {
  const imageWidth = Number(sponsorLogoCrop.imageWidth) || 1
  const imageHeight = Number(sponsorLogoCrop.imageHeight) || 1
  const imageRatio = imageWidth / imageHeight
  const frameRatio = sponsorLogoCropOutputWidth / sponsorLogoCropOutputHeight
  const baseWidthPercent = imageRatio >= frameRatio ? (imageRatio / frameRatio) * 100 : 100
  const baseHeightPercent = imageRatio >= frameRatio ? 100 : (frameRatio / imageRatio) * 100
  const widthPercent = baseWidthPercent * sponsorLogoCrop.scale
  const heightPercent = baseHeightPercent * sponsorLogoCrop.scale
  return {
    widthPercent,
    heightPercent,
    maxX: Math.max(0, (widthPercent - 100) / 2),
    maxY: Math.max(0, (heightPercent - 100) / 2),
  }
})
const sponsorLogoCropImageStyle = computed(() => ({
  width: `${sponsorLogoCropGeometry.value.widthPercent}%`,
  height: `${sponsorLogoCropGeometry.value.heightPercent}%`,
  left: `calc(50% + ${sponsorLogoCrop.x}%)`,
  top: `calc(50% + ${sponsorLogoCrop.y}%)`,
}))

onMounted(() => {
  loadStyleLibraries()
  loadDetail()
  loadJudgePool()
  nextTick(setupStyleDistributionObserver)
  roundProgressPollTimer = window.setInterval(refreshRoundProgress, roundProgressPollIntervalMs)
})
onUnmounted(() => {
  if (roundProgressPollTimer) window.clearInterval(roundProgressPollTimer)
  clearRoundTableNameAutoSave()
  if (styleDistributionResizeObserver) styleDistributionResizeObserver.disconnect()
  closeSponsorLogoCrop()
})
watch(() => route.params.id, loadDetail)
watch(() => route.query.tab, (tab) => {
  const nextTab = normalizeDetailTab(tab)
  if (nextTab !== activeTab.value) {
    activeTab.value = competition.value && resolveDetailTabDisabledReason(nextTab) ? 'overview' : nextTab
  }
})
watch(activeTab, (tab) => {
  syncActiveTabQuery(tab)
  if (tab === 'rounds') refreshRoundProgress()
  if (tab === 'feedback') loadFeedbackReview()
  if (tab === 'analysis') loadCompetitionAnalytics()
}, { immediate: true })
watch(() => competition.value?.id, () => {
  if (activeTab.value === 'feedback') loadFeedbackReview()
  if (activeTab.value === 'analysis') loadCompetitionAnalytics(true)
})
watch(() => currentRound.value?.status, () => {
  if (activeTab.value === 'rounds') refreshRoundProgress()
})
watch([
  () => judgeTableForm.map((table) => `${table.localId}:${table.tableName}`).join('|'),
  () => judgeAssignmentForm.map((assignment) => `${assignment.localId}:${assignment.tableLocalId}:${assignment.judgePublicId}:${assignment.role}`).join('|'),
], () => {
  if (!rounds.value.some((round) => round.roundNo === 1)) syncFirstRoundDraftTables()
})
watch(entryAutoAssignStats, (stats) => {
  const maxQuantity = Math.max(stats.unassigned, 1)
  if (Number(entryAutoAssignForm.quantity || 1) > maxQuantity) {
    entryAutoAssignForm.quantity = maxQuantity
  }
})
watch([styleCategorySummary, activeTab], () => {
  nextTick(() => {
    if (!styleDistributionResizeObserver && styleDistributionListRef.value) {
      setupStyleDistributionObserver()
      return
    }
    updateStyleDistributionOverflow()
  })
}, { immediate: true })
watch(feedbackFilteredEntries, (entries) => {
  if (!entries.length) {
    selectedFeedbackEntryKey.value = ''
    return
  }
  if (!entries.some((entry) => feedbackEntryKey(entry) === selectedFeedbackEntryKey.value)) {
    selectedFeedbackEntryKey.value = feedbackEntryKey(entries[0])
  }
})
watch(() => sponsorLogoCrop.scale, clampSponsorLogoCropOffset)
async function loadDetail() {
  loading.value = true
  try {
    const [data, sponsors] = await Promise.all([
      fetchCompetitionDetail(route.params.id),
      fetchCompetitionSponsors(route.params.id),
    ])
    competition.value = normalizeDetail({ ...data, sponsors })
    competitionAnalytics.value = null
    competitionAnalyticsCompetitionId.value = null
    resetForms()
    applyRoundState()
    ensureActiveTabAvailable()
    if (activeTab.value === 'analysis') {
      await loadCompetitionAnalytics(true)
    }
  } finally {
    loading.value = false
  }
}

async function refreshRoundProgress() {
  if (!shouldRefreshRoundProgress()) return
  if (roundProgressRefreshing) return
  roundProgressRefreshing = true
  const preferredRoundId = activeRoundId.value
  const preferredTableId = selectedRoundTableId.value
  try {
    const data = await fetchCompetitionDetail(route.params.id)
    competition.value = normalizeDetail({ ...data, sponsors: competition.value?.sponsors || [] })
    resetForms()
    applyRoundState(preferredRoundId, { preferredTableId, keepEntrySelection: true })
    ensureActiveTabAvailable()
  } catch {
    // 现场进度静默刷新失败时保留当前页面状态
  } finally {
    roundProgressRefreshing = false
  }
}

function shouldRefreshRoundProgress() {
  if (loading.value || activeTab.value !== 'rounds' || !competition.value) return false
  if (createRoundDialogOpen.value || entryAutoAssignDialogOpen.value || roundScoreDetailDialogOpen.value) return false
  if (typeof document !== 'undefined' && document.hidden) return false
  return currentRound.value?.type === 'SCORE' && currentRound.value?.status === 'PUBLISHED'
}

function normalizeDetailTab(tab) {
  const value = Array.isArray(tab) ? tab[0] : tab
  if (value === 'progress') return 'rounds'
  return detailTabKeys.has(value) ? value : 'overview'
}

function syncActiveTabQuery(tab) {
  if (normalizeDetailTab(route.query.tab) === tab && route.query.tab === tab) return
  router.replace({
    query: {
      ...route.query,
      tab,
    },
  }).catch(() => {})
}

async function loadJudgePool() {
  try {
    const data = await fetchJudges()
    judgePool.value = data || []
  } catch {
    judgePool.value = []
  }
}

async function loadFeedbackReview(force = false) {
  const competitionId = competition.value?.id || route.params.id
  if (!competitionId) return
  if (!force && feedbackReviewCompetitionId.value === competitionId && feedbackReviewEntries.value.length) return
  if (feedbackReviewLoading.value) return
  feedbackReviewLoading.value = true
  try {
    const data = await fetchCompetitionFeedbackReview(competitionId)
    feedbackReviewEntries.value = data || []
    feedbackReviewCompetitionId.value = competitionId
    if (!feedbackReviewEntries.value.some((entry) => feedbackEntryKey(entry) === selectedFeedbackEntryKey.value)) {
      selectedFeedbackEntryKey.value = feedbackEntryKey(feedbackReviewEntries.value[0])
    }
  } catch {
    feedbackReviewEntries.value = []
    feedbackReviewCompetitionId.value = competitionId
    ElMessage.error('反馈数据加载失败')
  } finally {
    feedbackReviewLoading.value = false
  }
}

async function loadCompetitionAnalytics(force = false) {
  const competitionId = competition.value?.id || route.params.id
  if (!competitionId) return
  if (!force && competitionAnalyticsCompetitionId.value === competitionId && competitionAnalytics.value) return
  if (competitionAnalyticsLoading.value) return
  competitionAnalyticsLoading.value = true
  try {
    const data = await fetchCompetitionAnalytics(competitionId)
    competitionAnalytics.value = data || null
    competitionAnalyticsCompetitionId.value = competitionId
  } catch {
    competitionAnalytics.value = null
    ElMessage.error('数据分析加载失败')
  } finally {
    competitionAnalyticsLoading.value = false
  }
}

function buildFeedbackOptions(values) {
  return ['全部', ...[...new Set(values.filter(Boolean))].sort((a, b) => String(a).localeCompare(String(b), 'zh-CN'))]
}

function feedbackEntryKey(entry) {
  if (!entry) return ''
  return `${entry.roundTableId || 'table'}-${entry.beerUuid || entry.beerEntryId || ''}`
}

function formatFeedbackEntryTitle(entry) {
  if (!entry) return ''
  const name = entry.entryName || entry.labelCode || entry.beerUuid || '未命名酒款'
  const code = entry.shortCode || entry.labelCode || entry.beerUuid || ''
  return code && code !== name ? `${name} · ${code}` : name
}

function feedbackRoleLabel(role) {
  if (role === 'CAPTAIN') return '桌长个人评分'
  return roleLabels[role] || '评审'
}

function feedbackJudgeAnomalyLabel(anomaly) {
  const labels = {
    not_submitted: '未提交',
    comment_missing: '评价缺失',
  }
  return labels[anomaly] || ''
}

function formatJudgeDuration(value) {
  const seconds = Number(value || 0)
  if (!seconds) return ''
  const totalSeconds = Math.max(0, Math.floor(seconds))
  if (totalSeconds >= 3600) {
    const hours = Math.floor(totalSeconds / 3600)
    const minutes = Math.floor((totalSeconds % 3600) / 60)
    return `${hours}小时${minutes}分`
  }
  const minutes = Math.floor(totalSeconds / 60)
  const remain = totalSeconds % 60
  if (minutes > 0) {
    return `${minutes}分${remain}秒`
  }
  return `${remain}秒`
}

function formatFeedbackTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

function countEffectiveChars(text) {
  return String(text || '').replace(/\s+/g, '').length
}

function resetFeedbackEditor() {
  feedbackEditor.visible = false
  feedbackEditor.saving = false
  feedbackEditor.type = ''
  feedbackEditor.scoreRecordId = null
  feedbackEditor.expectedUpdatedAt = ''
  feedbackEditor.entryTitle = ''
  feedbackEditor.entryMeta = ''
  feedbackEditor.targetLabel = ''
  feedbackEditor.comments = ''
  feedbackEditor.reason = ''
  feedbackEditor.dimensions = []
}

function openFeedbackCommentEditor(type, entry, judge = null) {
  if (!entry) return
  resetFeedbackEditor()
  feedbackEditor.visible = true
  feedbackEditor.type = type
  feedbackEditor.entryTitle = formatFeedbackEntryTitle(entry)
  feedbackEditor.entryMeta = [entry.roundName || '首轮', entry.tableName, entry.categoryName, entry.style].filter(Boolean).join(' · ')
  if (type === 'captain') {
    const opinion = entry.captainOpinion || {}
    if (!opinion.scoreRecordId) {
      ElMessage.warning('桌长尚未提交最终意见')
      resetFeedbackEditor()
      return
    }
    feedbackEditor.scoreRecordId = opinion.scoreRecordId
    feedbackEditor.expectedUpdatedAt = opinion.updatedAt || opinion.submittedAt || ''
    feedbackEditor.targetLabel = `桌长 ${opinion.captainName || '未知桌长'}`
    feedbackEditor.comments = opinion.comments || ''
    return
  }
  if (!judge?.scoreRecordId) {
    ElMessage.warning('该评审尚未提交评分')
    resetFeedbackEditor()
    return
  }
  feedbackEditor.scoreRecordId = judge.scoreRecordId
  feedbackEditor.expectedUpdatedAt = judge.updatedAt || judge.submittedAt || ''
  feedbackEditor.targetLabel = `${judge.judgeName || '未知评审'} · ${judge.roleLabel || feedbackRoleLabel(judge.role)}`
  feedbackEditor.dimensions = (judge.dimensions || []).map((dimension) => ({
    key: dimension.key,
    label: dimension.label,
    score: dimension.score,
    maxScore: dimension.maxScore,
    note: dimension.note || '',
  }))
  feedbackEditor.comments = buildFeedbackEditorComments()
}

function closeFeedbackCommentEditor() {
  if (feedbackEditor.saving) return
  resetFeedbackEditor()
}

function buildFeedbackEditorComments() {
  if (feedbackEditor.type === 'captain') return String(feedbackEditor.comments || '').trim()
  return feedbackEditor.dimensions
    .map((dimension) => ({
      label: dimension.label || dimension.key,
      note: String(dimension.note || '').trim(),
    }))
    .filter((dimension) => dimension.note)
    .map((dimension) => `${dimension.label}：${dimension.note}`)
    .join('\n')
}

async function submitFeedbackCommentEdit() {
  if (!canSubmitFeedbackEditor.value) return
  const competitionId = competition.value?.id || route.params.id
  if (!competitionId) return
  feedbackEditor.saving = true
  const payload = {
    comments: buildFeedbackEditorComments(),
    reason: feedbackEditor.reason,
    expectedUpdatedAt: feedbackEditor.expectedUpdatedAt || null,
  }
  if (feedbackEditor.type === 'judge') {
    payload.dimensionNotes = feedbackEditor.dimensions.map((dimension) => ({
      key: dimension.key,
      note: String(dimension.note || '').trim(),
    }))
  }
  try {
    await updateCompetitionFeedbackComment(competitionId, feedbackEditor.scoreRecordId, payload)
    ElMessage.success('评价已保存')
    resetFeedbackEditor()
    await loadFeedbackReview(true)
  } catch {
    feedbackEditor.saving = false
  }
}

function buildFeedbackIssues(entry) {
  if (!entry) return []
  const issues = []
  const missing = Math.max(0, Number(entry.personalTotal || 0) - Number(entry.personalSubmitted || 0))
  if (missing > 0) issues.push({ severity: 'blocking', message: `还有 ${missing} 位评审未提交个人评分` })
  if (!entry.captainOpinion?.submitted) {
    issues.push({ severity: 'blocking', message: '桌长尚未提交最终意见，发布后厂牌看不到最终反馈' })
  } else if (!String(entry.captainOpinion.comments || '').trim()) {
    issues.push({ severity: 'blocking', message: '桌长综合评语为空，发布后厂牌看不到最终反馈' })
  }
  ;(entry.judges || []).forEach((judge) => {
    if (judge.anomaly === 'comment_missing') {
      issues.push({ severity: 'warning', message: `评审「${judge.judgeName}」评价内容缺失` })
    }
  })
  return issues
}

function formatCommentCount(value) {
  const count = Number(value || 0)
  return count > 0 ? `${count}字` : '—'
}

function normalizeDetail(data) {
  return {
    ...data,
    date: data.competitionDate,
    categories: data.categories || [],
    styles: data.styles || [],
    entryFields: data.entryFields || [],
    judgeTables: data.judgeTables || [],
    scoreConfigs: data.scoreConfigs || [],
    checks: data.checks || [],
    alerts: data.alerts || [],
    dataIntegrityIssues: data.dataIntegrityIssues || [],
    entries: data.entries || [],
    entryPool: data.entryPool || data.entries || [],
    rounds: data.rounds || [],
    resultDrafts: data.resultDrafts || [],
    awardRules: data.awardRules || [],
    awardResults: data.awardResults || [],
    sponsors: data.sponsors || [],
    editableScopes: data.editableScopes || {},
    entriesSummary: data.entriesSummary || { total: 0, pendingPayment: 0, registered: 0, stored: 0, canceled: 0, resultPublished: 0 },
    progressSummary: data.progressSummary || { finalized: 0, total: 0, advanced: 0, commentWarnings: 0 },
    resultSetup: data.resultSetup || { awardsReady: false, published: false, championReady: false },
  }
}

function resetForms() {
  const logistics = competition.value.logistics || {}
  Object.assign(baseForm, {
    name: competition.value.name || '',
    code: competition.value.code || '',
    competitionDate: toInputDate(competition.value.competitionDate || competition.value.date),
    registrationStart: toInputDateTime(competition.value.registrationStart),
    registrationDeadline: toInputDateTime(competition.value.registrationDeadline),
    entryFee: competition.value.entryFee ?? 0,
    earlyBirdFee: competition.value.earlyBirdFee ?? '',
    earlyBirdDeadline: toInputDateTime(competition.value.earlyBirdDeadline),
    description: competition.value.description || '',
    rulesUrl: competition.value.rulesUrl || '',
    deliveryMethod: logistics.deliveryMethod || 'BOTH',
    sampleArrivalStart: toInputDateTime(logistics.sampleArrivalStart),
    sampleArrivalDeadline: toInputDateTime(logistics.sampleArrivalDeadline),
    sampleQuantityNote: logistics.sampleQuantityNote || '',
    deliveryRecipient: logistics.deliveryRecipient || '',
    deliveryPhone: logistics.deliveryPhone || '',
    deliveryAddress: logistics.deliveryAddress || '',
    deliveryNote: logistics.deliveryNote || '',
    logisticsVisibility: logistics.logisticsVisibility || 'PAYMENT_CONFIRMED',
  })
  sponsorForm.splice(0, sponsorForm.length, ...(competition.value.sponsors || []).map((item, index) => ({
    ...item,
    localId: item.id ? `sponsor-${item.id}` : `sponsor-new-${index}`,
    tierLabel: item.tierLabel || '',
    sponsorName: item.sponsorName || '',
    logoAssetId: item.logoAssetId || null,
    logoUrl: item.logoUrl || '',
    sortOrder: Number(item.sortOrder ?? index),
    featured: Boolean(item.featured),
    enabled: item.enabled !== false,
    uploading: false,
  })))
  selectedStyleLibraryVersion.value = competition.value.styleLibraryVersion || ''
  categoryForm.splice(0, categoryForm.length, ...competition.value.categories.map((item) => item.name))
  entryFieldForm.splice(0, entryFieldForm.length, ...competition.value.entryFields.map((item, index) => ({
    ...item,
    localId: item.id || `field-${index}`,
  })))
  const sourceJudgeTables = competition.value.judgeTables.length
    ? competition.value.judgeTables
    : [{ tableName: 'A桌', captainCount: 0, professionalCount: 0, crossCount: 0 }]
  judgeTableForm.splice(0, judgeTableForm.length, ...sourceJudgeTables.map((item, index) => ({
    ...item,
    localId: item.id || `table-${index}`,
  })))
  const persistedAssignments = judgeTableForm.flatMap((table) => (table.assignments || []).map((assignment) => ({
    localId: `assignment-${assignment.id || `${table.localId}-${assignment.judgePublicId}`}`,
    tableLocalId: table.localId,
    tableId: table.id,
    judgePublicId: assignment.judgePublicId,
    role: assignment.role,
  })))
  judgeAssignmentForm.splice(0, judgeAssignmentForm.length, ...persistedAssignments)
  selectedTableLocalId.value = judgeTableForm[0]?.localId || null
  const sourceScoreConfigs = competition.value.scoreConfigs.length ? competition.value.scoreConfigs : defaultScoreConfigs().map((item) => ({
    judgeRoleType: item.role,
    dimensions: item.dimensions,
  }))
  scoreConfigForm.splice(0, scoreConfigForm.length, ...sourceScoreConfigs.map((item) => ({
    role: item.judgeRoleType,
    minCommentLength: Number(item.minCommentLength ?? defaultMinCommentLength(item.judgeRoleType)),
    dimensions: item.dimensions.map((dimension, index) => ({
      key: dimension.key || `${item.judgeRoleType.toLowerCase()}_${index + 1}`,
      label: dimension.label,
      maxScore: Number(dimension.maxScore || 0),
      notePrompt: dimension.notePrompt || '',
      localId: `${item.judgeRoleType}-${index}`,
    })),
  })))
}

function applyRoundState(preferredRoundId = activeRoundId.value, options = {}) {
  roundEntryPool.value = (competition.value?.entryPool || competition.value?.entries || [])
    .map((entry) => ({
      ...entry,
      categoryId: entry.categoryId ?? null,
      breweryId: entry.breweryId ?? null,
      name: entry.name || '',
      breweryCompanyName: entry.breweryCompanyName || '',
      shortCode: entry.shortCode || '-',
      categoryName: entry.categoryName || entry.category || '-',
      style: entry.style || '未填写',
      paymentStatus: entry.paymentStatus || 'UNPAID',
      refundStatus: entry.refundStatus || '',
      refundReason: entry.refundReason || '',
      refundRequestedAt: entry.refundRequestedAt || '',
      refundProcessedAt: entry.refundProcessedAt || '',
      deliveryStatus: entry.deliveryStatus || 'NOT_SUBMITTED',
      carrier: entry.carrier || '',
      trackingNo: entry.trackingNo || '',
      canConfirmPayment: Boolean(entry.canConfirmPayment),
      canMarkStored: Boolean(entry.canMarkStored),
      canCancel: Boolean(entry.canCancel),
      stored: Boolean(entry.stored),
      advanced: Boolean(entry.advanced),
      sourceTable: entry.sourceTable || '',
      sourceResult: entry.sourceResult || '',
    }))
    .sort(compareEntryDisplayPriority)
  rounds.value = (competition.value?.rounds || []).map((round) => ({
    ...round,
    tables: (round.tables || []).map((table) => ({
      ...table,
      categoryId: table.categoryId ?? null,
      categoryMode: table.categoryMode || (table.categoryId != null ? 'CATEGORY' : 'EMPTY'),
      categoryName: table.categoryName || '',
      entryUuids: table.entryUuids || [],
      members: table.members || [],
      participantPublicIds: (table.members || [])
        .filter((member) => member.role !== 'CAPTAIN')
        .map((member) => member.judgePublicId)
        .filter(Boolean),
      rankings: table.rankings || buildEmptyRankings(Number(table.targetCount || 3), table.targetMode),
    })),
  }))
  rounds.value.forEach((round) => {
    const pool = getPoolEntriesForRound(round)
    round.tables.forEach((table) => syncRoundTableScope(table, pool))
  })
  if (!rounds.value.some((round) => round.roundNo === 1)) {
    syncFirstRoundDraftTables()
  } else {
    firstRoundDraft.tables.splice(0)
  }
  const preferred = rounds.value.find((round) => round.id === preferredRoundId)
  const current = preferred || competition.value?.currentRound || rounds.value[rounds.value.length - 1] || rounds.value[0] || firstRoundDraft
  activeRoundId.value = current?.id || firstRoundDraft.id
  selectedRoundTableId.value = current?.tables?.some((table) => table.id === options.preferredTableId)
    ? options.preferredTableId
    : current?.tables?.[0]?.id || ''
  if (!options.keepEntrySelection) selectedEntryUuids.value = []
}

function syncFirstRoundDraftTables() {
  const previousByName = new Map(firstRoundDraft.tables.map((table) => [table.name, table]))
  const previousById = new Map(firstRoundDraft.tables.map((table) => [table.id, table]))
  const storedPool = roundEntryPool.value.filter((entry) => entry.stored)
  const storedUuids = new Set(storedPool.map((entry) => entry.uuid))
  const tables = judgeTableForm
    .filter((table) => table.tableName?.trim())
    .map((table, index) => {
      const id = `draft-table-${table.localId}`
      const previous = previousByName.get(table.tableName) || previousById.get(id) || firstRoundDraft.tables[index] || {}
      const captain = assignmentsForTable(table, 'CAPTAIN')[0]
      const members = assignmentsForTable(table)
        .filter((assignment) => assignment.role !== 'CAPTAIN' && assignment.judgePublicId !== captain?.judgePublicId)
        .map((assignment) => {
          const judge = getJudge(assignment.judgePublicId)
          return {
            judgePublicId: assignment.judgePublicId,
            name: judge?.name || '',
            role: assignment.role,
            roleLabel: roleLabels[assignment.role] || assignment.role,
            systemTaskRequired: true,
          }
        })
      const draftTable = {
        id,
        name: table.tableName.trim(),
        captainPublicId: captain?.judgePublicId || '',
        members: [
          ...(captain?.judgePublicId ? [{
            judgePublicId: captain.judgePublicId,
            name: getJudge(captain.judgePublicId)?.name || '',
            role: 'CAPTAIN',
            roleLabel: '桌长',
            systemTaskRequired: true,
          }] : []),
          ...members,
        ],
        participantPublicIds: members.map((member) => member.judgePublicId),
        categoryId: previous.categoryId ?? null,
        categoryMode: previous.categoryMode || 'EMPTY',
        categoryName: previous.categoryName || '',
        targetCount: Number(previous.targetCount || 3),
        targetMode: 'ADVANCE_COUNT',
        sortOrder: index,
        entryUuids: (previous.entryUuids || []).filter((uuid) => storedUuids.has(uuid)),
        rankings: previous.rankings || buildEmptyRankings(Number(previous.targetCount || 3), 'ADVANCE_COUNT'),
      }
      syncRoundTableScope(draftTable, storedPool)
      return draftTable
    })
  firstRoundDraft.tables.splice(0, firstRoundDraft.tables.length, ...tables)
  if (!rounds.value.length || activeRoundId.value === firstRoundDraft.id) {
    activeRoundId.value = firstRoundDraft.id
    if (!tables.some((table) => table.id === selectedRoundTableId.value)) {
      selectedRoundTableId.value = tables[0]?.id || ''
    }
  }
}

function entryLatestProgress(entry) {
  if (!entry) return { label: '-', tone: 'muted' }
  const refundProgress = entryRefundProgress(entry)
  if (refundProgress) return refundProgress
  if (entry.status === 'CANCELED') return { label: entryStatusLabels.CANCELED, tone: 'muted' }
  if (entry.status === 'RESULT_PUBLISHED' || resultsPublished.value) return { label: entryStatusLabels.RESULT_PUBLISHED, tone: 'success' }
  const award = getEntryAward(entry.uuid)
  if (award) return { label: award, tone: 'success' }
  const path = getLatestEntryPathText(entry.uuid)
  if (path !== '-') return { label: path, tone: 'active' }
  if (entry.deliveryStatus === 'RECEIVED' || entry.status === 'STORED' || entry.stored) return { label: '已入库', tone: 'active' }
  if (entry.deliveryStatus === 'SUBMITTED') return { label: '已提交送样', tone: 'active' }
  if (entry.paymentStatus === 'PAID' || entry.status === 'REGISTERED') return { label: '已支付', tone: 'active' }
  if (entry.status === 'PENDING_PAYMENT' || entry.paymentStatus === 'UNPAID') return { label: '待支付', tone: 'warning' }
  return { label: entryStatusLabels[entry.status] || entry.status || '-', tone: 'muted' }
}

function entryRefundProgress(entry) {
  const status = entry?.refundStatus
  if (!status) return null
  const progress = {
    REQUESTED: { label: '待退款审核', tone: 'warning' },
    APPROVED: { label: '退款处理中', tone: 'warning' },
    PROCESSING: { label: '退款处理中', tone: 'warning' },
    SUCCESS: { label: '已退款', tone: 'muted' },
    FAILED: { label: '退款失败', tone: 'warning' },
    REJECTED: { label: '退款已驳回', tone: 'muted' },
  }
  return progress[status] || { label: status, tone: 'muted' }
}

function hasRefundPriority(entry) {
  return ['REQUESTED', 'FAILED'].includes(entry?.refundStatus)
}

function isRefundedEntry(entry) {
  return entry?.refundStatus === 'SUCCESS' || entry?.paymentStatus === 'REFUNDED'
}

function compareEntryDisplayPriority(left, right) {
  const priorityDelta = entryDisplayPriority(left) - entryDisplayPriority(right)
  if (priorityDelta !== 0) return priorityDelta
  return Number(right?.id || 0) - Number(left?.id || 0)
}

function entryDisplayPriority(entry) {
  if (hasRefundPriority(entry)) return 0
  if (isRefundedEntry(entry)) return 2
  return 1
}

function getEntryPathText(uuid) {
  const path = getEntryPath(uuid)
  return path === '-' ? '未分配' : path
}

function getLatestEntryPathText(uuid) {
  const path = getEntryPath(uuid)
  if (path === '-') return '-'
  const parts = path.split(' -> ').map((item) => item.trim()).filter(Boolean)
  return parts.at(-1) || path
}

async function loadStyleLibraries() {
  try {
    const data = await fetchStyleLibraries()
    styleLibraryOptions.value = normalizeStyleLibraries(data)
  } catch {
    styleLibraryOptions.value = normalizeStyleLibraries(fallbackStyleLibraries)
  }
}

async function ensureFirstRoundDraft(options = {}) {
  const existingRound = rounds.value.find((round) => Number(round.roundNo) === 1)
  if (existingRound) {
    if (activeRoundId.value === firstRoundDraft.id || options.activate) {
      applyRoundState(existingRound.id, { keepEntrySelection: true })
    }
    return existingRound
  }
  syncFirstRoundDraftTables()
  if (!options.allowIncomplete) {
    if (validationIssues.value.length) {
      if (!options.silent) ElMessage.warning(`还有 ${validationIssues.value.length} 项评审配置需要处理`)
      return null
    }
    if (roundValidationIssues.value.length) {
      if (!options.silent) ElMessage.warning(roundValidationIssues.value[0])
      return null
    }
  }
  if (!firstRoundDraft.tables.length) {
    if (!options.silent) ElMessage.warning('请先创建至少 1 张评审桌')
    return null
  }
  const payload = buildRoundAllocationPayload(firstRoundDraft)
  if (canEditBaseJudgeTables.value) {
    const saved = await saveJudgeDraft({ silent: true, allowIncomplete: Boolean(options.allowIncomplete) })
    if (!saved) return null
  }
  const detail = await createFirstRound(competition.value.id, {
    allocationStrategy: 'EVEN_SPLIT',
    defaultTargetCount: 3,
    tables: payload.tables,
  })
  const preferredMode = options.preferredMode || allocationMode.value
  competition.value = normalizeDetail(detail)
  resetForms()
  const createdRound = competition.value.rounds?.find((round) => Number(round.roundNo) === 1)
  applyRoundState(createdRound?.id, { keepEntrySelection: true })
  activeTab.value = 'judges'
  allocationMode.value = preferredMode
  if (!options.silent) ElMessage.success('首轮草稿已保存，可以继续调整分桌')
  return rounds.value.find((round) => Number(round.roundNo) === 1) || createdRound || null
}

async function generateFirstRoundFromJudges() {
  await ensureFirstRoundDraft({ preferredMode: 'entries' })
}

function resolveRegistrationWindowInfo() {
  const status = competition.value?.status
  const now = Date.now()
  const deadline = parseDateTime(competition.value?.registrationDeadline)
  if (status === 'DRAFT') return { label: '未发布', tone: 'neutral', detail: '开放报名后厂牌可立即提交' }
  if (status === 'REGISTRATION_OPEN') {
    if (deadline && now > deadline) return { label: '已过截止', tone: 'warning', detail: '已到报名截止时间，请结束报名' }
    return { label: '报名中', tone: 'success', detail: deadline ? `将于 ${formatDateTime(competition.value.registrationDeadline)} 自动截止` : '正在接受报名' }
  }
  if (status === 'REGISTRATION_CLOSED') return { label: '已截止', tone: 'warning', detail: '可进入评审准备' }
  return { label: '报名关闭', tone: 'neutral', detail: '报名窗口已结束' }
}

function buildHeaderMetaItems() {
  if (!competition.value) return []
  const status = competition.value.status
  const items = []
  const deadline = competition.value.registrationDeadline
  const date = competition.value.date || competition.value.competitionDate

  if (status === 'REGISTRATION_OPEN') {
    items.push({ key: 'registrationDeadline', label: deadline ? `报名截止 ${formatDateTime(deadline)}` : '报名截止待设置' })
  } else if (status === 'DRAFT') {
    items.push({ key: 'registrationWindow', label: '报名未发布' })
  } else if (status === 'REGISTRATION_CLOSED') {
    items.push({ key: 'registrationWindow', label: '报名已截止' })
  }

  if (currentRound.value && !currentRound.value.isPreparationDraft) {
    items.push({ key: 'currentRound', label: currentRound.value.name })
  }

  if (date) {
    items.push({ key: 'competitionDate', label: `比赛日 ${formatDate(date)}` })
  }

  return items
}

function parseDateTime(value) {
  if (!value) return null
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? null : time
}

function toInputDate(value) {
  return value ? String(value).slice(0, 10) : ''
}

function toInputDateTime(value) {
  if (!value) return ''
  return String(value).replace(' ', 'T').slice(0, 16)
}

function toBackendDateTime(value) {
  if (!value) return null
  return value.length === 16 ? `${value}:00` : value
}

function isValidHttpUrl(value) {
  return /^https?:\/\//i.test(String(value || '').trim())
}

function toChineseNumber(value) {
  const digits = ['', '一', '二', '三', '四', '五', '六', '七', '八', '九']
  const number = Number(value)
  if (!Number.isFinite(number) || number <= 0) return String(value)
  if (number < 10) return digits[number]
  if (number === 10) return '十'
  if (number < 20) return `十${digits[number % 10]}`
  if (number < 100) return `${digits[Math.floor(number / 10)]}十${digits[number % 10]}`
  return String(number)
}

function resolveStagePrimaryAction() {
  if (!competition.value) return { text: '加载中', enabled: false, action: 'noop' }
  if (competition.value.status === 'DRAFT') {
    return { text: '发布报名', enabled: true, action: 'publishRegistration' }
  }
  if (competition.value.status === 'REGISTRATION_OPEN') {
    return { text: '截止报名', enabled: true, action: 'closeRegistration' }
  }
  if (competition.value.status === 'REGISTRATION_CLOSED') {
    return { text: '完成样品入库核对，进入评审准备中', enabled: true, action: 'prepareJudging' }
  }
  if (competition.value.status === 'JUDGING_PREP' && !rounds.value.length) {
    return { text: '安排首轮', enabled: true, action: 'goToRoundAllocation' }
  }
  if (canLockCurrentDraftSourceRound.value) {
    return { text: '确认锁定上一轮', enabled: true, action: 'lockSourceRound' }
  }
  if (currentRound.value?.type === 'RANKING' && currentRound.value?.status === 'DRAFT' && currentRound.value?.sourceLocked && !currentRound.value?.candidatesSynced) {
    return { text: '同步候选酒款', enabled: true, action: 'syncCandidates' }
  }
  if (currentRound.value?.status === 'DRAFT') {
    return {
      text: currentRound.value.type === 'RANKING' ? '发布给桌长和参与评审' : '发布当前轮次',
      enabled: canPublishCurrentRound.value,
      disabledReason: roundPublishActionTitle.value,
      action: 'publishCurrentRound',
    }
  }
  if (currentRound.value?.status === 'SUBMITTED') {
    return { text: currentRound.value.type === 'SCORE' ? '确认首轮并锁定' : '确认排序并锁定', enabled: true, action: 'lockCurrentRound' }
  }
  if (currentRound.value?.status === 'LOCKED' && currentRoundIsTerminal.value) {
    return { text: '去确认结果', enabled: true, action: 'goToResults' }
  }
  if (canCreateNextRound.value) {
    return { text: createNextRoundButtonText.value, enabled: true, action: 'createNextRound' }
  }
  return { text: '打开投屏看板', enabled: true, action: 'openLiveDashboard' }
}

function resolveStageSecondaryActions() {
  const actions = [
    { key: 'rounds', label: '进入轮次编排', handler: () => handleDetailTabChange('rounds') },
    { key: 'export', label: '导出数据', handler: () => downloadScoringData() },
  ]
  if (competition.value?.status === 'REGISTRATION_CLOSED') {
    actions.push({
      key: 'reopenRegistration',
      label: '重新开放报名',
      handler: () => reopenRegistrationAction(),
    })
  }
  if (canReturnToSampleCheck.value) {
    actions.push({
      key: 'returnToSampleCheck',
      label: '退回样品入库核对',
      handler: () => returnToSampleCheckAction(),
    })
  }
  if (competition.value?.status !== 'ARCHIVED') {
    actions.push({
      key: 'delete',
      label: competition.value?.status === 'DRAFT' ? '删除/归档草稿' : '归档比赛',
      danger: true,
      handler: () => handleDeleteCompetition(),
    })
  }
  return actions
}

function buildOverviewActionItems() {
  const issues = []
  if (hasDataIssues.value) {
    return competition.value.dataIntegrityIssues.map((text, index) => ({ key: `data-${index}`, level: 'danger', text, targetTab: 'overview' }))
  }
  if (roundValidationIssues.value.length) {
    issues.push(...roundValidationIssues.value.slice(0, 3).map((text, index) => ({ key: `round-${index}`, level: 'warning', text, targetTab: 'rounds' })))
  }
  if (competition.value?.entriesSummary?.pendingPayment > 0) {
    issues.push({ key: 'payment', level: 'warning', text: `还有 ${competition.value.entriesSummary.pendingPayment} 款酒等待支付`, targetTab: 'entries' })
  }
  return issues
}

function buildFutureStageTasks() {
  const feedbackOnly = isFeedbackOnlyCompetition.value
  const tasks = [
    { key: 'storedEntries', label: '样品入库', targetTab: 'entries', detail: '报名酒款到场后确认入库状态', state: 'done', statusText: '已完成' },
  ]
  rounds.value.forEach((round) => {
    tasks.push({
      key: `${round.id}-setup`,
      label: `${round.name}编排`,
      targetTab: 'rounds',
      roundId: round.id,
      detail: round.type === 'SCORE'
        ? (feedbackOnly ? '确认桌次、酒款和诊断任务' : '确认桌次、酒款和晋级数量')
        : '确认桌长、酒款和排序名额',
      state: round.status === 'LOCKED' ? 'done' : 'pending',
      statusText: roundStatusLabels[round.status] || round.status,
    })
    tasks.push({
      key: `${round.id}-work`,
      label: round.type === 'SCORE' ? `${round.name}评审` : `${round.name}排序`,
      targetTab: 'rounds',
      roundId: round.id,
      detail: round.type === 'SCORE' ? '评审评分与桌长汇总' : '桌长提交排序后再确认锁定',
      state: round.status === 'LOCKED' ? 'done' : 'pending',
      statusText: round.status === 'LOCKED' ? '已完成' : '待处理',
    })
  })
  if (canCreateNextRound.value) {
    const lastRound = rounds.value[rounds.value.length - 1]
    const creatingFinal = isMedalRound(lastRound)
    const earlyDraft = createNextRoundIsEarlyDraft.value
    tasks.push({
      key: 'createNextRound',
      label: earlyDraft ? `提前创建${nextRoundName.value}草稿` : `创建${nextRoundName.value}`,
      targetTab: 'rounds',
      detail: creatingFinal
        ? '使用各组别金奖创建决赛桌'
        : (earlyDraft ? '先安排下一轮桌次和人员，候选酒款锁定后同步' : '使用晋级酒款创建排序草稿'),
      state: 'pending',
      statusText: creatingFinal ? '总冠军 1 名' : (earlyDraft && !advancedPool.value.length ? '候选待同步' : `${advancedPool.value.length} 款`),
      action: 'createNextRound',
    })
  }
  tasks.push({
    key: 'resultSetup',
    label: feedbackOnly ? '诊断发布' : '奖项确认',
    targetTab: 'results',
    detail: feedbackOnly ? '发布评分和桌长综合诊断' : '确认组别奖项和发布反馈',
    state: resultChecks.value.every((item) => item.done) ? 'done' : 'pending',
    statusText: resultChecks.value.every((item) => item.done) ? '可发布' : '待确认',
  })
  return tasks
}

function handleFutureTask(task) {
  if (task.action === 'createNextRound') {
    openCreateRoundDialog()
    return
  }
  if (task.roundId) selectRound(task.roundId)
  handleDetailTabChange(task.targetTab)
}

function selectRound(roundId) {
  activeRoundId.value = roundId
  const round = rounds.value.find((item) => item.id === roundId) || (roundId === firstRoundDraft.id ? firstRoundDraft : null)
  selectedRoundTableId.value = round?.tables[0]?.id || ''
  selectedEntryUuids.value = []
  closeEntryAutoAssignDialog()
}

function ensureActiveTabAvailable() {
  if (!competition.value) return
  if (resolveDetailTabDisabledReason(activeTab.value)) {
    activeTab.value = 'overview'
  }
}

function resolveDetailTabDisabledReason(tabKey) {
  if (!competition.value) return ''
  const status = competition.value.status
  const hasEntries = Number(competition.value.entriesSummary?.total || 0) > 0
  const hasFeedback = feedbackReviewEntries.value.length > 0
  const hasAwards = awardDrafts.value.length > 0

  if (tabKey === 'overview' || tabKey === 'baseInfo' || tabKey === 'entryConfig' || tabKey === 'score') {
    return ''
  }
  if (tabKey === 'entries') {
    return status === 'DRAFT' ? '报名发布后才会产生参赛酒款，草稿阶段先完成报名配置' : ''
  }
  if (tabKey === 'judges') {
    if (status === 'DRAFT') return '草稿阶段先完成报名配置并发布报名，避免提前分配不存在或未确认的酒款'
    return ''
  }
  if (tabKey === 'rounds') {
    if (status === 'DRAFT') return '草稿阶段不能编排正式轮次，请先发布报名'
    if (status === 'REGISTRATION_CLOSED' && !hasEntries) return '当前还没有参赛酒款，无法编排轮次'
    return ''
  }
  if (tabKey === 'feedback') {
    if (!['JUDGING', 'RESULT_CONFIRMING', 'PUBLISHED', 'ARCHIVED'].includes(status) && !hasFeedback) {
      return '评审开始并产生评分反馈后才能查看评价'
    }
    return ''
  }
  if (tabKey === 'results') {
    if (isFeedbackOnlyCompetition.value) {
      if (['PUBLISHED', 'ARCHIVED'].includes(status) || finalRoundLocked.value || canPublishResults.value) return ''
      return '首轮锁定后才能发布诊断结果'
    }
    if (!['RESULT_CONFIRMING', 'PUBLISHED', 'ARCHIVED'].includes(status) && !hasAwards) {
      return '结果发布需等评审轮次锁定并进入结果确认阶段'
    }
    return ''
  }
  return ''
}

async function handleDetailTabChange(nextTab) {
  if (nextTab === activeTab.value) return
  const disabledReason = resolveDetailTabDisabledReason(nextTab)
  if (disabledReason) {
    ElMessage.info(disabledReason)
    return
  }
  if (activeTab.value === 'judges') {
    const saved = await autoSaveAllocationDraft(allocationMode.value)
    if (!saved) return
  }
  activeTab.value = nextTab
}

async function handleAllocationModeChange(nextMode) {
  if (nextMode === allocationMode.value) return
  const saved = await autoSaveAllocationDraft(allocationMode.value)
  if (!saved) return
  allocationMode.value = nextMode
}

async function autoSaveAllocationDraft(mode = allocationMode.value) {
  if (allocationDraftSaving.value || activeTab.value !== 'judges' || !competition.value || !currentRound.value) return true
  if (mode !== 'judges' && mode !== 'entries') return true
  allocationDraftSaving.value = true
  try {
    if (mode === 'judges') {
      if (currentRound.value.type === 'RANKING' && currentRound.value.status === 'DRAFT') {
        await persistCurrentRoundAllocation()
      } else if (currentRound.value.type === 'SCORE' && !currentRound.value.isPreparationDraft && currentRound.value.status === 'DRAFT') {
        await persistCurrentRoundAllocation()
      } else if (canEditBaseJudgeTables.value) {
        if (currentRound.value.isPreparationDraft) syncFirstRoundDraftTables()
        await saveJudgeDraft({ silent: true, allowIncomplete: true })
      }
    } else if (mode === 'entries') {
      if (currentRound.value.isPreparationDraft) {
        syncFirstRoundDraftTables()
        if (firstRoundDraft.tables.length) {
          await ensureFirstRoundDraft({ silent: true, allowIncomplete: true, preferredMode: allocationMode.value })
        }
      } else {
        await persistCurrentRoundAllocation()
      }
    }
    return true
  } catch (error) {
    ElMessage.error('草稿自动保存失败，请稍后重试')
    return false
  } finally {
    allocationDraftSaving.value = false
  }
}

function publishRoundById(roundId) {
  selectRound(roundId)
  publishCurrentRound()
}

function canSubmitRound(round) {
  if (!round || round.type !== 'RANKING') return false
  return round.tables.every((table) => table.targetMode === 'MEDALS'
    ? getFilledRankingCount(table) > 0
    : getFilledRankingCount(table) === Number(table.targetCount || 0))
}

function buildRoundReadinessChecks() {
  if (!currentRound.value) return []
  const tableCount = currentRoundTables.value.length
  const captainCount = currentRoundTables.value.filter((table) => table.captainPublicId).length
  const assignedEntryCount = currentRoundEntryCount.value
  const totalEntryCount = currentPoolEntries.value.length
  const unassignedCount = Math.max(totalEntryCount - assignedEntryCount, 0)
  const invalidTableCount = currentRoundTables.value.filter((table) => getRoundTableIssues(table).length).length
  const checks = [
    { label: '桌次', value: `${tableCount} 桌`, done: tableCount > 0 },
    { label: '桌长', value: `${captainCount} / ${tableCount}`, done: tableCount > 0 && captainCount === tableCount },
    { label: '酒款', value: `${assignedEntryCount} / ${totalEntryCount}`, done: totalEntryCount > 0 && assignedEntryCount === totalEntryCount },
    { label: '未分配', value: `${unassignedCount} 款`, done: unassignedCount === 0 },
    { label: currentRoundTargetLabel.value, value: currentRoundTargetDisplay.value, done: currentRoundTargetCount.value > 0 },
  ]
  if (invalidTableCount) checks.push({ label: '桌次问题', value: `${invalidTableCount} 桌`, done: false })
  return checks
}

function buildFirstRoundCompletionStatus() {
  const tables = currentRoundTables.value
  const tableCount = tables.length
  const entryCount = currentRoundEntryCount.value
  const advancedCount = tables.reduce((sum, table) => sum + Number(table.advancedCount || 0), 0)
  const base = { ready: false, hint: '等待首轮汇总完成', tableCount, entryCount, advancedCount }
  if (!currentRound.value || currentRound.value.roundNo !== 1 || currentRound.value.type !== 'SCORE' || currentRound.value.status !== 'PUBLISHED') return base
  if (!tableCount) return { ...base, hint: '还没有桌次' }
  if (!entryCount) return { ...base, hint: '还没有酒款' }
  const scoringPending = tables.filter((table) => normalizeProgress(table.judgeProgress) < 100).length
  if (scoringPending) return { ...base, hint: `${scoringPending} 桌还在评分` }
  const captainPending = tables.filter((table) => normalizeProgress(table.captainProgress) < 100).length
  if (captainPending) return { ...base, hint: `${captainPending} 桌还未汇总` }
  if (isFeedbackOnlyCompetition.value) return { ...base, ready: true, hint: '全部桌次已完成诊断' }
  const advancePending = tables.filter((table) => {
    const target = Number(table.targetCount || 0)
    return target <= 0 || Number(table.advancedCount || 0) !== target
  }).length
  if (advancePending) return { ...base, hint: `${advancePending} 桌晋级数待核对` }
  return { ...base, ready: true, hint: '全部桌次已汇总' }
}

function buildCurrentRoundMetrics() {
  if (!currentRound.value) {
    return [
      { label: '桌数', value: '0' },
      { label: '酒款', value: '0' },
      { label: isFeedbackOnlyCompetition.value ? '诊断' : '晋级', value: '-' },
      { label: '问题', value: '-' },
    ]
  }
  if (currentRound.value.status === 'DRAFT') {
    const base = [
      { label: '桌数', value: currentRoundTables.value.length },
      { label: '酒款', value: `${currentRoundEntryCount.value} / ${currentPoolEntries.value.length}` },
    ]
    if (!isFeedbackOnlyCompetition.value) base.push({ label: currentRoundTargetLabel.value, value: currentRoundTargetDisplay.value })
    base.push({ label: '问题', value: roundValidationIssues.value.length })
    return base
  }
  if (currentRound.value.status === 'LOCKED') {
    const unlockedCount = currentRoundTables.value.filter((table) => table.status !== 'LOCKED').length
    if (isFeedbackOnlyCompetition.value) {
      return [
        { label: '诊断酒款', value: `${currentRoundEntryCount.value} 款` },
        { label: '诊断完成', value: `${feedbackFinalizedCount.value} / ${feedbackEntryCount.value}` },
        { label: '未锁定', value: `${unlockedCount} 桌` },
        { label: '结果', value: canPublishResults.value ? '可发布' : '已固定' },
      ]
    }
    if (currentRoundIsTerminal.value) {
      return [
        { label: '决赛桌', value: currentRoundTables.value.length },
        { label: '候选酒款', value: `${currentRoundEntryCount.value} 款` },
        { label: '总冠军', value: championConfirmed.value ? '已确认' : '待确认' },
        { label: '状态', value: '已锁定' },
      ]
    }
    return [
      { label: '晋级名单', value: `${advancedPool.value.length} 款` },
      { label: '下一轮', value: canCreateNextRound.value ? '可创建' : '待安排' },
      { label: '未锁定', value: `${unlockedCount} 桌` },
      { label: '结果', value: resultChecks.value.every((item) => item.done) ? '可发布' : '待确认' },
    ]
  }
  if (currentRound.value.type === 'RANKING') {
    const filled = currentRoundTables.value.reduce((sum, table) => sum + getFilledRankingCount(table), 0)
    if (currentRoundIsTerminal.value) {
      return [
        { label: '决赛桌', value: currentRoundTables.value.length },
        { label: '候选酒款', value: `${currentRoundEntryCount.value} 款` },
        { label: '总冠军', value: `${filled} / ${currentRoundTargetCount.value}` },
        { label: '状态', value: currentRound.value.status === 'SUBMITTED' ? '待确认' : currentRoundStatusText.value },
      ]
    }
    return [
      { label: '桌数', value: currentRoundTables.value.length },
      { label: '候选', value: `${currentRoundEntryCount.value} 款` },
      { label: '已选择', value: `${filled} / ${currentRoundTargetCount.value}` },
      { label: '问题', value: roundValidationIssues.value.length },
    ]
  }
  const judgeCounts = currentRoundTables.value.reduce((summary, table) => {
    const counts = getJudgeProgressCounts(table)
    summary.done += counts.done
    summary.total += counts.total
    return summary
  }, { done: 0, total: 0 })
  const captainAverage = getAverageProgress(currentRoundTables.value.map((table) => table.captainProgress))
  return [
    { label: '桌数', value: currentRoundTables.value.length },
    { label: '评审评分', value: `${judgeCounts.done} / ${judgeCounts.total}` },
    { label: '桌长汇总', value: `${captainAverage}%` },
    { label: '待处理', value: roundValidationIssues.value.length },
  ]
}

function buildRoundPyramidNodes() {
  const terminalLocked = terminalRoundLocked.value
  const published = competition.value?.status === 'PUBLISHED'
  const nodes = [{
    key: 'result',
    kind: 'result',
    label: '结果',
    subtitle: '奖项确认',
    statusText: published ? '已发布' : (resultChecks.value.every((item) => item.done) ? '可发布' : '待完成'),
    summary: '',
    note: rounds.value.length ? '待轮次完成' : '完成轮次后进入',
    placeholderText: '结果',
    hint: published ? '结果已发布' : (terminalLocked ? '确认奖项并发布' : '完成决赛轮后确认结果'),
    tableChips: [],
    extraTableCount: 0,
    width: 48,
    state: published ? 'done' : (resultChecks.value.every((item) => item.done) ? 'done' : 'ghost'),
    active: activeTab.value === 'results',
    actionable: terminalLocked || resultChecks.value.some((item) => item.done) || awardDrafts.value.length > 0,
  }]

  if (rounds.value.length > 0 && canCreateNextRound.value) {
    const lastRound = rounds.value[rounds.value.length - 1]
    const creatingFinal = isMedalRound(lastRound)
    const earlyDraft = createNextRoundIsEarlyDraft.value
    const nextRoundCandidateText = earlyDraft && !advancedPool.value.length ? '候选待同步' : `${advancedPool.value.length} 款晋级酒`
    nodes.push({
      key: 'next-round',
      kind: 'placeholder',
      label: creatingFinal ? '决赛轮' : nextRoundName.value,
      subtitle: creatingFinal ? '各组别金奖' : nextRoundCandidateText,
      statusText: '可创建',
      summary: creatingFinal ? '总冠军 1 名' : nextRoundCandidateText,
      note: creatingFinal ? '点击创建决赛轮' : (earlyDraft ? `提前创建${nextRoundName.value}草稿` : `点击创建${nextRoundName.value}`),
      placeholderText: '创建',
      hint: creatingFinal ? '创建决赛轮' : (earlyDraft ? '先排桌次和人员' : `创建${nextRoundName.value}`),
      tableChips: [],
      extraTableCount: 0,
      width: 58,
      state: 'create',
      active: false,
      actionable: true,
    })
  }

  const maxRoundNo = Math.max(...rounds.value.map((round) => Number(round.roundNo || 1)), 1)
  rounds.value
    .slice()
    .reverse()
    .forEach((round) => {
      const tableChips = round.tables.slice(0, 6).map((table) => ({
        id: table.id,
        label: table.name,
        active: round.id === activeRoundId.value && table.id === selectedRoundTableId.value,
        previewLines: buildPyramidTablePreview(round, table),
      }))
      const roundNo = Number(round.roundNo || 1)
      const width = Math.min(100, 58 + Math.max(maxRoundNo - roundNo, 0) * 13 + (roundNo === 1 ? 14 : 0))
      nodes.push({
        key: round.id,
        kind: 'round',
        roundId: round.id,
        label: round.name,
        subtitle: round.type === 'SCORE' ? '评分制' : '选择排序',
        statusText: roundStatusLabels[round.status] || round.status,
        summary: getRoundNodeDetail(round),
        note: round.id === activeRoundId.value ? '当前轮次' : '',
        placeholderText: '',
        hint: getRoundNodeHint(round),
        tableChips,
        extraTableCount: Math.max(round.tables.length - tableChips.length, 0),
        width,
        state: round.status === 'LOCKED' ? 'done' : round.status === 'DRAFT' ? 'draft' : 'running',
        active: round.id === activeRoundId.value,
        actionable: true,
      })
    })

  if (!rounds.value.length) {
    nodes.push({
      key: 'first-round-empty',
      kind: 'empty',
      label: '首轮',
      subtitle: '分桌预排',
      statusText: '未创建',
      summary: '',
      note: '',
      placeholderText: '首轮',
      hint: '先在分桌分配中安排首轮',
      tableChips: [],
      extraTableCount: 0,
      width: 96,
      state: 'pending',
      active: false,
      actionable: false,
    })
  }

  return nodes
}

function areMedalAwardsConfirmedLocally() {
  return medalAwards.value
    .filter((award) => ['CONFIRMED', 'PUBLISHED'].includes(award.status))
    .some((award) => Number(award.rankNo) >= 1 && Number(award.rankNo) <= 3)
}

function getRoundNodeDetail(round) {
  const entryCount = countRoundEntries(round)
  const targetCount = countRoundTarget(round)
  if (isFeedbackOnlyCompetition.value && round.type === 'SCORE') return `${entryCount} 款`
  if (round.type === 'SCORE') return `${entryCount} 款 · 晋级 ${targetCount} 款`
  return `${entryCount} 款 · ${formatRoundNodeTargetDisplay(round)}`
}

function getRoundNodeHint(round) {
  if (isFeedbackOnlyCompetition.value && round.type === 'SCORE') return '评审评分，桌长填写诊断意见'
  if (round.type === 'SCORE') return '评审评分，桌长从本桌酒款中选择晋级'
  return '桌长从晋级酒款中选择并排序'
}

function buildRoundTableSummary(table) {
  const issues = getRoundTableIssues(table)
  const captainName = getJudge(table.captainPublicId)?.name || '未指定'
  const isRankingRound = currentRound.value?.type === 'RANKING'
  const progress = getRoundTableProgressSummary(currentRound.value, table)
  const filledCount = isRankingRound ? getFilledRankingCount(table) : 0
  const statusText = issues[0]
    || progress.statusText
  return {
    id: table.id,
    name: table.name,
    entryCount: table.entryUuids.length,
    captainName,
    primaryProgressLabel: progress.primaryLabel,
    primaryProgress: progress.primaryValue,
    secondaryProgressLabel: progress.secondaryLabel,
    secondaryProgress: progress.secondaryValue,
    confirmationProgress: `${Number(table.confirmationConfirmedCount || 0)} / ${Number(table.confirmationRequiredCount || 0)}`,
    targetLabel: isFeedbackOnlyCompetition.value && !isRankingRound ? '诊断' : (isRankingRound ? resolveTableTargetLabel(table) : '晋级'),
    targetDisplay: isRankingRound && table.targetMode === 'CHAMPION'
      ? (filledCount ? '已选择' : '待选择')
      : (isFeedbackOnlyCompetition.value && !isRankingRound ? '首轮反馈' : (isRankingRound ? `${filledCount} / ${table.targetCount}` : `${table.targetCount} 款`)),
    statusText,
    tone: issues.length ? 'warning' : table.status === 'LOCKED' ? 'done' : 'ok',
  }
}

function resolveTableTargetLabel(table) {
  if (table?.targetMode === 'MEDALS') return '奖项名额'
  if (table?.targetMode === 'CHAMPION') return '总冠军'
  return '晋级数量'
}

function formatTargetModeName(mode) {
  if (mode === 'MEDALS') return '组别金银铜轮'
  if (mode === 'CHAMPION') return '决赛轮'
  return '普通排序轮'
}

function buildChampionTargetOption() {
  return {
    value: 'CHAMPION',
    label: '决赛轮',
    description: '从各组别金奖中选出 1 款全场总冠军',
    fixedTargetCount: 1,
    fixedTableCount: 1,
  }
}

function canUseChampionTargetForNextRound() {
  const sourceRound = rounds.value[rounds.value.length - 1]
  return canUseChampionTargetFromSource(sourceRound)
}

function canUseChampionTarget(round) {
  if (!round || round.type !== 'RANKING') return false
  const sourceRound = rounds.value.find((item) => item.id === round.sourceRoundId)
  return canUseChampionTargetFromSource(sourceRound)
}

function canUseChampionTargetFromSource(sourceRound) {
  if (competition.value?.status === 'RESULT_CONFIRMING') return true
  return hasRoundTargetMode(sourceRound, 'MEDALS')
}

function isChampionRound(round) {
  return hasRoundTargetMode(round, 'CHAMPION')
}

function isTerminalRound(round) {
  return isChampionRound(round)
}

function isMedalRound(round) {
  return hasRoundTargetMode(round, 'MEDALS')
}

function hasRoundTargetMode(round, mode) {
  return Boolean(round?.tables?.some((table) => table.targetMode === mode))
}

function getRoundTableProgressSummary(round, table) {
  if (!round) {
    return {
      primaryLabel: '评审评分',
      primaryValue: '-',
      secondaryLabel: '桌长汇总',
      secondaryValue: '-',
      statusText: '未开始',
    }
  }
  if (round.type === 'RANKING') {
    const filled = getFilledRankingCount(table)
    const target = Number(table.targetCount || 0)
    const submitted = ['SUBMITTED', 'LOCKED'].includes(table.status)
    const statusText = table.targetMode === 'MEDALS'
      ? (submitted ? '已提交' : (filled ? '已选择' : (roundStatusLabels[table.status] || '待排序')))
      : (filled >= target && target > 0 ? '已完成' : (roundStatusLabels[table.status] || '待排序'))
    return {
      primaryLabel: '候选',
      primaryValue: `${table.entryUuids.length} 款`,
      secondaryLabel: '已选',
      secondaryValue: `${filled} / ${target}`,
      statusText,
    }
  }
  const judgeProgress = normalizeProgress(table.judgeProgress)
  const captainProgress = normalizeProgress(table.captainProgress)
  const advancedCount = Number(table.advancedCount || 0)
  const target = Number(table.targetCount || 0)
  let statusText = roundStatusLabels[table.status] || '已发布'
  if (table.status === 'LOCKED' || (captainProgress >= 100 && target > 0 && advancedCount >= target)) statusText = '已完成'
  else if (captainProgress > 0) statusText = '桌长汇总中'
  else if (judgeProgress >= 100) statusText = '待桌长汇总'
  else if (judgeProgress > 0) statusText = '评分中'
  return {
    primaryLabel: '评审评分',
    primaryValue: formatJudgeProgress(table),
    secondaryLabel: '桌长汇总',
    secondaryValue: captainProgress >= 100 ? '已汇总' : '未汇总',
    statusText,
  }
}

function getJudgeProgressCounts(table) {
  const judgeCount = getRoundTableTaskJudgeCount(table)
  const total = table.entryUuids.length * judgeCount
  if (!total) return { done: 0, total: 0 }
  const progress = normalizeProgress(table.judgeProgress)
  const done = progress >= 100 ? total : Math.min(total, Math.max(0, Math.round(total * progress / 100)))
  return { done, total }
}

function formatJudgeProgress(table) {
  const counts = getJudgeProgressCounts(table)
  return `${counts.done} / ${counts.total}`
}

function openRoundTableScoreDetail(tableId) {
  roundScoreDetailTableId.value = tableId
  roundScoreDetailDialogOpen.value = true
}

function buildFallbackRoundScoreDetailJudges(table) {
  if (!table?.entryUuids?.length) return []
  const baseTable = judgeTableForm.find((item) => item.tableName === table.name)
  const assignments = baseTable
    ? judgeAssignmentForm.filter((assignment) => assignment.tableLocalId === baseTable.localId)
    : []
  return assignments.map((assignment) => {
    const judge = getJudge(assignment.judgePublicId)
    const entryScores = table.entryUuids.map((uuid) => ({
      beerUuid: uuid,
      scored: false,
      totalScore: null,
      submittedAt: null,
    }))
    return {
      judgePublicId: assignment.judgePublicId,
      judgeName: judge?.name || assignment.judgePublicId || '未知评审',
      role: assignment.role,
      roleLabel: roleLabels[assignment.role] || assignment.role,
      submittedCount: 0,
      totalCount: table.entryUuids.length,
      progress: 0,
      missingEntryUuids: [...table.entryUuids],
      entryScores,
    }
  })
}

function mergeCaptainIntoRoundScoreDetailJudges(table, judges) {
  if (!table?.captainPublicId) return judges
  const hasCaptain = judges.some((judge) => judge.judgePublicId === table.captainPublicId || judge.role === 'CAPTAIN')
  if (hasCaptain) return judges
  return [...judges, buildCaptainProgressFallback(table, judges)]
}

function buildCaptainProgressFallback(table, judges = []) {
  const judge = getJudge(table.captainPublicId)
  const submittedCount = resolveCaptainFallbackSubmittedCount(table, judges)
  const entryScores = (table.entryUuids || []).map((uuid, index) => ({
    beerUuid: uuid,
    scored: index < submittedCount,
    totalScore: null,
    submittedAt: null,
  }))
  return {
    judgePublicId: table.captainPublicId,
    judgeName: judge?.name || table.captainPublicId || '未指定桌长',
    role: 'CAPTAIN',
    roleLabel: roleLabels.CAPTAIN || '桌长',
    submittedCount,
    totalCount: entryScores.length,
    progress: calculateCountProgress(submittedCount, entryScores.length),
    missingEntryUuids: entryScores
      .filter((score) => !score.scored)
      .map((score) => score.beerUuid),
    entryScores,
  }
}

function resolveCaptainFallbackSubmittedCount(table, judges = []) {
  const entryCount = table?.entryUuids?.length || 0
  if (!entryCount) return 0
  const judgeCount = getRoundTableTaskJudgeCount(table)
  const total = entryCount * judgeCount
  if (!total) return 0
  const aggregateSubmitted = normalizeProgress(table?.judgeProgress) >= 100
    ? total
    : Math.min(total, Math.max(0, Math.round(total * normalizeProgress(table?.judgeProgress) / 100)))
  const knownSubmitted = judges.reduce((sum, judge) => sum + Number(judge.submittedCount || 0), 0)
  return Math.min(entryCount, Math.max(0, aggregateSubmitted - knownSubmitted))
}

function getRoundTableTaskJudgeCount(table) {
  if (!table) return 0
  if (table.judgeDetails?.length) {
    const taskJudgeIds = new Set(table.judgeDetails.map((judge) => judge.judgePublicId || `${judge.role}-${judge.judgeName}`))
    if (table.captainPublicId && !table.judgeDetails.some((judge) => judge.judgePublicId === table.captainPublicId || judge.role === 'CAPTAIN')) {
      taskJudgeIds.add(table.captainPublicId)
    }
    return taskJudgeIds.size
  }
  const ordinaryJudgeCount = Number(table.professionalCount || 0) + Number(table.crossCount || 0)
  return ordinaryJudgeCount + (table.captainPublicId ? 1 : 0)
}

function isCaptainDetailJudge(judge) {
  if (!judge) return false
  const captainPublicId = roundScoreDetailTable.value?.captainPublicId
  return judge.role === 'CAPTAIN' || (captainPublicId && judge.judgePublicId === captainPublicId)
}

function calculateCountProgress(submitted, total) {
  if (!total) return 0
  return Math.min(100, Math.max(0, Math.round(Number(submitted || 0) * 100 / total)))
}

function buildCountProgressFromPercent(progress, total) {
  const normalizedTotal = Math.max(0, Number(total || 0))
  const normalizedProgress = normalizeProgress(progress)
  const submitted = normalizedProgress >= 100
    ? normalizedTotal
    : Math.min(normalizedTotal, Math.max(0, Math.round(normalizedTotal * normalizedProgress / 100)))
  return {
    submitted,
    total: normalizedTotal,
    progress: calculateCountProgress(submitted, normalizedTotal),
    remaining: Math.max(0, normalizedTotal - submitted),
  }
}

function buildJudgeDetailTasks(judge) {
  const personalTotal = Number(judge.totalCount || 0)
  const personalSubmitted = Math.min(personalTotal, Math.max(0, Number(judge.submittedCount || 0)))
  const tasks = [{
    key: 'personal',
    label: '个人评分',
    submitted: personalSubmitted,
    total: personalTotal,
    progress: calculateCountProgress(personalSubmitted, personalTotal),
    remaining: Math.max(0, personalTotal - personalSubmitted),
  }]
  if (isCaptainDetailJudge(judge)) {
    const captainStats = roundScoreDetailCaptainStats.value
    tasks.push({
      key: 'consensus',
      label: '共识打分',
      ...captainStats.consensus,
    })
    if (!isFeedbackOnlyCompetition.value) tasks.push({
      key: 'advance',
      label: '选择晋级',
      ...captainStats.advance,
    })
  }
  return tasks.map((task) => ({
    ...task,
    statusText: task.remaining > 0 ? `剩余 ${task.remaining} 款` : '已完成',
  }))
}

function hasJudgeDetailPendingTasks(judge) {
  return buildJudgeDetailTasks(judge).some((task) => task.remaining > 0)
}

function buildPyramidTablePreview(round, table) {
  const progress = getRoundTableProgressSummary(round, table)
  const captainName = getJudge(table.captainPublicId)?.name || '未指定桌长'
  if (round?.type === 'RANKING') {
    return [
      `${table.entryUuids.length} 款 · ${captainName}`,
      `${progress.secondaryLabel} ${progress.secondaryValue}`,
      progress.statusText,
    ]
  }
  return [
    `${table.entryUuids.length} 款 · ${captainName}`,
    `${progress.primaryLabel} ${progress.primaryValue}`,
    `${progress.secondaryLabel} ${progress.secondaryValue}`,
  ]
}

function normalizeProgress(value) {
  const number = Number(value || 0)
  if (!Number.isFinite(number)) return 0
  return Math.max(0, Math.min(100, Math.round(number)))
}

function getAverageProgress(values) {
  const normalized = values.map((value) => normalizeProgress(value))
  if (!normalized.length) return 0
  return Math.round(normalized.reduce((sum, value) => sum + value, 0) / normalized.length)
}

function buildRoundTodoHint() {
  if (!currentRound.value) {
    return {
      tone: 'warning',
      title: '还没有轮次',
      detail: '先完成评审和酒款分配',
      action: '',
    }
  }
  if (currentRound.value.type === 'RANKING' && currentRound.value.status === 'DRAFT' && currentRound.value.sourceLocked === false && canLockCurrentDraftSourceRound.value) {
    return {
      tone: 'ready',
      title: `先锁定${currentDraftSourceRound.value?.name || '上一轮'}`,
      detail: '锁定后同步候选酒款，再核对决赛桌分配',
      action: 'lockSourceRound',
    }
  }
  if (currentRound.value.type === 'RANKING' && currentRound.value.status === 'DRAFT' && currentRound.value.sourceLocked && !currentRound.value.candidatesSynced) {
    return {
      tone: 'ready',
      title: '候选酒款待同步',
      detail: '同步上一轮锁定结果后，再核对桌次和发布任务',
      action: 'syncCandidates',
    }
  }
  if (roundValidationIssues.value.length && currentRound.value.status === 'DRAFT') {
    if (currentRound.value.type === 'RANKING' && currentRound.value.sourceLocked && !currentRoundEntryCount.value) {
      return {
        tone: 'warning',
        title: '还没有分配酒款',
        detail: '先在分桌分配中加入酒款',
        action: 'goToRoundAllocation',
      }
    }
    return {
      tone: 'warning',
      title: '发布前需要处理',
      detail: roundValidationIssues.value[0],
      action: '',
    }
  }
  if (currentRound.value.status === 'DRAFT') {
    return {
      tone: canPublishCurrentRound.value ? 'ready' : 'warning',
      title: canPublishCurrentRound.value ? `${currentRound.value.name}可以发布` : '继续完善本轮配置',
      detail: canPublishCurrentRound.value ? `发布后，${currentRoundPublishTarget.value}会看到本轮任务` : roundNextStepText.value,
      action: '',
    }
  }
  if (currentRound.value.status === 'PUBLISHED') {
    return {
      tone: 'ready',
      title: currentRound.value.type === 'SCORE' ? '等待评审评分' : '等待桌长排序',
      detail: currentRound.value.type === 'SCORE'
        ? '评审评分完成后，由桌长汇总本轮结果'
        : '桌长提交排序后，再确认并锁定本轮结果',
      action: 'focusRoundProgress',
    }
  }
  if (currentRound.value.status === 'IN_PROGRESS') {
    return {
      tone: 'ready',
      title: '排序处理中',
      detail: '等待各桌完成选择和排序',
      action: 'focusRoundProgress',
    }
  }
  if (currentRound.value.status === 'SUBMITTED') {
    return {
      tone: 'ready',
      title: currentRoundIsTerminal.value ? '可以确认锁定' : (canCreateNextRound.value ? `可提前创建${nextRoundName.value}草稿` : '可以确认锁定'),
      detail: currentRoundIsTerminal.value
        ? '确认总冠军结果无误后锁定，锁定后确认奖项'
        : (canCreateNextRound.value ? '先排下一轮桌次和人员；晋级酒款等本轮锁定后同步' : '确认本轮结果无误后锁定'),
      action: !currentRoundIsTerminal.value && canCreateNextRound.value ? 'createNextRound' : '',
    }
  }
  if (currentRound.value.status === 'LOCKED') {
    if (isFeedbackOnlyCompetition.value) {
      return {
        tone: canPublishResults.value ? 'ready' : 'done',
        title: '首轮诊断已固定',
        detail: canPublishResults.value ? '可以发布给厂商和公开结果页' : '诊断结果已固定',
        action: canPublishResults.value ? 'goToResults' : '',
      }
    }
    if (currentRoundIsTerminal.value) {
      return {
        tone: 'done',
        title: '决赛轮结果已固定',
        detail: '总冠军结果已固定，请确认奖项并发布结果',
        action: 'goToResults',
      }
    }
    return {
      tone: canCreateNextRound.value ? 'ready' : 'done',
      title: `${currentRound.value.name}结果已固定`,
      detail: isMedalRound(currentRound.value) ? '组别奖项已生成，下一步创建决赛轮' : (canCreateNextRound.value ? `${advancedPool.value.length} 款晋级酒可用于${nextRoundName.value}` : '本轮结果已固定'),
      action: canCreateNextRound.value ? 'createNextRound' : '',
    }
  }
  return {
    tone: 'ready',
    title: '继续推进当前轮次',
    detail: roundNextStepText.value,
    action: '',
  }
}

function handleRoundPyramidNode(node) {
  if (node.kind === 'round' && node.roundId) {
    selectRound(node.roundId)
    return
  }
  if (node.kind === 'placeholder' && node.actionable) {
    openCreateRoundDialog()
    return
  }
  if (node.kind === 'result' && node.actionable) {
    activeTab.value = 'results'
  }
}

function selectPyramidTable(node, table) {
  if (node.kind !== 'round' || !node.roundId) return
  selectRound(node.roundId)
  selectedRoundTableId.value = table.id
}

function submitRankingRoundById(roundId) {
  selectRound(roundId)
  submitRankingRound()
}

function lockRoundById(roundId) {
  const round = rounds.value.find((item) => item.id === roundId)
  if (!round || round.status !== 'SUBMITTED') return
  round.status = 'LOCKED'
  round.tables.forEach((table) => { table.status = 'LOCKED' })
  refreshAdvancedPoolFromRound(round)
  ElMessage.success(`${round.name}已锁定`)
}

function countRoundEntries(round) {
  return new Set((round?.tables || []).flatMap((table) => table.entryUuids)).size
}

function countRoundTarget(round) {
  return (round?.tables || []).reduce((sum, table) => sum + Number(table.targetCount || 0), 0)
}

function resolveRoundTargetLabel(round) {
  if (isFeedbackOnlyCompetition.value && round?.type === 'SCORE') return '诊断'
  if (round?.type === 'SCORE') return '晋级'
  const modes = new Set((round?.tables || []).map((table) => table.targetMode).filter(Boolean))
  if (modes.size === 1 && modes.has('MEDALS')) return '奖项名额'
  if (modes.size === 1 && modes.has('CHAMPION')) return '总冠军'
  return '晋级数量'
}

function formatRoundTargetDisplay(round) {
  if (isFeedbackOnlyCompetition.value && round?.type === 'SCORE') return '首轮诊断'
  const total = countRoundTarget(round)
  const targets = [...new Set((round?.tables || []).map((table) => Number(table.targetCount || 0)).filter(Boolean))]
  if ((round?.tables || []).every((table) => table.targetMode === 'MEDALS')) {
    return targets.length === 1 ? `每桌金银铜 ${targets[0]} 个名额，共 ${total}` : `共 ${total} 个名额`
  }
  if ((round?.tables || []).every((table) => table.targetMode === 'CHAMPION')) {
    return `总冠军 ${total} 名`
  }
  return targets.length === 1 ? `每桌晋级 ${targets[0]} 款，共 ${total}` : `共 ${total} 款`
}

function formatRoundNodeTargetDisplay(round) {
  if (isFeedbackOnlyCompetition.value && round?.type === 'SCORE') return '诊断'
  const tableCount = round?.tables?.length || 0
  const targetCount = countRoundTarget(round)
  if ((round?.tables || []).every((table) => table.targetMode === 'MEDALS')) {
    return `金银铜 ${tableCount} 桌`
  }
  if ((round?.tables || []).every((table) => table.targetMode === 'CHAMPION')) {
    return '总冠军 1 名'
  }
  return `晋级 ${targetCount} 款`
}

function getPoolEntriesForRound(round) {
  if (round?.type === 'SCORE') return roundEntryPool.value.filter((entry) => entry.stored)
  if (round?.type === 'RANKING' && round.sourceLocked === false) return []
  if (round?.sourceEntryUuids?.length) {
    return round.sourceEntryUuids
      .map((uuid) => roundEntryPool.value.find((entry) => entry.uuid === uuid))
      .filter(Boolean)
  }
  return advancedPool.value
}

function buildRoundValidationIssues(round) {
  if (!round) return ['请先创建轮次']
  const issues = []
  if (round.type === 'RANKING' && round.sourceLocked === false) issues.push('等待上一轮结果固定后再分配酒款')
  if (round.type === 'RANKING' && round.sourceLocked && !round.candidatesSynced) issues.push('请先同步上一轮候选酒款')
  if (!round.tables.length) issues.push(`${round.name}至少需要 1 张桌`)
  const tableNames = round.tables.map((table) => table.name?.trim() || '')
  const duplicateTableNames = tableNames.filter((name, index, list) => name && list.indexOf(name) !== index)
  if (tableNames.some((name) => !name)) issues.push('评审桌名称不能为空')
  if (duplicateTableNames.length) issues.push(`轮次桌名称不能重复：${duplicateTableNames[0]}`)
  const assigned = round.tables.flatMap((table) => table.entryUuids)
  if (!assigned.length) issues.push(`${round.name}尚未分配酒款`)
  const pool = getPoolEntriesForRound(round)
  const unassignedCount = pool.filter((entry) => !round.tables.some((table) => table.entryUuids.includes(entry.uuid))).length
  if (unassignedCount) issues.push(`还有 ${unassignedCount} 款酒未分配到本轮桌`)
  const duplicates = assigned.filter((uuid, index, list) => list.indexOf(uuid) !== index)
  if (duplicates.length) issues.push(`${round.name}存在重复分配酒款`)
  round.tables.forEach((table) => issues.push(...getRoundTableIssues(table)))
  return [...new Set(issues)]
}

function getRoundPublishStageIssue(round) {
  if (round?.status !== 'DRAFT') return ''
  if (round.type === 'SCORE' && competition.value?.status !== 'JUDGING_PREP') {
    if (competition.value?.status === 'REGISTRATION_OPEN') {
      return '报名仍在进行，当前轮次会保留为预排草稿；截止报名并进入评审准备中后才能发布给评审'
    }
    if (competition.value?.status === 'REGISTRATION_CLOSED') return '进入评审准备中后才能发布首轮'
    return '发布报名并进入评审准备中后才能发布首轮'
  }
  if (round.type === 'RANKING' && !['JUDGING', 'RESULT_CONFIRMING'].includes(competition.value?.status)) {
    return '当前阶段不能发布排序轮'
  }
  return ''
}

function getRoundTableIssues(table) {
  const issues = []
  const targetLabel = resolveTableTargetLabel(table)
  const tableName = table.name?.trim() || '未命名评审桌'
  if (!table.name?.trim()) issues.push('评审桌名称不能为空')
  if (!table.captainPublicId) issues.push(`${tableName}缺少桌长`)
  if (!table.entryUuids.length) issues.push(`${tableName}尚未分配酒款`)
  if (isFeedbackOnlyCompetition.value && currentRound.value?.type === 'SCORE') return issues
  if (!Number(table.targetCount || 0)) issues.push(`${tableName}${targetLabel}不能为空`)
  if (table.targetMode !== 'MEDALS' && Number(table.targetCount || 0) > table.entryUuids.length) issues.push(`${tableName}${targetLabel}超过候选酒款数`)
  if (table.targetMode === 'MEDALS' && Number(table.targetCount || 0) !== 3) issues.push(`${tableName}奖牌轮固定为金、银、铜 3 个名额`)
  if (table.targetMode === 'MEDALS' && table.categoryMode !== 'CATEGORY') issues.push(`${tableName}奖牌轮只能包含一个投递组别`)
  if (table.targetMode === 'CHAMPION' && Number(table.targetCount || 0) !== 1) issues.push(`${tableName}决赛轮固定为总冠军 1 名`)
  return issues
}

function getRoundTableConflictWarnings(table) {
  const judges = getRoundTableConflictJudges(table)
  const entries = (table?.entryUuids || [])
    .map((uuid) => roundEntryPool.value.find((entry) => entry.uuid === uuid))
    .filter((entry) => entry?.breweryCompanyName || entry?.breweryId)
  const warnings = []
  judges.forEach((judge) => {
    const keywords = splitBreweryConflictKeywords(judge.breweryConflictText)
    entries.forEach((entry) => {
      if (isPhoneBreweryConflictMatch(judge, entry)) {
        warnings.push(`本桌存在手机号匹配回避：${judge.name || '未知评审'} / ${entry.breweryCompanyName || judge.phoneConflictBreweryName || '关联厂牌'}`)
      }
      if (isBreweryConflictMatch(keywords, entry.breweryCompanyName)) {
        warnings.push(`本桌可能存在回避风险：${judge.name || '未知评审'} / ${entry.breweryCompanyName}`)
      }
    })
  })
  return [...new Set(warnings)]
}

function getRoundTableConflictJudges(table) {
  const publicIds = new Set()
  if (table?.captainPublicId) publicIds.add(table.captainPublicId)
  const members = table?.members || []
  members.forEach((member) => {
    if (member?.judgePublicId) publicIds.add(member.judgePublicId)
  })
  const baseTable = judgeTableForm.find((item) => item.tableName === table?.name)
  if (baseTable) {
    assignmentsForTable(baseTable).forEach((assignment) => publicIds.add(assignment.judgePublicId))
  }
  return [...publicIds]
    .map((publicId) => getJudge(publicId))
    .filter((judge) => (judge?.breweryConflictFlag && judge?.breweryConflictText) || judge?.phoneBreweryConflictFlag)
}

function splitBreweryConflictKeywords(text) {
  return String(text || '')
    .split(/[\n\r,，、;；]/)
    .map((item) => item.trim().toLowerCase())
    .filter((item) => item.length >= 2)
}

function isBreweryConflictMatch(keywords, breweryName) {
  const brewery = String(breweryName || '').trim().toLowerCase()
  if (!brewery) return false
  return keywords.some((keyword) => keyword.includes(brewery) || brewery.includes(keyword))
}

function isPhoneBreweryConflictMatch(judge, entry) {
  if (!judge?.phoneBreweryConflictFlag || !entry) return false
  if (judge.phoneConflictBreweryId == null || entry.breweryId == null) {
    return isBreweryConflictMatch(splitBreweryConflictKeywords(judge.phoneConflictBreweryName), entry.breweryCompanyName)
  }
  const conflictBreweryId = Number(judge.phoneConflictBreweryId)
  const entryBreweryId = Number(entry.breweryId)
  if (Number.isFinite(conflictBreweryId) && Number.isFinite(entryBreweryId) && conflictBreweryId === entryBreweryId) {
    return true
  }
  return isBreweryConflictMatch(splitBreweryConflictKeywords(judge.phoneConflictBreweryName), entry.breweryCompanyName)
}

function getRoundEntryAssignment(uuid) {
  const table = currentRoundTables.value.find((item) => item.entryUuids.includes(uuid))
  return table?.name || ''
}

function usesRoundJudgePoolForCurrentRound() {
  return currentRound.value?.type === 'RANKING'
    || (currentRound.value?.type === 'SCORE' && !currentRound.value?.isPreparationDraft)
}

const usesRoundJudgePool = computed(() => usesRoundJudgePoolForCurrentRound())

function compareRoundJudgePoolOrder(left, right) {
  const leftAssigned = isRoundJudgeAssigned(left.publicId)
  const rightAssigned = isRoundJudgeAssigned(right.publicId)
  if (leftAssigned !== rightAssigned) return leftAssigned ? 1 : -1
  return judgePool.value.findIndex((judge) => judge.publicId === left.publicId)
    - judgePool.value.findIndex((judge) => judge.publicId === right.publicId)
}

function isRoundJudgeAssigned(judgePublicId) {
  return currentRoundTables.value.some((table) => (
    table.captainPublicId === judgePublicId
      || getTableParticipantPublicIds(table).includes(judgePublicId)
  ))
}

function inferTableScope(table, poolEntries = currentPoolEntries.value) {
  const entries = (table?.entryUuids || [])
    .map((uuid) => poolEntries.find((entry) => entry.uuid === uuid))
    .filter(Boolean)
  const categories = new Map()
  entries.forEach((entry) => {
    if (entry.categoryId == null) return
    categories.set(entry.categoryId, entry.categoryName || getCategoryNameById(entry.categoryId))
  })
  if (!entries.length || categories.size === 0) {
    return { categoryId: null, categoryMode: 'EMPTY', categoryName: '' }
  }
  if (categories.size === 1) {
    const [[categoryId, categoryName]] = [...categories.entries()]
    return { categoryId, categoryMode: 'CATEGORY', categoryName }
  }
  return { categoryId: null, categoryMode: 'MIXED', categoryName: '混合' }
}

function syncRoundTableScope(table, poolEntries = currentPoolEntries.value) {
  if (!table) return
  const scope = inferTableScope(table, poolEntries)
  table.categoryId = scope.categoryId
  table.categoryMode = scope.categoryMode
  table.categoryName = scope.categoryName
}

function getCategoryNameById(categoryId) {
  return competition.value?.categories?.find((item) => item.id === categoryId)?.name
    || currentPoolEntries.value.find((entry) => entry.categoryId === categoryId)?.categoryName
    || '未命名组别'
}

function resetEntryAutoAssignForm(table = null) {
  entryAutoAssignForm.categoryId = table?.categoryId ?? null
  entryAutoAssignForm.quantity = 1
}

function toggleEntrySelection(uuid) {
  if (selectedEntryUuids.value.includes(uuid)) {
    selectedEntryUuids.value = selectedEntryUuids.value.filter((item) => item !== uuid)
    return
  }
  selectedEntryUuids.value = [...selectedEntryUuids.value, uuid]
}

function clearEntrySelection() {
  selectedEntryUuids.value = []
}

function openEntryAutoAssignDialog(tableId) {
  const table = currentRoundTables.value.find((item) => item.id === tableId)
  if (!table || currentRound.value?.status !== 'DRAFT') return
  entryAutoAssignTableId.value = tableId
  resetEntryAutoAssignForm(table)
  entryAutoAssignDialogOpen.value = true
}

function closeEntryAutoAssignDialog() {
  entryAutoAssignDialogOpen.value = false
  entryAutoAssignTableId.value = ''
  resetEntryAutoAssignForm()
}

function confirmEntryAutoAssign() {
  const table = entryAutoAssignTable.value
  if (!table) return
  const categoryId = entryAutoAssignForm.categoryId
  if (!categoryId) {
    ElMessage.warning('请选择范围')
    return
  }
  const quantity = Math.max(Number(entryAutoAssignForm.quantity || 1), 1)
  const candidates = currentPoolEntries.value
    .filter((entry) => entry.categoryId === categoryId && !getRoundEntryAssignment(entry.uuid))
    .slice(0, quantity)
  if (!candidates.length) {
    ElMessage.warning('当前范围没有可加入酒款')
    return
  }
  candidates.forEach((entry) => {
    table.entryUuids.push(entry.uuid)
  })
  syncRoundTableScope(table)
  clearEntrySelection()
  closeEntryAutoAssignDialog()
  ElMessage.success(`${table.name} 已加入 ${candidates.length} 款酒`)
}

function addSelectedEntriesToTable() {
  if (!selectedEntryUuids.value.length) {
    ElMessage.warning('请先选择酒款')
    return
  }
  selectedEntryUuids.value.forEach((uuid) => addEntryToSelectedRoundTable(uuid, false))
  ElMessage.success(`已加入 ${selectedEntryUuids.value.length} 款酒`)
  clearEntrySelection()
}

function addEntryToSelectedRoundTable(uuid, notify = true) {
  const table = selectedRoundTable.value
  if (!table) {
    ElMessage.warning('请先选择要加入的轮次桌')
    return
  }
  if (currentRound.value?.status !== 'DRAFT') return
  const entry = currentPoolEntries.value.find((item) => item.uuid === uuid)
  if (!entry) return
  currentRoundTables.value.forEach((item) => {
    if (!item.entryUuids.includes(uuid)) return
    item.entryUuids = item.entryUuids.filter((entryUuid) => entryUuid !== uuid)
    syncRoundTableScope(item)
  })
  table.entryUuids.push(uuid)
  syncRoundTableScope(table)
  if (notify) ElMessage.success(`${uuid} 已加入 ${table.name}`)
}

function updateRoundTableCaptain(tableId, judgePublicId) {
  if (currentRound.value?.status !== 'DRAFT') return
  const table = currentRoundTables.value.find((item) => item.id === tableId)
  if (!table) return
  if (judgePublicId) {
    const alreadyAssigned = currentRoundTables.value.some((item) => (
      item.id !== table.id
        && (item.captainPublicId === judgePublicId || getTableParticipantPublicIds(item).includes(judgePublicId))
    ))
    if (alreadyAssigned) {
      ElMessage.warning('这位评审已安排到其他桌')
      return
    }
  }
  table.captainPublicId = judgePublicId
  table.members = [
    ...(judgePublicId ? [{
      judgePublicId,
      name: getJudge(judgePublicId)?.name || '',
      role: 'CAPTAIN',
      roleLabel: '桌长',
      systemTaskRequired: true,
    }] : []),
    ...(table.members || []).filter((member) => member.role !== 'CAPTAIN' && member.judgePublicId !== judgePublicId),
  ]
  table.participantPublicIds = getTableParticipantPublicIds(table)
}

function updateRoundTableName(tableId, value, options = {}) {
  if (!currentRound.value || currentRound.value.status !== 'DRAFT') return
  const table = currentRoundTables.value.find((item) => item.id === tableId)
  if (!table) return
  const nextName = String(value ?? '')
  if (table.name === nextName && !options.commit) return
  table.name = options.commit ? nextName.trim() : nextName
  if (currentRound.value.isPreparationDraft) {
    const baseTable = findBaseJudgeTableForDraftRoundTable(tableId)
    if (baseTable) baseTable.tableName = table.name
  }
  if (options.commit) {
    scheduleRoundTableNameAutoSave()
  } else {
    clearRoundTableNameAutoSave()
  }
}

function findBaseJudgeTableForDraftRoundTable(tableId) {
  const prefix = 'draft-table-'
  if (typeof tableId !== 'string' || !tableId.startsWith(prefix)) return null
  const localId = tableId.slice(prefix.length)
  return judgeTableForm.find((table) => table.localId === localId) || null
}

function clearRoundTableNameAutoSave() {
  if (!roundTableNameAutoSaveTimer) return
  window.clearTimeout(roundTableNameAutoSaveTimer)
  roundTableNameAutoSaveTimer = null
}

function scheduleRoundTableNameAutoSave() {
  clearRoundTableNameAutoSave()
  roundTableNameAutoSaveTimer = window.setTimeout(async () => {
    roundTableNameAutoSaveTimer = null
    if (!currentRound.value || currentRound.value.status !== 'DRAFT') return
    if (hasRoundTableNameIssues(currentRound.value)) return
    try {
      if (currentRound.value.isPreparationDraft) {
        syncFirstRoundDraftTables()
        if (canEditBaseJudgeTables.value) await saveJudgeDraft({ silent: true, allowIncomplete: true })
      } else {
        await persistCurrentRoundAllocation()
      }
    } catch {
      ElMessage.error('桌名自动保存失败，请稍后重试')
    }
  }, 900)
}

function hasRoundTableNameIssues(round) {
  const names = (round?.tables || []).map((table) => table.name?.trim() || '')
  return names.some((name) => !name) || new Set(names).size !== names.length
}

function setRoundCaptainForSelectedTable(judgePublicId) {
  const table = selectedRoundTable.value
  if (!table || currentRound.value?.status !== 'DRAFT') return
  updateRoundTableCaptain(table.id, judgePublicId)
}

function getTableParticipantPublicIds(table) {
  return (table?.members || [])
    .filter((member) => member.role !== 'CAPTAIN')
    .map((member) => member.judgePublicId)
    .filter((publicId) => publicId && publicId !== table?.captainPublicId)
}

function addRoundParticipantToSelectedTable(judgePublicId, role = 'PROFESSIONAL') {
  const table = selectedRoundTable.value
  if (!table || currentRound.value?.status !== 'DRAFT') return
  if (!judgePublicId || judgePublicId === table.captainPublicId) {
    ElMessage.warning('桌长不需要重复加入参与评审')
    return
  }
  const alreadyAssigned = currentRoundTables.value.some((item) => (
    item.id !== table.id
      && (item.captainPublicId === judgePublicId || getTableParticipantPublicIds(item).includes(judgePublicId))
  ))
  if (alreadyAssigned) {
    ElMessage.warning('这位评审已安排到其他桌')
    return
  }
  if (getTableParticipantPublicIds(table).includes(judgePublicId)) return
  const judge = getJudge(judgePublicId)
  table.members = [
    ...(table.members || []),
    {
      judgePublicId,
      name: judge?.name || '',
      role: currentRound.value?.type === 'SCORE' ? normalizeScoreJudgeRole(role) : 'PROFESSIONAL',
      roleLabel: currentRound.value?.type === 'SCORE' ? roleLabels[normalizeScoreJudgeRole(role)] : '参与评审',
      systemTaskRequired: currentRound.value?.type === 'SCORE',
    },
  ]
  table.participantPublicIds = getTableParticipantPublicIds(table)
}

function dropRoundJudge(tableId, role) {
  if (!draggingItem.value || draggingItem.value.type !== 'judge') return
  if (currentRound.value?.status !== 'DRAFT') return
  selectedRoundTableId.value = tableId
  if (role === 'CAPTAIN') {
    updateRoundTableCaptain(tableId, draggingItem.value.judgePublicId)
  } else {
    addRoundParticipantToSelectedTable(draggingItem.value.judgePublicId, role)
  }
  clearDrag()
}

function removeRoundParticipant(tableId, judgePublicId) {
  const table = currentRoundTables.value.find((item) => item.id === tableId)
  if (!table || currentRound.value?.status !== 'DRAFT') return
  table.members = (table.members || []).filter((member) => member.role === 'CAPTAIN' || member.judgePublicId !== judgePublicId)
  table.participantPublicIds = getTableParticipantPublicIds(table)
}

function normalizeScoreJudgeRole(role) {
  return role === 'CROSS' ? 'CROSS' : 'PROFESSIONAL'
}

function updateRoundTableScope(tableId, scopeValue) {
  if (currentRound.value?.status !== 'DRAFT') return
  const table = currentRoundTables.value.find((item) => item.id === tableId)
  if (!table) return
  if (scopeValue === 'MIXED') {
    table.categoryId = null
    table.categoryMode = 'MIXED'
    table.categoryName = '混合'
    return
  }
  if (scopeValue?.startsWith('CATEGORY:')) {
    const categoryId = Number(scopeValue.slice('CATEGORY:'.length))
    if (!Number.isFinite(categoryId)) return
    table.categoryId = categoryId
    table.categoryMode = 'CATEGORY'
    table.categoryName = getCategoryNameById(categoryId)
    return
  }
  table.categoryId = null
  table.categoryMode = 'EMPTY'
  table.categoryName = ''
}

function updateRoundTableTarget(tableId, targetCount) {
  if (currentRound.value?.status !== 'DRAFT') return
  const table = currentRoundTables.value.find((item) => item.id === tableId)
  if (!table) return
  table.targetCount = Math.max(1, Number(targetCount || 1))
  table.rankings = buildEmptyRankings(table.targetCount, table.targetMode)
}

function updateRoundTargetMode(mode) {
  if (!currentRound.value || currentRound.value.type !== 'RANKING' || currentRound.value.status !== 'DRAFT') return
  const normalizedMode = ['TOP_N', 'MEDALS', 'CHAMPION'].includes(mode) ? mode : 'TOP_N'
  const fixedTargetCount = normalizedMode === 'CHAMPION' ? 1 : normalizedMode === 'MEDALS' ? 3 : null
  currentRound.value.tables.forEach((table) => {
    const nextTargetCount = fixedTargetCount || Math.max(1, Number(table.targetCount || createRoundForm.targetCount || 3))
    table.targetMode = normalizedMode
    table.targetCount = nextTargetCount
    table.rankings = buildEmptyRankings(nextTargetCount, normalizedMode)
  })
  ElMessage.success(`已切换为${formatTargetModeName(normalizedMode)}`)
}

function addRoundTable() {
  if (!currentRound.value || currentRound.value.status !== 'DRAFT') return
  const tables = currentRound.value.tables
  const mode = currentRound.value.type === 'SCORE' ? 'ADVANCE_COUNT' : (tables[0]?.targetMode || 'TOP_N')
  const targetCount = mode === 'CHAMPION' ? 1 : mode === 'MEDALS' ? 3 : Math.max(1, Number(tables[0]?.targetCount || 3))
  const index = tables.length
  const table = {
    id: `draft-${Date.now()}-${index}`,
    name: resolveDraftRoundTableName(currentRound.value.roundNo, index, index + 1),
    captainPublicId: '',
    categoryId: null,
    categoryMode: 'EMPTY',
    categoryName: '',
    entryUuids: [],
    members: [],
    participantPublicIds: [],
    targetCount,
    targetMode: mode,
    status: 'DRAFT',
    rankings: buildEmptyRankings(targetCount, mode),
  }
  tables.push(table)
  selectedRoundTableId.value = table.id
}

function removeEntryFromRoundTable(tableId, uuid) {
  if (currentRound.value?.status !== 'DRAFT') return
  const table = currentRoundTables.value.find((item) => item.id === tableId)
  if (!table) return
  table.entryUuids = table.entryUuids.filter((entryUuid) => entryUuid !== uuid)
  syncRoundTableScope(table)
  if (table.rankings?.length) {
    table.rankings.forEach((slot) => {
      if (slot.uuid === uuid) slot.uuid = ''
    })
  }
}

function startEntryDrag(uuid) {
  draggingItem.value = { type: 'entry', uuid }
}

function dropEntryOnRoundTable(tableId) {
  if (!draggingItem.value || draggingItem.value.type !== 'entry') return
  if (currentRound.value?.status !== 'DRAFT') return
  selectedRoundTableId.value = tableId
  addEntryToSelectedRoundTable(draggingItem.value.uuid)
  clearDrag()
}

function removeRoundTable(tableId) {
  if (!currentRound.value || currentRound.value.status !== 'DRAFT') return
  currentRound.value.tables = currentRound.value.tables.filter((table) => table.id !== tableId)
  selectedRoundTableId.value = currentRound.value.tables[0]?.id || ''
}

function resolveDraftRoundTableName(roundNo, index, tableCount) {
  if (tableCount === 1) return roundNo >= 3 ? '决赛桌' : `${roundNo}A桌`
  return `${roundNo}${String.fromCharCode(65 + index)}桌`
}

async function persistCurrentRoundAllocation() {
  if (!currentRound.value || currentRound.value.isPreparationDraft || currentRound.value.status !== 'DRAFT') return true
  const targetRoundId = currentRound.value.id
  const preferredTableId = selectedRoundTableId.value
  const payload = buildRoundAllocationPayload(currentRound.value)
  const detail = await saveRoundAllocation(competition.value.id, targetRoundId, payload)
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState(targetRoundId, { preferredTableId })
  return true
}

function buildRoundAllocationPayload(round) {
  const pool = getPoolEntriesForRound(round)
  round.tables.forEach((table) => syncRoundTableScope(table, pool))
  return {
    tables: round.tables.map((table, index) => ({
      id: Number.isFinite(Number(table.id)) ? Number(table.id) : undefined,
      name: table.name?.trim() || '',
      captainPublicId: table.captainPublicId,
      categoryId: table.categoryId ?? undefined,
      categoryMode: table.categoryMode || (table.categoryId != null ? 'CATEGORY' : 'EMPTY'),
      targetCount: Number(table.targetCount || 1),
      targetMode: table.targetMode || (round.type === 'SCORE' ? 'ADVANCE_COUNT' : 'TOP_N'),
      sortOrder: index,
      participantPublicIds: getTableParticipantPublicIds(table),
      members: round.type === 'SCORE' ? getTableRoundMemberPayload(table) : [],
      entryUuids: table.entryUuids || [],
    })),
  }
}

function getTableRoundMemberPayload(table) {
  return (table?.members || [])
    .filter((member) => member.role !== 'CAPTAIN')
    .map((member) => ({
      judgePublicId: member.judgePublicId,
      role: normalizeScoreJudgeRole(member.role),
    }))
    .filter((member) => member.judgePublicId && member.judgePublicId !== table?.captainPublicId)
}

function publishCurrentRound() {
  if (!currentRound.value) return
  const blockedReason = resolveRoundPublishBlockedReason()
  if (blockedReason) {
    openRoundPublishBlockedDialog(blockedReason)
    return
  }
  roundPublishConfirmOpen.value = true
}

function resolveRoundPublishBlockedReason() {
  if (!currentRound.value) return ''
  if (currentRound.value.isPreparationDraft) {
    return roundPublishDisabledReason.value
      || validationIssues.value[0]
      || roundValidationIssues.value[0]
      || ''
  }
  return roundPublishDisabledReason.value
    || roundValidationIssues.value[0]
    || ''
}

async function confirmPublishCurrentRound() {
  if (!currentRound.value || roundPublishLoading.value) return
  roundPublishLoading.value = true
  try {
    if (currentRound.value.isPreparationDraft) {
      const createdRound = await ensureFirstRoundDraft({ silent: true, preferredMode: allocationMode.value })
      if (!createdRound) return
    }
    const targetRoundId = currentRound.value.id
    if (currentRound.value.status === 'DRAFT') await persistCurrentRoundAllocation()
    const detail = await publishRound(competition.value.id, targetRoundId)
    competition.value = normalizeDetail(detail)
    resetForms()
    applyRoundState(targetRoundId)
    roundPublishConfirmOpen.value = false
    ElMessage.success(currentRound.value.type === 'SCORE' ? '当前轮次已发布到评审端' : '排序任务已发布给桌长和参与评审')
  } finally {
    roundPublishLoading.value = false
  }
}

function closeRoundPublishConfirm() {
  if (roundPublishLoading.value) return
  roundPublishConfirmOpen.value = false
}

function openRoundPublishBlockedDialog(message) {
  roundPublishBlockedMessage.value = message || '请先处理发布前检查'
  roundPublishBlockedDialogOpen.value = true
}

function closeRoundPublishBlockedDialog() {
  roundPublishBlockedDialogOpen.value = false
  roundPublishBlockedMessage.value = ''
}

async function syncCurrentRoundCandidates() {
  if (!currentRound.value || currentRound.value.type !== 'RANKING') return
  if (!currentRound.value.sourceLocked) {
    ElMessage.warning('请先确认上一轮完成')
    return
  }
  const targetRoundId = currentRound.value.id
  if (currentRound.value.status === 'DRAFT') await persistCurrentRoundAllocation()
  const detail = await syncRoundCandidates(competition.value.id, targetRoundId)
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState(targetRoundId)
  allocationMode.value = 'entries'
  ElMessage.success('候选酒款已同步，请核对分桌后发布')
}

function goToRoundAllocation() {
  if (currentRound.value?.id) applyRoundState(currentRound.value.id, { preferredTableId: selectedRoundTableId.value })
  activeTab.value = 'judges'
  allocationMode.value = 'judges'
}

async function completeFirstRoundAction() {
  if (!currentRound.value) return
  const completionStatus = firstRoundCompletionStatus.value
  if (!completionStatus.ready) {
    ElMessage.warning(completionStatus.hint)
    return
  }
  firstRoundCompleteConfirmOpen.value = true
}

function closeFirstRoundCompleteConfirm() {
  if (firstRoundCompleteLoading.value) return
  firstRoundCompleteConfirmOpen.value = false
}

async function confirmCompleteFirstRound() {
  if (!currentRound.value) return
  const targetRoundId = currentRound.value.id
  firstRoundCompleteLoading.value = true
  try {
    const detail = await completeFirstRound(competition.value.id, targetRoundId)
    competition.value = normalizeDetail(detail)
    resetForms()
    applyRoundState(targetRoundId)
    firstRoundCompleteConfirmOpen.value = false
    ElMessage.success(isFeedbackOnlyCompetition.value ? '首轮已完成，评审结果已锁定' : '首轮已完成，晋级名单已生成')
  } finally {
    firstRoundCompleteLoading.value = false
  }
}

async function overrideRoundScoreConfirmation() {
  if (!competition.value?.id || !roundScoreDetailTable.value?.id) return
  openBusinessConfirm({
    action: 'overrideRoundConfirmation',
    kicker: '现场确认',
    title: '确认跳过同桌确认？',
    copy: '该操作会记录为现场确认，并提交本桌结果。适用于评审离场、设备异常等无法完成常规确认的情况，请填写原因，便于赛后追溯',
    summary: [
      { label: '评审桌', value: roundScoreDetailTable.value.name || '-' },
      { label: '当前轮次', value: currentRound.value?.name || '-' },
      { label: '确认进度', value: roundScoreDetailConfirmationText.value },
    ],
    reasonLabel: '现场确认原因',
    reasonPlaceholder: '例如：参与评审设备异常，现场已纸面确认',
    confirmText: '确认并提交本桌',
    loadingText: '记录中',
  })
}

async function runOverrideRoundScoreConfirmation() {
  const detail = await overrideRoundTableConfirmation(competition.value.id, roundScoreDetailTable.value.id, {
    reason: businessConfirm.reason.trim(),
  })
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState(currentRound.value?.id, { preferredTableId: roundScoreDetailTable.value.id })
  ElMessage.success('已记录现场确认，本桌结果已提交')
}

async function lockCurrentRound() {
  if (!currentRound.value || currentRound.value.status !== 'SUBMITTED') return
  roundLockTargetRoundId.value = currentRound.value.id
  roundLockConfirmOpen.value = true
}

async function lockCurrentDraftSourceRound() {
  if (!canLockCurrentDraftSourceRound.value) return
  roundLockTargetRoundId.value = currentDraftSourceRound.value.id
  roundLockConfirmOpen.value = true
}

function closeRoundLockConfirm() {
  if (roundLockLoading.value) return
  roundLockConfirmOpen.value = false
  roundLockTargetRoundId.value = ''
}

async function confirmLockCurrentRound() {
  const targetRound = roundLockTargetRound.value
  if (!targetRound) return
  const targetRoundId = targetRound.id
  const activeBeforeLock = currentRound.value?.id
  const terminal = isTerminalRound(targetRound)
  roundLockLoading.value = true
  try {
    const detail = await lockRound(competition.value.id, targetRoundId)
    competition.value = normalizeDetail(detail)
    resetForms()
    applyRoundState(targetRoundId === activeBeforeLock ? targetRoundId : activeBeforeLock)
    roundLockConfirmOpen.value = false
    roundLockTargetRoundId.value = ''
    if (terminal) {
      activeTab.value = 'results'
      ElMessage.success('决赛轮已锁定，请确认奖项')
      return
    }
    ElMessage.success(`${targetRound.name || '当前轮次'}已锁定`)
  } finally {
    roundLockLoading.value = false
  }
}

async function deleteCurrentDraftRound() {
  if (!currentRound.value || currentRound.value.type !== 'RANKING' || currentRound.value.status !== 'DRAFT') return
  openBusinessConfirm({
    action: 'deleteDraftRound',
    kicker: '草稿轮删除',
    title: `确认删除${currentRound.value.name || '草稿轮'}？`,
    copy: '删除后，该轮的桌次、人员和酒款分配都会移除，需要重新创建本轮编排',
    summary: [
      { label: '当前轮次', value: currentRound.value.name || '-' },
      { label: '桌数/酒款', value: `${currentRoundTables.value.length} 桌 / ${currentRoundEntryCount.value} 款` },
      { label: '轮次状态', value: currentRoundStatusText.value },
    ],
    confirmText: '删除草稿轮',
    loadingText: '删除中',
  })
}

async function runDeleteCurrentDraftRound() {
  const targetRoundId = currentRound.value.id
  const detail = await deleteDraftRound(competition.value.id, targetRoundId)
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState(rounds.value[rounds.value.length - 1]?.id)
  activeTab.value = rounds.value.length ? 'rounds' : 'results'
  ElMessage.success('草稿轮已删除')
}

function openCreateRoundDialog() {
  if (!canCreateNextRound.value) {
    ElMessage.warning('当前已有待处理的轮次')
    return
  }
  resetCreateRoundForm()
  createRoundDialogOpen.value = true
}

function closeCreateRoundDialog() {
  createRoundDialogOpen.value = false
}

async function finishCreateRound() {
  const lastRound = rounds.value[rounds.value.length - 1]
  if (!lastRound) return
  const mode = createRoundForm.targetMode
  const targetCount = mode === 'CHAMPION' ? 1 : mode === 'MEDALS' ? 3 : Math.max(1, Number(createRoundForm.targetCount || 3))
  const tableCount = mode === 'CHAMPION' ? 1 : Math.max(1, Number(createRoundForm.tableCount || 1))
  const detail = await createNextRound(competition.value.id, {
    sourceRoundId: lastRound.id,
    roundName: nextRoundName.value,
    strategy: 'MANUAL',
    tableCount,
    targetMode: mode,
    targetCount,
    captainPublicIds: [],
  })
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState(competition.value.rounds?.[competition.value.rounds.length - 1]?.id)
  activeTab.value = 'judges'
  allocationMode.value = 'judges'
  selectedEntryUuids.value = []
  createRoundDialogOpen.value = false
  ElMessage.success(mode === 'CHAMPION' ? '决赛轮草稿已创建，请安排桌长、参与评审和候选酒款' : `${nextRoundName.value}排序草稿已创建，请安排桌长和参与评审`)
}

function resetCreateRoundForm() {
  const defaultOption = createRoundTargetOptions.value.find((item) => item.value === 'MEDALS') || createRoundTargetOptions.value[0]
  updateCreateRoundTargetMode(defaultOption?.value || 'MEDALS')
}

function updateCreateRoundTargetMode(mode) {
  const option = createRoundTargetOptions.value.find((item) => item.value === mode) || createRoundTargetOptions.value[0]
  createRoundForm.targetMode = option?.value || 'TOP_N'
  createRoundForm.targetCount = option?.fixedTargetCount || option?.defaultTargetCount || 3
  createRoundForm.tableCount = option?.fixedTableCount || createRoundForm.tableCount || 1
}

function buildEmptyRankings(count, mode = 'TOP3') {
  const labels = mode === 'MEDALS' ? ['金奖', '银奖', '铜奖'] : mode === 'CHAMPION' ? ['总冠军'] : ['第 1 名', '第 2 名', '第 3 名']
  return Array.from({ length: count }, (_, index) => ({ rank: index + 1, label: labels[index] || `第 ${index + 1} 名`, uuid: '' }))
}

function getRankingSlots(table) {
  if (table.rankings?.length) return table.rankings
  return buildEmptyRankings(Number(table.targetCount || 3))
}

function getFilledRankingCount(table) {
  return getRankingSlots(table).filter((slot) => slot.uuid).length
}

function buildRoundDetailEntryDisplay(uuid) {
  if (!uuid) return { name: '待选择', meta: '尚未填入酒款' }
  const entry = entryLookup.value.get(uuid) || {}
  const code = entry.shortCode || uuid
  const category = entry.categoryName || entry.category || '-'
  return {
    name: entry.name || '未命名酒款',
    meta: `${code} · ${category}`,
  }
}

function setRankingSlot(tableId, rank, uuid) {
  const table = currentRoundTables.value.find((item) => item.id === tableId)
  if (!table || !table.entryUuids.includes(uuid)) return
  if (!table.rankings?.length) {
    table.rankings = buildEmptyRankings(Number(table.targetCount || 3), table.targetMode)
  }
  table.rankings.forEach((slot) => {
    if (slot.uuid === uuid) slot.uuid = ''
  })
  const targetSlot = table.rankings.find((slot) => slot.rank === rank)
  if (targetSlot) targetSlot.uuid = uuid
}

function clearRankingSlot(tableId, rank) {
  const table = currentRoundTables.value.find((item) => item.id === tableId)
  const targetSlot = table?.rankings?.find((slot) => slot.rank === rank)
  if (targetSlot) targetSlot.uuid = ''
}

function submitRankingRound() {
  if (!currentRound.value || currentRound.value.type !== 'RANKING') return
  ElMessage.warning('排序结果需要由桌长在评审端提交')
}

function refreshAdvancedPoolFromRound(round) {
  if (!round || round.type !== 'RANKING') return
  const advancedUuids = new Set(
    round.tables.flatMap((table) => getRankingSlots(table).map((slot) => slot.uuid).filter(Boolean)),
  )
  roundEntryPool.value.forEach((entry) => {
    entry.advanced = advancedUuids.has(entry.uuid)
    if (entry.advanced) {
      const table = round.tables.find((item) => item.entryUuids.includes(entry.uuid))
      const slot = table ? getRankingSlots(table).find((item) => item.uuid === entry.uuid) : null
      entry.sourceTable = table?.name || ''
      entry.sourceResult = slot?.label || '晋级'
    }
  })
}

function buildAwardDrafts() {
  const latestRankingRound = [...rounds.value].reverse().find((round) => round.type === 'RANKING')
  const categories = [...new Set(roundEntryPool.value.map((entry) => entry.categoryName))]
  return categories.flatMap((category) => {
    const categoryEntries = roundEntryPool.value.filter((entry) => entry.categoryName === category && entry.advanced)
    const rankedUuid = latestRankingRound?.tables.flatMap((table) => getRankingSlots(table)).find((slot) => categoryEntries.some((entry) => entry.uuid === slot.uuid))?.uuid
    return ['金奖', '银奖', '铜奖'].map((slot, index) => ({ category, slot, uuid: index === 0 ? rankedUuid || categoryEntries[index]?.uuid : categoryEntries[index]?.uuid || '' }))
  }).filter((item) => item.uuid)
}

function getEntryAward(uuid) {
  const entry = roundEntryPool.value.find((item) => item.uuid === uuid)
  const awards = awardDrafts.value.filter((award) => award.uuid === uuid || (entry && award.beerEntryId === entry.id))
  return awards.map((award) => award.awardName).filter(Boolean).join(' / ')
}

function formatAwardStatus(award) {
  if (award.status === 'CONFIRMED') return '已确认'
  if (award.status === 'PUBLISHED') return '已发布至厂牌端'
  return '待确认'
}

function canEditAwardSelection(award) {
  return Boolean(award) && !resultsPublished.value && !isAwardConfirmedOrPublished(award)
}

function isAwardConfirmedOrPublished(award) {
  return ['CONFIRMED', 'PUBLISHED'].includes(award?.status)
}

function formatAwardEntryName(award) {
  const entry = resolveAwardEntry(award)
  if (!entry) return '待选择'
  return formatAwardEntryLabel(entry)
}

function resolveAwardEntry(award) {
  return roundEntryPool.value.find((item) => item.id === award?.beerEntryId)
    || competition.value?.entries?.find((item) => item.id === award?.beerEntryId)
    || null
}

function formatAwardScope(award) {
  if (award?.awardType === 'CHAMPION') return '全场总冠军'
  return award?.categoryName || '未分组'
}

function formatAwardEntryLabel(entry) {
  const beerName = entry?.name || entry?.style || entry?.uuid || '未命名酒款'
  const breweryName = entry?.breweryCompanyName || '未关联厂牌'
  return `${beerName} · ${breweryName}`
}

function awardEntryOptions(award) {
  if (award.awardType === 'CHAMPION') {
    const goldEntryIds = awardDrafts.value
      .filter((item) => item.awardType === 'MEDAL' && (item.rankNo === 1 || item.awardName === '金奖'))
      .map((item) => item.beerEntryId)
      .filter(Boolean)
    const goldEntries = roundEntryPool.value.filter((entry) => goldEntryIds.includes(entry.id))
    return goldEntries.length ? goldEntries : roundEntryPool.value.filter((entry) => entry.advanced)
  }
  return roundEntryPool.value.filter((entry) => !award.categoryId || entry.categoryId === award.categoryId)
}

function getEntryPath(uuid) {
  const parts = []
  rounds.value.forEach((round) => {
    const table = round.tables.find((item) => item.entryUuids.includes(uuid))
    if (!table) return
    if (round.type === 'SCORE') {
      const scoreResult = isFeedbackOnlyCompetition.value
        ? '诊断'
        : (roundEntryPool.value.find((entry) => entry.uuid === uuid)?.advanced ? '晋级' : '待定')
      parts.push(`${round.name} ${table.name} ${scoreResult}`)
    } else {
      const slot = getRankingSlots(table).find((item) => item.uuid === uuid)
      parts.push(`${round.name} ${table.name}${slot ? ` ${slot.label}` : ''}`)
    }
  })
  return parts.join(' -> ') || '-'
}

function getAwardPath(award) {
  const entry = resolveAwardEntry(award)
  return entry ? getEntryPath(entry.uuid) : '-'
}

function openBusinessConfirm(config) {
  Object.assign(businessConfirm, {
    open: true,
    loading: false,
    action: '',
    kicker: '',
    title: '',
    copy: '',
    summary: [],
    confirmText: '确认',
    cancelText: '取消',
    loadingText: '处理中',
    tone: 'warning',
    reasonLabel: '',
    reasonPlaceholder: '',
    reason: '',
    reasonMaxLength: 255,
    deadlineLabel: '',
    deadlineValue: '',
    deadlineRequired: false,
    payload: null,
    ...config,
  })
}

function closeBusinessConfirm() {
  if (businessConfirm.loading) return
  businessConfirm.open = false
}

function competitionSummaryItems(extra = []) {
  return [
    { label: '比赛', value: competition.value?.name || '-' },
    { label: '当前阶段', value: statusInfo.value?.label || competition.value?.status || '-' },
    ...extra,
  ]
}

function awardSummaryItems(extra = []) {
  return [
    { label: '奖项草稿', value: `${awardDrafts.value.length} 项` },
    { label: '组别奖项', value: `${medalAwards.value.length} 项` },
    { label: '总冠军', value: championAwards.value.some((award) => award.beerEntryId) ? '已选择' : '待选择' },
    ...extra,
  ]
}

async function confirmBusinessAction() {
  if (!businessConfirm.action || businessConfirmReasonInvalid.value || businessConfirmDeadlineInvalid.value) return
  businessConfirm.loading = true
  try {
    if (businessConfirm.action === 'publishResults') await runPublishResults()
    if (businessConfirm.action === 'generateAwards') await runGenerateAwards()
    if (businessConfirm.action === 'confirmAwards') await runConfirmAwards()
    if (businessConfirm.action === 'deleteCompetition') await runDeleteCompetition()
    if (businessConfirm.action === 'deleteDraftRound') await runDeleteCurrentDraftRound()
    if (businessConfirm.action === 'overrideRoundConfirmation') await runOverrideRoundScoreConfirmation()
    if (businessConfirm.action === 'prepareJudging') await runPrepareJudging()
    if (businessConfirm.action === 'reopenRegistration') await runReopenRegistration()
    if (businessConfirm.action === 'returnToSampleCheck') await runReturnToSampleCheck()
    if (businessConfirm.action === 'replaceCertificate') runChooseAwardCertificate(businessConfirm.payload?.award)
    if (businessConfirm.action === 'deleteCertificate') await runDeleteAwardCertificate(businessConfirm.payload?.award)
    businessConfirm.open = false
  } finally {
    businessConfirm.loading = false
  }
}

async function publishResultsAction() {
  const feedbackOnly = isFeedbackOnlyCompetition.value
  openBusinessConfirm({
    action: 'publishResults',
    kicker: feedbackOnly ? '诊断发布' : '结果发布',
    title: feedbackOnly ? '确认发布诊断结果？' : '确认发布到厂牌端？',
    copy: feedbackOnly
      ? '发布后，厂商可查看评分、评语和桌长综合诊断，公开结果页会展示诊断名单。请确认首轮结果已经核对完成'
      : '发布后，厂牌端和公开结果页将展示获奖名单、奖项信息和已上传奖状，请确认奖项结果已经核对完成',
    summary: feedbackOnly
      ? competitionSummaryItems([
        { label: '诊断记录', value: `${feedbackFinalizedCount.value} / ${feedbackEntryCount.value} 款` },
        { label: '发布检查', value: `${resultChecks.value.filter((item) => item.done).length} / ${resultChecks.value.length}` },
      ])
      : competitionSummaryItems([
        { label: '奖项确认', value: `${resultChecks.value.filter((item) => item.done).length} / ${resultChecks.value.length}` },
        { label: '奖状 PDF', value: `${certificateUploadedCount.value} / ${certificateTotalCount.value}` },
      ]),
    confirmText: feedbackOnly ? '发布诊断结果' : '发布结果',
    loadingText: '发布中',
  })
}

async function runPrepareJudging() {
  const detail = await prepareCompetitionJudging(competition.value.id)
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState()
  activeTab.value = 'judges'
  ElMessage.success('已进入评审准备')
}

async function runReopenRegistration() {
  const detail = await reopenCompetitionRegistration(competition.value.id, {
    reason: businessConfirm.reason.trim(),
    registrationDeadline: businessConfirm.deadlineValue ? toBackendDateTime(businessConfirm.deadlineValue) : null,
  })
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState()
  activeTab.value = 'baseInfo'
  ElMessage.success('报名窗口已恢复')
}

async function runReturnToSampleCheck() {
  const detail = await returnCompetitionToSampleCheck(competition.value.id, {
    reason: businessConfirm.reason.trim(),
  })
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState()
  activeTab.value = 'entries'
  ElMessage.success('已退回样品入库核对，请重新确认入库状态')
}

function prepareJudgingAction() {
  openBusinessConfirm({
    action: 'prepareJudging',
    kicker: '评审准备',
    title: '确认进入评审准备中？',
    copy: '进入后将开始按入库酒款和评审配置安排首轮，若发现仍需继续接收报名，需要先退回样品入库核对后再重新开放报名',
    summary: competitionSummaryItems([
      { label: '已入库酒款', value: `${competition.value?.entriesSummary?.stored ?? 0} 款` },
      { label: '评审桌', value: `${competition.value?.judgeTables?.length ?? 0} 张` },
      { label: '评分表', value: `${competition.value?.scoreConfigs?.length ?? 0} 套` },
    ]),
    confirmText: '进入评审准备中',
    loadingText: '处理中',
  })
}

function reopenRegistrationAction() {
  const oldDeadline = competition.value?.registrationDeadline
  const deadlineExpired = !oldDeadline || (parseDateTime(oldDeadline) || 0) <= Date.now()
  openBusinessConfirm({
    action: 'reopenRegistration',
    kicker: '报名恢复',
    title: '确认重新开放报名？',
    copy: '重新开放后，厂牌端会恢复本场比赛的报名入口，请确认报名截止时间和现场安排仍然匹配',
    summary: competitionSummaryItems([
      { label: '当前截止', value: oldDeadline ? formatDateTime(oldDeadline) : '未设置' },
      { label: '报名酒款', value: `${competition.value?.entriesSummary?.total ?? 0} 款` },
    ]),
    reasonLabel: '操作原因',
    reasonPlaceholder: '例如：误点截止报名，现场确认继续开放',
    reasonMaxLength: 120,
    deadlineLabel: deadlineExpired ? '新的报名截止时间' : '新的报名截止时间（可选）',
    deadlineValue: deadlineExpired ? '' : toInputDateTime(oldDeadline),
    deadlineRequired: deadlineExpired,
    confirmText: '重新开放报名',
    loadingText: '恢复中',
  })
}

function returnToSampleCheckAction() {
  openBusinessConfirm({
    action: 'returnToSampleCheck',
    kicker: '阶段退回',
    title: '确认退回样品入库核对？',
    copy: '退回后，比赛回到报名截止后的核对状态；已有首轮草稿会保留，发布前请重新核对入库酒款和分桌',
    summary: competitionSummaryItems([
      { label: '已入库酒款', value: `${competition.value?.entriesSummary?.stored ?? 0} 款` },
      { label: '首轮草稿', value: firstRoundCreated.value ? '已保留' : '未创建' },
    ]),
    reasonLabel: '退回原因',
    reasonPlaceholder: '例如：发现仍有样品入库状态需要核对',
    reasonMaxLength: 120,
    confirmText: '退回样品入库核对',
    loadingText: '退回中',
  })
}

async function runPublishResults() {
  const detail = await publishCompetitionResults(competition.value.id)
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState()
  ElMessage.success(isFeedbackOnlyCompetition.value ? '诊断结果已发布' : '结果已发布')
}

async function downloadScoringData() {
  if (!competition.value?.id) return
  const blob = await exportCompetitionScoringData(competition.value.id)
  downloadBlob(blob, `${competition.value.name || '比赛'}-评分数据.xlsx`)
}

async function chooseAwardCertificate(award) {
  if (!award?.id) {
    ElMessage.warning('请先确认奖项后再上传奖状')
    return
  }
  if (!canUploadAwardCertificate(award)) {
    ElMessage.warning('请先确认奖项后再上传奖状')
    return
  }
  if (award.status === 'PUBLISHED' && award.certificateUploaded) {
    openBusinessConfirm({
      action: 'replaceCertificate',
      kicker: '奖状变更',
      title: '确认更换已发布奖状？',
      copy: '更换后，厂牌端下载到的奖状会同步变化，请确认新文件已经核对无误',
      summary: awardSummaryItems([
        { label: '奖项', value: award.awardName || formatAwardScope(award) },
        { label: '当前文件', value: award.certificateFilename || '已上传' },
      ]),
      confirmText: '更换奖状',
      loadingText: '处理中',
      payload: { award },
    })
    return
  }
  runChooseAwardCertificate(award)
}

function runChooseAwardCertificate(award) {
  if (!award) return
  certificateTargetAward.value = award
  if (awardCertificateInput.value) {
    awardCertificateInput.value.value = ''
    awardCertificateInput.value.click()
  }
}

async function handleAwardCertificateSelected(event) {
  const input = event.target
  const file = input.files?.[0]
  const award = certificateTargetAward.value
  certificateTargetAward.value = null
  if (!file || !award) return
  if (!isPdfFile(file)) {
    ElMessage.warning('奖状文件必须是 PDF')
    input.value = ''
    return
  }
  setCertificateActionBusy(award.id, true)
  try {
    const updated = await uploadAwardCertificate(competition.value.id, award.id, file)
    Object.assign(award, updated)
    ElMessage.success('奖状已上传')
  } finally {
    setCertificateActionBusy(award.id, false)
    input.value = ''
  }
}

async function previewAwardCertificate(award) {
  if (!award?.id) return
  setCertificateActionBusy(award.id, true)
  try {
    const blob = await downloadAwardCertificate(competition.value.id, award.id)
    const url = window.URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }))
    window.open(url, '_blank', 'noopener,noreferrer')
    window.setTimeout(() => window.URL.revokeObjectURL(url), 30000)
  } finally {
    setCertificateActionBusy(award.id, false)
  }
}

async function deleteAwardCertificateAction(award) {
  if (!award?.id) return
  const message = award.status === 'PUBLISHED'
    ? '删除后，厂牌端将不能下载这份奖状'
    : '删除后，这个奖项将回到未上传奖状状态'
  openBusinessConfirm({
    action: 'deleteCertificate',
    kicker: '奖状删除',
    title: '确认删除奖状？',
    copy: message,
    summary: awardSummaryItems([
      { label: '奖项', value: award.awardName || formatAwardScope(award) },
      { label: '文件', value: award.certificateFilename || '已上传' },
    ]),
    confirmText: '删除奖状',
    loadingText: '删除中',
    payload: { award },
  })
}

async function runDeleteAwardCertificate(award) {
  if (!award?.id) return
  setCertificateActionBusy(award.id, true)
  try {
    await deleteAwardCertificate(competition.value.id, award.id)
    Object.assign(award, {
      certificateUploaded: false,
      certificateFilename: null,
      certificateUploadedAt: null,
      certificateDownloadUrl: null,
    })
    ElMessage.success('奖状已删除')
  } finally {
    setCertificateActionBusy(award.id, false)
  }
}

function canUploadAwardCertificate(award) {
  return ['CONFIRMED', 'PUBLISHED'].includes(award?.status)
}

function isCertificateActionBusy(award) {
  return certificateActionIds.value.has(award?.id)
}

function setCertificateActionBusy(awardId, busy) {
  const next = new Set(certificateActionIds.value)
  if (busy) next.add(awardId)
  else next.delete(awardId)
  certificateActionIds.value = next
}

function isPdfFile(file) {
  return file.type === 'application/pdf' || file.name?.toLowerCase().endsWith('.pdf')
}

function downloadBlob(blob, fileName) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

async function generateAwardsAction() {
  openBusinessConfirm({
    action: 'generateAwards',
    kicker: '奖项生成',
    title: awardDrafts.value.length ? '确认重新生成获奖名单？' : '确认生成获奖名单？',
    copy: awardDrafts.value.length
      ? '将按已锁定的轮次结果重新生成奖项草稿，当前未确认的选择可能会被覆盖，生成后仍可在确认前调整'
      : '将按已锁定的轮次结果生成奖项草稿，生成后可在确认获奖名单前继续核对和调整',
    summary: competitionSummaryItems([
      { label: '决赛轮', value: finalRoundLocked.value ? '已锁定' : '未锁定' },
      { label: '现有草稿', value: `${awardDrafts.value.length} 项` },
    ]),
    confirmText: awardDrafts.value.length ? '重新生成' : '生成名单',
    loadingText: '生成中',
  })
}

async function runGenerateAwards() {
  await generateCompetitionAwards(competition.value.id)
  await loadDetail()
  activeTab.value = 'results'
  ElMessage.success('奖项草稿已生成')
}

async function confirmAwardsAction() {
  openBusinessConfirm({
    action: 'confirmAwards',
    kicker: '奖项确认',
    title: '确认获奖名单？',
    copy: '确认后，获奖名单将进入正式结果流程，并用于奖状上传和最终发布，请确认总冠军和组别奖项已经核对无误',
    summary: awardSummaryItems([
      { label: '可确认奖项', value: `${confirmableAwardDrafts.value.length} 项` },
    ]),
    confirmText: '确认获奖名单',
    loadingText: '确认中',
  })
}

async function runConfirmAwards() {
  const awards = confirmableAwardDrafts.value.map((award) => ({
    id: award.id,
    categoryId: award.categoryId,
    beerEntryId: award.beerEntryId,
    awardRuleId: award.awardRuleId,
    awardType: award.awardType,
    awardName: award.awardName,
    rankNo: award.rankNo,
    sourceRoundId: award.sourceRoundId,
    sourceRoundTableId: award.sourceRoundTableId,
    sourceResultId: award.sourceResultId,
  }))
  await confirmCompetitionAwards(competition.value.id, { awards })
  await loadDetail()
  activeTab.value = 'results'
  ElMessage.success('奖项已确认')
}

function openAdminEntries() {
  router.push({ path: '/admin/entries', query: { competitionId: competition.value.id } })
}

function openAdminEntry(entry) {
  router.push({ path: '/admin/entries', query: { competitionId: competition.value.id, entryId: entry.id } })
}

async function markEntryStoredAction(entry) {
  if (isRefundedEntry(entry)) return
  try {
    await ElMessageBox.confirm(
      `确认「${entry.name || entry.shortCode || entry.uuid}」样品已经到场并完成入库吗？确认后该酒款将进入后续分桌和评审准备流程`,
      '确认样品入库？',
      {
        confirmButtonText: '确认入库',
        cancelButtonText: '再核对',
        type: 'warning',
      },
    )
  } catch {
    return
  }
  await markEntryStored(entry.id)
  await loadDetail()
  ElMessage.success(`${entry.name || entry.uuid} 已确认入库`)
}

function addEntryField() {
  entryFieldForm.push({
    localId: `new-field-${Date.now()}`,
    fieldKey: createEntryFieldKey(),
    fieldLabel: '',
    fieldType: 'text',
    helpText: '',
    options: [],
    required: false,
    visibleToJudges: true,
  })
}

function createEntryFieldKey() {
  return `custom_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`
}

function addJudgeTable() {
  if (!canEditBaseJudgeTables.value) return
  const code = String.fromCharCode(65 + judgeTableForm.length)
  const table = { localId: `new-table-${Date.now()}`, tableName: `${code}桌`, captainCount: 0, professionalCount: 0, crossCount: 0 }
  judgeTableForm.push(table)
  selectedTableLocalId.value = table.localId
}

function removeItem(list, index) {
  list.splice(index, 1)
}

function removeJudgeTable(index) {
  if (!canEditBaseJudgeTables.value) return
  const [table] = judgeTableForm.splice(index, 1)
  if (table) {
    for (let i = judgeAssignmentForm.length - 1; i >= 0; i -= 1) {
      if (judgeAssignmentForm[i].tableLocalId === table.localId) judgeAssignmentForm.splice(i, 1)
    }
  }
  selectedTableLocalId.value = judgeTableForm[0]?.localId || null
}

function assignmentsForTable(table, role) {
  return judgeAssignmentForm.filter((assignment) => assignment.tableLocalId === table.localId && (!role || assignment.role === role))
}

function selectAssignmentTarget(table, role) {
  selectedTableLocalId.value = table.localId
  selectedRole.value = role
}

function addJudgeToTarget(judge) {
  if (!canEditBaseJudgeTables.value) return
  if (!selectedTable.value) {
    ElMessage.warning('请先选择要加入的基础桌')
    return
  }
  if (!isJudgeActive(judge)) {
    ElMessage.warning('停用评审不能加入本场比赛')
    return
  }
  const existing = judgeAssignmentForm.find((assignment) => assignment.judgePublicId === judge.publicId)
  if (existing) {
    existing.tableLocalId = selectedTable.value.localId
    existing.role = selectedRole.value
    ElMessage.success(`${judge.name}已移动到${selectedTargetLabel.value}`)
    return
  }
  judgeAssignmentForm.push({
    localId: `assignment-${Date.now()}-${judge.publicId}`,
    tableLocalId: selectedTable.value.localId,
    judgePublicId: judge.publicId,
    role: selectedRole.value,
  })
}

function removeAssignment(assignment) {
  if (!canEditBaseJudgeTables.value) return
  const index = judgeAssignmentForm.findIndex((item) => item.localId === assignment.localId)
  if (index >= 0) judgeAssignmentForm.splice(index, 1)
}

function countAssignedRole(role) {
  return judgeAssignmentForm.filter((assignment) => assignment.role === role).length
}

function startJudgeDrag(judge) {
  const canDragRoundJudge = currentRound.value?.status === 'DRAFT'
    && (currentRound.value?.type === 'RANKING' || (currentRound.value?.type === 'SCORE' && !currentRound.value?.isPreparationDraft))
  if (!canDragRoundJudge && !canEditBaseJudgeTables.value) return
  if (!isJudgeActive(judge)) return
  draggingItem.value = { type: 'judge', judgePublicId: judge.publicId }
}

function startAssignmentDrag(assignment) {
  if (!canEditBaseJudgeTables.value) return
  draggingItem.value = { type: 'assignment', localId: assignment.localId, judgePublicId: assignment.judgePublicId }
}

function clearDrag() {
  draggingItem.value = null
}

function dropOnRole(table, role) {
  if (!canEditBaseJudgeTables.value || !draggingItem.value) return
  selectedTableLocalId.value = table.localId
  selectedRole.value = role
  if (draggingItem.value.type === 'assignment') {
    const assignment = judgeAssignmentForm.find((item) => item.localId === draggingItem.value.localId)
    if (assignment) {
      assignment.tableLocalId = table.localId
      assignment.role = role
    }
    clearDrag()
    return
  }
  const judge = getJudge(draggingItem.value.judgePublicId)
  if (judge) addJudgeToTarget(judge)
  clearDrag()
}

function tableValidationIssues(table) {
  const issues = []
  const captainCount = assignmentsForTable(table, 'CAPTAIN').length
  const totalCount = assignmentsForTable(table).length
  if (!table.tableName?.trim()) issues.push('基础桌名称不能为空')
  if (captainCount === 0) issues.push(`${table.tableName || '未命名基础桌'}缺少桌长`)
  if (captainCount > 1) issues.push(`${table.tableName || '未命名基础桌'}有 ${captainCount} 名桌长`)
  if (totalCount === 0) issues.push(`${table.tableName || '未命名基础桌'}尚未分配评审`)
  return issues
}

function getJudgeAssignmentSummary(judgePublicId) {
  const assignment = judgeAssignmentForm.find((item) => item.judgePublicId === judgePublicId)
  if (!assignment) return ''
  const table = judgeTableForm.find((item) => item.localId === assignment.tableLocalId)
  const roleLabel = roleLabels[assignment.role] || assignment.role
  return [table?.tableName, roleLabel].filter(Boolean).join(' · ')
}

function getJudge(judgePublicId) {
  return judgePool.value.find((judge) => judge.publicId === judgePublicId)
}

function getJudgeInitial(name) {
  return name?.trim()?.slice(0, 1) || '评'
}

function isJudgeActive(judge) {
  return Number(judge.status) === 1
}

function isAssigned(judgePublicId) {
  return judgeAssignmentForm.some((assignment) => assignment.judgePublicId === judgePublicId)
}

function inferJudgeRoles(judge) {
  const qualification = `${judge.qualification || ''}${judge.name || ''}`.toLowerCase()
  const roles = []
  if (qualification.includes('桌长') || qualification.includes('bjcp') || qualification.includes('captain')) roles.push('CAPTAIN')
  if (qualification.includes('专业') || qualification.includes('酿酒') || qualification.includes('bjcp') || qualification.includes('judge')) roles.push('PROFESSIONAL')
  if (qualification.includes('跨界') || qualification.includes('媒体') || qualification.includes('餐饮') || qualification.includes('大众')) roles.push('CROSS')
  return roles.length ? roles : ['PROFESSIONAL']
}

async function saveJudgeDraft(options = {}) {
  if (!canEditBaseJudgeTables.value) {
    if (!options.silent) ElMessage.warning('首轮已生成，基础评审桌已锁定')
    return false
  }
  if (validationIssues.value.length && !options.allowIncomplete) {
    ElMessage.warning(`还有 ${validationIssues.value.length} 项评审配置需要处理`)
    return false
  }
  const assignmentDraft = judgeAssignmentForm
    .map((assignment) => ({
      tableName: judgeTableForm.find((table) => table.localId === assignment.tableLocalId)?.tableName,
      judgePublicId: assignment.judgePublicId,
      role: assignment.role,
    }))
    .filter((assignment) => assignment.tableName?.trim())
  let detail = await updateCompetitionJudgeTables(competition.value.id, {
    items: judgeTableForm.filter((table) => table.tableName).map((table) => ({ tableName: table.tableName })),
  })
  const tableByName = new Map((detail.judgeTables || []).map((table) => [table.tableName, table]))
  const items = assignmentDraft
    .map((assignment) => ({
      tableId: tableByName.get(assignment.tableName)?.id,
      judgePublicId: assignment.judgePublicId,
      role: assignment.role,
    }))
    .filter((assignment) => assignment.tableId)
  await updateCompetitionJudgeAssignments(competition.value.id, { items })
  detail = await fetchCompetitionDetail(competition.value.id)
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState()
  if (!options.silent) ElMessage.success('评审人员已保存')
  return true
}

async function saveBaseInfo() {
  if (!baseForm.description) {
    ElMessage.warning('请填写赛事简介')
    return
  }
  if (baseForm.rulesUrl && !isValidHttpUrl(baseForm.rulesUrl)) {
    ElMessage.warning('参赛细则链接必须以 http:// 或 https:// 开头')
    return
  }
  if (!baseForm.name || !baseForm.code || !baseForm.competitionDate || !baseForm.registrationDeadline) {
    ElMessage.warning('请补齐比赛名称、编号、比赛日期和报名截止时间')
    return
  }
  const hasEarlyBirdFee = baseForm.earlyBirdFee !== '' && baseForm.earlyBirdFee !== null && baseForm.earlyBirdFee !== undefined
  const hasEarlyBirdDeadline = Boolean(baseForm.earlyBirdDeadline)
  if (hasEarlyBirdFee !== hasEarlyBirdDeadline) {
    ElMessage.warning('早鸟价和早鸟价截止时间需要同时填写')
    return
  }
  if (hasEarlyBirdFee) {
    if (Number(baseForm.earlyBirdFee) > Number(baseForm.entryFee || 0)) {
      ElMessage.warning('早鸟价不能高于报名费')
      return
    }
    const start = parseDateTime(baseForm.registrationStart)
    const earlyDeadline = parseDateTime(baseForm.earlyBirdDeadline)
    const registrationDeadline = parseDateTime(baseForm.registrationDeadline)
    if ((start && earlyDeadline <= start) || (registrationDeadline && earlyDeadline > registrationDeadline)) {
      ElMessage.warning('早鸟价截止时间需在报名窗口内')
      return
    }
  }
  const detail = await updateCompetitionBaseInfo(competition.value.id, {
    name: baseForm.name,
    code: baseForm.code,
    competitionDate: baseForm.competitionDate,
    registrationStart: toBackendDateTime(baseForm.registrationStart),
    registrationDeadline: toBackendDateTime(baseForm.registrationDeadline),
    entryFee: Number(baseForm.entryFee || 0),
    earlyBirdFee: hasEarlyBirdFee ? Number(baseForm.earlyBirdFee) : null,
    earlyBirdDeadline: toBackendDateTime(baseForm.earlyBirdDeadline),
    description: baseForm.description,
    rulesUrl: baseForm.rulesUrl || null,
    deliveryMethod: baseForm.deliveryMethod,
    sampleArrivalStart: toBackendDateTime(baseForm.sampleArrivalStart),
    sampleArrivalDeadline: toBackendDateTime(baseForm.sampleArrivalDeadline),
    sampleQuantityNote: baseForm.sampleQuantityNote,
    deliveryRecipient: baseForm.deliveryRecipient,
    deliveryPhone: baseForm.deliveryPhone,
    deliveryAddress: baseForm.deliveryAddress,
    deliveryNote: baseForm.deliveryNote,
    logisticsVisibility: baseForm.logisticsVisibility,
  })
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState()
  ElMessage.success('基础信息已保存')
}

function addSponsor() {
  const index = sponsorForm.length
  sponsorForm.push({
    localId: `sponsor-new-${Date.now()}-${index}`,
    tierLabel: index === 0 ? '主办方' : '赞助商',
    sponsorName: '',
    logoAssetId: null,
    logoUrl: '',
    sortOrder: index,
    featured: index < 2,
    enabled: true,
    uploading: false,
  })
}

function removeSponsor(index) {
  sponsorForm.splice(index, 1)
}

function setSponsorLogoInputRef(el, localId) {
  if (!localId) return
  if (el) sponsorLogoInputRefs.set(localId, el)
  else sponsorLogoInputRefs.delete(localId)
}

function triggerSponsorLogoInput(localId) {
  sponsorLogoInputRefs.get(localId)?.click()
}

async function handleSponsorLogoChange(event, sponsor) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file || !competition.value?.id) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.warning('Logo 仅支持 JPG、PNG、WebP 图片')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('Logo 图片不能超过 5MB')
    return
  }
  openSponsorLogoCrop(file, sponsor)
}

function openSponsorLogoCrop(file, sponsor) {
  closeSponsorLogoCrop()
  sponsorLogoCrop.open = true
  sponsorLogoCrop.sponsor = sponsor
  sponsorLogoCrop.file = file
  sponsorLogoCrop.previewUrl = URL.createObjectURL(file)
  sponsorLogoCrop.imageWidth = 0
  sponsorLogoCrop.imageHeight = 0
  sponsorLogoCrop.scale = 1
  sponsorLogoCrop.x = 0
  sponsorLogoCrop.y = 0
  sponsorLogoCrop.dragging = false
  sponsorLogoCrop.uploading = false
}

function closeSponsorLogoCrop() {
  if (sponsorLogoCrop.previewUrl) URL.revokeObjectURL(sponsorLogoCrop.previewUrl)
  sponsorLogoCrop.open = false
  sponsorLogoCrop.sponsor = null
  sponsorLogoCrop.file = null
  sponsorLogoCrop.previewUrl = ''
  sponsorLogoCrop.dragging = false
  sponsorLogoCrop.uploading = false
}

function cancelSponsorLogoCrop() {
  if (sponsorLogoCrop.uploading) return
  closeSponsorLogoCrop()
}

function handleSponsorLogoPreviewLoad(event) {
  sponsorLogoCrop.imageWidth = event.target.naturalWidth || 1
  sponsorLogoCrop.imageHeight = event.target.naturalHeight || 1
  resetSponsorLogoCrop()
}

function resetSponsorLogoCrop() {
  sponsorLogoCrop.scale = 1
  sponsorLogoCrop.x = 0
  sponsorLogoCrop.y = 0
}

function startSponsorLogoDrag(event) {
  if (!sponsorLogoCrop.previewUrl || sponsorLogoCrop.uploading) return
  sponsorLogoCrop.dragging = true
  sponsorLogoCrop.dragStartX = event.clientX
  sponsorLogoCrop.dragStartY = event.clientY
  sponsorLogoCrop.startX = sponsorLogoCrop.x
  sponsorLogoCrop.startY = sponsorLogoCrop.y
  event.currentTarget.setPointerCapture?.(event.pointerId)
}

function moveSponsorLogoDrag(event) {
  if (!sponsorLogoCrop.dragging) return
  const bounds = event.currentTarget.getBoundingClientRect()
  const deltaXPercent = ((event.clientX - sponsorLogoCrop.dragStartX) / bounds.width) * 100
  const deltaYPercent = ((event.clientY - sponsorLogoCrop.dragStartY) / bounds.height) * 100
  sponsorLogoCrop.x = sponsorLogoCrop.startX + deltaXPercent
  sponsorLogoCrop.y = sponsorLogoCrop.startY + deltaYPercent
  clampSponsorLogoCropOffset()
}

function endSponsorLogoDrag(event) {
  if (!sponsorLogoCrop.dragging) return
  sponsorLogoCrop.dragging = false
  event.currentTarget.releasePointerCapture?.(event.pointerId)
}

function zoomSponsorLogoCrop(event) {
  if (!sponsorLogoCrop.previewUrl || sponsorLogoCrop.uploading) return
  const nextScale = sponsorLogoCrop.scale + (event.deltaY > 0 ? -0.08 : 0.08)
  sponsorLogoCrop.scale = Math.min(3, Math.max(1, Number(nextScale.toFixed(2))))
  clampSponsorLogoCropOffset()
}

function clampSponsorLogoCropOffset() {
  const { maxX, maxY } = sponsorLogoCropGeometry.value
  sponsorLogoCrop.x = Math.min(maxX, Math.max(-maxX, Number(sponsorLogoCrop.x) || 0))
  sponsorLogoCrop.y = Math.min(maxY, Math.max(-maxY, Number(sponsorLogoCrop.y) || 0))
}

async function confirmSponsorLogoCrop() {
  if (!sponsorLogoCrop.sponsor || !competition.value?.id) return
  const targetSponsor = sponsorLogoCrop.sponsor
  sponsorLogoCrop.uploading = true
  targetSponsor.uploading = true
  try {
    const croppedFile = await buildSponsorLogoCropFile()
    const uploaded = await uploadCompetitionSponsorLogo(competition.value.id, croppedFile)
    targetSponsor.logoAssetId = uploaded.fileAssetId
    targetSponsor.logoUrl = uploaded.publicUrl
    ElMessage.success('Logo 已上传')
    closeSponsorLogoCrop()
  } catch (error) {
    ElMessage.error(error?.message || 'Logo 上传失败')
  } finally {
    targetSponsor.uploading = false
    sponsorLogoCrop.uploading = false
  }
}

function buildSponsorLogoCropFile() {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => {
      const canvas = document.createElement('canvas')
      canvas.width = sponsorLogoCropOutputWidth
      canvas.height = sponsorLogoCropOutputHeight
      const context = canvas.getContext('2d')
      if (!context) {
        reject(new Error('无法处理 Logo 图片'))
        return
      }
      context.fillStyle = '#ffffff'
      context.fillRect(0, 0, canvas.width, canvas.height)
      const { widthPercent, heightPercent } = sponsorLogoCropGeometry.value
      const drawWidth = canvas.width * (widthPercent / 100)
      const drawHeight = canvas.height * (heightPercent / 100)
      const drawX = (canvas.width - drawWidth) / 2 + canvas.width * (sponsorLogoCrop.x / 100)
      const drawY = (canvas.height - drawHeight) / 2 + canvas.height * (sponsorLogoCrop.y / 100)
      context.imageSmoothingQuality = 'high'
      context.drawImage(image, drawX, drawY, drawWidth, drawHeight)
      canvas.toBlob((blob) => {
        if (!blob) {
          reject(new Error('Logo 图片导出失败'))
          return
        }
        const sourceName = sponsorLogoCrop.file?.name || 'sponsor-logo.png'
        const filename = sourceName.replace(/\.[^.]+$/, '') || 'sponsor-logo'
        resolve(new File([blob], `${filename}-display.png`, { type: 'image/png' }))
      }, 'image/png')
    }
    image.onerror = () => reject(new Error('Logo 图片读取失败'))
    image.src = sponsorLogoCrop.previewUrl
  })
}

async function saveSponsors() {
  const invalid = sponsorForm.find((sponsor) => !sponsor.tierLabel || !sponsor.sponsorName)
  if (invalid) {
    ElMessage.warning('请补齐赞助等级和品牌名称')
    return
  }
  const sponsors = await updateCompetitionSponsors(competition.value.id, {
    items: sponsorForm.map((sponsor, index) => ({
      id: sponsor.id || null,
      tierLabel: sponsor.tierLabel,
      sponsorName: sponsor.sponsorName,
      logoAssetId: sponsor.logoAssetId || null,
      sortOrder: Number.isFinite(Number(sponsor.sortOrder)) ? Number(sponsor.sortOrder) : index,
      featured: Boolean(sponsor.featured),
      enabled: sponsor.enabled !== false,
    })),
  })
  competition.value = normalizeDetail({ ...competition.value, sponsors })
  resetForms()
  ElMessage.success('赞助商配置已保存')
}

function getSelectedStyleCategoryNames() {
  const names = []
  const seen = new Set()
  selectedStyleItems.value.forEach((style) => {
    const name = formatStyleItemName(style)
    if (!name || seen.has(name)) return
    seen.add(name)
    names.push(name)
  })
  return names
}

async function syncCategoryFormFromStyleLibrary() {
  const names = getSelectedStyleCategoryNames()
  if (!names.length) {
    ElMessage.warning('当前风格库没有可同步的风格')
    return
  }
  if (categoryForm.filter(Boolean).length) {
    try {
      await ElMessageBox.confirm('将用当前风格库的可用风格替换现有投递组别，保存前仍可继续调整。', '同步投递组别', {
        confirmButtonText: '同步',
        cancelButtonText: '取消',
        type: 'warning',
      })
    } catch {
      return
    }
  }
  categoryForm.splice(0, categoryForm.length, ...names)
  ElMessage.success('已同步当前风格库的投递组别')
}

async function saveEntryConfig() {
  const library = getStyleLibrary(selectedStyleLibraryVersion.value, styleLibraryOptions.value)
  const categoryItems = categoryForm.filter(Boolean).map((name, index) => ({ name, sortOrder: index }))
  const fieldItems = entryFieldForm
    .filter((field) => field.fieldLabel)
    .map((field, index) => ({
      fieldKey: field.fieldKey || createEntryFieldKey(),
      fieldLabel: field.fieldLabel,
      fieldType: field.fieldType,
      helpText: field.helpText,
      options: field.options || [],
      required: Boolean(field.required),
      visibleToJudges: Boolean(field.visibleToJudges),
      sortOrder: index,
    }))
  if (!categoryItems.length) {
    ElMessage.warning('请至少保留 1 个投递组别')
    return
  }
  if (!library?.value) {
    ElMessage.warning('请选择风格库')
    return
  }
  let detail = await updateCompetitionStyles(competition.value.id, {
    styleLibraryVersion: library.value,
  })
  detail = await updateCompetitionCategories(competition.value.id, { items: categoryItems })
  if (fieldItems.length) detail = await updateCompetitionEntryFields(competition.value.id, { items: fieldItems })
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState()
  ElMessage.success('报名配置已保存')
}

async function saveScoreConfigs() {
  const crossConfig = scoreConfigForm.find((config) => config.role === 'CROSS')
  if (!crossConfig || crossConfig.dimensions.length < 2 || crossConfig.dimensions.length > 3) {
    ElMessage.warning('跨界评审需配置 2-3 个维度')
    return
  }
  const professionalConfig = scoreConfigForm.find((config) => config.role === 'PROFESSIONAL')
  if (!isProfessionalScoreConfigFixed(professionalConfig)) {
    ElMessage.warning('专业评审评分表需固定为香气12、外观3、味道20、口感5、整体印象10')
    return
  }
  if (!scoreConfigForm.every((config) => getScoreTotal(config) === 50)) {
    ElMessage.warning('三类评分表总分都需要为 50 分')
    return
  }
  await updateScoreConfigs(competition.value.id, {
    configs: scoreConfigForm.map((config) => ({
      judgeRoleType: config.role,
      minCommentLength: Number(config.minCommentLength || 0),
      dimensions: config.dimensions.map((dimension) => ({
        key: dimension.key,
        label: dimension.label,
        maxScore: Number(dimension.maxScore || 0),
        notePrompt: dimension.notePrompt,
      })),
    })),
  })
  await loadDetail()
  ElMessage.success('评分表已保存')
}

function isProfessionalScoreConfigFixed(config) {
  const expected = [
    ['香气', 12],
    ['外观', 3],
    ['味道', 20],
    ['口感', 5],
    ['整体印象', 10],
  ]
  return Boolean(config)
    && config.dimensions.length === expected.length
    && expected.every(([label, maxScore], index) => (
      config.dimensions[index]?.label === label
      && Number(config.dimensions[index]?.maxScore) === maxScore
    ))
}

function addCrossScoreDimension(config) {
  if (config.role !== 'CROSS') return
  if (config.dimensions.length >= 3) {
    ElMessage.warning('跨界评审最多配置 3 个维度')
    return
  }
  const existingKeys = new Set(config.dimensions.map((dimension) => dimension.key))
  let nextIndex = config.dimensions.length + 1
  while (existingKeys.has(`cross_${nextIndex}`)) nextIndex += 1
  config.dimensions.push({
    key: `cross_${nextIndex}`,
    label: '',
    maxScore: 10,
    notePrompt: '',
    localId: `CROSS-${Date.now()}-${nextIndex}`,
  })
}

function removeScoreDimension(config, index) {
  if (config.role !== 'CROSS') return
  if (config.dimensions.length <= 2) {
    ElMessage.warning('跨界评审至少保留 2 个维度')
    return
  }
  config.dimensions.splice(index, 1)
}

function defaultMinCommentLength(role) {
  if (role === 'CROSS') return 50
  if (role === 'PROFESSIONAL') return 30
  return 0
}

async function handlePrimaryAction() {
  const action = stagePrimaryAction.value.action
  if (action === 'publishRegistration') {
    openPublishRegistrationConfirm()
    return
  }
  if (action === 'closeRegistration') {
    openCloseRegistrationConfirm()
    return
  }
  await handleStageAction(action)
}

function openPublishRegistrationConfirm() {
  if (!stagePrimaryAction.value.enabled || !competition.value) return
  publishRegistrationConfirmOpen.value = true
}

function closePublishRegistrationConfirm() {
  if (publishRegistrationLoading.value) return
  publishRegistrationConfirmOpen.value = false
}

async function confirmPublishRegistration() {
  if (!competition.value || publishRegistrationLoading.value) return
  publishRegistrationLoading.value = true
  try {
    const detail = await openCompetitionRegistration(competition.value.id)
    competition.value = normalizeDetail(detail)
    resetForms()
    applyRoundState()
    publishRegistrationConfirmOpen.value = false
    ElMessage.success('报名已发布')
  } finally {
    publishRegistrationLoading.value = false
  }
}

function openCloseRegistrationConfirm() {
  if (!stagePrimaryAction.value.enabled || !competition.value) return
  closeRegistrationConfirmOpen.value = true
}

function closeRegistrationConfirm() {
  if (closeRegistrationLoading.value) return
  closeRegistrationConfirmOpen.value = false
}

async function confirmCloseRegistration() {
  if (!competition.value || closeRegistrationLoading.value) return
  closeRegistrationLoading.value = true
  try {
    const detail = await closeCompetitionRegistration(competition.value.id)
    competition.value = normalizeDetail(detail)
    resetForms()
    applyRoundState()
    closeRegistrationConfirmOpen.value = false
    ElMessage.success('报名已截止')
  } finally {
    closeRegistrationLoading.value = false
  }
}

async function handleDeleteCompetition() {
  if (!competition.value) return
  const draft = competition.value.status === 'DRAFT'
  const title = draft ? '删除/归档草稿比赛' : '归档比赛'
  const confirmText = draft ? '删除/归档草稿' : '归档比赛'
  const message = draft
    ? `如果「${competition.value.name}」尚未产生报名或轮次数据，将从管理台账中删除；如果已有业务数据，将改为归档并保留记录`
    : `归档后，「${competition.value.name}」将退出厂牌端和常用管理列表，报名、评分、轮次与结果记录会保留用于追溯`
  openBusinessConfirm({
    action: 'deleteCompetition',
    kicker: draft ? '草稿处理' : '比赛归档',
    title,
    copy: message,
    summary: competitionSummaryItems([
      { label: '报名酒款', value: `${competition.value.entriesSummary?.total ?? competition.value.entries?.length ?? 0} 款` },
      { label: '轮次', value: `${rounds.value.length} 轮` },
    ]),
    confirmText,
    loadingText: draft ? '处理中' : '归档中',
  })
}

async function runDeleteCompetition() {
  const draft = competition.value.status === 'DRAFT'
  await deleteCompetition(competition.value.id)
  ElMessage.success(draft ? '草稿比赛已处理' : '比赛已归档')
  router.replace('/admin/competitions')
}

async function handleStageAction(action) {
  if (action === 'publishRegistration') {
    openPublishRegistrationConfirm()
    return
  }
  if (action === 'closeRegistration') {
    openCloseRegistrationConfirm()
    return
  }
  if (action === 'prepareJudging') {
    prepareJudgingAction()
    return
  }
  if (action === 'goToRoundAllocation') {
    goToRoundAllocation()
    return
  }
  if (action === 'createFirstRound') {
    activeTab.value = 'judges'
    goToRoundAllocation()
    return
  }
  if (action === 'publishCurrentRound') {
    publishCurrentRound()
    return
  }
  if (action === 'lockCurrentRound') {
    lockCurrentRound()
    return
  }
  if (action === 'lockSourceRound') {
    lockCurrentDraftSourceRound()
    return
  }
  if (action === 'syncCandidates') {
    syncCurrentRoundCandidates()
    return
  }
  if (action === 'createNextRound') {
    activeTab.value = 'rounds'
    openCreateRoundDialog()
    return
  }
  if (action === 'goToResults') {
    goToResults()
    return
  }
  if (action === 'openLiveDashboard') {
    openLiveDashboard()
  }
}

function showPendingStageMessage(actionName) {
  ElMessage.info(`${actionName}暂未开放`)
}

function openLiveDashboard() {
  router.push({
    path: '/admin/live-board',
    query: competition.value?.id ? { competitionId: competition.value.id } : {},
  })
}

function openSelectedStyleLibrary() {
  const libraryValue = selectedStyleLibrary.value?.value || selectedStyleLibraryVersion.value || competition.value?.styleLibraryVersion
  router.push({
    path: '/admin/style-libraries',
    query: libraryValue ? { library: libraryValue } : {},
  })
}

function setupStyleDistributionObserver() {
  updateStyleDistributionOverflow()
  if (typeof ResizeObserver === 'undefined' || !styleDistributionListRef.value) return
  styleDistributionResizeObserver = new ResizeObserver(updateStyleDistributionOverflow)
  styleDistributionResizeObserver.observe(styleDistributionListRef.value)
}

function updateStyleDistributionOverflow() {
  const element = styleDistributionListRef.value
  if (!element || activeTab.value !== 'entryConfig') {
    styleDistributionHasOverflow.value = false
    return
  }
  styleDistributionHasOverflow.value = element.scrollHeight > element.clientHeight + 1
}

function goToResults() {
  activeTab.value = 'results'
}

function focusRoundProgress() {
  activeTab.value = 'rounds'
  requestAnimationFrame(() => {
    roundProgressSection.value?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  })
}
</script>

<style scoped>
.competition-detail {
  --panel: rgba(22, 32, 36, 0.9);
  --panel-strong: rgba(26, 39, 44, 0.96);
  --line: rgba(219, 232, 237, 0.1);
  --text: #e6edf0;
  --muted: #8da1aa;
  --faint: #5f737d;
  --gold-soft: #e0b84a;
  --green: #6fcf7a;
  --orange: #f2994a;
  --red: #e05252;
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  height: 100vh;
  padding: 26px 28px;
  color: var(--text);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    radial-gradient(circle at 16% 8%, rgba(216, 169, 53, 0.12), transparent 18rem),
    linear-gradient(135deg, #0d1418 0%, #111c20 50%, #0c1519 100%);
  background-size: 48px 48px, 48px 48px, auto, auto;
}

h1,
h2,
h3,
p {
  margin: 0;
}

button,
input,
select,
textarea {
  font: inherit;
}

button {
  cursor: pointer;
}

button:disabled,
input:disabled,
select:disabled,
textarea:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

svg {
  width: 1em;
  height: 1em;
}

.detail-head,
.breadcrumb-link,
.tool-button,
.head-action-group,
.detail-tabs,
.meta-line,
.panel-heading,
.alert-list p,
.check-item,
.panel-actions,
.progress-row,
.publish-actions,
.edit-banner {
  display: flex;
  align-items: center;
}

.detail-head {
  flex: 0 0 auto;
  display: grid;
  gap: 12px;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--line);
}

.head-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: start;
}

.title-line {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  align-items: baseline;
  min-width: 0;
}

.title-line h1 {
  font-size: 28px;
  line-height: 1.15;
}

.meta-line {
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.meta-line > span:not(.state-badge) {
  padding: 6px 8px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
}

.breadcrumb-link {
  justify-self: start;
  gap: 7px;
  min-height: 28px;
  padding: 0;
  color: var(--muted);
  border: 0;
  background: transparent;
  font-size: 13px;
  font-weight: 800;
}

.breadcrumb-link:hover {
  color: var(--gold-soft);
}

.tool-button,
.icon-button {
  gap: 8px;
  min-height: 42px;
  padding: 0 12px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.tool-button.primary {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.tool-button.blocked {
  color: rgba(224, 184, 74, 0.42);
  border-color: rgba(216, 169, 53, 0.16);
  background: rgba(216, 169, 53, 0.045);
}

.tool-button.danger {
  color: #ffb4aa;
  border-color: rgba(255, 132, 116, 0.35);
  background: rgba(255, 132, 116, 0.08);
}

.disabled-action-tip {
  position: relative;
  display: inline-flex;
}

.disabled-action-tip[data-disabled-reason]:hover::after {
  content: attr(data-disabled-reason);
  position: absolute;
  z-index: 80;
  right: 0;
  bottom: calc(100% + 9px);
  width: max-content;
  max-width: min(340px, 72vw);
  padding: 8px 10px;
  color: var(--text);
  border: 1px solid rgba(216, 169, 53, 0.32);
  border-radius: 8px;
  background: rgba(7, 14, 17, 0.98);
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.38);
  font-size: 12px;
  font-weight: 750;
  line-height: 1.5;
  white-space: normal;
  overflow-wrap: anywhere;
  pointer-events: none;
}

.disabled-action-tip[data-disabled-reason]:hover::before {
  content: '';
  position: absolute;
  z-index: 81;
  right: 18px;
  bottom: calc(100% + 4px);
  width: 8px;
  height: 8px;
  border-right: 1px solid rgba(216, 169, 53, 0.32);
  border-bottom: 1px solid rgba(216, 169, 53, 0.32);
  background: rgba(7, 14, 17, 0.98);
  transform: rotate(45deg);
  pointer-events: none;
}

.stage-confirm-backdrop {
  --confirm-panel: #111b1f;
  --confirm-line: rgba(219, 232, 237, 0.13);
  --confirm-text: #edf4f6;
  --confirm-muted: #8ea1aa;
  --confirm-gold: #e0b84a;
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(1, 7, 9, 0.72);
  backdrop-filter: blur(10px);
}

.stage-confirm-dialog {
  box-sizing: border-box;
  display: grid;
  gap: 16px;
  width: min(520px, 100%);
  padding: 20px;
  color: var(--confirm-text);
  border: 1px solid var(--confirm-line);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.014)),
    var(--confirm-panel);
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.48);
}

.stage-confirm-dialog.warning {
  border-color: rgba(224, 184, 74, 0.28);
  background:
    linear-gradient(180deg, rgba(224, 184, 74, 0.07), rgba(255, 255, 255, 0.012)),
    var(--confirm-panel);
}

.stage-confirm-dialog header {
  display: grid;
  gap: 7px;
}

.confirm-kicker {
  color: var(--confirm-gold);
  font-size: 12px;
  font-weight: 800;
}

.stage-confirm-dialog h2 {
  font-size: 22px;
  line-height: 1.18;
}

.confirm-copy {
  color: #bfd0d7;
  line-height: 1.65;
}

.confirm-summary {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid rgba(219, 232, 237, 0.09);
  border-radius: 8px;
  background: rgba(7, 14, 17, 0.58);
}

.confirm-summary span {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.confirm-summary small {
  color: var(--confirm-muted);
  font-size: 12px;
  font-weight: 800;
}

.confirm-summary strong {
  min-width: 0;
  overflow: hidden;
  color: var(--confirm-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.confirm-reason {
  display: grid;
  gap: 8px;
}

.confirm-reason span {
  color: var(--confirm-muted);
  font-size: 12px;
  font-weight: 800;
}

.confirm-reason textarea {
  width: 100%;
  min-height: 86px;
  resize: vertical;
  padding: 10px 12px;
  color: var(--confirm-text);
  border: 1px solid var(--confirm-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  font: inherit;
  line-height: 1.5;
}

.confirm-date-field input {
  width: 100%;
  min-height: 40px;
  box-sizing: border-box;
  padding: 0 12px;
  color: var(--confirm-text);
  border: 1px solid var(--confirm-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  font: inherit;
}

.stage-confirm-dialog footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  align-items: center;
}

.confirm-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 14px;
  color: var(--confirm-text);
  border: 1px solid var(--confirm-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  font-weight: 800;
}

.confirm-button.primary {
  color: var(--confirm-gold);
  border-color: rgba(216, 169, 53, 0.36);
  background: rgba(216, 169, 53, 0.1);
}

.stage-confirm-dialog.warning .confirm-button.primary {
  color: #f1c85d;
  border-color: rgba(224, 184, 74, 0.5);
  background: rgba(216, 169, 53, 0.14);
}

.confirm-button.ghost:hover,
.confirm-button.primary:hover {
  border-color: rgba(224, 184, 74, 0.45);
  background: rgba(216, 169, 53, 0.08);
}

.confirm-button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

:global(.entry-auto-assign-dialog-shell) {
  --entry-dialog-bg: #10191d;
  --entry-dialog-panel: rgba(7, 14, 17, 0.72);
  --entry-dialog-text: #e6edf0;
  --entry-dialog-muted: #8da1aa;
  --entry-dialog-line: rgba(219, 232, 237, 0.12);
  --entry-dialog-gold: #e0b84a;
}

:global(.entry-auto-assign-dialog-shell.el-dialog) {
  overflow: hidden;
  border: 1px solid var(--entry-dialog-line);
  border-radius: 8px;
  background: var(--entry-dialog-bg);
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.46);
}

:global(.entry-auto-assign-dialog-shell .el-dialog__header) {
  margin: 0;
  padding: 14px 16px 10px;
  border-bottom: 1px solid rgba(219, 232, 237, 0.08);
}

:global(.entry-auto-assign-dialog-shell .el-dialog__title) {
  color: var(--entry-dialog-text);
  font-size: 16px;
  font-weight: 900;
}

:global(.entry-auto-assign-dialog-shell .el-dialog__headerbtn) {
  top: 4px;
}

:global(.entry-auto-assign-dialog-shell .el-dialog__close) {
  color: var(--entry-dialog-muted);
}

:global(.entry-auto-assign-dialog-shell .el-dialog__body) {
  padding: 14px 16px;
}

:global(.entry-auto-assign-dialog-shell .el-dialog__footer) {
  padding: 10px 16px 14px;
  border-top: 1px solid rgba(219, 232, 237, 0.08);
}

.entry-auto-assign-dialog {
  display: grid;
  gap: 10px;
}

:global(.round-score-detail-dialog-shell.el-dialog) {
  overflow: hidden;
  border: 1px solid rgba(219, 232, 237, 0.12);
  border-radius: 8px;
  background: #10191d;
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.46);
}

:global(.round-score-detail-dialog-shell .el-dialog__header) {
  margin: 0;
  padding: 16px 18px 10px;
  border-bottom: 1px solid rgba(219, 232, 237, 0.08);
}

:global(.round-score-detail-dialog-shell .el-dialog__title) {
  color: var(--text);
  font-size: 18px;
  font-weight: 900;
}

:global(.round-score-detail-dialog-shell .el-dialog__headerbtn) {
  top: 6px;
}

:global(.round-score-detail-dialog-shell .el-dialog__close) {
  color: var(--muted);
}

:global(.round-score-detail-dialog-shell .el-dialog__body) {
  padding: 16px 18px 18px;
}

.round-score-detail-dialog {
  display: grid;
  gap: 14px;
}

.score-detail-summary {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.confirmation-override-button {
  width: 100%;
}

.score-detail-summary span {
  display: grid;
  gap: 7px;
  min-height: 68px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.score-detail-summary small {
  color: var(--muted);
  font-weight: 800;
}

.score-detail-summary strong {
  font-size: 20px;
}

.score-detail-summary .pending-judge-summary {
  grid-column: span 2;
}

.score-detail-summary .pending-judge-summary strong {
  overflow: hidden;
  font-size: 16px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ranking-detail-summary {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.ranking-detail-summary strong {
  overflow: hidden;
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ranking-detail-slots,
.ranking-detail-candidates {
  display: grid;
  gap: 8px;
}

.ranking-detail-slots header,
.ranking-detail-candidates header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.ranking-detail-slots h3,
.ranking-detail-candidates h3 {
  margin: 0;
  color: var(--text);
  font-size: 15px;
  font-weight: 900;
}

.ranking-detail-slots header span,
.ranking-detail-candidates header span {
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
}

.ranking-detail-slots article,
.ranking-detail-candidates article {
  display: grid;
  gap: 7px;
  align-items: center;
  min-width: 0;
  padding: 11px 12px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.ranking-detail-slots article {
  grid-template-columns: 100px minmax(0, 1fr) minmax(180px, 0.5fr);
}

.ranking-detail-candidates article {
  grid-template-columns: minmax(0, 1fr) minmax(120px, 0.34fr) minmax(140px, 0.4fr);
}

.ranking-detail-slots article.filled {
  border-color: rgba(111, 207, 122, 0.22);
  background: rgba(111, 207, 122, 0.055);
}

.ranking-detail-slots strong,
.ranking-detail-candidates strong {
  color: var(--text);
  font-weight: 900;
}

.ranking-detail-slots span,
.ranking-detail-slots small,
.ranking-detail-candidates span,
.ranking-detail-candidates small {
  min-width: 0;
  overflow: hidden;
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.score-detail-progress-list {
  display: grid;
  gap: 8px;
}

.score-detail-progress-row {
  display: grid;
  grid-template-columns: minmax(150px, 0.32fr) minmax(0, 1fr);
  gap: 14px;
  align-items: start;
  min-height: 58px;
  padding: 11px 12px;
  border: 1px solid rgba(111, 207, 122, 0.18);
  border-radius: 8px;
  background: rgba(111, 207, 122, 0.045);
}

.score-detail-progress-row.pending {
  border-color: rgba(242, 153, 74, 0.26);
  background: rgba(242, 153, 74, 0.065);
}

.score-detail-progress-row.captain {
  border-color: rgba(224, 184, 74, 0.28);
  background: rgba(224, 184, 74, 0.055);
}

.score-detail-judge-name {
  display: grid;
  gap: 5px;
  min-width: 0;
  padding-top: 2px;
}

.score-detail-judge-name strong,
.score-detail-judge-name small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.score-detail-judge-name strong {
  color: var(--text);
  font-size: 15px;
  font-weight: 900;
}

.score-detail-judge-name small {
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

.score-detail-task-list {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.score-detail-task {
  display: grid;
  grid-template-columns: 76px minmax(180px, 1fr) 74px 96px;
  gap: 12px;
  align-items: center;
  min-width: 0;
}

.score-detail-task small {
  overflow: hidden;
  color: var(--muted);
  font-size: 12px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.score-detail-progress-track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
}

.score-detail-progress-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #e0b84a, #6fcf7a);
}

.score-detail-task span,
.score-detail-task em {
  color: var(--muted);
  font-style: normal;
  font-weight: 900;
  text-align: right;
  white-space: nowrap;
}

.score-detail-task.pending em {
  color: #f1bd79;
}

.entry-auto-assign-field {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.entry-auto-assign-field > span {
  color: #8da1aa;
  font-size: 13px;
  font-weight: 800;
}

.entry-auto-assign-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-left: 62px;
}

.entry-auto-assign-stats span {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
  min-width: 0;
  min-height: 30px;
  padding: 0 8px;
  color: #8da1aa;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.entry-auto-assign-stats strong {
  color: #e6edf0;
  font-size: 14px;
  line-height: 1;
}

.entry-auto-assign-stats .highlight {
  color: #e0b84a;
  border-color: rgba(216, 169, 53, 0.24);
  background: rgba(216, 169, 53, 0.07);
}

.entry-auto-assign-stats .highlight strong {
  color: #e0b84a;
}

.entry-auto-assign-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.entry-auto-assign-option span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.entry-auto-assign-option small {
  flex: 0 0 auto;
  color: #8da1aa;
  font-size: 12px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

:global(.entry-auto-assign-dialog-shell .el-input__wrapper) {
  background: var(--entry-dialog-panel);
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  box-shadow: none;
}

:global(.entry-auto-assign-dialog-shell .el-input__wrapper:hover),
:global(.entry-auto-assign-dialog-shell .el-input__wrapper.is-focus) {
  border-color: rgba(216, 169, 53, 0.36);
  box-shadow: 0 0 0 2px rgba(216, 169, 53, 0.08);
}

:global(.entry-auto-assign-dialog-shell .el-select__wrapper) {
  min-height: 42px;
  background: var(--entry-dialog-panel);
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  box-shadow: none;
}

:global(.entry-auto-assign-dialog-shell .el-select__wrapper:hover),
:global(.entry-auto-assign-dialog-shell .el-select__wrapper.is-focused) {
  border-color: rgba(216, 169, 53, 0.36);
  box-shadow: 0 0 0 2px rgba(216, 169, 53, 0.08);
}

:global(.entry-auto-assign-dialog-shell .el-input__inner) {
  color: var(--entry-dialog-text);
}

:global(.entry-auto-assign-dialog-shell .el-select__selected-item),
:global(.entry-auto-assign-dialog-shell .el-select__placeholder) {
  color: var(--entry-dialog-text);
}

:global(.entry-auto-assign-dialog-shell .el-select__placeholder.is-transparent) {
  color: var(--entry-dialog-muted);
}

:global(.entry-auto-assign-dialog-shell .el-select__caret) {
  color: var(--entry-dialog-muted);
}

:global(.entry-auto-assign-dialog-shell .el-input-number) {
  width: 100%;
}

:global(.entry-auto-assign-dialog-shell .el-input-number__decrease),
:global(.entry-auto-assign-dialog-shell .el-input-number__increase) {
  color: var(--entry-dialog-muted);
  border-color: rgba(219, 232, 237, 0.1);
  background: rgba(255, 255, 255, 0.035);
}

:global(.entry-auto-assign-popper.el-popper) {
  border: 1px solid rgba(219, 232, 237, 0.12);
  background: #10191d;
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.36);
}

:global(.entry-auto-assign-popper .el-select-dropdown) {
  background: #10191d;
}

:global(.entry-auto-assign-popper .el-select-dropdown__list) {
  background: #10191d;
}

:global(.entry-auto-assign-popper .el-popper__arrow::before) {
  border-color: rgba(219, 232, 237, 0.12);
  background: #10191d;
}

:global(.entry-auto-assign-popper .el-select-dropdown__item) {
  color: #e6edf0;
}

:global(.entry-auto-assign-popper .el-select-dropdown__item.hover),
:global(.entry-auto-assign-popper .el-select-dropdown__item.is-hovering),
:global(.entry-auto-assign-popper .el-select-dropdown__item:hover) {
  background: rgba(216, 169, 53, 0.08);
}

:global(.entry-auto-assign-popper .el-select-dropdown__item.selected),
:global(.entry-auto-assign-popper .el-select-dropdown__item.is-selected) {
  color: #e0b84a;
  background: rgba(216, 169, 53, 0.11);
}

.icon-button {
  display: grid;
  place-items: center;
  width: 36px;
  min-height: 36px;
  padding: 0;
}

.full-width {
  justify-content: center;
  width: 100%;
}

.state-badge,
.window-badge {
  display: inline-flex;
  padding: 7px 10px;
  font-weight: 800;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.state-badge {
  color: var(--green);
  border-color: rgba(111, 207, 122, 0.2);
  background: rgba(111, 207, 122, 0.1);
}

.state-badge.gold,
.state-badge.warning,
.window-badge.gold,
.window-badge.warning {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.3);
  background: rgba(216, 169, 53, 0.08);
}

.state-badge.neutral,
.window-badge.neutral {
  color: #a9bbc2;
}

.window-badge.success {
  color: var(--green);
  border-color: rgba(111, 207, 122, 0.22);
  background: rgba(111, 207, 122, 0.08);
}

.head-action-group {
  position: relative;
  justify-content: flex-end;
  gap: 10px;
}

.more-actions {
  position: relative;
}

.more-actions summary {
  display: inline-flex;
  align-items: center;
  min-height: 42px;
  padding: 0 12px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  list-style: none;
  cursor: pointer;
}

.more-actions summary::-webkit-details-marker {
  display: none;
}

.more-actions-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 30;
  display: grid;
  width: 170px;
  padding: 8px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(15, 24, 28, 0.98);
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.28);
}

.more-actions-menu button {
  min-height: 34px;
  padding: 0 8px;
  color: var(--text);
  text-align: left;
  border: 0;
  border-radius: 6px;
  background: transparent;
}

.more-actions-menu button.danger {
  color: #ff9089;
}

.more-actions-menu button.danger:hover {
  background: rgba(224, 82, 82, 0.09);
}

.detail-shell {
  flex: 1 1 auto;
  min-height: 0;
  margin-top: 18px;
  display: flex;
  flex-direction: column;
}

.detail-tabbar {
  flex: 0 0 auto;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  width: min(100%, 1360px);
}

.detail-tabs {
  flex: 1 1 auto;
  justify-content: flex-start;
  gap: 8px;
  flex-wrap: wrap;
  width: auto;
  margin: 0;
  padding-left: 4px;
  box-sizing: border-box;
}

.detail-tabs button {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 40px;
  padding: 0 12px;
  color: #a9bbc2;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.025);
}

.detail-tabs button.active {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.detail-tabs button.disabled {
  color: #596a70;
  cursor: not-allowed;
  border-color: rgba(219, 232, 237, 0.055);
  background: rgba(255, 255, 255, 0.012);
  box-shadow: none;
  opacity: 0.72;
}

.detail-tabs button.disabled svg {
  color: #4f6066;
}

.detail-tabs button.disabled:hover {
  color: #66777d;
  border-color: rgba(219, 232, 237, 0.08);
  background: rgba(255, 255, 255, 0.018);
}

.tab-save-actions {
  flex: 0 0 auto;
  display: flex;
  justify-content: flex-end;
  min-height: 40px;
}

.tab-save-actions .tool-button {
  min-width: 148px;
  min-height: 40px;
  justify-content: center;
  white-space: nowrap;
}

.tab-content {
  flex: 1 1 auto;
  min-height: 0;
  min-width: 0;
  margin-top: 14px;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: rgba(216, 169, 53, 0.5) rgba(255, 255, 255, 0.05);
}

.tab-content::-webkit-scrollbar {
  width: 10px;
}

.tab-content::-webkit-scrollbar-track {
  margin: 6px 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
}

.tab-content::-webkit-scrollbar-thumb {
  border: 2px solid rgba(11, 18, 22, 0.9);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(216, 169, 53, 0.7), rgba(216, 169, 53, 0.36));
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
}

.tab-content::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(224, 184, 74, 0.82), rgba(216, 169, 53, 0.48));
}

.tab-panel {
  display: grid;
  gap: 14px;
  min-width: 0;
  padding-bottom: 28px;
}

.entry-config-panel,
.score-config-panel {
  width: min(100%, 1280px);
  margin: 0 auto;
  align-content: start;
}

.overview-grid,
.score-panels {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.two-column {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.metric-card,
.panel-card,
.judge-nav-panel,
.panel-card.tight,
.judge-table-card,
.judge-command-bar,
.round-pool-panel,
.round-check-panel .panel-card,
.round-table-card {
  min-width: 0;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.16);
}

.metric-card,
.panel-card {
  padding: 16px;
}

.metric-card {
  display: grid;
  align-content: start;
  gap: 8px;
}

.metric-card strong {
  color: var(--text);
  font-size: 24px;
}

small,
.panel-heading span,
.empty-line,
.metric-card p,
.round-note,
.round-members span,
.round-entry-card small,
.round-entry-card em,
.wizard-panel p {
  color: var(--muted);
}

.panel-heading {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-actions {
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.panel-heading.compact {
  margin-bottom: 8px;
}

.panel-heading.compact h2 {
  font-size: 16px;
}

.check-list,
.alert-list,
.progress-list,
.dimension-list,
.library-summary,
.round-entry-list,
.award-list {
  display: grid;
  gap: 10px;
}

.check-item,
.alert-list p,
.table-row,
.edit-banner {
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.alert-list p span,
.check-item span {
  flex: 1;
}

.edit-banner {
  color: var(--green);
}

.edit-banner.locked {
  color: #f1bd79;
  background: rgba(242, 153, 74, 0.09);
}

.check-item.done,
.alert-list p.success,
.success {
  color: var(--green);
}

.check-item.pending,
.alert-list p.warning {
  color: #f1bd79;
  background: rgba(242, 153, 74, 0.09);
}

.check-item.optional {
  color: var(--muted);
}

.alert-list p.danger,
.danger {
  color: #ff9089;
}

.link-action {
  flex: 0 0 auto;
  min-height: 32px;
  padding: 0 10px;
  color: var(--gold-soft);
  border: 1px solid rgba(216, 169, 53, 0.28);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.07);
}

.future-task-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.future-task {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  min-height: 82px;
  padding: 12px;
  color: var(--text);
  text-align: left;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.022);
}

.future-task span {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.future-task em {
  color: #f1bd79;
  font-style: normal;
  font-weight: 800;
  white-space: nowrap;
}

.future-task.done em {
  color: var(--green);
}

.round-path {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.round-path span {
  display: grid;
  gap: 3px;
  min-width: 132px;
  padding: 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.round-path span.active {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.round-path.wide span {
  flex: 1;
}

.pill-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pill-list span {
  padding: 7px 10px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.library-heading {
  margin-bottom: 4px;
}

.library-config-block,
.library-summary {
  display: grid;
  gap: 8px;
  align-content: start;
}

.library-summary small {
  color: var(--muted);
}

.library-control-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.library-control-row select {
  flex: 0 1 460px;
  width: min(460px, 100%);
}

.library-summary strong {
  color: var(--text);
  font-size: 18px;
}

.library-summary p {
  margin: 0;
  color: var(--muted);
  line-height: 1.6;
}

.style-distribution {
  display: grid;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

.style-distribution-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.style-distribution-head span {
  color: var(--muted);
}

.style-distribution-list {
  --style-distribution-pill-height: 36px;
  position: relative;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-content: flex-start;
  max-height: calc(var(--style-distribution-pill-height) * 2 + 8px);
  overflow: hidden;
}

.style-distribution-list.has-more {
  padding-right: 82px;
}

.style-distribution-list span {
  display: inline-grid;
  grid-template-columns: minmax(0, auto) auto;
  gap: 8px;
  align-items: baseline;
  box-sizing: border-box;
  max-width: 100%;
  min-height: var(--style-distribution-pill-height);
  padding: 7px 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.022);
  line-height: 20px;
  white-space: nowrap;
}

.style-distribution-list b {
  overflow: hidden;
  text-overflow: ellipsis;
}

.style-distribution-list small {
  color: var(--muted);
}

.style-distribution-more {
  position: absolute;
  right: 0;
  bottom: 0;
  z-index: 1;
  min-width: 72px;
  min-height: var(--style-distribution-pill-height);
  padding: 0 11px;
  color: var(--gold-soft);
  border: 1px solid rgba(216, 169, 53, 0.32);
  border-radius: 8px;
  background: #121d21;
  font-weight: 900;
  line-height: 20px;
  white-space: nowrap;
}

.style-distribution-more:hover {
  background: rgba(216, 169, 53, 0.11);
}

.category-editor-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 280px));
  gap: 10px;
  justify-content: start;
}

.category-editor-list label {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 40px;
  gap: 8px;
}

.library-card {
  display: grid;
  gap: 14px;
}

input,
select,
textarea {
  width: 100%;
  box-sizing: border-box;
  min-width: 0;
  min-height: 40px;
  padding: 0 10px;
  color: var(--text);
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  outline: 0;
  background-color: rgba(7, 14, 17, 0.72);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.035);
}

input:focus,
select:focus,
textarea:focus {
  border-color: rgba(224, 184, 74, 0.5);
  background-color: rgba(10, 20, 24, 0.92);
  box-shadow: 0 0 0 3px rgba(216, 169, 53, 0.09);
}

textarea {
  min-height: 96px;
  padding-top: 10px;
  line-height: 1.55;
  resize: vertical;
}

input::placeholder,
textarea::placeholder {
  color: var(--faint);
}

.base-info-panel,
.base-info-card {
  display: grid;
  gap: 14px;
}

.base-info-groups {
  display: grid;
  gap: 18px;
  max-width: 1040px;
}

.form-subgroup {
  display: grid;
  gap: 12px;
  padding-top: 2px;
}

.form-subgroup + .form-subgroup {
  padding-top: 16px;
  border-top: 1px solid rgba(219, 232, 237, 0.08);
}

.form-subgroup h3 {
  color: #c7d5db;
  font-size: 13px;
  line-height: 1.2;
}

.base-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(260px, 1fr));
  gap: 14px;
  max-width: 1040px;
}

.base-form-grid label {
  display: grid;
  gap: 8px;
}

.base-form-grid label > span {
  color: var(--muted);
  font-size: 13px;
}

.base-form-grid .wide-field {
  grid-column: 1 / -1;
}

input[type="checkbox"] {
  display: inline-grid;
  place-content: center;
  width: 16px;
  min-width: 16px;
  height: 16px;
  min-height: 16px;
  margin: 0;
  padding: 0;
  accent-color: var(--gold-soft);
}

.check-control {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--muted);
  white-space: nowrap;
}

.data-table,
.path-table {
  display: grid;
  gap: 8px;
}

.table-head,
.table-row {
  display: grid;
  gap: 10px;
  align-items: center;
  min-width: 0;
}

.table-head {
  color: var(--muted);
  font-size: 13px;
}

.field-table .table-head,
.field-table .table-row {
  grid-template-columns: minmax(150px, 0.9fr) minmax(104px, 124px) minmax(260px, 1.8fr) 72px 96px 38px;
}

.entries-table .table-head,
.entries-table .table-row {
  grid-template-columns: minmax(250px, 1.12fr) minmax(150px, 0.66fr) minmax(160px, 0.72fr) minmax(190px, 0.82fr) minmax(150px, 0.64fr) minmax(282px, auto);
  gap: 14px;
}

.entries-table .table-head {
  padding: 0 12px;
}

.entries-table .table-row {
  min-height: 68px;
  padding: 10px 12px;
}

.entries-table .table-row.refund-priority {
  border-color: rgba(255, 122, 107, 0.24);
  background: rgba(255, 122, 107, 0.055);
}

.entries-table .table-row.refunded {
  opacity: 0.52;
  background: rgba(255, 255, 255, 0.02);
}

.entry-main,
.entry-meta,
.entry-code-cell,
.entry-style-cell,
.entry-tracking-cell,
.entry-actions {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.entry-main strong,
.entry-code-cell strong,
.entry-style-cell strong,
.entry-tracking-cell strong {
  min-width: 0;
  overflow-wrap: anywhere;
  color: var(--text);
  line-height: 1.35;
}

.entry-main small,
.entry-meta small,
.entry-code-cell small,
.entry-style-cell small,
.entry-tracking-cell small {
  color: var(--muted);
  font-size: 12px;
  overflow-wrap: anywhere;
}

.entry-followup {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  min-width: 0;
}

.entry-followup span,
.entry-followup em,
.entry-state-pill {
  max-width: 100%;
  padding: 5px 8px;
  color: var(--text);
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
  line-height: 1.2;
  white-space: nowrap;
  overflow-wrap: anywhere;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.026);
}

.entry-followup em {
  min-width: 0;
  max-width: 190px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
}

.entry-state-pill {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.25);
  background: rgba(216, 169, 53, 0.08);
}

.entry-state-pill.active {
  color: #bfe6f2;
  border-color: rgba(103, 159, 184, 0.28);
  background: rgba(103, 159, 184, 0.12);
}

.entry-state-pill.success {
  color: #a8efb7;
  border-color: rgba(82, 211, 123, 0.24);
  background: rgba(82, 211, 123, 0.12);
}

.entry-state-pill.warning {
  color: #ffdc73;
  border-color: rgba(216, 169, 53, 0.28);
  background: rgba(216, 169, 53, 0.1);
}

.entry-state-pill.muted {
  color: var(--muted);
  border-color: rgba(219, 232, 237, 0.1);
  background: rgba(255, 255, 255, 0.026);
}

.entry-actions {
  align-self: center;
  grid-template-columns: repeat(3, minmax(82px, 1fr));
  gap: 8px;
}

.mini-action {
  min-height: 32px;
  padding: 0 10px;
  color: var(--text);
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
}

.mini-action.danger {
  color: #ffb4b4;
  border-color: rgba(224, 82, 82, 0.4);
}

@media (max-width: 1280px) {
  .entries-table .table-head {
    display: none;
  }

  .entries-table .table-row {
    grid-template-columns: minmax(220px, 1fr) minmax(150px, 0.7fr);
    align-items: stretch;
  }

  .entry-followup,
  .entry-actions {
    grid-column: 1 / -1;
  }

  .entry-actions {
    justify-content: start;
    grid-template-columns: repeat(3, minmax(92px, 128px));
  }
}

@media (max-width: 760px) {
  .entries-table .table-row,
  .entry-actions {
    grid-template-columns: minmax(0, 1fr);
  }

  .mini-action {
    justify-content: center;
    min-height: 38px;
  }
}

.path-table .table-head,
.path-table .table-row {
  grid-template-columns: minmax(180px, 1fr) 110px minmax(360px, 2fr) 110px;
}

.judge-workbench {
  display: grid;
  grid-template-columns: 168px minmax(0, 1fr) 340px;
  gap: 10px;
  align-items: start;
}

.judge-nav-panel,
.judge-right-panel {
  position: sticky;
  top: 0;
  display: grid;
  gap: 12px;
}

.judge-nav-panel,
.panel-card.tight {
  padding: 12px;
}

.table-nav-item,
.table-nav-add,
.pool-filters button,
.empty-assignment {
  width: 100%;
  min-height: 38px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.table-nav-item {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  padding: 0 9px;
  text-align: left;
}

.table-nav-item.active {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(216, 169, 53, 0.08);
}

.table-nav-add,
.empty-assignment {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--gold-soft);
  border-style: dashed;
}

.left-check-summary {
  display: grid;
  gap: 7px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--line);
}

.left-check-summary div,
.left-check-summary p {
  display: flex;
  align-items: center;
}

.left-check-summary div {
  justify-content: space-between;
  gap: 8px;
}

.left-check-summary p {
  gap: 6px;
  color: #f1bd79;
  font-size: 12px;
  line-height: 1.35;
}

.assignment-board {
  display: grid;
  gap: 10px;
}

.judge-command-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
}

.metric-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.metric-strip span {
  padding: 7px 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.judge-table-card {
  padding: 12px;
}

.judge-table-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.judge-table-head label {
  display: grid;
  gap: 6px;
}

.role-lanes {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.role-lane {
  display: grid;
  gap: 10px;
  min-height: 124px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.022);
}

.role-lane.active {
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(216, 169, 53, 0.06);
}

.assignment-card-list,
.judge-pool-list {
  display: grid;
  gap: 8px;
}

.assignment-card,
.pool-card {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  gap: 9px;
  align-items: center;
  padding: 9px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.avatar {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  color: var(--gold-soft);
  border: 1px solid rgba(216, 169, 53, 0.24);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.08);
  font-weight: 900;
}

.avatar.small {
  width: 28px;
  height: 28px;
}

.assignment-main,
.pool-card div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.assignment-main strong,
.assignment-main small,
.pool-card strong,
.pool-card small,
.pool-card em {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pool-card em {
  color: var(--gold-soft);
  font-style: normal;
  font-size: 12px;
}

.inline-search {
  position: relative;
  display: block;
}

.inline-search svg {
  position: absolute;
  top: 50%;
  left: 10px;
  color: var(--muted);
  transform: translateY(-50%);
}

.inline-search input {
  padding-left: 32px;
}

.pool-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.pool-filters button {
  width: auto;
  min-height: 30px;
  padding: 0 9px;
  color: var(--muted);
  font-size: 12px;
}

.pool-filters button.active {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.28);
  background: rgba(216, 169, 53, 0.08);
}

.round-flow {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.round-flow.compact-flow {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.round-flow-item {
  display: grid;
  gap: 4px;
  min-height: 78px;
  padding: 12px;
  color: var(--text);
  text-align: left;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.round-flow-item.active {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(216, 169, 53, 0.08);
}

.round-flow-item.create {
  border-style: dashed;
}

.round-flow-item em {
  color: var(--muted);
  font-style: normal;
}

.round-summary-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
}

.next-round-banner {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 12px;
  color: var(--text);
  border: 1px solid rgba(216, 169, 53, 0.24);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.07);
}

.next-round-banner div {
  display: grid;
  gap: 4px;
}

.next-round-banner span {
  color: var(--muted);
}

.next-round-banner button {
  min-height: 38px;
  padding: 0 12px;
  color: var(--gold-soft);
  border: 1px solid rgba(216, 169, 53, 0.32);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.08);
  font: inherit;
  font-weight: 800;
}

.next-round-banner.muted {
  color: var(--muted);
  border-color: var(--line);
  background: rgba(255, 255, 255, 0.026);
}

.rounds-panel {
  align-content: start;
}

.round-planning-shell {
  display: grid;
  grid-template-columns: 388px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
  width: min(100%, 1500px);
}

.round-planning-shell.feedback-only-rounds {
  grid-template-columns: minmax(0, 1fr);
}

.round-pyramid-panel,
.round-current-card,
.round-table-overview,
.round-task-hint {
  min-width: 0;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.16);
}

.round-pyramid-panel {
  display: grid;
  gap: 12px;
  align-content: start;
  padding: 18px;
}

.round-pyramid-head,
.round-table-overview header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.round-pyramid-head h2,
.round-current-card h2,
.round-table-overview h3,
.round-task-hint strong,
.round-current-main p {
  margin: 0;
}

.round-pyramid-head h2 {
  color: var(--text);
  font-size: 18px;
}

.round-table-overview header > span {
  flex: 0 0 auto;
  padding: 6px 9px;
  color: var(--gold-soft);
  font-size: 12px;
  font-weight: 900;
  border: 1px solid rgba(216, 169, 53, 0.24);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.08);
}

.round-pyramid {
  display: grid;
  gap: 14px;
  justify-items: center;
  padding: 2px 4px 0;
}

.pyramid-node {
  position: relative;
  display: grid;
  justify-items: center;
  gap: 6px;
  width: var(--node-width);
  min-width: 170px;
  min-height: 54px;
  padding: 0;
  color: var(--text);
  text-align: center;
  border: 0;
  background: transparent;
  font: inherit;
  cursor: default;
}

.pyramid-node::after {
  content: "";
  position: absolute;
  left: 50%;
  bottom: -14px;
  width: 1px;
  height: 14px;
  background: linear-gradient(180deg, rgba(216, 169, 53, 0.28), rgba(219, 232, 237, 0.06));
}

.pyramid-node:last-child::after {
  display: none;
}

.pyramid-node.actionable {
  cursor: pointer;
}

.pyramid-node.active {
  color: var(--gold-soft);
}

.pyramid-node.done {
  color: var(--green);
}

.pyramid-node.create {
  color: var(--gold-soft);
}

.pyramid-node.pending,
.pyramid-node.draft,
.pyramid-node.ghost {
  color: var(--muted);
}

.pyramid-node.ghost {
  opacity: 0.58;
}

.pyramid-layer-caption {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  max-width: 100%;
}

.pyramid-layer-caption strong {
  min-width: 0;
  overflow: hidden;
  color: inherit;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pyramid-layer-caption em {
  flex: 0 0 auto;
  color: var(--muted);
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.pyramid-node small,
.pyramid-node b,
.round-current-card small,
.round-current-main p,
.round-table-overview small,
.round-task-hint span {
  color: var(--muted);
}

.pyramid-node b {
  font-size: 12px;
  font-weight: 800;
}

.pyramid-table-row {
  position: relative;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
  width: 100%;
}

.pyramid-table-row button,
.pyramid-table-row i,
.pyramid-placeholder-mark {
  position: relative;
  z-index: 1;
  display: inline-grid;
  place-items: center;
  min-width: 52px;
  max-width: 68px;
  min-height: 34px;
  padding: 0 8px;
  overflow: hidden;
  color: var(--text);
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 6px;
  background: #142024;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.14);
  font-family: inherit;
}

.pyramid-table-row button {
  overflow: visible;
  cursor: pointer;
}

.pyramid-table-row button.active {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.42);
  background: rgba(216, 169, 53, 0.13);
}

.pyramid-table-row i {
  min-width: 46px;
  color: var(--muted);
}

.pyramid-placeholder-mark {
  min-width: 72px;
  color: var(--muted);
  border-style: dashed;
  background: rgba(255, 255, 255, 0.018);
  box-shadow: none;
}

button.pyramid-placeholder-mark {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.34);
  cursor: pointer;
}

.pyramid-table-popover {
  position: absolute;
  left: 50%;
  bottom: calc(100% + 10px);
  z-index: 8;
  display: none;
  width: 184px;
  padding: 10px;
  color: var(--text);
  text-align: left;
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  background: rgba(12, 20, 24, 0.98);
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.34);
  transform: translateX(-50%);
}

.pyramid-table-popover::after {
  content: "";
  position: absolute;
  left: 50%;
  bottom: -5px;
  width: 9px;
  height: 9px;
  border-right: 1px solid rgba(219, 232, 237, 0.14);
  border-bottom: 1px solid rgba(219, 232, 237, 0.14);
  background: rgba(12, 20, 24, 0.98);
  transform: translateX(-50%) rotate(45deg);
}

.pyramid-table-popover strong,
.pyramid-table-popover small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pyramid-table-popover strong {
  margin-bottom: 5px;
  color: var(--gold-soft);
  font-size: 13px;
}

.pyramid-table-popover small {
  color: var(--muted);
  font-size: 12px;
  line-height: 1.55;
}

.pyramid-table-row button:hover .pyramid-table-popover,
.pyramid-table-row button:focus-visible .pyramid-table-popover {
  display: block;
}

.round-workspace {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 12px;
  min-width: 0;
}

.round-current-card {
  display: grid;
  grid-template-columns: minmax(280px, 1.1fr) minmax(420px, 1fr);
  gap: 12px;
  align-items: start;
  padding: 16px;
  border-color: rgba(216, 169, 53, 0.28);
  background: rgba(216, 169, 53, 0.038);
}

.round-current-main {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.round-current-card h2 {
  font-size: 24px;
  line-height: 1.25;
}

.round-current-card .round-console-metrics {
  align-self: stretch;
}

.round-current-card .round-primary-actions {
  grid-column: 1 / -1;
}

.round-primary-actions .tool-button {
  flex: 0 1 auto;
  max-width: 100%;
  line-height: 1.2;
  white-space: normal;
}

.round-table-overview {
  display: grid;
  gap: 10px;
  align-content: start;
  padding: 14px;
}

.round-table-overview h3 {
  margin-top: 3px;
  font-size: 18px;
}

.round-table-list {
  display: grid;
  gap: 8px;
}

.round-table-row {
  display: grid;
  gap: 8px;
  align-items: center;
  min-height: 52px;
  padding: 9px 10px;
  color: var(--text);
  text-align: left;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.022);
  font: inherit;
  cursor: pointer;
}

.round-table-row.score-row {
  grid-template-columns: minmax(64px, 0.55fr) minmax(52px, 0.38fr) minmax(112px, 0.85fr) minmax(104px, 0.76fr) minmax(112px, 0.78fr) minmax(88px, 0.58fr) minmax(96px, 0.68fr) minmax(120px, 0.9fr) 82px;
}

.round-table-row.ranking-row {
  grid-template-columns: minmax(72px, 0.55fr) minmax(52px, 0.35fr) minmax(118px, 0.78fr) minmax(118px, 0.72fr) minmax(118px, 0.72fr) minmax(150px, 0.92fr) minmax(190px, 1.05fr) 82px;
}

.round-table-row.active {
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(216, 169, 53, 0.075);
}

.round-table-row.warning {
  color: #f1bd79;
  border-color: rgba(242, 153, 74, 0.24);
  background: rgba(242, 153, 74, 0.065);
}

.round-table-row.done {
  border-color: rgba(111, 207, 122, 0.22);
}

.round-table-row > strong,
.round-table-row span,
.round-table-row em {
  min-width: 0;
  overflow: hidden;
  color: var(--muted);
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.round-table-row > strong {
  color: var(--text);
}

.round-table-row em {
  justify-self: start;
  color: inherit;
  font-weight: 900;
  line-height: 1.35;
  white-space: normal;
}

.round-table-detail-button {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  gap: 5px;
  justify-self: end;
  min-width: 76px;
  min-height: 34px;
  color: var(--text);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.045);
  font-weight: 800;
}

.round-table-detail-button svg {
  width: 15px;
  height: 15px;
}

.round-table-detail-button:disabled {
  cursor: not-allowed;
  opacity: 0.42;
}

.round-task-hint {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  min-height: 62px;
  padding: 12px 14px;
}

.round-task-hint div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.round-task-hint.ready {
  border-color: rgba(216, 169, 53, 0.24);
  background: rgba(216, 169, 53, 0.055);
}

.round-task-hint.warning {
  border-color: rgba(242, 153, 74, 0.26);
  background: rgba(242, 153, 74, 0.075);
}

.round-task-hint.warning strong {
  color: #f1bd79;
}

.round-task-hint.done {
  border-color: rgba(111, 207, 122, 0.24);
  background: rgba(111, 207, 122, 0.065);
}

.round-task-hint.done strong {
  color: var(--green);
}

.round-console {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 12px;
  align-items: start;
}

.round-control-panel,
.round-side-panel article,
.round-table-summary article {
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
}

.round-control-panel {
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(216, 169, 53, 0.045);
}

.round-side-panel,
.round-check-list,
.round-table-summary {
  display: grid;
  gap: 10px;
}

.round-control-panel header,
.round-primary-actions,
.round-check-list p,
.round-table-summary article header {
  display: flex;
  align-items: center;
}

.round-control-panel header,
.round-check-list p,
.round-table-summary article header {
  justify-content: space-between;
  gap: 12px;
}

.round-control-panel h2,
.round-control-panel p,
.round-side-panel h3,
.round-side-panel p,
.round-check-list p,
.round-table-summary p {
  margin: 0;
}

.round-control-panel small,
.round-control-panel p,
.round-side-panel p,
.round-table-summary span {
  color: var(--muted);
}

.round-control-panel header em,
.round-table-summary header em {
  flex: 0 0 auto;
  padding: 5px 8px;
  color: var(--gold-soft);
  border: 1px solid rgba(216, 169, 53, 0.24);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.08);
  font-style: normal;
  font-weight: 800;
}

.round-readiness {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid rgba(216, 169, 53, 0.22);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.06);
}

.round-readiness strong {
  color: var(--gold-soft);
  font-size: 18px;
}

.round-console-metrics,
.round-table-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.round-console-metrics span,
.round-table-summary article div,
.round-impact {
  display: grid;
  gap: 3px;
  padding: 9px;
  color: var(--muted);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.round-console-metrics strong,
.round-table-summary strong {
  color: var(--text);
}

.round-primary-actions {
  flex-wrap: wrap;
  gap: 8px;
}

.round-target-control {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 10px;
  color: var(--muted);
  border: 1px solid rgba(219, 232, 237, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.round-target-control span {
  font-size: 13px;
  white-space: nowrap;
}

.round-target-control select {
  min-width: 130px;
  color: var(--gold-soft);
  border: 0;
  outline: 0;
  background: transparent;
  font: inherit;
  font-weight: 800;
}

.round-check-list p {
  padding: 9px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.round-check-list svg {
  width: 16px;
  height: 16px;
  flex: 0 0 auto;
}

.next-round-card.muted {
  color: var(--muted);
}

.round-summary-strip span {
  padding: 7px 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.round-workbench {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr) 300px;
  gap: 10px;
  align-items: start;
}

.round-pool-panel,
.round-check-panel {
  position: sticky;
  top: 0;
}

.round-pool-panel {
  display: grid;
  gap: 10px;
  padding: 12px;
}

.round-entry-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  padding: 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.round-entry-card.assigned {
  border-color: rgba(111, 207, 122, 0.22);
  background: rgba(111, 207, 122, 0.055);
}

.round-entry-card div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.round-entry-card strong,
.round-entry-card small,
.round-entry-card em {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.round-entry-card em {
  font-style: normal;
  font-size: 12px;
}

.round-table-board {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.round-table-card {
  display: grid;
  gap: 12px;
  padding: 12px;
}

.round-table-card.active {
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(216, 169, 53, 0.045);
}

.round-table-card.danger:not(.active) {
  border-color: rgba(242, 153, 74, 0.22);
}

.round-table-card header {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.round-table-card h3 {
  margin-top: 3px;
  font-size: 22px;
}

.mini-label {
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

.round-status {
  align-self: start;
  padding: 5px 8px;
  color: var(--gold-soft);
  border: 1px solid rgba(216, 169, 53, 0.24);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.08);
  font-style: normal;
  font-weight: 800;
  white-space: nowrap;
}

.round-status.LOCKED,
.round-status.PUBLISHED {
  color: var(--green);
  border-color: rgba(111, 207, 122, 0.24);
  background: rgba(111, 207, 122, 0.08);
}

.round-table-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.round-table-meta span,
.round-members,
.round-table-entries article,
.ranking-slots span,
.award-list article,
.wizard-stat-grid span,
.wizard-preview-tables article {
  padding: 9px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.round-table-meta span,
.round-members {
  display: grid;
  gap: 3px;
}

.round-table-entries {
  display: grid;
  gap: 7px;
}

.round-table-entries article {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.round-table-entries article span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.round-table-entries button {
  color: var(--gold-soft);
  border: 0;
  background: transparent;
}

.table-warning {
  display: flex;
  gap: 7px;
  align-items: center;
  color: #f1bd79;
  font-size: 12px;
}

.stack-field {
  display: grid;
  gap: 6px;
  margin-bottom: 12px;
}

.stack-field span {
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

.score-panels {
  grid-template-columns: 1fr;
  gap: 14px;
}

.score-config-card {
  display: grid;
  gap: 12px;
  padding: 18px;
}

.score-config-card .panel-heading {
  align-items: flex-start;
  margin-bottom: 2px;
}

.score-card-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.score-total-pill {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 10px;
  color: var(--muted);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.025);
  font-weight: 800;
  white-space: nowrap;
}

.score-total-pill.success {
  color: var(--green);
  border-color: rgba(111, 207, 122, 0.2);
  background: rgba(111, 207, 122, 0.055);
}

.score-total-pill.danger {
  color: #ff9089;
  border-color: rgba(224, 82, 82, 0.24);
  background: rgba(224, 82, 82, 0.07);
}

.score-warning {
  margin: 0 0 2px;
  color: #ffb4ad;
}

.score-comment-limit {
  display: inline-grid;
  grid-template-columns: auto auto;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  margin-bottom: 4px;
}

.score-comment-limit span {
  color: var(--muted);
}

.score-comment-limit strong {
  display: inline-flex;
  align-items: center;
  color: var(--text);
}

.compact-number-field {
  display: inline-grid;
  grid-template-columns: 72px auto;
  align-items: center;
  font-style: normal;
  font-size: 13px;
}

.compact-number-field input {
  width: 72px;
}

.dimension-head,
.dimension-row {
  display: grid;
  grid-template-columns: minmax(180px, 260px) 96px minmax(0, 1fr) 40px;
  gap: 10px;
  align-items: center;
  min-width: 0;
}

.dimension-head {
  padding: 0 10px;
  color: var(--muted);
  font-size: 12px;
}

.dimension-row {
  padding: 7px 8px;
  border: 1px solid rgba(219, 232, 237, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.016);
}

.dimension-row strong,
.dimension-row span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dimension-action-placeholder {
  min-width: 36px;
}

.progress-card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.progress-card {
  display: grid;
  gap: 12px;
}

.progress-row {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr) 46px;
  gap: 10px;
}

.progress-bar {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.07);
}

.progress-bar span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, var(--green), var(--gold-soft));
}

.ranking-slots {
  display: grid;
  gap: 8px;
}

.ranking-slots span {
  display: grid;
  gap: 4px;
}

.ranking-slots span.filled {
  color: var(--green);
  border-color: rgba(111, 207, 122, 0.24);
  background: rgba(111, 207, 122, 0.07);
}

.results-layout {
  align-items: start;
}

.feedback-review-panel {
  min-height: 0;
  height: 100%;
  padding: 0;
  overflow: hidden;
}

.feedback-review-shell {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  border: 1px solid var(--line);
  background: rgba(8, 16, 19, 0.58);
}

.feedback-filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 0 0 auto;
  min-width: 0;
  padding: 12px 18px;
  border-bottom: 1px solid var(--line);
}

.feedback-search-field,
.feedback-select-field {
  display: flex;
  align-items: center;
}

.feedback-search-field {
  position: relative;
  width: 290px;
  flex: 0 0 290px;
}

.feedback-search-field svg {
  position: absolute;
  left: 12px;
  color: var(--muted);
  font-size: 16px;
  pointer-events: none;
}

.feedback-search-field input,
.feedback-select-field select {
  height: 36px;
  color: var(--text);
  border: 1px solid rgba(219, 232, 237, 0.13);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.045);
  outline: none;
}

.feedback-search-field input {
  width: 100%;
  padding: 0 12px 0 36px;
}

.feedback-search-field input::placeholder {
  color: var(--muted);
}

.feedback-search-field input:focus,
.feedback-select-field select:focus {
  border-color: rgba(224, 184, 74, 0.52);
}

.feedback-select-field {
  gap: 8px;
  color: var(--muted);
  font-size: 12px;
  white-space: nowrap;
}

.feedback-select-field select {
  min-width: 82px;
  padding: 0 28px 0 12px;
}

.feedback-select-field option {
  color: var(--text);
  background: #121d21;
}

.feedback-status-segments {
  display: flex;
  gap: 2px;
  margin-left: auto;
  padding: 4px;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.04);
}

.feedback-status-segments button {
  min-height: 28px;
  padding: 0 12px;
  color: var(--muted);
  border: 0;
  border-radius: 5px;
  background: transparent;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.feedback-status-segments button.active {
  color: #20170c;
  background: var(--gold-soft);
}

.feedback-split-layout {
  display: grid;
  grid-template-columns: minmax(330px, 34fr) 66fr;
  min-height: 0;
  flex: 1 1 auto;
}

.feedback-entry-list {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  border-right: 1px solid var(--line);
  overflow: hidden;
}

.feedback-entry-scroll {
  min-height: 0;
  flex: 1 1 auto;
  overflow: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.12) transparent;
}

.feedback-detail-panel {
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.12) transparent;
}

.feedback-entry-scroll::-webkit-scrollbar,
.feedback-detail-panel::-webkit-scrollbar {
  width: 5px;
}

.feedback-entry-scroll::-webkit-scrollbar-track,
.feedback-detail-panel::-webkit-scrollbar-track {
  background: transparent;
}

.feedback-entry-scroll::-webkit-scrollbar-thumb,
.feedback-detail-panel::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
}

.feedback-entry-scroll::-webkit-scrollbar-thumb:hover,
.feedback-detail-panel::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.13);
}

.feedback-entry-list footer {
  flex: 0 0 auto;
  padding: 10px 18px;
  color: var(--muted);
  border-top: 1px solid var(--line);
  font-size: 12px;
}

.feedback-entry-row {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  min-height: 62px;
  padding: 11px 18px;
  color: var(--text);
  text-align: left;
  border: 0;
  border-bottom: 1px solid rgba(219, 232, 237, 0.07);
  background: transparent;
}

.feedback-entry-row:hover {
  background: rgba(255, 255, 255, 0.035);
}

.feedback-entry-row.active {
  background: rgba(224, 184, 74, 0.085);
  box-shadow: inset 0 0 0 1px rgba(224, 184, 74, 0.36);
}

.feedback-entry-row > span {
  display: grid;
  gap: 4px;
  min-width: 0;
  flex: 1 1 auto;
}

.feedback-entry-row strong,
.feedback-entry-row small,
.feedback-detail-head strong,
.feedback-detail-head small {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.feedback-entry-row > span strong {
  color: var(--text);
  font-size: 15px;
  font-weight: 900;
}

.feedback-entry-row > span small,
.feedback-entry-row em small {
  color: var(--muted);
  font-size: 12px;
}

.feedback-entry-row em {
  display: grid;
  justify-items: end;
  gap: 4px;
  flex: 0 0 auto;
  font-style: normal;
}

.feedback-entry-row em strong {
  color: var(--gold-soft);
  font-size: 15px;
}

.feedback-entry-row em small.pending {
  color: var(--red);
  font-weight: 900;
}

.feedback-detail-panel {
  min-width: 0;
  min-height: 0;
  overflow: auto;
}

.feedback-detail-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 10px;
  padding: 22px 28px;
  border-bottom: 1px solid var(--line);
}

.feedback-detail-head strong {
  font-size: 20px;
}

.feedback-detail-head span {
  color: var(--muted);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 15px;
}

.feedback-detail-head small {
  flex: 0 0 100%;
  color: var(--muted);
  font-size: 14px;
}

.feedback-detail-body {
  display: grid;
  gap: 24px;
  padding: 24px 28px 34px;
}

.feedback-block {
  display: grid;
  gap: 13px;
}

.feedback-block-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.feedback-block h2,
.feedback-issue-title {
  margin: 0;
  color: var(--text);
  font-size: 16px;
}

.feedback-block-title span {
  color: var(--muted);
  font-size: 13px;
}

.feedback-edit-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  margin-left: auto;
  padding: 0 10px;
  color: var(--gold-soft);
  border: 1px solid rgba(224, 184, 74, 0.28);
  border-radius: 6px;
  background: rgba(224, 184, 74, 0.08);
  font-size: 12px;
  font-weight: 900;
}

.feedback-edit-button svg {
  width: 14px;
  height: 14px;
}

.feedback-edit-button.compact {
  min-height: 26px;
  margin-left: 0;
  padding: 0 8px;
}

.feedback-edit-button:hover {
  border-color: rgba(224, 184, 74, 0.54);
  background: rgba(224, 184, 74, 0.14);
}

.captain-opinion-card {
  padding: 20px;
  border: 1px solid rgba(224, 184, 74, 0.24);
  border-radius: 8px;
  background: rgba(224, 184, 74, 0.06);
}

.captain-opinion-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 22px;
  color: var(--muted);
  font-size: 15px;
}

.captain-opinion-meta strong {
  color: var(--text);
}

.captain-opinion-meta .score-highlight {
  color: var(--gold-soft);
  font-size: 20px;
}

.captain-opinion-meta em {
  padding: 4px 9px;
  color: var(--muted);
  border: 1px solid var(--line);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.04);
  font-size: 13px;
  font-style: normal;
  font-weight: 800;
}

.captain-opinion-meta em.advanced {
  color: var(--green);
  border-color: rgba(111, 207, 122, 0.22);
  background: rgba(111, 207, 122, 0.1);
}

.captain-opinion-meta small {
  margin-left: auto;
  color: var(--muted);
}

.captain-opinion-card p {
  margin-top: 14px;
  color: var(--text);
  font-size: 17px;
  line-height: 1.8;
}

.captain-missing-card,
.feedback-issue-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  border-radius: 8px;
}

.captain-missing-card {
  align-items: center;
  padding: 12px 14px;
  color: #ff9a9a;
  border: 1px solid rgba(224, 82, 82, 0.28);
  background: rgba(224, 82, 82, 0.08);
  font-weight: 800;
}

.judge-score-list {
  display: grid;
  gap: 14px;
}

.judge-score-card {
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.025);
}

.judge-score-card.missing {
  border-color: rgba(224, 82, 82, 0.22);
}

.judge-score-card header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.judge-score-card header > span:first-child {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 9px;
  min-width: 0;
  flex: 1 1 300px;
}

.judge-score-card header strong {
  color: var(--text);
  font-size: 17px;
}

.judge-score-card header small,
.judge-score-meta small {
  color: var(--muted);
}

.judge-score-card header small {
  padding: 4px 8px;
  border-radius: 5px;
  background: rgba(255, 255, 255, 0.05);
  font-size: 12px;
  font-weight: 800;
}

.judge-score-card header small.captain {
  color: var(--gold-soft);
  background: rgba(224, 184, 74, 0.12);
}

.judge-anomaly {
  color: #efbd76;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.judge-anomaly.not_submitted,
.judge-anomaly.comment_missing {
  color: #ff9a9a;
}

.judge-score-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
  gap: 8px 12px;
  color: var(--muted);
  font-size: 13px;
  min-width: 260px;
  max-width: 58%;
}

.judge-score-meta small {
  white-space: nowrap;
}

.judge-score-meta strong {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 15px;
  white-space: nowrap;
}

.judge-missing-text {
  margin-top: 10px;
  color: var(--muted);
  font-size: 12px;
}

.judge-dimension-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.judge-dimension-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 5px 18px;
  padding-top: 11px;
  border-top: 1px solid rgba(219, 232, 237, 0.07);
}

.judge-dimension-row:first-child {
  padding-top: 0;
  border-top: 0;
}

.judge-dimension-row span {
  color: var(--muted);
  font-size: 14px;
}

.judge-dimension-row strong {
  color: var(--text);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 14px;
}

.judge-dimension-row p {
  grid-column: 1 / -1;
  color: rgba(230, 237, 240, 0.82);
  font-size: 14px;
  line-height: 1.7;
}

:global(.feedback-editor-dialog.el-dialog) {
  border: 1px solid rgba(219, 232, 237, 0.12);
  background: #10191d;
}

:global(.feedback-editor-dialog .el-dialog__header) {
  padding: 18px 22px 12px;
  border-bottom: 1px solid rgba(219, 232, 237, 0.1);
}

:global(.feedback-editor-dialog .el-dialog__body) {
  padding: 0;
}

.feedback-editor-title {
  display: grid;
  gap: 4px;
}

.feedback-editor-title strong {
  color: var(--text);
  font-size: 18px;
}

.feedback-editor-title small {
  color: var(--muted);
  font-size: 12px;
}

.feedback-editor {
  display: grid;
  gap: 16px;
  padding: 18px 22px 20px;
}

.feedback-editor-context {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.feedback-editor-context span {
  min-height: 28px;
  padding: 6px 10px;
  color: var(--muted);
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.035);
  font-size: 12px;
  font-weight: 800;
}

.feedback-editor-field,
.feedback-editor-dimensions {
  display: grid;
  gap: 9px;
}

.feedback-editor-field > span {
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
}

.feedback-editor-field textarea,
.feedback-editor-field input,
.feedback-editor-dimension textarea {
  width: 100%;
  color: var(--text);
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 7px;
  background: rgba(3, 10, 13, 0.58);
  outline: none;
}

.feedback-editor-field textarea,
.feedback-editor-dimension textarea {
  resize: vertical;
  min-height: 96px;
  padding: 11px 12px;
  line-height: 1.7;
}

.feedback-editor-field input {
  height: 40px;
  padding: 0 12px;
}

.feedback-editor-field textarea:focus,
.feedback-editor-field input:focus,
.feedback-editor-dimension textarea:focus {
  border-color: rgba(224, 184, 74, 0.48);
}

.feedback-editor-dimensions {
  max-height: min(48vh, 440px);
  padding-right: 4px;
  overflow: auto;
}

.feedback-editor-dimension {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid rgba(219, 232, 237, 0.09);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.025);
}

.feedback-editor-dimension > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.feedback-editor-dimension strong {
  color: var(--text);
  font-size: 14px;
}

.feedback-editor-dimension small {
  color: var(--gold-soft);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 13px;
}

.feedback-editor-dimension textarea {
  min-height: 78px;
}

.feedback-editor-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding-top: 2px;
}

.feedback-editor-footer > span {
  color: var(--muted);
  font-size: 12px;
}

.feedback-editor-footer > span.warning {
  color: #ff9a9a;
  font-weight: 900;
}

.feedback-editor-footer > div {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.feedback-issue-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.feedback-issue-list li {
  padding: 10px 12px;
  font-size: 12px;
  line-height: 1.6;
}

.feedback-issue-list li.blocking {
  color: #ff9a9a;
  border: 1px solid rgba(224, 82, 82, 0.25);
  background: rgba(224, 82, 82, 0.08);
}

.feedback-issue-list li.warning {
  color: #efbd76;
  border: 1px solid rgba(242, 153, 74, 0.22);
  background: rgba(242, 153, 74, 0.07);
}

.feedback-empty-state {
  display: grid;
  place-items: center;
  min-height: 160px;
  color: var(--muted);
  font-size: 13px;
}

.results-workbench {
  display: grid;
  grid-template-columns: minmax(780px, 1fr) minmax(320px, 380px);
  gap: 14px;
  align-items: start;
}

.award-table-card,
.result-check-card {
  display: grid;
  align-content: start;
  gap: 12px;
}

.award-table-card .panel-heading {
  margin-bottom: 0;
}

.result-check-card {
  position: sticky;
  top: 0;
}

.award-missing-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  color: #f1bd79;
  border: 1px solid rgba(242, 153, 74, 0.2);
  border-radius: 8px;
  background: rgba(242, 153, 74, 0.08);
  font-weight: 800;
}

.award-result-table {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.award-result-row {
  display: grid;
  grid-template-columns: minmax(86px, 0.46fr) minmax(96px, 0.55fr) minmax(280px, 1.36fr) minmax(240px, 1.05fr) minmax(86px, 0.48fr) minmax(110px, 0.5fr) 96px;
  gap: 10px;
  align-items: center;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.024);
}

.award-result-row.champion {
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.075);
}

.award-result-row.table-head {
  padding-top: 0;
  padding-bottom: 0;
  color: var(--muted);
  border-color: transparent;
  background: transparent;
  font-size: 12px;
  font-weight: 900;
}

.award-name-cell,
.award-certificate-cell {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.award-name-cell strong,
.award-certificate-cell strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.award-name-cell strong {
  color: var(--gold-soft);
}

.award-name-cell small,
.award-certificate-cell small {
  min-width: 0;
  overflow: hidden;
  color: var(--muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.award-result-row > span,
.award-path-cell {
  min-width: 0;
  overflow: hidden;
  color: var(--text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.award-path-cell {
  color: var(--muted);
}

.award-result-table :deep(.el-select) {
  width: 100%;
  min-width: 0;
}

.award-result-table :deep(.el-select__wrapper) {
  min-height: 34px;
  padding: 0 11px;
  border: 1px solid rgba(219, 232, 237, 0.12);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.045);
  box-shadow: none;
}

.award-result-table :deep(.el-select__wrapper:hover),
.award-result-table :deep(.el-select__wrapper.is-focused) {
  border-color: rgba(216, 169, 53, 0.36);
  background: rgba(216, 169, 53, 0.06);
  box-shadow: 0 0 0 2px rgba(216, 169, 53, 0.08);
}

.award-result-table :deep(.el-select__selected-item),
.award-result-table :deep(.el-select__placeholder),
.award-result-table :deep(.el-select__input) {
  color: var(--text);
}

.award-result-table :deep(.el-select__placeholder.is-transparent) {
  color: var(--muted);
}

.award-result-table :deep(.el-select__caret) {
  color: var(--muted);
}

.feedback-result-card .diagnosis-summary {
  min-height: 92px;
  display: flex;
  align-items: center;
  padding: 18px;
  border: 1px solid rgba(219, 232, 237, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.feedback-result-card .diagnosis-summary strong {
  color: #eef7f7;
  font-size: 18px;
}

.award-status-pill {
  justify-self: start;
  padding: 5px 8px;
  color: #f1bd79;
  border: 1px solid rgba(242, 153, 74, 0.22);
  border-radius: 999px;
  background: rgba(242, 153, 74, 0.08);
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
  white-space: nowrap;
}

.award-status-pill.CONFIRMED,
.award-status-pill.PUBLISHED,
.award-certificate-cell strong.uploaded {
  color: var(--green);
}

.award-status-pill.CONFIRMED,
.award-status-pill.PUBLISHED {
  border-color: rgba(111, 207, 122, 0.24);
  background: rgba(111, 207, 122, 0.08);
}

.award-row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
}

.award-row-actions button,
.path-toggle {
  min-height: 28px;
  padding: 0 9px;
  color: var(--gold-soft);
  border: 1px solid rgba(216, 169, 53, 0.22);
  border-radius: 6px;
  background: rgba(216, 169, 53, 0.06);
  font-size: 12px;
  font-weight: 900;
}

.award-row-actions .award-upload-icon-button {
  display: inline-grid;
  place-items: center;
  width: 30px;
  min-width: 30px;
  height: 30px;
  min-height: 30px;
  padding: 0;
  border-radius: 8px;
  font-size: 18px;
  line-height: 1;
}

.award-row-actions button:disabled {
  opacity: 0.48;
}

.result-action-panel {
  display: grid;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid rgba(219, 232, 237, 0.08);
}

.result-action-panel h3,
.result-action-panel p {
  margin: 0;
}

.result-action-panel h3 {
  color: var(--text);
  font-size: 15px;
}

.result-action-panel .tool-button {
  justify-content: center;
  width: 100%;
  min-height: 40px;
}

.result-action-panel p {
  color: var(--muted);
  line-height: 1.6;
}

.path-audit-card {
  margin-top: 14px;
}

.path-audit-card .panel-heading {
  margin-bottom: 0;
}

.path-audit-card .panel-heading > div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.path-audit-card .path-table {
  margin-top: 12px;
}

.award-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: start;
}

.award-list article {
  display: grid;
  min-width: 0;
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.award-list .champion-award-card,
.award-section-title {
  grid-column: 1 / -1;
}

.champion-award-card {
  grid-template-columns: minmax(0, 1fr) minmax(240px, 0.7fr) auto;
  align-items: center;
  border-color: rgba(216, 169, 53, 0.32);
  background: linear-gradient(135deg, rgba(216, 169, 53, 0.12), rgba(255, 255, 255, 0.025));
}

.champion-award-missing {
  grid-template-columns: minmax(0, 1fr) auto;
}

.champion-award-missing p {
  margin: 0;
  color: var(--muted);
}

.file-input {
  display: none;
}

.award-card-title {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.award-section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 2px;
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
}

.award-section-title strong {
  color: var(--text);
}

.award-list :deep(.el-select) {
  width: 100%;
  min-width: 0;
}

.award-list :deep(.el-select__wrapper) {
  min-height: 34px;
  padding: 0 11px;
  border: 1px solid rgba(219, 232, 237, 0.12);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.045);
  box-shadow: none;
}

.award-list :deep(.el-select__wrapper:hover),
.award-list :deep(.el-select__wrapper.is-focused) {
  border-color: rgba(216, 169, 53, 0.36);
  background: rgba(216, 169, 53, 0.06);
  box-shadow: 0 0 0 2px rgba(216, 169, 53, 0.08);
}

.award-list :deep(.el-select__selected-item),
.award-list :deep(.el-select__placeholder),
.award-list :deep(.el-select__input) {
  color: var(--text);
}

.award-list :deep(.el-select__placeholder.is-transparent) {
  color: var(--muted);
}

.award-list :deep(.el-select__caret) {
  color: var(--muted);
}

:global(.award-select-popper.el-popper) {
  border: 1px solid rgba(219, 232, 237, 0.12);
  background: #10191d;
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.36);
}

:global(.award-select-popper .el-select-dropdown),
:global(.award-select-popper .el-select-dropdown__list) {
  background: #10191d;
}

:global(.award-select-popper .el-popper__arrow::before) {
  border-color: rgba(219, 232, 237, 0.12);
  background: #10191d;
}

:global(.award-select-popper .el-select-dropdown__item) {
  height: 36px;
  padding: 0 14px;
  color: #e6edf0;
  line-height: 36px;
}

:global(.award-select-popper .el-select-dropdown__item.hover),
:global(.award-select-popper .el-select-dropdown__item.is-hovering),
:global(.award-select-popper .el-select-dropdown__item:hover) {
  background: rgba(216, 169, 53, 0.08);
}

:global(.award-select-popper .el-select-dropdown__item.selected),
:global(.award-select-popper .el-select-dropdown__item.is-selected) {
  color: #e0b84a;
  background: rgba(216, 169, 53, 0.11);
}

:global(.award-select-popper .el-select-dropdown__item.is-disabled) {
  color: rgba(230, 237, 240, 0.3);
}

.award-list strong {
  color: var(--gold-soft);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.award-list em {
  color: var(--muted);
  font-style: normal;
}

.award-certificate-row {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: auto auto minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  min-width: 0;
  padding-top: 4px;
  color: var(--muted);
  font-size: 12px;
  border-top: 1px solid rgba(219, 232, 237, 0.08);
}

.award-certificate-row strong {
  color: #f1bd79;
  font-size: 12px;
}

.award-certificate-row strong.uploaded {
  color: var(--green);
}

.award-certificate-row small {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.award-certificate-actions {
  display: flex;
  gap: 6px;
  justify-content: flex-end;
}

.award-certificate-actions button {
  min-height: 26px;
  padding: 0 8px;
  color: var(--gold-soft);
  border: 1px solid rgba(216, 169, 53, 0.22);
  border-radius: 6px;
  background: rgba(216, 169, 53, 0.06);
  font-size: 12px;
  font-weight: 800;
}

.award-certificate-actions button:disabled {
  opacity: 0.48;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(1, 7, 9, 0.72);
}

.round-dialog {
  width: min(920px, 100%);
  max-height: min(760px, 92vh);
  overflow: auto;
  display: grid;
  gap: 16px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(15, 24, 28, 0.98);
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.42);
}

.round-dialog header,
.round-dialog footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.round-dialog footer {
  justify-content: flex-end;
}

.wizard-steps {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
}

.wizard-steps button {
  min-height: 34px;
  color: var(--muted);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.wizard-steps button.active {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.wizard-panel {
  display: grid;
  gap: 12px;
  min-height: 260px;
  align-content: start;
}

.wizard-stat-grid,
.strategy-grid,
.wizard-preview-tables {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.strategy-grid button {
  display: grid;
  gap: 6px;
  min-height: 88px;
  padding: 12px;
  color: var(--text);
  text-align: left;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.strategy-grid button.active {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.wizard-table-list {
  display: grid;
  gap: 10px;
}

.wizard-table-list label {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.wizard-preview-tables article {
  display: grid;
  gap: 5px;
}

.wizard-preview-tables small {
  line-height: 1.5;
}

.sponsor-config-card {
  display: grid;
  gap: 16px;
}

.sponsor-editor-list {
  display: grid;
  gap: 12px;
}

.sponsor-editor-row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.sponsor-logo-box {
  display: grid;
  place-items: center;
  width: 96px;
  aspect-ratio: 1;
  overflow: hidden;
  border: 1px solid rgba(216, 169, 53, 0.2);
  border-radius: 8px;
  background: #fff;
}

.sponsor-logo-box img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.sponsor-logo-box span {
  color: var(--gold-soft);
  font-size: 13px;
  font-weight: 900;
}

.sponsor-fields {
  display: grid;
  grid-template-columns: minmax(160px, 0.7fr) minmax(220px, 1fr) 90px;
  gap: 10px;
  min-width: 0;
}

.sponsor-fields label {
  min-width: 0;
}

.sponsor-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
}

.inline-check {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
}

.hidden-file-input {
  display: none;
}

.sponsor-logo-crop-mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 32px;
  background: rgba(2, 5, 7, 0.76);
  backdrop-filter: blur(10px);
}

.sponsor-logo-crop-dialog {
  display: grid;
  gap: 16px;
  width: min(620px, 100%);
  padding: 18px;
  border: 1px solid rgba(216, 169, 53, 0.26);
  border-radius: 10px;
  background: #101719;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.42);
}

.sponsor-logo-crop-dialog header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.sponsor-logo-crop-dialog h3 {
  margin: 0;
  color: var(--text);
  font-size: 20px;
  line-height: 1.15;
}

.sponsor-logo-crop-dialog header span {
  display: block;
  margin-top: 6px;
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
}

.sponsor-logo-crop-stage {
  position: relative;
  width: min(460px, 100%);
  aspect-ratio: 1;
  justify-self: center;
  overflow: hidden;
  border-radius: 8px;
  background:
    linear-gradient(45deg, rgba(255, 255, 255, 0.86) 25%, transparent 25%),
    linear-gradient(-45deg, rgba(255, 255, 255, 0.86) 25%, transparent 25%),
    linear-gradient(45deg, transparent 75%, rgba(255, 255, 255, 0.86) 75%),
    linear-gradient(-45deg, transparent 75%, rgba(255, 255, 255, 0.86) 75%),
    #eef1f2;
  background-position: 0 0, 0 10px, 10px -10px, -10px 0;
  background-size: 20px 20px;
  cursor: grab;
  touch-action: none;
  user-select: none;
}

.sponsor-logo-crop-stage:active {
  cursor: grabbing;
}

.sponsor-logo-crop-stage img {
  position: absolute;
  transform: translate(-50%, -50%);
  object-fit: fill;
  user-select: none;
  pointer-events: none;
}

.sponsor-logo-crop-frame {
  position: absolute;
  inset: 0;
  border: 2px solid rgba(216, 169, 53, 0.92);
  box-shadow: inset 0 0 0 1px rgba(7, 12, 14, 0.24);
  pointer-events: none;
}

.sponsor-logo-crop-tools {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 10px;
  align-items: center;
}

.sponsor-logo-crop-tools label {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  color: var(--muted);
  font-size: 13px;
  font-weight: 900;
}

.sponsor-logo-crop-tools input[type='range'] {
  width: 100%;
  accent-color: var(--gold);
}

.sponsor-empty {
  min-height: 160px;
}

.not-found {
  display: grid;
  place-content: center;
  gap: 14px;
  min-height: 100%;
}

@media (max-width: 1400px) {
  .round-planning-shell {
    grid-template-columns: 350px minmax(0, 1fr);
  }

  .round-current-card {
    grid-template-columns: 1fr;
  }

  .round-workbench {
    grid-template-columns: 280px minmax(0, 1fr) 280px;
  }

  .judge-workbench {
    grid-template-columns: 150px minmax(0, 1fr) 300px;
  }

  .future-task-list,
  .overview-grid,
  .sponsor-editor-row,
  .sponsor-fields {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .sponsor-logo-box,
  .sponsor-controls {
    grid-column: 1 / -1;
  }
}

@media (max-width: 1100px) {
  .competition-detail {
    padding: 18px;
    overflow: auto;
  }

  .head-main,
  .detail-tabbar,
  .two-column,
  .judge-workbench,
  .round-planning-shell,
  .round-current-card,
  .round-workbench,
  .round-console,
  .round-table-summary,
  .round-table-board,
  .score-panels,
  .progress-card-grid,
  .base-form-grid,
  .award-list {
    grid-template-columns: 1fr;
  }

  .judge-nav-panel,
  .judge-right-panel,
  .round-pool-panel,
  .round-check-panel {
    position: static;
  }

  .round-flow {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .round-pyramid-panel {
    min-height: auto;
  }

  .round-table-row {
    grid-template-columns: minmax(72px, 0.7fr) minmax(68px, 0.45fr) minmax(130px, 1fr);
  }

  .round-table-row span:nth-of-type(n + 3),
  .round-table-row em {
    grid-column: span 1;
  }

  .round-table-detail-button {
    grid-column: 1 / -1;
    justify-self: stretch;
  }

  .detail-tabbar {
    align-items: stretch;
  }

  .tab-save-actions {
    justify-content: flex-start;
    padding-left: 4px;
  }
}
</style>
