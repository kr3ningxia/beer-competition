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
            <template v-if="competition.edition">
              <span class="title-divider">|</span>
              <span class="title-edition">{{ competition.edition }}</span>
            </template>
          </div>
          <div class="meta-line">
            <span :class="['state-badge', statusInfo.tone]">{{ statusInfo.label }}</span>
            <span :class="['window-badge', registrationWindowInfo.tone]">{{ registrationWindowInfo.label }}</span>
            <span>{{ currentRound?.name || '未创建轮次' }}</span>
            <span>{{ currentRoundTypeLabel }}</span>
            <span>比赛日 {{ formatDate(competition.date) }}</span>
            <span>{{ competition.entryFee }} 元 / 款</span>
          </div>
        </div>
        <div class="head-action-group">
          <button
            v-if="activeTab !== 'rounds'"
            class="tool-button primary"
            type="button"
            :disabled="!stagePrimaryAction.enabled"
            @click="handlePrimaryAction"
          >
            {{ stagePrimaryAction.text }}
            <Right />
          </button>
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
            v-for="tab in tabs"
            :key="tab.key"
            :class="{ active: activeTab === tab.key }"
            type="button"
            @click="activeTab = tab.key"
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
              <p>{{ currentRoundEntryCount }} 款酒 · 目标 {{ currentRoundTargetCount }} 款</p>
            </article>
            <article class="metric-card">
              <small>晋级池</small>
              <strong>{{ advancedPool.length }}</strong>
              <p>锁定上一轮后用于创建下一轮</p>
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
                  <span>当前阶段没有阻塞项，可以继续推进。</span>
                </p>
                <p v-for="item in overviewActionItems" v-else :key="item.key" :class="item.level">
                  <component :is="item.level === 'danger' ? Warning : Clock" />
                  <span>{{ item.text }}</span>
                  <button v-if="item.targetTab" class="link-action" type="button" @click="activeTab = item.targetTab">去处理</button>
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

        <section v-if="activeTab === 'entryConfig'" class="tab-panel entry-config-panel">
          <div v-if="!editable.entryStructure" class="edit-banner locked">
            <Lock />
            报名已开放，报名结构已锁定。若需修改投递组别或补充字段，请先关闭报名或由管理员修正数据。
          </div>

          <article class="panel-card category-card">
            <div class="panel-heading">
              <div>
                <h2>投递组别</h2>
              </div>
              <button v-if="editable.entryStructure" class="tool-button" type="button" @click="categoryForm.push('')">
                <Plus />
                添加组别
              </button>
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
              <p v-if="categoryForm.length === 0" class="empty-line">当前没有投递组别数据，请管理员修正。</p>
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
                <button class="tool-button" type="button" @click="router.push('/admin/style-libraries')">
                  查看风格库
                </button>
              </div>
            </div>
            <div class="style-distribution">
              <div class="style-distribution-head">
                <strong>分类分布</strong>
              </div>
              <div class="style-distribution-list">
                <span v-for="category in styleCategorySummary" :key="category.name">
                  <b>{{ category.name }}</b>
                  <small>{{ category.count }} 个风格</small>
                </span>
                <p v-if="styleCategorySummary.length === 0" class="empty-line">当前风格库还没有可用分类。</p>
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
                <input v-model.trim="field.helpText" :disabled="!editable.entryStructure" placeholder="给厂商看的填写说明" />
                <label class="check-control"><input v-model="field.required" type="checkbox" :disabled="!editable.entryStructure" /> 必填</label>
                <label class="check-control"><input v-model="field.visibleToJudges" type="checkbox" :disabled="!editable.entryStructure" /> 评审可见</label>
                <button class="icon-button" type="button" :disabled="!editable.entryStructure" @click="removeItem(entryFieldForm, index)">
                  <Delete />
                </button>
              </div>
              <p v-if="entryFieldForm.length === 0" class="empty-line">当前没有报名补充字段。</p>
            </div>
          </article>
        </section>

        <section v-if="activeTab === 'entries'" class="tab-panel">
          <div class="overview-grid">
            <article class="metric-card"><small>全部</small><strong>{{ competition.entriesSummary.total }}</strong><p>已报名酒款</p></article>
            <article class="metric-card"><small>待付款</small><strong>{{ competition.entriesSummary.pendingPayment }}</strong><p>需跟进支付</p></article>
            <article class="metric-card"><small>已入库</small><strong>{{ competition.entriesSummary.stored }}</strong><p>现场已确认</p></article>
            <article class="metric-card"><small>晋级轨迹</small><strong>{{ roundEntryPool.length }}</strong><p>真实轮次分配</p></article>
          </div>
          <article class="panel-card">
            <div class="panel-heading">
              <h2>参赛酒款</h2>
              <span>匿名编号、短编号和轮次轨迹</span>
            </div>
            <div class="data-table entries-table">
              <div class="table-head">
                <span>匿名编号</span>
                <span>短编号</span>
                <span>投递组别</span>
                <span>基础风格</span>
                <span>轮次轨迹</span>
                <span>状态</span>
              </div>
              <div v-for="entry in roundEntryPool" :key="entry.uuid" class="table-row">
                <strong>{{ entry.uuid }}</strong>
                <span>{{ entry.shortCode }}</span>
                <span>{{ entry.categoryName }}</span>
                <span>{{ entry.style }}</span>
                <span>{{ getEntryPath(entry.uuid) }}</span>
                <span>{{ entryStatusLabels[entry.status] || entry.status }}</span>
              </div>
            </div>
          </article>
        </section>

        <section v-if="activeTab === 'judges'" class="tab-panel">
          <TableAllocationWorkbench
            :allocation-mode="allocationMode"
            :rounds="rounds"
            :active-round-id="activeRoundId"
            :current-round="currentRound"
            :current-round-tables="currentRoundTables"
            :selected-round-table="selectedRoundTable"
            :selected-round-table-id="selectedRoundTableId"
            :editable-judges="Boolean(editable.judgeTables)"
            :judge-table-form="judgeTableForm"
            :role-options="roleOptions"
            :role-filters="roleFilters"
            :judge-keyword="judgeKeyword"
            :judge-role-filter="judgeRoleFilter"
            :selected-table-local-id="selectedTableLocalId"
            :selected-role="selectedRole"
            :judge-metrics="judgeMetrics"
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
            :captain-candidates="captainCandidates"
            :round-validation-issues="roundValidationIssues"
            :can-publish="canPublishCurrentRound"
            :get-judge="getJudge"
            :get-judge-initial="getJudgeInitial"
            :is-assigned="isAssigned"
            :is-judge-active="isJudgeActive"
            :get-round-entry-assignment="getRoundEntryAssignment"
            :get-round-table-issues="getRoundTableIssues"
            @update:allocation-mode="allocationMode = $event"
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
            @generate-first-round="generateFirstRoundFromJudges"
            @toggle-entry-selection="toggleEntrySelection"
            @add-selected-to-table="addSelectedEntriesToTable"
            @add-entry-to-selected-table="addEntryToSelectedRoundTable"
            @start-entry-drag="startEntryDrag"
            @assign-evenly="assignCurrentRoundEvenly"
            @assign-by-category="assignCurrentRoundByCategory"
            @clear-round-assignments="clearCurrentRoundAssignments"
            @select-round-table="selectedRoundTableId = $event"
            @drop-entry-on-round-table="dropEntryOnRoundTable"
            @remove-entry-from-round-table="removeEntryFromRoundTable"
            @update-table-captain="updateRoundTableCaptain"
            @update-table-target="updateRoundTableTarget"
            @publish-current-round="publishCurrentRound"
          />
        </section>

        <section v-if="activeTab === 'rounds'" class="tab-panel rounds-panel">
          <RoundStepper
            :rounds="rounds"
            :active-round-id="activeRoundId"
            :round-status-labels="roundStatusLabels"
            @select-round="selectRound"
          />

          <section class="round-console">
            <article class="round-control-panel">
              <header>
                <div>
                  <small>{{ currentRound?.name }} · {{ currentRoundTypeLabel }} · {{ currentRoundStatusText }}</small>
                  <h2>{{ currentRound?.name || '未创建轮次' }}</h2>
                </div>
              </header>

              <div class="round-readiness">
                <strong>{{ roundReadinessTitle }}</strong>
                <span>{{ roundReadinessDetail }}</span>
              </div>

              <div class="round-console-metrics">
                <span>桌数 <strong>{{ currentRoundTables.length }}</strong></span>
                <span>酒款 <strong>{{ currentRoundEntryCount }} / {{ currentPoolEntries.length }}</strong></span>
                <span>{{ currentRoundTargetLabel }} <strong>{{ currentRoundTargetDisplay }}</strong></span>
                <span>问题 <strong>{{ roundValidationIssues.length }}</strong></span>
              </div>

              <div class="round-primary-actions">
                <button class="tool-button" type="button" @click="editRoundAllocation(currentRound.id, 'entries')">
                  查看分桌
                </button>
                <button
                  v-if="currentRound?.status === 'DRAFT'"
                  class="tool-button primary"
                  type="button"
                  :disabled="!canPublishCurrentRound"
                  @click="publishCurrentRound"
                >
                  {{ currentRound?.type === 'SCORE' ? '发布给评委' : '发布给桌长' }}
                </button>
                <button v-if="currentRound?.roundNo === 1 && currentRound?.status === 'PUBLISHED'" class="tool-button primary" type="button" @click="completeFirstRoundAction">
                  确认第一轮完成
                </button>
                <button v-if="currentRound?.type === 'RANKING' && ['DRAFT', 'IN_PROGRESS'].includes(currentRound?.status)" class="tool-button primary" type="button" :disabled="!canSubmitRankingRound" @click="submitRankingRound">
                  提交排序
                </button>
                <button v-if="currentRound?.status === 'SUBMITTED'" class="tool-button primary" type="button" @click="lockCurrentRound">
                  确认锁定
                </button>
              </div>
            </article>

            <aside class="round-side-panel">
              <article>
                <h3>发布前检查</h3>
                <div class="round-check-list">
                  <p v-for="check in roundReadinessChecks" :key="check.label" :class="check.done ? 'ok' : 'warning'">
                    <CircleCheck v-if="check.done" />
                    <Warning v-else />
                    <span>{{ check.label }}</span>
                    <strong>{{ check.value }}</strong>
                  </p>
                </div>
              </article>

              <article v-if="canCreateNextRound" class="next-round-card">
                <h3>下一步</h3>
                <p>{{ advancedPool.length }} 款晋级酒款可用于创建{{ nextRoundName }}。</p>
                <button class="tool-button primary" type="button" @click="openCreateRoundDialog">创建{{ nextRoundName }}</button>
              </article>
              <article v-else class="next-round-card muted">
                <h3>下一步</h3>
                <p>{{ roundNextStepText }}</p>
              </article>
            </aside>
          </section>

          <section class="round-table-summary">
            <article v-for="table in currentRoundTables" :key="`round-summary-${table.id}`">
              <header>
                <strong>{{ table.name }}</strong>
                <em>{{ table.entryUuids.length }} 款</em>
              </header>
              <div>
                <span>桌长</span>
                <strong>{{ getJudge(table.captainPublicId)?.name || '未指定' }}</strong>
              </div>
              <div>
                <span>{{ currentRound?.type === 'SCORE' ? '每桌晋级' : '排序目标' }}</span>
                <strong>{{ table.targetCount }}</strong>
              </div>
              <p v-if="getRoundTableIssues(table).length" class="warning">{{ getRoundTableIssues(table)[0] }}</p>
              <p v-else class="ok">本桌已就绪</p>
            </article>
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
                    :disabled="config.dimensions.length >= 4"
                    @click="addCrossScoreDimension(config)"
                  >
                    <Plus />
                    添加维度
                  </button>
                </div>
              </div>
              <p v-if="getScoreTotal(config) !== 50" class="score-warning">
                当前总分 {{ getScoreTotal(config) }}，必须调整为 50 后才能保存。
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
                  <input v-if="editable.scoreConfigs" v-model.trim="dimension.label" placeholder="维度名称" />
                  <span v-else>{{ dimension.label }}</span>
                  <input v-if="editable.scoreConfigs" v-model.number="dimension.maxScore" min="1" type="number" />
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

        <section v-if="activeTab === 'progress'" class="tab-panel">
          <div class="round-flow compact-flow">
            <button
              v-for="round in rounds"
              :key="`progress-${round.id}`"
              :class="['round-flow-item', { active: round.id === activeRoundId }]"
              type="button"
              @click="selectRound(round.id)"
            >
              <small>{{ round.type === 'SCORE' ? '评分制' : '选择排序' }}</small>
              <strong>{{ round.name }}</strong>
              <em>{{ roundStatusLabels[round.status] || round.status }}</em>
            </button>
          </div>
          <article class="panel-card">
            <div class="panel-heading">
              <h2>桌次重组</h2>
              <span>{{ roundRestructureText }}</span>
            </div>
            <div class="round-path wide">
              <span v-for="round in rounds" :key="`path-${round.id}`">
                <strong>{{ round.name }}</strong>
                <small>{{ round.tables.map((table) => table.name).join(' / ') }}</small>
              </span>
            </div>
          </article>
          <div class="progress-card-grid">
            <article v-for="table in currentRoundTables" :key="`progress-${table.id}`" class="panel-card progress-card">
              <div class="panel-heading">
                <h2>{{ table.name }}</h2>
                <span>{{ getJudge(table.captainPublicId)?.name || '未指定桌长' }}</span>
              </div>
              <template v-if="currentRound?.type === 'SCORE'">
                <div class="progress-row">
                  <strong>普通评委评分</strong>
                  <div class="progress-bar"><span :style="{ width: `${table.judgeProgress || 0}%` }" /></div>
                  <em>{{ table.judgeProgress || 0 }}%</em>
                </div>
                <div class="progress-row">
                  <strong>桌长汇总</strong>
                  <div class="progress-bar"><span :style="{ width: `${table.captainProgress || 0}%` }" /></div>
                  <em>{{ table.captainProgress || 0 }}%</em>
                </div>
                <p>已选晋级 {{ table.advancedCount || 0 }} / {{ table.targetCount }}</p>
              </template>
              <template v-else>
                <div class="ranking-slots">
                  <span v-for="slot in getRankingSlots(table)" :key="slot.label" :class="{ filled: slot.uuid }">
                    <small>{{ slot.label }}</small>
                    <strong>{{ slot.uuid || '待选择' }}</strong>
                  </span>
                </div>
                <p>桌长排序 {{ getFilledRankingCount(table) }} / {{ table.targetCount }} · {{ roundStatusLabels[table.status] || table.status }}</p>
              </template>
            </article>
          </div>
        </section>

        <section v-if="activeTab === 'results'" class="tab-panel">
          <div class="two-column results-layout">
            <article class="panel-card">
              <div class="panel-heading">
                <h2>组别奖项草稿</h2>
                <span>{{ awardDrafts.length }} 项</span>
              </div>
              <div class="award-list">
                <article v-for="award in awardDrafts" :key="`${award.category}-${award.slot}`">
                  <span>{{ award.category }}</span>
                  <strong>{{ award.slot }}</strong>
                  <em>{{ award.uuid || '待确认' }}</em>
                </article>
              </div>
            </article>
            <article class="panel-card">
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
              </div>
              <div class="publish-actions">
                <button class="tool-button" type="button" @click="showPendingStageMessage('导出评分数据')">导出评分数据</button>
                <button class="tool-button primary" type="button" :disabled="!resultChecks.every((item) => item.done)" @click="publishResultsAction">
                  发布结果
                </button>
              </div>
            </article>
          </div>
          <article class="panel-card">
            <div class="panel-heading">
              <h2>晋级路径摘要</h2>
              <span>{{ roundRestructureText ? `${roundRestructureText} -> 奖项` : '暂无轮次路径' }}</span>
            </div>
            <div class="path-table">
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

    <CreateRoundWizard
      :open="createRoundDialogOpen"
      :next-round-name="nextRoundName"
      :advanced-pool="advancedPool"
      :advanced-category-stats="advancedCategoryStats"
      @close="closeCreateRoundDialog"
      @finish="finishCreateRound"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Back,
  Calendar,
  Check,
  CircleCheck,
  Clock,
  DataAnalysis,
  Delete,
  Files,
  Finished,
  Lock,
  Medal,
  Plus,
  Right,
  Search,
  Setting,
  Tickets,
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
  createFirstRound,
  createNextRound,
  deleteCompetition,
  fetchCompetitionDetail,
  fetchJudges,
  fetchStyleLibraries,
  lockRound,
  openCompetitionRegistration,
  prepareCompetitionJudging,
  publishCompetitionResults,
  publishRound,
  saveRoundAllocation,
  updateCompetitionCategories,
  updateCompetitionEntryFields,
  updateCompetitionJudgeAssignments,
  updateCompetitionJudgeTables,
  updateCompetitionStyles,
  updateScoreConfigs,
} from '@/api/admin'
import { fallbackStyleLibraries, getStyleLibrary, normalizeStyleLibraries } from './styleLibraries'
import CreateRoundWizard from './components/competition-detail/CreateRoundWizard.vue'
import RoundStepper from './components/competition-detail/RoundStepper.vue'
import TableAllocationWorkbench from './components/competition-detail/TableAllocationWorkbench.vue'

