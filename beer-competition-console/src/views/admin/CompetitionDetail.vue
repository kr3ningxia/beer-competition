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
              <button v-for="action in stageSecondaryActions" :key="action.key" type="button" @click="action.handler">
                {{ action.label }}
              </button>
            </div>
          </details>
        </div>
      </div>
    </section>

    <section v-if="competition" class="detail-shell">
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
              <p>用于创建第二轮或决赛轮</p>
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
                <span>{{ categoryForm.length }} 个组别</span>
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
            <h2>基础风格库</h2>
            <div class="library-summary">
              <strong>{{ styleLibraryLabel }}</strong>
              <div>
                <span>报名必填</span>
                <span>支持搜索</span>
                <span>评审可见</span>
              </div>
            </div>
          </article>

          <article class="panel-card field-config-card">
            <div class="panel-heading">
              <h2>报名补充信息</h2>
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
            <footer v-if="editable.entryStructure" class="panel-actions">
              <button class="tool-button primary" type="button" @click="saveEntryConfig">
                <Check />
                保存报名配置
              </button>
            </footer>
          </article>
        </section>

        <section v-if="activeTab === 'entries'" class="tab-panel">
          <div class="overview-grid">
            <article class="metric-card"><small>全部</small><strong>{{ competition.entriesSummary.total }}</strong><p>已报名酒款</p></article>
            <article class="metric-card"><small>待付款</small><strong>{{ competition.entriesSummary.pendingPayment }}</strong><p>需跟进支付</p></article>
            <article class="metric-card"><small>已入库</small><strong>{{ competition.entriesSummary.stored }}</strong><p>现场已确认</p></article>
            <article class="metric-card"><small>晋级轨迹</small><strong>{{ roundEntryPool.length }}</strong><p>含 mock 轮次分配</p></article>
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
          <div class="edit-banner">
            <Files />
            基础桌次用于生成第一轮；第二轮及以后可从晋级池重新合并、减少或重组桌次。
          </div>
          <section class="judge-workbench">
            <aside class="judge-nav-panel">
              <div class="panel-heading compact">
                <h2>基础桌次</h2>
                <span>{{ judgeTableForm.length }} 桌</span>
              </div>
              <button
                v-for="table in judgeTableForm"
                :key="table.localId"
                :class="['table-nav-item', { active: selectedTableLocalId === table.localId, danger: tableValidationIssues(table).length }]"
                type="button"
                @click="selectedTableLocalId = table.localId"
              >
                <strong>{{ table.tableName || '未命名评审桌' }}</strong>
                <span>{{ assignmentsForTable(table).length }} 人</span>
              </button>
              <button v-if="editable.judgeTables" class="table-nav-add" type="button" @click="addJudgeTable">
                <Plus />
                新增基础桌
              </button>

              <div class="left-check-summary">
                <div>
                  <strong>检查</strong>
                  <span :class="{ success: validationIssues.length === 0, danger: validationIssues.length > 0 }">
                    {{ validationIssues.length === 0 ? '通过' : `${validationIssues.length} 项` }}
                  </span>
                </div>
                <p v-for="issue in validationIssues.slice(0, 3)" :key="issue">
                  <Warning />
                  {{ issue }}
                </p>
                <p v-if="validationIssues.length === 0">
                  <CircleCheck />
                  可生成第一轮桌任务
                </p>
              </div>
            </aside>

            <section class="assignment-board">
              <div class="judge-command-bar">
                <div class="metric-strip">
                  <span>已分配 <strong>{{ judgeMetrics.assigned }}</strong></span>
                  <span>桌长 <strong>{{ judgeMetrics.captain }} / {{ judgeTableForm.length }}</strong></span>
                  <span>专业 <strong>{{ judgeMetrics.professional }}</strong></span>
                  <span>跨界 <strong>{{ judgeMetrics.cross }}</strong></span>
                </div>
                <button v-if="editable.judgeTables" class="tool-button" type="button" @click="saveJudgeDraft">
                  <Check />
                  保存
                </button>
              </div>

              <article v-for="(table, index) in judgeTableForm" :key="table.localId" class="judge-table-card">
                <header class="judge-table-head">
                  <div>
                    <label v-if="editable.judgeTables">
                      <span>桌号</span>
                      <input v-model.trim="table.tableName" placeholder="例如 A桌" />
                    </label>
                    <h2 v-else>{{ table.tableName }}</h2>
                    <p>{{ tableValidationIssues(table).length ? tableValidationIssues(table)[0] : '本桌可生成第一轮任务' }}</p>
                  </div>
                  <button v-if="editable.judgeTables" class="icon-button" type="button" @click="removeJudgeTable(index)">
                    <Delete />
                  </button>
                </header>

                <div class="role-lanes">
                  <section
                    v-for="role in roleOptions"
                    :key="role.value"
                    :class="['role-lane', role.value.toLowerCase(), { active: selectedTableLocalId === table.localId && selectedRole === role.value }]"
                    @click="selectAssignmentTarget(table, role.value)"
                    @dragover.prevent
                    @drop.prevent="dropOnRole(table, role.value)"
                  >
                    <header>
                      <div>
                        <strong>{{ role.label }}</strong>
                        <span>{{ assignmentsForTable(table, role.value).length }} 人</span>
                      </div>
                    </header>

                    <div class="assignment-card-list">
                      <article
                        v-for="assignment in assignmentsForTable(table, role.value)"
                        :key="assignment.localId"
                        class="assignment-card"
                        :draggable="editable.judgeTables"
                        @dragstart="startAssignmentDrag(assignment)"
                        @dragend="clearDrag"
                      >
                        <span class="avatar small">{{ getJudgeInitial(getJudge(assignment.judgePublicId)?.name) }}</span>
                        <div class="assignment-main">
                          <strong>{{ getJudge(assignment.judgePublicId)?.name || '未知评委' }}</strong>
                          <small>{{ getJudge(assignment.judgePublicId)?.qualification || '未填写资质' }}</small>
                        </div>
                        <button v-if="editable.judgeTables" class="icon-button ghost" type="button" @click.stop="removeAssignment(assignment)">
                          <Delete />
                        </button>
                      </article>
                      <button
                        v-if="editable.judgeTables && assignmentsForTable(table, role.value).length === 0"
                        class="empty-assignment"
                        type="button"
                        @click.stop="selectAssignmentTarget(table, role.value)"
                      >
                        <Plus />
                        添加
                      </button>
                    </div>
                  </section>
                </div>
              </article>
            </section>

            <aside class="judge-right-panel">
              <section class="panel-card tight">
                <div class="panel-heading compact">
                  <h2>评委池</h2>
                  <span>{{ filteredJudgePool.length }} 人</span>
                </div>
                <label class="inline-search">
                  <Search />
                  <input v-model.trim="judgeKeyword" placeholder="搜索姓名、手机号、资质" />
                </label>
                <div class="pool-filters">
                  <button
                    v-for="filter in roleFilters"
                    :key="filter.value"
                    :class="{ active: judgeRoleFilter === filter.value }"
                    type="button"
                    @click="judgeRoleFilter = filter.value"
                  >
                    {{ filter.label }}
                  </button>
                </div>
                <div class="judge-pool-list">
                  <article
                    v-for="judge in filteredJudgePool"
                    :key="judge.publicId"
                    :class="['pool-card', { assigned: isAssigned(judge.publicId), inactive: !isJudgeActive(judge) }]"
                    :draggable="editable.judgeTables && isJudgeActive(judge)"
                    @dragstart="startJudgeDrag(judge)"
                    @dragend="clearDrag"
                  >
                    <span class="avatar">{{ getJudgeInitial(judge.name) }}</span>
                    <div>
                      <strong>{{ judge.name || '未填写姓名' }}</strong>
                      <small>{{ judge.qualification || '未填写资质' }}</small>
                      <em>{{ isAssigned(judge.publicId) ? '已分配' : '未分配' }}</em>
                    </div>
                    <button
                      v-if="editable.judgeTables"
                      class="link-action"
                      type="button"
                      :disabled="!isJudgeActive(judge)"
                      @click="addJudgeToTarget(judge)"
                    >
                      加入
                    </button>
                  </article>
                </div>
              </section>
            </aside>
          </section>
        </section>

        <section v-if="activeTab === 'rounds'" class="tab-panel rounds-panel">
          <div class="round-flow">
            <button
              v-for="round in rounds"
              :key="round.id"
              :class="['round-flow-item', { active: round.id === activeRoundId }]"
              type="button"
              @click="selectRound(round.id)"
            >
              <small>{{ round.type === 'SCORE' ? '评分制' : '选择排序' }}</small>
              <strong>{{ round.name }}</strong>
              <em>{{ roundStatusLabels[round.status] || round.status }}</em>
            </button>
            <button class="round-flow-item create" type="button" @click="openCreateRoundDialog">
              <Plus />
              <strong>创建下一轮</strong>
              <em>从晋级池生成</em>
            </button>
          </div>

          <div class="round-summary-strip">
            <span>评审方式 <strong>{{ currentRoundTypeLabel }}</strong></span>
            <span>桌数 <strong>{{ currentRoundTables.length }}</strong></span>
            <span>酒款 <strong>{{ currentRoundEntryCount }}</strong></span>
            <span>目标 <strong>{{ currentRoundTargetCount }}</strong></span>
            <span>状态 <strong>{{ currentRoundStatusText }}</strong></span>
          </div>

          <section class="round-workbench">
            <aside class="round-pool-panel">
              <div class="panel-heading compact">
                <h2>{{ currentRound?.type === 'SCORE' ? '已入库酒款池' : '晋级酒款池' }}</h2>
                <span>{{ filteredRoundPool.length }} 款</span>
              </div>
              <label class="inline-search">
                <Search />
                <input v-model.trim="roundKeyword" placeholder="搜索匿名编号、短编号、风格" />
              </label>
              <div class="pool-filters">
                <button
                  v-for="category in roundCategoryFilters"
                  :key="category"
                  :class="{ active: roundCategoryFilter === category }"
                  type="button"
                  @click="roundCategoryFilter = category"
                >
                  {{ category }}
                </button>
              </div>
              <div class="round-entry-list">
                <article
                  v-for="entry in filteredRoundPool"
                  :key="entry.uuid"
                  :class="['round-entry-card', { assigned: getRoundEntryAssignment(entry.uuid) }]"
                  draggable="true"
                  @dragstart="startEntryDrag(entry.uuid)"
                  @dragend="clearDrag"
                >
                  <div>
                    <strong>{{ entry.uuid }}</strong>
                    <small>{{ entry.shortCode }} · {{ entry.categoryName }} · {{ entry.style }}</small>
                    <em v-if="entry.sourceTable">{{ entry.sourceTable }} · {{ entry.sourceResult }}</em>
                  </div>
                  <button class="link-action" type="button" @click="addEntryToSelectedRoundTable(entry.uuid)">
                    {{ getRoundEntryAssignment(entry.uuid) ? '移动' : '加入' }}
                  </button>
                </article>
              </div>
            </aside>

            <section class="round-table-board">
              <article
                v-for="table in currentRoundTables"
                :key="table.id"
                :class="['round-table-card', { active: selectedRoundTableId === table.id, danger: getRoundTableIssues(table).length }]"
                @click="selectedRoundTableId = table.id"
                @dragover.prevent
                @drop.prevent="dropEntryOnRoundTable(table.id)"
              >
                <header>
                  <div>
                    <span class="mini-label">{{ currentRoundTypeLabel }}</span>
                    <h3>{{ table.name }}</h3>
                  </div>
                  <em :class="['round-status', table.status]">{{ roundStatusLabels[table.status] || table.status }}</em>
                </header>
                <div class="round-table-meta">
                  <span>桌长 <strong>{{ getJudge(table.captainPublicId)?.name || '未指定' }}</strong></span>
                  <span>酒款 <strong>{{ table.entryUuids.length }}</strong></span>
                  <span>{{ currentRound?.type === 'SCORE' ? '晋级' : '排序' }} <strong>{{ table.targetCount }}</strong></span>
                </div>
                <p v-if="currentRound?.type !== 'SCORE'" class="round-note">本轮仅桌长选择排序，普通评委无需系统操作。</p>
                <div v-if="currentRound?.type === 'SCORE'" class="round-members">
                  <span>普通评委</span>
                  <strong>{{ table.professionalCount || 0 }} 专业 · {{ table.crossCount || 0 }} 跨界</strong>
                </div>
                <div class="round-table-entries">
                  <article v-for="uuid in table.entryUuids" :key="uuid">
                    <span>{{ uuid }}</span>
                    <button type="button" @click.stop="removeEntryFromRoundTable(table.id, uuid)">移除</button>
                  </article>
                  <p v-if="!table.entryUuids.length" class="empty-line">从左侧酒款池加入本桌。</p>
                </div>
                <p v-for="issue in getRoundTableIssues(table)" :key="issue" class="table-warning">
                  <Warning />
                  {{ issue }}
                </p>
              </article>
            </section>

            <aside class="round-check-panel">
              <div class="panel-card tight">
                <div class="panel-heading compact">
                  <h2>配置与校验</h2>
                  <span>{{ roundValidationIssues.length }} 项</span>
                </div>
                <template v-if="selectedRoundTable">
                  <label class="stack-field">
                    <span>当前桌</span>
                    <strong>{{ selectedRoundTable.name }}</strong>
                  </label>
                  <label class="stack-field">
                    <span>桌长</span>
                    <select v-model="selectedRoundTable.captainPublicId">
                      <option value="">未指定</option>
                      <option v-for="judge in captainCandidates" :key="judge.publicId" :value="judge.publicId">
                        {{ judge.name }} · {{ judge.qualification }}
                      </option>
                    </select>
                  </label>
                  <label class="stack-field">
                    <span>{{ currentRound?.type === 'SCORE' ? '晋级数量' : '排序目标' }}</span>
                    <input v-model.number="selectedRoundTable.targetCount" min="1" type="number" />
                  </label>
                  <button
                    v-if="currentRound?.status === 'DRAFT'"
                    class="tool-button"
                    type="button"
                    @click="removeRoundTable(selectedRoundTable.id)"
                  >
                    <Delete />
                    删除草稿桌
                  </button>
                </template>
                <div class="check-list">
                  <div v-for="issue in roundValidationIssues" :key="issue" class="check-item pending">
                    <Warning />
                    <span>{{ issue }}</span>
                  </div>
                  <div v-if="roundValidationIssues.length === 0" class="check-item done">
                    <CircleCheck />
                    <span>当前轮次可发布或锁定。</span>
                  </div>
                </div>
                <button class="tool-button primary full-width" type="button" :disabled="!canPublishCurrentRound" @click="publishCurrentRound">
                  <Check />
                  {{ currentRound?.status === 'SUBMITTED' ? '确认排序并锁定' : '发布当前轮次' }}
                </button>
              </div>
            </aside>
          </section>
        </section>

        <section v-if="activeTab === 'score'" class="tab-panel score-config-panel">
          <div class="edit-banner">
            <Finished />
            评分表仅用于第一轮；第二轮及以后由桌长选择排序，不需要打分和备注。
          </div>
          <div class="score-panels">
            <article v-for="config in scoreConfigForm" :key="config.role" class="panel-card score-config-card">
              <div class="panel-heading">
                <div>
                  <h2>{{ roleLabels[config.role] }}</h2>
                  <small>{{ scoreConfigHint(config) }}</small>
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
          <footer v-if="editable.scoreConfigs" class="panel-actions">
            <button class="tool-button primary" type="button" @click="saveScoreConfigs">
              <Check />
              保存评分表
            </button>
          </footer>
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
                <button class="tool-button primary" type="button" :disabled="!resultChecks.every((item) => item.done)" @click="publishMockResults">
                  发布结果
                </button>
              </div>
            </article>
          </div>
          <article class="panel-card">
            <div class="panel-heading">
              <h2>晋级路径摘要</h2>
              <span>第一轮桌 -> 第二轮桌 -> 决赛桌 -> 奖项</span>
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

    <div v-if="createRoundDialogOpen" class="modal-backdrop" @click.self="closeCreateRoundDialog">
      <section class="round-dialog" role="dialog" aria-modal="true" aria-labelledby="create-round-title">
        <header>
          <div>
            <h2 id="create-round-title">从晋级池创建下一轮</h2>
            <p>当前是前端原型，完成后只更新页面 mock 状态。</p>
          </div>
          <button class="icon-button" type="button" @click="closeCreateRoundDialog">×</button>
        </header>

        <div class="wizard-steps">
          <button v-for="step in 6" :key="step" :class="{ active: createRoundStep === step }" type="button" @click="createRoundStep = step">
            {{ step }}
          </button>
        </div>

        <section v-if="createRoundStep === 1" class="wizard-panel">
          <h3>选择候选来源</h3>
          <p>默认使用上一轮晋级酒款池，共 {{ advancedPool.length }} 款。</p>
          <div class="wizard-stat-grid">
            <span v-for="item in advancedCategoryStats" :key="item.category">
              <strong>{{ item.count }}</strong>
              {{ item.category }}
            </span>
          </div>
        </section>

        <section v-if="createRoundStep === 2" class="wizard-panel">
          <h3>选择桌次策略</h3>
          <div class="strategy-grid">
            <button
              v-for="strategy in roundStrategies"
              :key="strategy.value"
              :class="{ active: createRoundForm.strategy === strategy.value }"
              type="button"
              @click="createRoundForm.strategy = strategy.value"
            >
              <strong>{{ strategy.label }}</strong>
              <small>{{ strategy.detail }}</small>
            </button>
          </div>
          <label v-if="createRoundForm.strategy === 'EVEN'" class="stack-field">
            <span>平均分成几桌</span>
            <input v-model.number="createRoundForm.tableCount" min="1" max="4" type="number" />
          </label>
        </section>

        <section v-if="createRoundStep === 3" class="wizard-panel">
          <h3>指定桌长</h3>
          <p>每张后续轮桌必须指定 1 名桌长。</p>
          <div class="wizard-table-list">
            <label v-for="table in previewNextRoundTables" :key="table.id">
              <span>{{ table.name }}</span>
              <select v-model="table.captainPublicId">
                <option value="">未指定</option>
                <option v-for="judge in captainCandidates" :key="judge.publicId" :value="judge.publicId">
                  {{ judge.name }} · {{ judge.qualification }}
                </option>
              </select>
            </label>
          </div>
        </section>

        <section v-if="createRoundStep === 4" class="wizard-panel">
          <h3>分配酒款</h3>
          <p>按当前策略自动分配，发布前仍可在轮次编排中微调。</p>
          <div class="wizard-preview-tables">
            <article v-for="table in previewNextRoundTables" :key="`preview-${table.id}`">
              <strong>{{ table.name }}</strong>
              <span>{{ table.entryUuids.length }} 款</span>
              <small>{{ table.entryUuids.join('、') }}</small>
            </article>
          </div>
        </section>

        <section v-if="createRoundStep === 5" class="wizard-panel">
          <h3>设置排序目标</h3>
          <label class="stack-field">
            <span>目标名称</span>
            <select v-model="createRoundForm.targetMode">
              <option value="TOP3">选前 3</option>
              <option value="MEDALS">金银铜</option>
              <option value="CHAMPION">总冠军候选</option>
            </select>
          </label>
          <label class="stack-field">
            <span>每桌目标数量</span>
            <input v-model.number="createRoundForm.targetCount" min="1" type="number" />
          </label>
        </section>

        <section v-if="createRoundStep === 6" class="wizard-panel">
          <h3>发布确认</h3>
          <div class="check-list">
            <div v-for="issue in createRoundPreviewIssues" :key="issue" class="check-item pending">
              <Warning />
              <span>{{ issue }}</span>
            </div>
            <div v-if="createRoundPreviewIssues.length === 0" class="check-item done">
              <CircleCheck />
              <span>下一轮可创建。发布后只给桌长端生成任务。</span>
            </div>
          </div>
        </section>

        <footer>
          <button class="tool-button" type="button" :disabled="createRoundStep === 1" @click="createRoundStep -= 1">上一步</button>
          <button v-if="createRoundStep < 6" class="tool-button primary" type="button" @click="createRoundStep += 1">下一步</button>
          <button v-else class="tool-button primary" type="button" :disabled="createRoundPreviewIssues.length > 0" @click="finishCreateRound">
            创建下一轮
          </button>
        </footer>
      </section>
    </div>
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
  fetchCompetitionDetail,
  fetchJudges,
  openCompetitionRegistration,
  updateCompetitionCategories,
  updateCompetitionEntryFields,
  updateCompetitionJudgeAssignments,
  updateCompetitionJudgeTables,
  updateScoreConfigs,
} from '@/api/admin'

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
const selectedTableLocalId = ref(null)
const selectedRole = ref('CAPTAIN')
const draggingItem = ref(null)