const route = useRoute()
const router = useRouter()
const activeTab = ref(route.query.tab || 'overview')
const loading = ref(false)
const competition = ref(null)
const categoryForm = reactive([])
const entryFieldForm = reactive([])
const judgeTableForm = reactive([])
const judgeAssignmentForm = reactive([])
const scoreConfigForm = reactive([])
const judgePool = ref([])
const judgeKeyword = ref('')
const judgeRoleFilter = ref('ALL')
const allocationMode = ref('judges')
const selectedTableLocalId = ref(null)
const selectedRole = ref('CAPTAIN')
const draggingItem = ref(null)

const rounds = ref([])
const roundEntryPool = ref([])
const activeRoundId = ref('')
const selectedRoundTableId = ref('')
const roundKeyword = ref('')
const roundCategoryFilter = ref('全部')
const selectedEntryUuids = ref([])
const createRoundDialogOpen = ref(false)
const selectedStyleLibraryVersion = ref('')
const styleLibraryOptions = ref(normalizeStyleLibraries(fallbackStyleLibraries))

const tabs = [
  { key: 'overview', label: '概览', icon: DataAnalysis },
  { key: 'entryConfig', label: '报名配置', icon: Setting },
  { key: 'entries', label: '参赛酒款', icon: Tickets },
  { key: 'judges', label: '分桌分配', icon: Files },
  { key: 'rounds', label: '轮次编排', icon: Calendar },
  { key: 'score', label: '评分表', icon: Finished },
  { key: 'progress', label: '现场进度', icon: Clock },
  { key: 'results', label: '结果发布', icon: Medal },
]