const rounds = ref([])
const roundEntryPool = ref([])
const activeRoundId = ref('round-2')
const selectedRoundTableId = ref('')
const roundKeyword = ref('')
const roundCategoryFilter = ref('全部')
const createRoundDialogOpen = ref(false)
const createRoundStep = ref(1)
const previewNextRoundTables = ref([])
const createRoundForm = reactive({
  strategy: 'EVEN',
  tableCount: 2,
  targetMode: 'TOP3',
  targetCount: 3,
})

const tabs = [
  { key: 'overview', label: '概览', icon: DataAnalysis },
  { key: 'entryConfig', label: '报名配置', icon: Setting },
  { key: 'entries', label: '参赛酒款', icon: Tickets },
  { key: 'judges', label: '评审人员', icon: Files },
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

const styleLibraryLabels = {
  BJCP_2021_CN: 'BJCP 2021 中文标准库',
  CUSTOM_STANDARD: '主办方标准风格库',
}

const roundStatusLabels = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  IN_PROGRESS: '处理中',
  SUBMITTED: '已提交',
  LOCKED: '已锁定',
}

const roundStrategies = [
  { value: 'MERGE', label: '合并为 1 桌', detail: '晋级酒款较少，集中交给 1 位桌长排序。' },
  { value: 'CATEGORY', label: '按组别建桌', detail: '每个投递组别生成独立后续轮桌。' },
  { value: 'EVEN', label: '平均分成 N 桌', detail: '保留多桌处理能力，酒款均匀拆分。' },
  { value: 'CARRY', label: '沿用上一轮', detail: '保留上一轮桌长结构，减少现场变动。' },
  { value: 'MANUAL', label: '手动编排', detail: '先生成空桌，再由主办方手动拖入酒款。' },
]

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

const fallbackEntries = [
  { uuid: 'BC-2026-IPA-0001', shortCode: 'A19K', categoryName: 'IPA', style: 'Double IPA', status: 'STORED', stored: true },
  { uuid: 'BC-2026-IPA-0002', shortCode: 'F72C', categoryName: 'IPA', style: 'American IPA', status: 'STORED', stored: true },
  { uuid: 'BC-2026-IPA-0003', shortCode: 'H48Q', categoryName: 'IPA', style: 'Hazy IPA', status: 'STORED', stored: true },
  { uuid: 'BC-2026-LAG-0042', shortCode: 'L08P', categoryName: '拉格', style: 'Pilsner', status: 'STORED', stored: true },
  { uuid: 'BC-2026-LAG-0045', shortCode: 'N11R', categoryName: '拉格', style: 'Helles', status: 'STORED', stored: true },
  { uuid: 'BC-2026-STO-0108', shortCode: 'S63T', categoryName: '世涛', style: 'Imperial Stout', status: 'STORED', stored: true },
  { uuid: 'BC-2026-STO-0112', shortCode: 'D36M', categoryName: '世涛', style: 'Barrel Aged Stout', status: 'STORED', stored: true },
  { uuid: 'BC-2026-SOU-0149', shortCode: 'Q90A', categoryName: '酸啤', style: 'Mixed Fermentation Sour', status: 'STORED', stored: true },
  { uuid: 'BC-2026-SOU-0150', shortCode: 'R12U', categoryName: '酸啤', style: 'Gose', status: 'STORED', stored: true },
  { uuid: 'BC-2026-EXP-0202', shortCode: 'X56B', categoryName: '特色实验', style: 'Experimental Beer', status: 'STORED', stored: true },
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
const styleLibraryLabel = computed(() => styleLibraryLabels[competition.value?.styleLibraryVersion] || competition.value?.styleLibraryVersion || '未选择')
const registrationWindowInfo = computed(() => resolveRegistrationWindowInfo())
const stagePrimaryAction = computed(() => resolveStagePrimaryAction())
const stageSecondaryActions = computed(() => resolveStageSecondaryActions())
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
const currentPoolEntries = computed(() => {
  if (currentRound.value?.type === 'SCORE') return roundEntryPool.value.filter((entry) => entry.stored)
  return advancedPool.value
})
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
const roundValidationIssues = computed(() => buildRoundValidationIssues(currentRound.value))
const canPublishCurrentRound = computed(() => currentRound.value && ['DRAFT', 'SUBMITTED'].includes(currentRound.value.status) && roundValidationIssues.value.length === 0)
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
const createRoundPreviewIssues = computed(() => {
  const issues = []
  if (!advancedPool.value.length) issues.push('上一轮没有晋级酒款，无法创建下一轮')
  previewNextRoundTables.value.forEach((table) => {
    if (!table.captainPublicId) issues.push(`${table.name}缺少桌长`)
    if (table.entryUuids.length && Number(createRoundForm.targetCount) > table.entryUuids.length) {
      issues.push(`${table.name}排序目标超过酒款数量`)
    }
  })
  return issues
})
const roundRestructureText = computed(() => rounds.value.map((round) => `${round.name} ${round.tables.length}桌`).join(' -> '))
const awardDrafts = computed(() => buildAwardDrafts())
const resultPathEntries = computed(() => roundEntryPool.value.filter((entry) => entry.advanced || getEntryAward(entry.uuid)).slice(0, 8))
const resultChecks = computed(() => [
  { label: '第一轮桌长汇总已完成', done: rounds.value.find((round) => round.id === 'round-1')?.status === 'LOCKED' },
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
  loadDetail()
  loadJudgePool()
})
watch(() => route.params.id, loadDetail)
watch(() => route.query.tab, (tab) => {
  if (tab) activeTab.value = tab
})
watch(() => createRoundForm.strategy, refreshPreviewNextRoundTables)
watch(() => createRoundForm.tableCount, refreshPreviewNextRoundTables)
watch(() => createRoundForm.targetCount, () => {
  previewNextRoundTables.value.forEach((table) => {
    table.targetCount = Number(createRoundForm.targetCount || 1)
  })
})

async function loadDetail() {
  loading.value = true
  try {
    const data = await fetchCompetitionDetail(route.params.id)
    competition.value = normalizeDetail(data)
    resetForms()
    seedRoundPrototype()
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
    editableScopes: data.editableScopes || {},
    entriesSummary: data.entriesSummary || { total: 0, pendingPayment: 0, registered: 0, stored: 0, canceled: 0, resultPublished: 0 },
    progressSummary: data.progressSummary || { finalized: 0, total: 0, advanced: 0, commentWarnings: 0 },
    resultSetup: data.resultSetup || { awardsReady: false, published: false, championReady: false },
  }
}

function resetForms() {
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

function seedRoundPrototype() {
  const apiEntries = competition.value.entries?.length >= 6 ? competition.value.entries : fallbackEntries
  const sourceEntries = apiEntries.map((entry, index) => ({
    ...entry,
    uuid: entry.uuid,
    shortCode: entry.shortCode || `S${String(index + 17).padStart(2, '0')}`,
    categoryName: entry.categoryName || entry.category || 'IPA',
    style: entry.style || '未填写',
    status: entry.status || 'STORED',
    stored: entry.stored ?? true,
    advanced: index < 6,
    sourceTable: index < 3 ? 'A桌' : index < 6 ? 'B桌' : '',
    sourceResult: index < 6 ? `晋级 ${Math.min(index % 3 + 1, 3)}` : '',
  }))
  roundEntryPool.value = sourceEntries
  const uuids = sourceEntries.map((entry) => entry.uuid)
  rounds.value = [
    {
      id: 'round-1',
      name: '第一轮',
      type: 'SCORE',
      status: 'LOCKED',
      tables: [
        {
          id: 'r1-a',
          name: 'A桌',
          captainPublicId: 'J-DEMO-001',
          professionalCount: 2,
          crossCount: 1,
          targetCount: 3,
          status: 'LOCKED',
          entryUuids: uuids.slice(0, 5),
          advancedCount: 3,
          judgeProgress: 100,
          captainProgress: 100,
        },
        {
          id: 'r1-b',
          name: 'B桌',
          captainPublicId: 'J-DEMO-008',
          professionalCount: 2,
          crossCount: 1,
          targetCount: 3,
          status: 'LOCKED',
          entryUuids: uuids.slice(5, 10),
          advancedCount: 3,
          judgeProgress: 100,
          captainProgress: 100,
        },
      ],
    },
    {
      id: 'round-2',
      name: '第二轮',
      type: 'RANKING',
      status: 'DRAFT',
      tables: [
        {
          id: 'r2-a',
          name: '2A桌',
          captainPublicId: 'J-DEMO-001',
          targetCount: 3,
          status: 'DRAFT',
          entryUuids: uuids.slice(0, 3),
          rankings: [
            { rank: 1, label: '第 1 名', uuid: uuids[0] },
            { rank: 2, label: '第 2 名', uuid: uuids[1] },
            { rank: 3, label: '第 3 名', uuid: '' },
          ],
        },
        {
          id: 'r2-b',
          name: '2B桌',
          captainPublicId: '',
          targetCount: 3,
          status: 'DRAFT',
          entryUuids: uuids.slice(3, 6),
          rankings: [],
        },
      ],
    },
    {
      id: 'round-final',
      name: '决赛轮',
      type: 'RANKING',
      status: 'DRAFT',
      tables: [
        {
          id: 'rf-a',
          name: '决赛桌',
          captainPublicId: 'J-DEMO-008',
          targetCount: 1,
          status: 'DRAFT',
          entryUuids: uuids.slice(0, 3),
          rankings: [{ rank: 1, label: '总冠军', uuid: '' }],
        },
      ],
    },
  ]
  selectedRoundTableId.value = rounds.value.find((round) => round.id === activeRoundId.value)?.tables[0]?.id || ''
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

function resolveStagePrimaryAction() {
  if (!competition.value) return { text: '加载中', enabled: false, action: 'noop' }
  if (currentRound.value?.status === 'DRAFT') {
    return {
      text: currentRound.value.id === 'round-2' ? '发布给桌长' : '发布当前轮次',
      enabled: canPublishCurrentRound.value,
      action: 'publishCurrentRound',
    }
  }
  if (currentRound.value?.status === 'SUBMITTED') {
    return { text: '确认排序并锁定', enabled: canPublishCurrentRound.value, action: 'publishCurrentRound' }
  }
  if (currentRound.value?.id === 'round-1' && currentRound.value?.status === 'LOCKED') {
    return { text: '从晋级池创建第二轮', enabled: true, action: 'createNextRound' }
  }
  if (competition.value.status === 'DRAFT') {
    return { text: '发布报名', enabled: true, action: 'publishRegistration' }
  }
  return { text: '查看现场进度', enabled: true, action: 'viewProgress' }
}

function resolveStageSecondaryActions() {
  return [
    { key: 'rounds', label: '进入轮次编排', handler: () => { activeTab.value = 'rounds' } },
    { key: 'export', label: '导出数据', handler: () => showPendingStageMessage('导出数据') },
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
  const firstRound = rounds.value.find((round) => round.id === 'round-1')
  const secondRound = rounds.value.find((round) => round.id === 'round-2')
  const finalRound = rounds.value.find((round) => round.id === 'round-final')
  return [
    { key: 'storedEntries', label: '酒款入库', targetTab: 'entries', detail: '报名酒款到场后确认入库状态', state: 'done', statusText: '已完成' },
    { key: 'round1Setup', label: '第一轮编排', targetTab: 'rounds', roundId: 'round-1', detail: '评分制桌次、酒款和晋级数量', state: firstRound?.status === 'LOCKED' ? 'done' : 'pending', statusText: firstRound?.status === 'LOCKED' ? '已锁定' : '待发布' },
    { key: 'round1Review', label: '第一轮评审', targetTab: 'progress', roundId: 'round-1', detail: '普通评委评分与桌长汇总', state: firstRound?.status === 'LOCKED' ? 'done' : 'pending', statusText: firstRound?.status === 'LOCKED' ? '已完成' : '进行中' },
    { key: 'round2Create', label: '第二轮创建', targetTab: 'rounds', roundId: 'round-2', detail: '从晋级池生成 2A / 2B', state: secondRound ? 'done' : 'pending', statusText: secondRound ? '已创建' : '待创建' },
    { key: 'round2Sort', label: '第二轮排序', targetTab: 'rounds', roundId: 'round-2', detail: '仅桌长选择并排序', state: secondRound?.status === 'LOCKED' ? 'done' : 'pending', statusText: roundStatusLabels[secondRound?.status] || '待处理' },
    { key: 'finalRound', label: '决赛轮', targetTab: 'rounds', roundId: 'round-final', detail: '合并为 1 桌确认总冠军', state: finalRound?.status === 'LOCKED' ? 'done' : 'pending', statusText: roundStatusLabels[finalRound?.status] || '待创建' },
    { key: 'resultSetup', label: '奖项确认', targetTab: 'results', detail: '确认组别奖项和发布反馈', state: resultChecks.value.every((item) => item.done) ? 'done' : 'pending', statusText: resultChecks.value.every((item) => item.done) ? '可发布' : '待确认' },
  ]
}

function handleFutureTask(task) {
  if (task.roundId) selectRound(task.roundId)
  activeTab.value = task.targetTab
}

function selectRound(roundId) {
  activeRoundId.value = roundId
  const round = rounds.value.find((item) => item.id === roundId)
  selectedRoundTableId.value = round?.tables[0]?.id || ''
}

function countRoundEntries(round) {
  return new Set((round?.tables || []).flatMap((table) => table.entryUuids)).size
}

function buildRoundValidationIssues(round) {
  if (!round) return ['请先创建轮次']
  const issues = []
  if (!round.tables.length) issues.push(`${round.name}至少需要 1 张桌`)
  const assigned = round.tables.flatMap((table) => table.entryUuids)
  if (!assigned.length) issues.push(`${round.name}尚未分配酒款`)
  const duplicates = assigned.filter((uuid, index, list) => list.indexOf(uuid) !== index)
  if (duplicates.length) issues.push(`${round.name}存在重复分配酒款`)
  round.tables.forEach((table) => issues.push(...getRoundTableIssues(table)))
  return [...new Set(issues)]
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

function addEntryToSelectedRoundTable(uuid) {
  const table = selectedRoundTable.value
  if (!table) {
    ElMessage.warning('请先选择要加入的轮次桌')
    return
  }
  currentRoundTables.value.forEach((item) => {
    item.entryUuids = item.entryUuids.filter((entryUuid) => entryUuid !== uuid)
  })
  table.entryUuids.push(uuid)
  ElMessage.success(`${uuid} 已加入 ${table.name}`)
}

function removeEntryFromRoundTable(tableId, uuid) {
  const table = currentRoundTables.value.find((item) => item.id === tableId)
  if (!table) return
  table.entryUuids = table.entryUuids.filter((entryUuid) => entryUuid !== uuid)
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

function publishCurrentRound() {
  if (!currentRound.value || roundValidationIssues.value.length) return
  if (currentRound.value.status === 'SUBMITTED') {
    currentRound.value.status = 'LOCKED'
    currentRound.value.tables.forEach((table) => { table.status = 'LOCKED' })
    ElMessage.success(`${currentRound.value.name}已确认并锁定`)
    return
  }
  currentRound.value.status = currentRound.value.type === 'SCORE' ? 'PUBLISHED' : 'IN_PROGRESS'
  currentRound.value.tables.forEach((table) => { table.status = currentRound.value.status })
  ElMessage.success(currentRound.value.type === 'SCORE' ? '当前轮次已发布到评审端' : '排序任务已发布给桌长')
}

function openCreateRoundDialog() {
  createRoundDialogOpen.value = true
  createRoundStep.value = 1
  refreshPreviewNextRoundTables()
}

function closeCreateRoundDialog() {
  createRoundDialogOpen.value = false
}

function refreshPreviewNextRoundTables() {
  const source = advancedPool.value
  let tables = []
  if (createRoundForm.strategy === 'MERGE') {
    tables = [{ id: 'preview-1', name: '2A桌', captainPublicId: captainCandidates.value[0]?.publicId || '', entryUuids: source.map((entry) => entry.uuid) }]
  } else if (createRoundForm.strategy === 'CATEGORY') {
    const categories = [...new Set(source.map((entry) => entry.categoryName))]
    tables = categories.map((category, index) => ({
      id: `preview-${index + 1}`,
      name: `${category}桌`,
      captainPublicId: captainCandidates.value[index]?.publicId || '',
      entryUuids: source.filter((entry) => entry.categoryName === category).map((entry) => entry.uuid),
    }))
  } else {
    const count = Math.max(1, Number(createRoundForm.strategy === 'EVEN' ? createRoundForm.tableCount : 2) || 2)
    tables = Array.from({ length: count }, (_, index) => ({
      id: `preview-${index + 1}`,
      name: `2${String.fromCharCode(65 + index)}桌`,
      captainPublicId: captainCandidates.value[index]?.publicId || '',
      entryUuids: createRoundForm.strategy === 'MANUAL' ? [] : source.filter((_, entryIndex) => entryIndex % count === index).map((entry) => entry.uuid),
    }))
  }
  previewNextRoundTables.value = tables.map((table) => ({ ...table, targetCount: Number(createRoundForm.targetCount || 3), status: 'DRAFT', rankings: [] }))
}

function finishCreateRound() {
  const nextRound = {
    id: 'round-2',
    name: '第二轮',
    type: 'RANKING',
    status: 'DRAFT',
    tables: previewNextRoundTables.value.map((table, index) => ({
      ...table,
      id: `r2-${index + 1}`,
      targetCount: Number(createRoundForm.targetCount || 3),
      rankings: buildEmptyRankings(Number(createRoundForm.targetCount || 3), createRoundForm.targetMode),
    })),
  }
  const existingIndex = rounds.value.findIndex((round) => round.id === 'round-2')
  if (existingIndex >= 0) rounds.value.splice(existingIndex, 1, nextRound)
  else rounds.value.splice(1, 0, nextRound)
  activeRoundId.value = 'round-2'
  selectedRoundTableId.value = nextRound.tables[0]?.id || ''
  createRoundDialogOpen.value = false
  ElMessage.success('第二轮已从晋级池创建')
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

function buildAwardDrafts() {
  const second = rounds.value.find((round) => round.id === 'round-2')
  const categories = [...new Set(roundEntryPool.value.map((entry) => entry.categoryName))]
  return categories.flatMap((category) => {
    const categoryEntries = roundEntryPool.value.filter((entry) => entry.categoryName === category && entry.advanced)
    const rankedUuid = second?.tables.flatMap((table) => getRankingSlots(table)).find((slot) => categoryEntries.some((entry) => entry.uuid === slot.uuid))?.uuid
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

function publishMockResults() {
  ElMessage.success('结果发布前端原型已完成，后端发布接口待接入')
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
  seedRoundPrototype()
  ElMessage.success('评审人员已保存')
}

async function saveEntryConfig() {
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
  let detail = await updateCompetitionCategories(competition.value.id, { items: categoryItems })
  if (fieldItems.length) detail = await updateCompetitionEntryFields(competition.value.id, { items: fieldItems })
  competition.value = normalizeDetail(detail)
  resetForms()
  seedRoundPrototype()
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

function scoreConfigHint(config) {
  if (config.role === 'CROSS') return '可配置 2-4 个维度，总分 50'
  if (config.role === 'PROFESSIONAL') return '专业评审固定 5 个维度'
  return '桌长填写共识分和综合评语'
}

function defaultMinCommentLength(role) {
  if (role === 'CROSS') return 50
  if (role === 'PROFESSIONAL') return 30
  return 0
}

async function handlePrimaryAction() {
  await handleStageAction(stagePrimaryAction.value.action)
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
      seedRoundPrototype()
      ElMessage.success('报名已发布')
    } catch {
      // User cancelled.
    }
    return
  }
  if (action === 'publishCurrentRound') {
    publishCurrentRound()
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
  ElMessage.info(`${actionName}的后端接口待接入，当前先完成前端原型`)
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

.detail-shell {
  flex: 1 1 auto;
  min-height: 0;
  margin-top: 18px;
  display: flex;
  flex-direction: column;
}

.detail-tabs {
  flex: 0 0 auto;
  gap: 8px;
  flex-wrap: wrap;
  width: min(100%, 1360px);
  margin: 0 auto;
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

.tab-content {
  flex: 1 1 auto;
  min-height: 0;
  min-width: 0;
  margin-top: 14px;
  overflow-y: auto;
  overflow-x: hidden;
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

.pill-list,
.library-summary div {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pill-list span,
.library-summary span {
  padding: 7px 10px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.library-summary {
  align-content: start;
}

.library-summary strong {
  color: var(--text);
  font-size: 18px;
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
  grid-template-columns: 140px minmax(0, 1fr);
  gap: 24px;
  align-items: center;
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
  grid-template-columns: minmax(150px, 1fr) minmax(104px, 124px) minmax(190px, 1.2fr) 72px 96px 38px;
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
  .two-column,
  .judge-workbench,
  .round-workbench,
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
}
</style>