const entryStatusLabels = {
  PENDING_PAYMENT: '待付款',
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
  { label: '全部', value: 'ALL' },
  { label: '未分配', value: 'UNASSIGNED' },
  { label: '桌长候选', value: 'CAPTAIN' },
  { label: '专业', value: 'PROFESSIONAL' },
  { label: '跨界', value: 'CROSS' },
]

const roundStatusLabels = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  IN_PROGRESS: '处理中',
  SUBMITTED: '已提交',
  LOCKED: '已锁定',
}

const fallbackJudgePool = [
  { publicId: 'J-DEMO-001', name: '张远', maskedPhone: '138****0001', qualification: 'BJCP 认证 · 桌长候选', status: 1 },
  { publicId: 'J-DEMO-002', name: '李澄', maskedPhone: '138****0002', qualification: '专业评审 · 酒厂顾问', status: 1 },
  { publicId: 'J-DEMO-003', name: '王禾', maskedPhone: '138****0003', qualification: '专业评审 · 酿酒师', status: 1 },
  { publicId: 'J-DEMO-004', name: '陈乐', maskedPhone: '138****0004', qualification: '跨界评审 · 媒体', status: 1 },
  { publicId: 'J-DEMO-005', name: '赵予', maskedPhone: '138****0005', qualification: 'BJCP 认证 · 专业评审', status: 1 },
  { publicId: 'J-DEMO-006', name: '周亦', maskedPhone: '138****0006', qualification: '跨界评审 · 餐饮主理人', status: 1 },
  { publicId: 'J-DEMO-007', name: '吴嘉', maskedPhone: '-', qualification: '专业评审 · 待补手机号', status: 1 },
  { publicId: 'J-DEMO-008', name: '郑南', maskedPhone: '138****0008', qualification: '桌长候选 · 赛事经验', status: 1 },
]

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
const stagePrimaryAction = computed(() => resolveStagePrimaryAction())
const stageSecondaryActions = computed(() => resolveStageSecondaryActions())
const tabSaveAction = computed(() => {
  if (activeTab.value === 'entryConfig' && editable.value.entryStructure) {
    return { label: '保存报名配置', handler: saveEntryConfig }
  }
  if (activeTab.value === 'score' && editable.value.scoreConfigs) {
    return { label: '保存评分表', handler: saveScoreConfigs }
  }
  return null
})
const futureStageTasks = computed(() => buildFutureStageTasks())
const currentRound = computed(() => rounds.value.find((round) => round.id === activeRoundId.value) || rounds.value[0])
const currentRoundTables = computed(() => currentRound.value?.tables || [])
const selectedRoundTable = computed(() => currentRoundTables.value.find((table) => table.id === selectedRoundTableId.value) || currentRoundTables.value[0])
const currentRoundTypeLabel = computed(() => (currentRound.value?.type === 'SCORE' ? '评分制' : '选择排序制'))
const currentRoundStatusText = computed(() => roundStatusLabels[currentRound.value?.status] || currentRound.value?.status || '-')
const currentRoundEntryCount = computed(() => new Set(currentRoundTables.value.flatMap((table) => table.entryUuids)).size)
const currentRoundTargetCount = computed(() => currentRoundTables.value.reduce((sum, table) => sum + Number(table.targetCount || 0), 0))
const advancedPool = computed(() => roundEntryPool.value.filter((entry) => entry.advanced))
const overviewActionItems = computed(() => buildOverviewActionItems())
const roundCategoryFilters = computed(() => ['全部', ...new Set(currentPoolEntries.value.map((entry) => entry.categoryName).filter(Boolean))])
const currentPoolEntries = computed(() => getPoolEntriesForRound(currentRound.value))
const filteredRoundPool = computed(() => {
  const query = roundKeyword.value.toLowerCase()
  return currentPoolEntries.value.filter((entry) => {
    const matchesCategory = roundCategoryFilter.value === '全部' || entry.categoryName === roundCategoryFilter.value
    const matchesQuery = !query || [entry.uuid, entry.shortCode, entry.categoryName, entry.style, entry.sourceTable]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query))
    return matchesCategory && matchesQuery
  })
})
const unassignedRoundEntries = computed(() => currentPoolEntries.value.filter((entry) => !getRoundEntryAssignment(entry.uuid)))
const roundCategoryStats = computed(() => {
  const map = new Map()
  currentPoolEntries.value.forEach((entry) => {
    const current = map.get(entry.categoryName) || { category: entry.categoryName, total: 0, assigned: 0, unassigned: 0 }
    current.total += 1
    if (getRoundEntryAssignment(entry.uuid)) current.assigned += 1
    else current.unassigned += 1
    map.set(entry.categoryName, current)
  })
  return [...map.values()]
})
const roundValidationIssues = computed(() => buildRoundValidationIssues(currentRound.value))
const canPublishCurrentRound = computed(() => currentRound.value?.status === 'DRAFT' && roundValidationIssues.value.length === 0)
const roundReadinessChecks = computed(() => buildRoundReadinessChecks())
const currentRoundTargetLabel = computed(() => (currentRound.value?.type === 'SCORE' ? '晋级' : '排序'))
const currentRoundTargetDisplay = computed(() => {
  if (!currentRound.value) return '-'
  if (currentRound.value.type !== 'SCORE') return `${currentRoundTargetCount.value} 款`
  const targets = [...new Set(currentRoundTables.value.map((table) => Number(table.targetCount || 0)).filter(Boolean))]
  return targets.length === 1 ? `每桌 ${targets[0]}，共 ${currentRoundTargetCount.value}` : `共 ${currentRoundTargetCount.value}`
})
const currentRoundPublishTarget = computed(() => (currentRound.value?.type === 'RANKING' ? '桌长' : '评委'))
const roundReadinessTitle = computed(() => {
  if (!currentRound.value) return '还没有轮次'
  if (currentRound.value.status === 'DRAFT') return canPublishCurrentRound.value ? `${currentRound.value.name}已准备好，可以发布给${currentRoundPublishTarget.value}` : `${currentRound.value.name}发布前还有问题`
  if (currentRound.value.status === 'PUBLISHED') return `${currentRound.value.name}已发布给${currentRoundPublishTarget.value}`
  if (currentRound.value.status === 'IN_PROGRESS') return '本轮排序进行中'
  if (currentRound.value.status === 'SUBMITTED') return '排序已提交，等待确认'
  if (currentRound.value.status === 'LOCKED') return `${currentRound.value.name}已锁定`
  return currentRoundStatusText.value
})
const roundReadinessDetail = computed(() => {
  if (!currentRound.value) return '请先创建轮次。'
  if (roundValidationIssues.value.length) return roundValidationIssues.value[0]
  if (currentRound.value.status === 'DRAFT') return currentRound.value.type === 'SCORE'
    ? '发布后，评委开始评分，桌长可查看本桌酒款并汇总结果。'
    : '发布后，桌长可以进入排序任务并提交本桌结果。'
  if (currentRound.value.status === 'LOCKED') return canCreateNextRound.value ? `可以创建${nextRoundName.value}。` : '可以进入结果确认或等待后续安排。'
  return '请根据现场完成情况继续推进。'
})
const roundNextStepText = computed(() => {
  if (!currentRound.value) return '请先创建并配置第一轮。'
  if (currentRound.value.status === 'DRAFT') return canPublishCurrentRound.value ? `点击发布，让${currentRoundPublishTarget.value}开始${currentRound.value.name}。` : '先处理发布前检查里的问题。'
  if (currentRound.value.status === 'PUBLISHED') return '等待评委评分完成，再由桌长汇总第一轮结果。'
  if (currentRound.value.status === 'IN_PROGRESS') return '等待桌长提交排序。'
  if (currentRound.value.status === 'SUBMITTED') return '确认排序无误后锁定本轮。'
  if (currentRound.value.status === 'LOCKED') return advancedPool.value.length ? '晋级池已有酒款，可在满足条件时创建下一轮。' : '本轮已锁定，暂无可创建下一轮的晋级酒款。'
  return '继续推进当前轮次。'
})
const canCreateNextRound = computed(() => {
  const lastRound = rounds.value[rounds.value.length - 1]
  return lastRound?.status === 'LOCKED' && advancedPool.value.length > 0
})
const nextRoundNumber = computed(() => rounds.value.length + 1)
const nextRoundName = computed(() => `第${toChineseNumber(nextRoundNumber.value)}轮`)
const canSubmitRankingRound = computed(() => {
  if (!currentRound.value || currentRound.value.type !== 'RANKING') return false
  if (!['IN_PROGRESS', 'DRAFT'].includes(currentRound.value.status)) return false
  return currentRound.value.tables.every((table) => getFilledRankingCount(table) === Number(table.targetCount || 0))
})
const captainCandidates = computed(() => {
  const pool = [...judgePool.value]
  fallbackJudgePool.forEach((judge) => {
    if (!pool.some((item) => item.publicId === judge.publicId)) pool.push(judge)
  })
  return pool.filter((judge) => inferJudgeRoles(judge).includes('CAPTAIN') && isJudgeActive(judge))
})
const advancedCategoryStats = computed(() => {
  const map = new Map()
  advancedPool.value.forEach((entry) => map.set(entry.categoryName, (map.get(entry.categoryName) || 0) + 1))
  return [...map.entries()].map(([category, count]) => ({ category, count }))
})
const roundRestructureText = computed(() => rounds.value.map((round) => `${round.name} ${round.tables.length}桌`).join(' -> '))
const awardDrafts = computed(() => competition.value?.resultDrafts || [])
const resultPathEntries = computed(() => roundEntryPool.value.filter((entry) => entry.advanced || getEntryAward(entry.uuid)).slice(0, 8))
const resultChecks = computed(() => [
  { label: '第一轮桌长汇总已完成', done: rounds.value.find((round) => round.roundNo === 1)?.status === 'LOCKED' },
  { label: '后续轮排序已锁定', done: rounds.value.filter((round) => round.type === 'RANKING').every((round) => round.status === 'LOCKED') },
  { label: '组别奖项无重复', done: awardDrafts.value.every((award) => award.uuid) },
  { label: '总冠军候选已确认', done: Boolean(getEntryAward(roundEntryPool.value[0]?.uuid) || awardDrafts.value.find((award) => award.slot === '金奖')?.uuid) },
])

const selectedTable = computed(() => judgeTableForm.find((table) => table.localId === selectedTableLocalId.value))
const selectedRoleLabel = computed(() => roleOptions.find((role) => role.value === selectedRole.value)?.label || '角色')
const selectedTargetLabel = computed(() => selectedTable.value ? `${selectedTable.value.tableName || '未命名评审桌'} · ${selectedRoleLabel.value}` : '请先选择评审桌')
const judgeMetrics = computed(() => ({
  assigned: judgeAssignmentForm.length,
  captain: countAssignedRole('CAPTAIN'),
  professional: countAssignedRole('PROFESSIONAL'),
  cross: countAssignedRole('CROSS'),
}))
const validationIssues = computed(() => {
  const issues = []
  if (!judgeTableForm.length) issues.push('至少需要创建 1 张基础桌')
  judgeTableForm.forEach((table) => issues.push(...tableValidationIssues(table)))
  const duplicateJudgeIds = judgeAssignmentForm
    .map((assignment) => assignment.judgePublicId)
    .filter((judgePublicId, index, list) => list.indexOf(judgePublicId) !== index)
  new Set(duplicateJudgeIds).forEach((judgePublicId) => {
    issues.push(`${getJudge(judgePublicId)?.name || '某位评委'}在本场比赛中被重复分配`)
  })
  return issues
})
const filteredJudgePool = computed(() => {
  const query = judgeKeyword.value.toLowerCase()
  return judgePool.value.filter((judge) => {
    const matchesKeyword = !query || [judge.name, judge.maskedPhone, judge.qualification]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query))
    const matchesRole =
      judgeRoleFilter.value === 'ALL' ||
      (judgeRoleFilter.value === 'UNASSIGNED' && !isAssigned(judge.publicId)) ||
      inferJudgeRoles(judge).includes(judgeRoleFilter.value)
    return matchesKeyword && matchesRole
  })
})

onMounted(() => {
  loadStyleLibraries()
  loadDetail()
  loadJudgePool()
})
watch(() => route.params.id, loadDetail)
watch(() => route.query.tab, (tab) => {
  if (tab) activeTab.value = tab
})
async function loadDetail() {
  loading.value = true
  try {
    const data = await fetchCompetitionDetail(route.params.id)
    competition.value = normalizeDetail(data)
    resetForms()
    applyRoundState()
  } finally {
    loading.value = false
  }
}

async function loadJudgePool() {
  try {
    const data = await fetchJudges()
    judgePool.value = mergeJudgePool(data || [])
  } catch {
    judgePool.value = mergeJudgePool([])
  }
}

function mergeJudgePool(source) {
  const merged = [...source]
  fallbackJudgePool.forEach((judge) => {
    if (!merged.some((item) => item.publicId === judge.publicId)) merged.push(judge)
  })
  return merged
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
    editableScopes: data.editableScopes || {},
    entriesSummary: data.entriesSummary || { total: 0, pendingPayment: 0, registered: 0, stored: 0, canceled: 0, resultPublished: 0 },
    progressSummary: data.progressSummary || { finalized: 0, total: 0, advanced: 0, commentWarnings: 0 },
    resultSetup: data.resultSetup || { awardsReady: false, published: false, championReady: false },
  }
}

function resetForms() {
  selectedStyleLibraryVersion.value = competition.value.styleLibraryVersion || ''
  categoryForm.splice(0, categoryForm.length, ...competition.value.categories.map((item) => item.name))
  entryFieldForm.splice(0, entryFieldForm.length, ...competition.value.entryFields.map((item, index) => ({
    ...item,
    localId: item.id || `field-${index}`,
  })))
  judgeTableForm.splice(0, judgeTableForm.length, ...competition.value.judgeTables.map((item, index) => ({
    ...item,
    localId: item.id || `table-${index}`,
  })))
  if (!judgeTableForm.length) {
    judgeTableForm.push(
      { localId: 'base-A', tableName: 'A桌', captainCount: 1, professionalCount: 2, crossCount: 1 },
      { localId: 'base-B', tableName: 'B桌', captainCount: 1, professionalCount: 2, crossCount: 1 },
    )
  }
  const persistedAssignments = judgeTableForm.flatMap((table) => (table.assignments || []).map((assignment) => ({
    localId: `assignment-${assignment.id || `${table.localId}-${assignment.judgePublicId}`}`,
    tableLocalId: table.localId,
    tableId: table.id,
    judgePublicId: assignment.judgePublicId,
    role: assignment.role,
  })))
  judgeAssignmentForm.splice(0, judgeAssignmentForm.length, ...persistedAssignments)
  if (!judgeAssignmentForm.length) seedJudgeAssignments()
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

function applyRoundState(preferredRoundId = activeRoundId.value) {
  roundEntryPool.value = (competition.value?.entryPool || competition.value?.entries || []).map((entry) => ({
    ...entry,
    shortCode: entry.shortCode || '-',
    categoryName: entry.categoryName || entry.category || '-',
    style: entry.style || '未填写',
    stored: Boolean(entry.stored),
    advanced: Boolean(entry.advanced),
    sourceTable: entry.sourceTable || '',
    sourceResult: entry.sourceResult || '',
  }))
  rounds.value = (competition.value?.rounds || []).map((round) => ({
    ...round,
    tables: (round.tables || []).map((table) => ({
      ...table,
      entryUuids: table.entryUuids || [],
      rankings: table.rankings || buildEmptyRankings(Number(table.targetCount || 3), table.targetMode),
    })),
  }))
  const preferred = rounds.value.find((round) => round.id === preferredRoundId)
  const current = preferred || competition.value?.currentRound || rounds.value[rounds.value.length - 1] || rounds.value[0]
  activeRoundId.value = current?.id || ''
  selectedRoundTableId.value = current?.tables?.[0]?.id || ''
  selectedEntryUuids.value = []
}

async function loadStyleLibraries() {
  try {
    const data = await fetchStyleLibraries()
    styleLibraryOptions.value = normalizeStyleLibraries(data)
  } catch {
    styleLibraryOptions.value = normalizeStyleLibraries(fallbackStyleLibraries)
  }
}

function seedJudgeAssignments() {
  const sourceJudges = fallbackJudgePool
  const seeded = []
  judgeTableForm.forEach((table, tableIndex) => {
    const offset = tableIndex * 4
    const roles = [
      ['CAPTAIN', sourceJudges[offset] || sourceJudges[0]],
      ['PROFESSIONAL', sourceJudges[offset + 1] || sourceJudges[1]],
      ['PROFESSIONAL', sourceJudges[offset + 2] || sourceJudges[2]],
      ['CROSS', sourceJudges[offset + 3] || sourceJudges[3]],
    ]
    roles.forEach(([role, judge], index) => {
      seeded.push({
        localId: `assignment-${table.localId}-${role}-${index}`,
        tableLocalId: table.localId,
        judgePublicId: judge.publicId,
        role,
      })
    })
  })
  judgeAssignmentForm.splice(0, judgeAssignmentForm.length, ...seeded)
}

async function generateFirstRoundFromJudges() {
  if (validationIssues.value.length) {
    ElMessage.warning(`还有 ${validationIssues.value.length} 项评审配置需要处理`)
    return
  }
  const detail = await createFirstRound(competition.value.id, {
    allocationStrategy: 'EVEN_SPLIT',
    defaultTargetCount: 3,
  })
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState(competition.value.rounds?.find((round) => round.roundNo === 1)?.id)
  activeTab.value = 'judges'
  allocationMode.value = 'entries'
  ElMessage.success('第一轮编排已生成，可以继续调整分桌')
}

function resolveRegistrationWindowInfo() {
  const status = competition.value?.status
  const now = Date.now()
  const start = parseDateTime(competition.value?.registrationStart)
  const deadline = parseDateTime(competition.value?.registrationDeadline)
  if (status === 'DRAFT') return { label: '未发布', tone: 'neutral', detail: start ? `发布后按 ${formatDateTime(competition.value.registrationStart)} 开放` : '报名窗口尚未完整' }
  if (status === 'REGISTRATION_OPEN') {
    if (start && now < start) return { label: '待开始', tone: 'gold', detail: `将于 ${formatDateTime(competition.value.registrationStart)} 自动开放` }
    if (deadline && now > deadline) return { label: '已过截止', tone: 'warning', detail: '已到报名截止时间，建议结束报名' }
    return { label: '报名中', tone: 'success', detail: deadline ? `将于 ${formatDateTime(competition.value.registrationDeadline)} 自动截止` : '正在接受报名' }
  }
  if (status === 'REGISTRATION_CLOSED') return { label: '已截止', tone: 'warning', detail: '可进入评审准备' }
  return { label: '报名关闭', tone: 'neutral', detail: '报名窗口已结束' }
}

function parseDateTime(value) {
  if (!value) return null
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? null : time
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
    return { text: '进入评审准备', enabled: true, action: 'prepareJudging' }
  }
  if (competition.value.status === 'JUDGING_PREP' && !rounds.value.length) {
    return { text: '创建第一轮', enabled: true, action: 'createFirstRound' }
  }
  if (currentRound.value?.status === 'DRAFT') {
    return {
      text: currentRound.value.type === 'RANKING' ? '发布给桌长' : '发布当前轮次',
      enabled: canPublishCurrentRound.value,
      action: 'publishCurrentRound',
    }
  }
  if (currentRound.value?.status === 'SUBMITTED') {
    return { text: '确认排序并锁定', enabled: true, action: 'lockCurrentRound' }
  }
  if (canCreateNextRound.value) {
    return { text: `创建${nextRoundName.value}`, enabled: true, action: 'createNextRound' }
  }
  return { text: '查看现场进度', enabled: true, action: 'viewProgress' }
}

function resolveStageSecondaryActions() {
  return [
    { key: 'rounds', label: '进入轮次编排', handler: () => { activeTab.value = 'rounds' } },
    { key: 'export', label: '导出数据', handler: () => showPendingStageMessage('导出数据') },
    { key: 'delete', label: '删除比赛', danger: true, handler: () => handleDeleteCompetition() },
  ]
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
    issues.push({ key: 'payment', level: 'warning', text: `还有 ${competition.value.entriesSummary.pendingPayment} 款酒等待付款。`, targetTab: 'entries' })
  }
  return issues
}

function buildFutureStageTasks() {
  const tasks = [
    { key: 'storedEntries', label: '酒款入库', targetTab: 'entries', detail: '报名酒款到场后确认入库状态', state: 'done', statusText: '已完成' },
  ]
  rounds.value.forEach((round) => {
    tasks.push({
      key: `${round.id}-setup`,
      label: `${round.name}编排`,
      targetTab: 'rounds',
      roundId: round.id,
      detail: round.type === 'SCORE' ? '确认桌次、酒款和晋级数量' : '确认桌长、酒款和排序槽位',
      state: round.status === 'LOCKED' ? 'done' : 'pending',
      statusText: roundStatusLabels[round.status] || round.status,
    })
    tasks.push({
      key: `${round.id}-work`,
      label: round.type === 'SCORE' ? `${round.name}评审` : `${round.name}排序`,
      targetTab: round.type === 'SCORE' ? 'progress' : 'rounds',
      roundId: round.id,
      detail: round.type === 'SCORE' ? '评委评分与桌长汇总' : '桌长提交排序后再确认锁定',
      state: round.status === 'LOCKED' ? 'done' : 'pending',
      statusText: round.status === 'LOCKED' ? '已完成' : '待处理',
    })
  })
  if (canCreateNextRound.value) {
    tasks.push({
      key: 'createNextRound',
      label: `创建${nextRoundName.value}`,
      targetTab: 'rounds',
      detail: '使用晋级酒款创建空草稿桌',
      state: 'pending',
      statusText: `${advancedPool.value.length} 款`,
      action: 'createNextRound',
    })
  }
  tasks.push({
    key: 'resultSetup',
    label: '奖项确认',
    targetTab: 'results',
    detail: '确认组别奖项和发布反馈',
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
  activeTab.value = task.targetTab
}

function selectRound(roundId) {
  activeRoundId.value = roundId
  const round = rounds.value.find((item) => item.id === roundId)
  selectedRoundTableId.value = round?.tables[0]?.id || ''
  selectedEntryUuids.value = []
}

function editRoundAllocation(roundId, mode = 'entries') {
  selectRound(roundId)
  allocationMode.value = mode
  activeTab.value = 'judges'
}

function publishRoundById(roundId) {
  selectRound(roundId)
  publishCurrentRound()
}

function canSubmitRound(round) {
  if (!round || round.type !== 'RANKING') return false
  return round.tables.every((table) => getFilledRankingCount(table) === Number(table.targetCount || 0))
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
    { label: currentRound.value.type === 'SCORE' ? '晋级' : '排序目标', value: currentRoundTargetDisplay.value, done: currentRoundTargetCount.value > 0 },
  ]
  if (invalidTableCount) checks.push({ label: '桌次问题', value: `${invalidTableCount} 桌`, done: false })
  return checks
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

function getPoolEntriesForRound(round) {
  if (round?.type === 'SCORE') return roundEntryPool.value.filter((entry) => entry.stored)
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
  const stageIssue = getRoundPublishStageIssue(round)
  if (stageIssue) issues.push(stageIssue)
  if (!round.tables.length) issues.push(`${round.name}至少需要 1 张桌`)
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
    return '进入评审准备后才能发布第一轮'
  }
  if (round.type === 'RANKING' && competition.value?.status !== 'JUDGING') {
    return '评审中才能发布后续轮'
  }
  return ''
}

function getRoundTableIssues(table) {
  const issues = []
  if (!table.captainPublicId) issues.push(`${table.name}缺少桌长`)
  if (!table.entryUuids.length) issues.push(`${table.name}尚未分配酒款`)
  if (!Number(table.targetCount || 0)) issues.push(`${table.name}目标数量不能为空`)
  if (Number(table.targetCount || 0) > table.entryUuids.length) issues.push(`${table.name}目标数量超过酒款数`)
  return issues
}

function getRoundEntryAssignment(uuid) {
  const table = currentRoundTables.value.find((item) => item.entryUuids.includes(uuid))
  return table?.name || ''
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
  currentRoundTables.value.forEach((item) => {
    item.entryUuids = item.entryUuids.filter((entryUuid) => entryUuid !== uuid)
  })
  table.entryUuids.push(uuid)
  if (notify) ElMessage.success(`${uuid} 已加入 ${table.name}`)
}

function assignCurrentRoundEvenly() {
  if (!currentRound.value || !currentRoundTables.value.length) return
  const entries = currentPoolEntries.value.map((entry) => entry.uuid)
  currentRoundTables.value.forEach((table) => { table.entryUuids = [] })
  entries.forEach((uuid, index) => {
    currentRoundTables.value[index % currentRoundTables.value.length].entryUuids.push(uuid)
  })
  clearEntrySelection()
  ElMessage.success('已按桌数平均分配酒款')
}

function assignCurrentRoundByCategory() {
  if (!currentRound.value || !currentRoundTables.value.length) return
  const groups = new Map()
  currentPoolEntries.value.forEach((entry) => {
    const list = groups.get(entry.categoryName) || []
    list.push(entry.uuid)
    groups.set(entry.categoryName, list)
  })
  currentRoundTables.value.forEach((table) => { table.entryUuids = [] })
  let tableIndex = 0
  groups.forEach((uuids) => {
    uuids.forEach((uuid) => {
      currentRoundTables.value[tableIndex % currentRoundTables.value.length].entryUuids.push(uuid)
    })
    tableIndex += 1
  })
  clearEntrySelection()
  ElMessage.success('已按投递组别分配酒款')
}

function clearCurrentRoundAssignments() {
  currentRoundTables.value.forEach((table) => {
    table.entryUuids = []
    table.rankings = buildEmptyRankings(Number(table.targetCount || 3))
  })
  clearEntrySelection()
  ElMessage.success('当前轮次酒款已清空')
}

function updateRoundTableCaptain(tableId, judgePublicId) {
  const table = currentRoundTables.value.find((item) => item.id === tableId)
  if (table) table.captainPublicId = judgePublicId
}

function updateRoundTableTarget(tableId, targetCount) {
  const table = currentRoundTables.value.find((item) => item.id === tableId)
  if (!table) return
  table.targetCount = Math.max(1, Number(targetCount || 1))
  table.rankings = buildEmptyRankings(table.targetCount, table.targetMode)
}

function removeEntryFromRoundTable(tableId, uuid) {
  const table = currentRoundTables.value.find((item) => item.id === tableId)
  if (!table) return
  table.entryUuids = table.entryUuids.filter((entryUuid) => entryUuid !== uuid)
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
  selectedRoundTableId.value = tableId
  addEntryToSelectedRoundTable(draggingItem.value.uuid)
  clearDrag()
}

function removeRoundTable(tableId) {
  if (!currentRound.value || currentRound.value.status !== 'DRAFT') return
  currentRound.value.tables = currentRound.value.tables.filter((table) => table.id !== tableId)
  selectedRoundTableId.value = currentRound.value.tables[0]?.id || ''
}

async function persistCurrentRoundAllocation() {
  if (!currentRound.value || currentRound.value.status !== 'DRAFT') return
  const payload = {
    tables: currentRoundTables.value.map((table, index) => ({
      id: Number.isFinite(Number(table.id)) ? Number(table.id) : undefined,
      name: table.name,
      captainPublicId: table.captainPublicId,
      targetCount: Number(table.targetCount || 1),
      targetMode: table.targetMode || (currentRound.value.type === 'SCORE' ? 'ADVANCE_COUNT' : 'TOP_N'),
      sortOrder: index,
      entryUuids: table.entryUuids || [],
    })),
  }
  const detail = await saveRoundAllocation(competition.value.id, currentRound.value.id, payload)
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState(currentRound.value?.id)
}

async function publishCurrentRound() {
  if (!currentRound.value || roundValidationIssues.value.length) return
  const targetRoundId = currentRound.value.id
  if (currentRound.value.status === 'DRAFT') await persistCurrentRoundAllocation()
  const detail = await publishRound(competition.value.id, targetRoundId)
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState(targetRoundId)
  ElMessage.success(currentRound.value.type === 'SCORE' ? '当前轮次已发布到评审端' : '排序任务已发布给桌长')
}

async function completeFirstRoundAction() {
  if (!currentRound.value) return
  const detail = await completeFirstRound(competition.value.id, currentRound.value.id)
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState(currentRound.value?.id)
  ElMessage.success('第一轮已确认完成，晋级池已生成')
}

async function lockCurrentRound() {
  if (!currentRound.value) return
  const targetRoundId = currentRound.value.id
  const detail = await lockRound(competition.value.id, targetRoundId)
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState(targetRoundId)
  ElMessage.success('当前轮次已锁定')
}

function openCreateRoundDialog() {
  if (!canCreateNextRound.value) {
    ElMessage.warning('请先完成并锁定上一轮')
    return
  }
  createRoundDialogOpen.value = true
}

function closeCreateRoundDialog() {
  createRoundDialogOpen.value = false
}

async function finishCreateRound() {
  const lastRound = rounds.value[rounds.value.length - 1]
  if (!lastRound) return
  const detail = await createNextRound(competition.value.id, {
    sourceRoundId: lastRound.id,
    roundName: nextRoundName.value,
    strategy: 'MANUAL',
    tableCount: 1,
    targetMode: 'TOP_N',
    targetCount: 3,
    captainPublicIds: [],
  })
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState(competition.value.rounds?.[competition.value.rounds.length - 1]?.id)
  activeTab.value = 'judges'
  allocationMode.value = 'entries'
  selectedEntryUuids.value = []
  createRoundDialogOpen.value = false
  ElMessage.success(`${nextRoundName.value}已创建，请重新分配桌长和酒款`)
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
  }).filter((item) => item.uuid || item.slot === '金奖')
}

function getEntryAward(uuid) {
  return awardDrafts.value.find((award) => award.uuid === uuid)?.slot || ''
}

function getEntryPath(uuid) {
  const parts = []
  rounds.value.forEach((round) => {
    const table = round.tables.find((item) => item.entryUuids.includes(uuid))
    if (!table) return
    if (round.type === 'SCORE') {
      parts.push(`${round.name} ${table.name} ${roundEntryPool.value.find((entry) => entry.uuid === uuid)?.advanced ? '晋级' : '待定'}`)
    } else {
      const slot = getRankingSlots(table).find((item) => item.uuid === uuid)
      parts.push(`${round.name} ${table.name}${slot ? ` ${slot.label}` : ''}`)
    }
  })
  return parts.join(' -> ') || '-'
}

async function publishResultsAction() {
  const detail = await publishCompetitionResults(competition.value.id)
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState()
  ElMessage.success('结果已发布')
}

function addEntryField() {
  entryFieldForm.push({
    localId: `new-field-${Date.now()}`,
    fieldKey: `custom_${entryFieldForm.length + 1}`,
    fieldLabel: '',
    fieldType: 'text',
    helpText: '',
    options: [],
    required: false,
    visibleToJudges: true,
  })
}

function addJudgeTable() {
  const code = String.fromCharCode(65 + judgeTableForm.length)
  const table = { localId: `new-table-${Date.now()}`, tableName: `${code}桌`, captainCount: 0, professionalCount: 0, crossCount: 0 }
  judgeTableForm.push(table)
  selectedTableLocalId.value = table.localId
}

function removeItem(list, index) {
  list.splice(index, 1)
}

function removeJudgeTable(index) {
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
  if (!selectedTable.value) {
    ElMessage.warning('请先选择要加入的基础桌')
    return
  }
  if (!isJudgeActive(judge)) {
    ElMessage.warning('停用评委不能加入本场比赛')
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
  const index = judgeAssignmentForm.findIndex((item) => item.localId === assignment.localId)
  if (index >= 0) judgeAssignmentForm.splice(index, 1)
}

function countAssignedRole(role) {
  return judgeAssignmentForm.filter((assignment) => assignment.role === role).length
}

function startJudgeDrag(judge) {
  if (!editable.value.judgeTables || !isJudgeActive(judge)) return
  draggingItem.value = { type: 'judge', judgePublicId: judge.publicId }
}

function startAssignmentDrag(assignment) {
  if (!editable.value.judgeTables) return
  draggingItem.value = { type: 'assignment', localId: assignment.localId, judgePublicId: assignment.judgePublicId }
}

function clearDrag() {
  draggingItem.value = null
}

function dropOnRole(table, role) {
  if (!editable.value.judgeTables || !draggingItem.value) return
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
  if (totalCount === 0) issues.push(`${table.tableName || '未命名基础桌'}尚未分配评委`)
  return issues
}

function getJudge(judgePublicId) {
  return judgePool.value.find((judge) => judge.publicId === judgePublicId) || fallbackJudgePool.find((judge) => judge.publicId === judgePublicId)
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

async function saveJudgeDraft() {
  if (validationIssues.value.length) {
    ElMessage.warning(`还有 ${validationIssues.value.length} 项评审配置需要处理`)
    return
  }
  const assignmentDraft = judgeAssignmentForm.map((assignment) => ({
    tableName: judgeTableForm.find((table) => table.localId === assignment.tableLocalId)?.tableName,
    judgePublicId: assignment.judgePublicId,
    role: assignment.role,
  }))
  let detail = await updateCompetitionJudgeTables(competition.value.id, {
    items: judgeTableForm.filter((table) => table.tableName).map((table) => ({ tableName: table.tableName })),
  })
  const tableByName = new Map((detail.judgeTables || []).map((table) => [table.tableName, table]))
  const items = assignmentDraft.map((assignment) => ({
    tableId: tableByName.get(assignment.tableName)?.id,
    judgePublicId: assignment.judgePublicId,
    role: assignment.role,
  }))
  await updateCompetitionJudgeAssignments(competition.value.id, { items })
  detail = await fetchCompetitionDetail(competition.value.id)
  competition.value = normalizeDetail(detail)
  resetForms()
  applyRoundState()
  ElMessage.success('评审人员已保存')
}

async function saveEntryConfig() {
  const library = getStyleLibrary(selectedStyleLibraryVersion.value, styleLibraryOptions.value)
  const categoryItems = categoryForm.filter(Boolean).map((name, index) => ({ name, sortOrder: index }))
  const fieldItems = entryFieldForm
    .filter((field) => field.fieldLabel)
    .map((field, index) => ({
      fieldKey: field.fieldKey || `custom_${index + 1}`,
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
  if (!crossConfig || crossConfig.dimensions.length < 2 || crossConfig.dimensions.length > 4) {
    ElMessage.warning('跨界评审需配置 2-4 个维度')
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

function addCrossScoreDimension(config) {
  if (config.role !== 'CROSS') return
  if (config.dimensions.length >= 4) {
    ElMessage.warning('跨界评审最多配置 4 个维度')
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
  await handleStageAction(stagePrimaryAction.value.action)
}

async function handleDeleteCompetition() {
  if (!competition.value) return
  try {
    await ElMessageBox.confirm(
      `删除后，这场比赛会从管理台账中移除，已保存的数据会保留在后台。确认删除「${competition.value.name}」吗？`,
      '删除比赛',
      {
        confirmButtonText: '删除比赛',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await deleteCompetition(competition.value.id)
    ElMessage.success('比赛已删除')
    router.replace('/admin/competitions')
  } catch {
    // 用户取消或请求失败时保持当前页面不变。
  }
}

async function handleStageAction(action) {
  if (action === 'publishRegistration') {
    try {
      await ElMessageBox.confirm('发布后厂商可看到本场比赛。', '发布报名？', {
        confirmButtonText: '发布报名',
        cancelButtonText: '取消',
        type: 'warning',
      })
      const detail = await openCompetitionRegistration(competition.value.id)
      competition.value = normalizeDetail(detail)
      resetForms()
      applyRoundState()
      ElMessage.success('报名已发布')
    } catch {
      // User cancelled.
    }
    return
  }
  if (action === 'closeRegistration') {
    try {
      await ElMessageBox.confirm('截止后厂商将不能继续提交报名。', '截止报名？', {
        confirmButtonText: '截止报名',
        cancelButtonText: '取消',
        type: 'warning',
      })
      const detail = await closeCompetitionRegistration(competition.value.id)
      competition.value = normalizeDetail(detail)
      resetForms()
      applyRoundState()
      ElMessage.success('报名已截止')
    } catch {
      // User cancelled.
    }
    return
  }
  if (action === 'prepareJudging') {
    const detail = await prepareCompetitionJudging(competition.value.id)
    competition.value = normalizeDetail(detail)
    resetForms()
    applyRoundState()
    activeTab.value = 'judges'
    ElMessage.success('已进入评审准备')
    return
  }
  if (action === 'createFirstRound') {
    activeTab.value = 'judges'
    await generateFirstRoundFromJudges()
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
  if (action === 'createNextRound') {
    activeTab.value = 'rounds'
    openCreateRoundDialog()
    return
  }
  if (action === 'viewProgress') {
    activeTab.value = 'progress'
  }
}

function showPendingStageMessage(actionName) {
  ElMessage.info(`${actionName}暂未开放`)
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
select {
  font: inherit;
}

button {
  cursor: pointer;
}

button:disabled,
input:disabled,
select:disabled {
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

.title-divider {
  color: rgba(141, 161, 170, 0.55);
  font-size: 18px;
  font-weight: 700;
}

.title-edition {
  color: var(--muted);
  font-size: 17px;
  font-weight: 700;
  white-space: nowrap;
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
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.style-distribution-list span {
  display: inline-grid;
  grid-template-columns: auto auto;
  gap: 8px;
  align-items: baseline;
  padding: 7px 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.022);
}

.style-distribution-list small {
  color: var(--muted);
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
select {
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
select:focus {
  border-color: rgba(224, 184, 74, 0.5);
  background-color: rgba(10, 20, 24, 0.92);
  box-shadow: 0 0 0 3px rgba(216, 169, 53, 0.09);
}

input::placeholder {
  color: var(--faint);
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
  grid-template-columns: minmax(180px, 1.2fr) 88px 110px 160px minmax(260px, 1.5fr) 96px;
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

.award-list {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.award-list article {
  display: grid;
  gap: 4px;
}

.award-list strong {
  color: var(--gold-soft);
}

.award-list em {
  color: var(--muted);
  font-style: normal;
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

.not-found {
  display: grid;
  place-content: center;
  gap: 14px;
  min-height: 100%;
}

@media (max-width: 1400px) {
  .round-workbench {
    grid-template-columns: 280px minmax(0, 1fr) 280px;
  }

  .judge-workbench {
    grid-template-columns: 150px minmax(0, 1fr) 300px;
  }

  .future-task-list,
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
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
  .round-workbench,
  .round-console,
  .round-table-summary,
  .round-table-board,
  .score-panels,
  .progress-card-grid,
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

  .detail-tabbar {
    align-items: stretch;
  }

  .tab-save-actions {
    justify-content: flex-start;
    padding-left: 4px;
  }
}
</style>
