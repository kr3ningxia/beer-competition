<template>
  <section :class="['allocation-workbench', { 'overview-workbench': allocationMode === 'overview' }]">
    <section v-if="allocationMode === 'judges'" class="allocation-grid">
      <template v-if="usesRoundJudgeEditor">
      <aside class="resource-panel judge-resource-panel">
        <label class="search-box">
          <Search />
          <input v-model="judgeKeywordModel" placeholder="搜索姓名、手机号、资质、回避" />
        </label>
        <div class="resource-list">
          <article
            v-for="judge in filteredJudgePool"
            :key="judge.publicId"
            :class="['resource-card', { assigned: isRoundJudgeAssigned(judge.publicId), disabled: !isJudgeActive(judge) }]"
            :draggable="currentRound?.status === 'DRAFT' && isJudgeActive(judge) && !isRoundJudgeAssigned(judge.publicId)"
            @dragstart="$emit('startJudgeDrag', judge)"
            @dragend="$emit('clearDrag')"
          >
            <span class="avatar">{{ getJudgeInitial(judge.name) }}</span>
            <div class="resource-body">
              <strong>{{ judge.name || '未填写姓名' }}</strong>
              <small v-if="hasJudgeBreweryConflict(judge)" class="judge-conflict-line" :data-full="formatJudgeBreweryConflict(judge)">
                {{ formatJudgeBreweryConflict(judge) }}
              </small>
              <small class="judge-qualification" :data-full="formatJudgeQualification(judge.qualification)">
                {{ formatJudgeQualification(judge.qualification) }}
              </small>
              <em v-if="getRoundJudgeAssignmentSummary(judge.publicId)" class="assignment-status">{{ getRoundJudgeAssignmentSummary(judge.publicId) }}</em>
            </div>
            <div class="resource-card-actions">
              <button
                type="button"
                :disabled="!canAddRoundJudge(judge)"
                @click="addJudgeToRankingTarget(judge)"
              >
                {{ getRoundJudgeActionLabel(judge) }}
              </button>
            </div>
          </article>
        </div>
      </aside>

      <main class="table-board judge-board">
        <article
          v-for="table in currentRoundTables"
          :key="table.id"
          :class="['desk-card', { active: selectedRoundTableId === table.id, danger: getRoundTableIssues(table).length }]"
          @click="selectRankingTable(table.id)"
        >
          <header class="desk-summary">
            <div class="desk-summary-main">
              <input
                v-if="isRoundTableNameEditable(table)"
                class="round-table-name-input"
                :value="table.name"
                aria-label="评审桌名称"
                @input="updateRoundTableName(table, $event.target.value)"
                @blur="commitRoundTableName(table, $event.target.value)"
                @keydown.enter.prevent="$event.target.blur()"
              />
              <h3 v-else>{{ table.name }}</h3>
            </div>
            <div class="desk-summary-side">
              <em :class="['desk-status', getRoundTableIssues(table).length ? 'warning' : 'ok']">
                {{ getRoundTableIssues(table)[0] || '分桌完整' }}
              </em>
              <button
                v-if="currentRound?.status === 'DRAFT'"
                class="icon-action desk-delete-action"
                type="button"
                title="删除桌"
                @click.stop="$emit('removeRoundTable', table.id)"
              >
                <Delete />
              </button>
            </div>
          </header>
          <section :class="['role-grid', 'ranking-role-grid', { 'score-role-grid': currentRound?.type === 'SCORE' }]">
            <div
              :class="['role-lane', { active: isSelectedRankingRole(table.id, 'CAPTAIN') }]"
              @click.stop="selectRankingRole(table.id, 'CAPTAIN')"
              @dragover.prevent
              @drop.prevent="dropRankingJudge(table.id, 'CAPTAIN')"
            >
              <header>
                <strong>桌长</strong>
                <span>{{ table.captainPublicId ? 1 : 0 }} 人</span>
              </header>
              <article v-if="table.captainPublicId" class="mini-card">
                <span class="avatar small">{{ getJudgeInitial(getJudge(table.captainPublicId)?.name) }}</span>
                <div>
                  <strong>{{ getJudge(table.captainPublicId)?.name || '未知评审' }}</strong>
                  <small>桌长</small>
                  <small v-if="getJudgeBreweryConflict(table.captainPublicId)" class="judge-conflict-line" :data-full="getJudgeBreweryConflict(table.captainPublicId)">
                    {{ getJudgeBreweryConflict(table.captainPublicId) }}
                  </small>
                </div>
                <button class="icon-action" type="button" @click.stop="$emit('updateTableCaptain', table.id, '')">
                  <Delete />
                </button>
              </article>
              <p v-else class="role-empty">
                从左侧拖入，或选中这里后点击“加入”
              </p>
            </div>
            <div
              v-if="currentRound?.type === 'SCORE'"
              v-for="role in scoreRoundMemberRoles"
              :key="role.value"
              :class="['role-lane', { active: isSelectedRankingRole(table.id, role.value) }]"
              @click.stop="selectRankingRole(table.id, role.value)"
              @dragover.prevent
              @drop.prevent="dropRankingJudge(table.id, role.value)"
            >
              <header>
                <strong>{{ role.label }}</strong>
                <span>{{ getScoreRoleMembers(table, role.value).length }} 人</span>
              </header>
              <article
                v-for="member in getScoreRoleMembers(table, role.value)"
                :key="member.judgePublicId"
                class="mini-card"
              >
                <span class="avatar small">{{ getJudgeInitial(member.name) }}</span>
                <div>
                  <strong>{{ member.name || getJudge(member.judgePublicId)?.name || '未知评审' }}</strong>
                  <small>{{ member.roleLabel || formatRoundMemberRole(member.role) }}</small>
                  <small v-if="getJudgeBreweryConflict(member.judgePublicId)" class="judge-conflict-line" :data-full="getJudgeBreweryConflict(member.judgePublicId)">
                    {{ getJudgeBreweryConflict(member.judgePublicId) }}
                  </small>
                </div>
                <button class="icon-action" type="button" @click.stop="$emit('removeRoundParticipant', table.id, member.judgePublicId)">
                  <Delete />
                </button>
              </article>
              <p v-if="getScoreRoleMembers(table, role.value).length === 0" class="role-empty">
                从左侧拖入，或选中这里后点击“加入”
              </p>
            </div>
            <div
              v-else
              :class="['role-lane', { active: isSelectedRankingRole(table.id, 'PARTICIPANT') }]"
              @click.stop="selectRankingRole(table.id, 'PARTICIPANT')"
              @dragover.prevent
              @drop.prevent="dropRankingJudge(table.id, 'PARTICIPANT')"
            >
              <header>
                <strong>参与评审</strong>
                <span>{{ getRankingParticipants(table).length }} 人</span>
              </header>
              <article
                v-for="member in getRankingParticipants(table)"
                :key="member.judgePublicId"
                class="mini-card"
              >
                <span class="avatar small">{{ getJudgeInitial(member.name) }}</span>
                <div>
                  <strong>{{ member.name || getJudge(member.judgePublicId)?.name || '未知评审' }}</strong>
                  <small>参与评审</small>
                  <small v-if="getJudgeBreweryConflict(member.judgePublicId)" class="judge-conflict-line" :data-full="getJudgeBreweryConflict(member.judgePublicId)">
                    {{ getJudgeBreweryConflict(member.judgePublicId) }}
                  </small>
                </div>
                <button class="icon-action" type="button" @click.stop="$emit('removeRoundParticipant', table.id, member.judgePublicId)">
                  <Delete />
                </button>
              </article>
              <p v-if="getRankingParticipants(table).length === 0" class="role-empty">
                默认加入到这里，也可以拖入
              </p>
            </div>
          </section>
        </article>
      </main>

      <aside class="control-panel check-panel">
        <section class="control-section">
          <strong>当前轮次</strong>
          <div class="round-switch" aria-label="当前轮次">
            <button
              v-for="round in displayRounds"
              :key="round.id"
              :class="{ active: round.id === activeRoundId }"
              type="button"
              @click="$emit('selectRound', round.id)"
            >
              {{ round.name }}
            </button>
          </div>
        </section>
        <section class="control-section">
          <strong>分配对象</strong>
          <div class="mode-switch" aria-label="编辑对象">
            <button :class="{ active: allocationMode === 'judges' }" type="button" @click="$emit('update:allocationMode', 'judges')">评审</button>
            <button :class="{ active: allocationMode === 'entries' }" type="button" @click="$emit('update:allocationMode', 'entries')">酒款</button>
            <button :class="{ active: allocationMode === 'overview' }" type="button" @click="$emit('update:allocationMode', 'overview')">总览</button>
          </div>
        </section>
        <section v-if="currentRound?.type === 'RANKING'" class="control-section">
          <strong>轮次目标</strong>
          <label class="stack-field target-mode-field">
            <select
              :value="currentRoundTargetMode"
              :disabled="currentRound?.status !== 'DRAFT'"
              @change="$emit('updateRoundTargetMode', $event.target.value)"
            >
              <option v-for="option in roundTargetOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
            <small>{{ currentRoundTargetHint }}</small>
          </label>
        </section>
        <section class="control-section">
          <strong>操作</strong>
          <button v-if="currentRound?.status === 'DRAFT'" class="add-desk" type="button" @click="$emit('addRoundTable')">
            新增桌
          </button>
          <button
            v-if="canSyncRoundCandidates"
            class="main-action"
            type="button"
            @click="$emit('syncRoundCandidates')"
          >
            同步候选酒款
          </button>
          <button
            :class="['main-action', { blocked: !canRunPrimaryRoundAction }]"
            type="button"
            :aria-disabled="!canRunPrimaryRoundAction"
            @click="$emit('publishCurrentRound')"
          >
            {{ primaryRoundActionLabel }}
          </button>
        </section>
        <section class="control-section">
          <strong>{{ currentRound?.name }}检查</strong>
          <p v-for="issue in roundValidationIssues" :key="issue" class="warning">
            <Warning />
            <span>{{ issue }}</span>
          </p>
          <p v-if="roundValidationIssues.length === 0 && publishDisabledReason" class="warning">
            <Warning />
            <span>{{ publishDisabledReason }}</span>
          </p>
          <p v-if="roundValidationIssues.length === 0 && !publishDisabledReason" class="ok">
            <CircleCheck />
            <span>{{ currentRound?.type === 'SCORE' ? '首轮可以发布' : '排序轮可以发布' }}</span>
          </p>
        </section>
      </aside>
      </template>

      <template v-else>
      <aside class="resource-panel judge-resource-panel">
        <label class="search-box">
          <Search />
          <input v-model="judgeKeywordModel" placeholder="搜索姓名、手机号、资质、回避" />
        </label>
        <div class="filter-row primary-filter-row">
          <button
            v-for="filter in roleFilters"
            :key="filter.value"
            :class="{ active: judgeRoleFilter === filter.value }"
            type="button"
            @click="$emit('update:judgeRoleFilter', filter.value)"
          >
            <span>{{ filter.label }}</span>
            <b>{{ judgeFilterCounts[filter.value] || 0 }}</b>
          </button>
        </div>
        <div class="resource-list">
          <article
            v-for="judge in filteredJudgePool"
            :key="judge.publicId"
            :class="['resource-card', { assigned: isAssigned(judge.publicId), disabled: !isJudgeActive(judge) }]"
            :draggable="editableJudges && isJudgeActive(judge)"
            @dragstart="$emit('startJudgeDrag', judge)"
            @dragend="$emit('clearDrag')"
          >
            <span class="avatar">{{ getJudgeInitial(judge.name) }}</span>
            <div class="resource-body">
              <strong>{{ judge.name || '未填写姓名' }}</strong>
              <small v-if="hasJudgeBreweryConflict(judge)" class="judge-conflict-line" :data-full="formatJudgeBreweryConflict(judge)">
                {{ formatJudgeBreweryConflict(judge) }}
              </small>
              <small class="judge-qualification" :data-full="formatJudgeQualification(judge.qualification)">
                {{ formatJudgeQualification(judge.qualification) }}
              </small>
              <em v-if="getJudgeAssignmentSummary(judge.publicId)" class="assignment-status">{{ getJudgeAssignmentSummary(judge.publicId) }}</em>
            </div>
            <button type="button" :disabled="!editableJudges || !isJudgeActive(judge)" @click="$emit('addJudgeToTarget', judge)">
              {{ isAssigned(judge.publicId) ? '调整到当前' : '加入' }}
            </button>
          </article>
        </div>
      </aside>

      <main class="table-board judge-board">
        <article
          v-for="(table, index) in judgeTableForm"
          :key="table.localId"
          :class="['desk-card', { active: selectedTableLocalId === table.localId, danger: tableValidationIssues(table).length }]"
        >
          <header class="desk-summary">
            <div class="desk-summary-main">
              <label v-if="editableJudges" class="desk-name-field">
                <span>桌号：</span>
                <input :value="table.tableName" placeholder="例如 A桌" @input="table.tableName = $event.target.value" />
              </label>
              <h3 v-else>{{ table.tableName }}</h3>
            </div>
            <div class="desk-summary-side">
              <em :class="['desk-status', tableValidationIssues(table).length ? 'warning' : 'ok']">
                {{ tableValidationIssues(table)[0] || '人员完整' }}
              </em>
              <button v-if="editableJudges" class="icon-action" type="button" @click="$emit('removeJudgeTable', index)">
                <Delete />
              </button>
            </div>
          </header>
          <section class="role-grid">
            <div
              v-for="role in roleOptions"
              :key="role.value"
              :class="['role-lane', { active: selectedTableLocalId === table.localId && selectedRole === role.value }]"
              @click="$emit('selectAssignmentTarget', table, role.value)"
              @dragover.prevent
              @drop.prevent="$emit('dropOnRole', table, role.value)"
            >
              <header>
                <strong>{{ role.label }}</strong>
                <span>{{ assignmentsForTable(table, role.value).length }} 人</span>
              </header>
              <article
                v-for="assignment in assignmentsForTable(table, role.value)"
                :key="assignment.localId"
                class="mini-card"
                :draggable="editableJudges"
                @dragstart="$emit('startAssignmentDrag', assignment)"
                @dragend="$emit('clearDrag')"
              >
                <span class="avatar small">{{ getJudgeInitial(getJudge(assignment.judgePublicId)?.name) }}</span>
                <div>
                  <strong>{{ getJudge(assignment.judgePublicId)?.name || '未知评审' }}</strong>
                  <small v-if="getJudgeBreweryConflict(assignment.judgePublicId)" class="judge-conflict-line" :data-full="getJudgeBreweryConflict(assignment.judgePublicId)">
                    {{ getJudgeBreweryConflict(assignment.judgePublicId) }}
                  </small>
                </div>
                <button v-if="editableJudges" class="icon-action" type="button" @click.stop="$emit('removeAssignment', assignment)">
                  <Delete />
                </button>
              </article>
              <p v-if="assignmentsForTable(table, role.value).length === 0" class="role-empty">
                点击或拖拽到这里分配{{ role.label }}
              </p>
            </div>
          </section>
        </article>
      </main>

      <aside class="control-panel check-panel">
        <section class="control-section">
          <strong>当前轮次</strong>
          <div class="round-switch" aria-label="当前轮次">
            <button
              v-for="round in displayRounds"
              :key="round.id"
              :class="{ active: round.id === activeRoundId }"
              type="button"
              @click="$emit('selectRound', round.id)"
            >
              {{ round.name }}
            </button>
          </div>
        </section>
        <section class="control-section">
          <strong>分配对象</strong>
          <div class="mode-switch" aria-label="编辑对象">
            <button :class="{ active: allocationMode === 'judges' }" type="button" @click="$emit('update:allocationMode', 'judges')">评审</button>
            <button :class="{ active: allocationMode === 'entries' }" type="button" @click="$emit('update:allocationMode', 'entries')">酒款</button>
            <button :class="{ active: allocationMode === 'overview' }" type="button" @click="$emit('update:allocationMode', 'overview')">总览</button>
          </div>
        </section>
        <section class="control-section">
          <strong>操作</strong>
          <button v-if="editableJudges" class="add-desk" type="button" @click="$emit('addJudgeTable')">新增基础桌</button>
          <button v-if="editableJudges" type="button" @click="$emit('saveJudgeDraft')">保存评审人员</button>
        </section>
        <section class="control-section">
        <strong>人员检查</strong>
        <p v-for="issue in validationIssues" :key="issue" class="warning">
          <Warning />
          <span>{{ issue }}</span>
        </p>
        <p v-if="validationIssues.length === 0 && !firstRoundExists" class="ok">
          <CircleCheck />
          <span>可以继续分配首轮酒款</span>
        </p>
        <p v-if="validationIssues.length === 0 && firstRoundExists" class="ok">
          <CircleCheck />
          <span>可以继续调整首轮草稿</span>
        </p>
        </section>
      </aside>
      </template>
    </section>

    <section v-else-if="allocationMode === 'entries'" class="allocation-grid entry-allocation-grid">
      <aside class="resource-panel">
        <div class="entry-search-row">
          <label class="search-box">
            <Search />
            <input v-model="roundKeywordModel" placeholder="搜索酒名、编号、短编号、风格" />
          </label>
          <button
            v-if="currentRound?.status === 'DRAFT'"
            class="entry-add-selected"
            type="button"
            :disabled="!selectedEntryUuids.length"
            @click="$emit('addSelectedToTable')"
          >
            加入
          </button>
        </div>
        <div class="filter-row">
          <button
            v-for="category in roundCategoryFilters"
            :key="category"
            :class="{ active: roundCategoryFilter === category }"
            type="button"
            @click="$emit('update:roundCategoryFilter', category)"
          >
            {{ category }}
          </button>
        </div>
        <div class="resource-list">
          <article
            v-for="entry in filteredRoundPool"
            :key="entry.uuid"
            :class="['resource-card entry', { assigned: getRoundEntryAssignment(entry.uuid), selected: selectedEntryUuids.includes(entry.uuid) }]"
            :draggable="currentRound?.status === 'DRAFT' && !getRoundEntryAssignment(entry.uuid)"
            @dragstart="$emit('startEntryDrag', entry.uuid)"
            @dragend="$emit('clearDrag')"
          >
            <input
              type="checkbox"
              :checked="selectedEntryUuids.includes(entry.uuid)"
              :disabled="Boolean(getRoundEntryAssignment(entry.uuid))"
              @change="$emit('toggleEntrySelection', entry.uuid)"
            />
            <div>
              <strong>{{ formatEntryName(entry) }}</strong>
              <span class="entry-meta-line">
                <small>{{ entry.shortCode }} · {{ entry.categoryName }} · {{ entry.style }}</small>
                <em :class="{ pending: !getRoundEntryAssignment(entry.uuid) }">{{ getRoundEntryStatusLabel(entry.uuid) }}</em>
              </span>
            </div>
            <button
              type="button"
              :disabled="!selectedRoundTableId || Boolean(getRoundEntryAssignment(entry.uuid))"
              @click="$emit('addEntryToSelectedTable', entry.uuid)"
            >
              {{ getRoundEntryActionLabel(entry.uuid) }}
            </button>
          </article>
          <p v-if="!filteredRoundPool.length" class="resource-empty">暂无可分配酒款</p>
        </div>
      </aside>

      <main class="table-board">
        <article
          v-for="table in currentRoundTables"
          :key="table.id"
          :class="['desk-card', { active: selectedRoundTableId === table.id, danger: getRoundTableIssues(table).length || getRoundTableConflictWarnings(table).length }]"
          @click="$emit('selectRoundTable', table.id)"
          @dragover.prevent
          @drop.prevent="$emit('dropEntryOnRoundTable', table.id)"
        >
          <header class="entry-desk-header">
            <div class="entry-desk-title">
              <input
                v-if="isRoundTableNameEditable(table)"
                class="round-table-name-input"
                :value="table.name"
                aria-label="评审桌名称"
                @input="updateRoundTableName(table, $event.target.value)"
                @blur="commitRoundTableName(table, $event.target.value)"
                @keydown.enter.prevent="$event.target.blur()"
              />
              <h3 v-else>{{ table.name }}</h3>
              <span>{{ table.entryUuids.length }} 款</span>
              <span>桌长 {{ getJudge(table.captainPublicId)?.name || '未指定' }}</span>
            </div>
            <div class="desk-header-actions">
              <button
                v-if="currentRound?.status === 'DRAFT'"
                class="icon-action desk-delete-action"
                type="button"
                title="删除桌"
                @click.stop="$emit('removeRoundTable', table.id)"
              >
                <Delete />
              </button>
              <button
                v-if="currentRound?.status === 'DRAFT'"
                class="desk-auto-action"
                type="button"
                @click.stop="$emit('openEntryAutoAssign', table.id)"
              >
                自动分配
              </button>
            </div>
          </header>
          <p
            v-for="warning in getRoundTableConflictWarnings(table)"
            :key="warning"
            class="desk-warning-line"
          >
            <Warning />
            <span>{{ warning }}</span>
          </p>
          <div :class="['desk-config-row', { 'ranking-config-row': currentRound?.type === 'RANKING' }]">
            <label v-if="currentRound?.type === 'SCORE'" class="inline-scope compact-scope" @click.stop>
              <span>范围</span>
              <select
                :value="getTableScopeValue(table)"
                :disabled="currentRound?.status !== 'DRAFT'"
                @change="$emit('updateTableScope', table.id, $event.target.value)"
              >
                <option v-for="option in tableScopeOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
            <label v-if="!isFeedbackOnlyScoreRound" class="inline-target compact-target" @click.stop>
              <span class="target-label">
                <span>{{ getTargetCountLabel(table) }}</span>
                <select
                  v-if="currentRound?.type === 'RANKING'"
                  class="target-scope-select"
                  :value="getTableScopeValue(table)"
                  :disabled="currentRound?.status !== 'DRAFT'"
                  aria-label="范围"
                  @change="$emit('updateTableScope', table.id, $event.target.value)"
                >
                  <option v-for="option in tableScopeOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </span>
              <input
                :value="table.targetCount"
                min="1"
                type="number"
                :disabled="isTargetCountFixed(table)"
                @input="$emit('updateTableTarget', table.id, Number($event.target.value || 1))"
              />
            </label>
          </div>
          <div class="entry-list">
            <article
              v-for="uuid in table.entryUuids"
              :key="uuid"
              class="assigned-entry-card"
            >
              <span class="assigned-entry-name">{{ formatEntryBrief(uuid) }}</span>
              <div class="entry-hover-card" role="tooltip">
                <header>
                  <strong>{{ formatEntryName(getEntry(uuid)) }}</strong>
                  <em>{{ getEntry(uuid)?.shortCode || uuid }}</em>
                </header>
                <dl>
                  <template v-for="item in getEntryHoverRows(uuid)" :key="item.label">
                    <dt>{{ item.label }}</dt>
                    <dd>{{ item.value }}</dd>
                  </template>
                </dl>
              </div>
              <button type="button" @click.stop="$emit('removeEntryFromRoundTable', table.id, uuid)">移除</button>
            </article>
            <p v-if="!table.entryUuids.length">从左侧加入酒款</p>
          </div>
        </article>
        <section v-if="!currentRoundTables.length" class="desk-empty-state">
          <strong>暂无桌次</strong>
          <span>先在右侧新增桌，再分配酒款</span>
        </section>
      </main>

      <aside class="control-panel check-panel">
        <section class="control-section">
          <strong>当前轮次</strong>
          <div class="round-switch" aria-label="当前轮次">
            <button
              v-for="round in displayRounds"
              :key="round.id"
              :class="{ active: round.id === activeRoundId }"
              type="button"
              @click="$emit('selectRound', round.id)"
            >
              {{ round.name }}
            </button>
          </div>
        </section>
        <section class="control-section">
          <strong>分配对象</strong>
          <div class="mode-switch" aria-label="编辑对象">
            <button :class="{ active: allocationMode === 'judges' }" type="button" @click="$emit('update:allocationMode', 'judges')">评审</button>
            <button :class="{ active: allocationMode === 'entries' }" type="button" @click="$emit('update:allocationMode', 'entries')">酒款</button>
            <button :class="{ active: allocationMode === 'overview' }" type="button" @click="$emit('update:allocationMode', 'overview')">总览</button>
          </div>
        </section>
        <section v-if="currentRound?.type === 'RANKING'" class="control-section">
          <strong>轮次目标</strong>
          <label class="stack-field target-mode-field">
            <select
              :value="currentRoundTargetMode"
              :disabled="currentRound?.status !== 'DRAFT'"
              @change="$emit('updateRoundTargetMode', $event.target.value)"
            >
              <option v-for="option in roundTargetOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
            <small>{{ currentRoundTargetHint }}</small>
          </label>
        </section>
        <section class="control-section">
          <strong>操作</strong>
          <button v-if="currentRound?.type === 'RANKING' && currentRound?.status === 'DRAFT'" class="add-desk" type="button" @click="$emit('addRoundTable')">
            新增桌
          </button>
          <button
            v-if="canSyncRoundCandidates"
            class="main-action"
            type="button"
            @click="$emit('syncRoundCandidates')"
          >
            同步候选酒款
          </button>
          <button
            v-if="currentRound"
            :class="['main-action', { blocked: !canRunPrimaryRoundAction }]"
            type="button"
            :aria-disabled="!canRunPrimaryRoundAction"
            @click="$emit('publishCurrentRound')"
          >
            {{ primaryRoundActionLabel }}
          </button>
        </section>
        <section class="control-section">
          <strong>{{ currentRound?.name }}检查</strong>
          <p v-for="issue in roundValidationIssues" :key="issue" class="warning">
            <Warning />
            <span>{{ issue }}</span>
          </p>
          <p v-for="warning in currentRoundConflictWarnings" :key="warning" class="warning">
            <Warning />
            <span>{{ warning }}</span>
          </p>
          <p v-if="roundValidationIssues.length === 0 && publishDisabledReason && !currentRound?.isPreparationDraft" class="warning">
            <Warning />
            <span>{{ publishDisabledReason }}</span>
          </p>
          <p v-if="roundValidationIssues.length === 0 && (!publishDisabledReason || currentRound?.isPreparationDraft)" class="ok">
            <CircleCheck />
            <span>{{ currentRound?.isPreparationDraft ? '当前分配会在发布时保存为首轮' : '当前轮次可以发布' }}</span>
          </p>
        </section>
      </aside>
    </section>

    <section v-else class="overview-shell">
      <main class="overview-board">
        <section class="overview-summary">
          <article>
            <span>桌数</span>
            <strong>{{ currentRoundTables.length }}</strong>
          </article>
          <article>
            <span>评审</span>
            <strong>{{ overviewMetrics.assignedJudges }}</strong>
          </article>
          <article>
            <span>酒款</span>
            <strong>{{ overviewMetrics.assignedEntries }} / {{ currentPoolEntries.length }}</strong>
          </article>
          <article :class="{ warning: overviewMetrics.unassignedEntries > 0 }">
            <span>未分配</span>
            <strong>{{ overviewMetrics.unassignedEntries }}</strong>
          </article>
          <article :class="overviewMetrics.issueCount ? 'warning' : 'ok'">
            <span>问题</span>
            <strong>{{ overviewMetrics.issueCount }}</strong>
          </article>
          <article :class="overviewStatusClass">
            <span>状态</span>
            <strong>{{ overviewStatusText }}</strong>
          </article>
        </section>

        <section class="overview-toolbar">
          <div class="overview-toolbar-group">
            <strong>当前轮次</strong>
            <div class="round-switch compact-switch" aria-label="当前轮次">
              <button
                v-for="round in displayRounds"
                :key="round.id"
                :class="{ active: round.id === activeRoundId }"
                type="button"
                @click="$emit('selectRound', round.id)"
              >
                {{ round.name }}
              </button>
            </div>
          </div>
          <div class="overview-toolbar-group">
            <strong>分配对象</strong>
            <div class="mode-switch compact-switch" aria-label="编辑对象">
              <button :class="{ active: allocationMode === 'judges' }" type="button" @click="$emit('update:allocationMode', 'judges')">评审</button>
              <button :class="{ active: allocationMode === 'entries' }" type="button" @click="$emit('update:allocationMode', 'entries')">酒款</button>
              <button :class="{ active: allocationMode === 'overview' }" type="button" @click="$emit('update:allocationMode', 'overview')">总览</button>
            </div>
          </div>
        </section>

        <section class="overview-grid">
          <article
            v-for="table in currentRoundTables"
            :key="table.id"
            :class="['overview-card', { danger: getOverviewTableIssues(table).length }]"
          >
            <header class="overview-card-header">
              <div class="overview-title-block">
                <h3>{{ table.name }}</h3>
                <div class="overview-card-meta">
                  <span>{{ getOverviewJudgeItems(table).length }} 名成员</span>
                  <span>{{ table.entryUuids.length }} 款酒</span>
                  <span v-if="!isFeedbackOnlyScoreRound">{{ getTargetCountLabel(table) }} {{ table.targetCount }}</span>
                </div>
              </div>
              <em :class="getOverviewTableIssues(table).length ? 'warning' : 'ok'">
                {{ getOverviewTableIssues(table).length ? '需处理' : '已就绪' }}
              </em>
            </header>

            <div class="overview-content-grid">
              <section class="overview-block overview-members">
                <div class="overview-block-head">
                  <span>成员</span>
                  <small>{{ formatRoleSummary(table) }}</small>
                </div>
                <ul v-if="getOverviewJudgeItems(table).length" class="overview-member-list">
                  <li
                    v-for="judge in getOverviewJudgeItems(table)"
                    :key="judge.key"
                    class="overview-member-card"
                  >
                    <em :class="`role-${judge.role}`">{{ judge.roleLabel }}</em>
                    <div>
                      <strong>{{ judge.name }}</strong>
                      <small v-if="judge.breweryConflict" class="judge-conflict-line" :data-full="judge.breweryConflict">{{ judge.breweryConflict }}</small>
                    </div>
                  </li>
                </ul>
                <p v-else class="overview-empty">还没有安排成员</p>
              </section>

              <section class="overview-block overview-entries">
                <div class="overview-block-head">
                  <span>酒款</span>
                  <small>{{ formatCategorySummary(table) }}</small>
                </div>
                <ul v-if="getOverviewEntryItems(table).length" class="overview-entry-list">
                  <li
                    v-for="entry in getOverviewEntryItems(table)"
                    :key="entry.uuid"
                    class="overview-entry-card"
                  >
                    <strong>{{ entry.code }}</strong>
                    <div>
                      <small>{{ entry.categoryName }} · {{ entry.style }}</small>
                    </div>
                  </li>
                </ul>
                <p v-else class="overview-empty">还没有分配酒款</p>
              </section>
            </div>

            <p v-if="getOverviewTableIssues(table).length" class="overview-issue">
              <Warning />
              <span>{{ getOverviewTableIssues(table)[0] }}</span>
            </p>
          </article>
        </section>
      </main>
    </section>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { CircleCheck, Delete, Search, Warning } from '@element-plus/icons-vue'

const props = defineProps({
  allocationMode: { type: String, default: 'judges' },
  rounds: { type: Array, required: true },
  activeRoundId: { type: String, default: '' },
  currentRound: { type: Object, default: null },
  currentRoundTables: { type: Array, required: true },
  selectedRoundTable: { type: Object, default: null },
  selectedRoundTableId: { type: String, default: '' },
  editableJudges: Boolean,
  judgeTableForm: { type: Array, required: true },
  roleOptions: { type: Array, required: true },
  roleFilters: { type: Array, required: true },
  judgeFilterCounts: { type: Object, required: true },
  judgeKeyword: { type: String, default: '' },
  judgeRoleFilter: { type: String, default: 'ALL' },
  selectedTableLocalId: { type: [String, Number], default: null },
  selectedRole: { type: String, default: 'CAPTAIN' },
  validationIssues: { type: Array, required: true },
  filteredJudgePool: { type: Array, required: true },
  assignmentsForTable: { type: Function, required: true },
  tableValidationIssues: { type: Function, required: true },
  filteredRoundPool: { type: Array, required: true },
  currentPoolEntries: { type: Array, required: true },
  roundCategoryFilters: { type: Array, required: true },
  roundCategoryFilter: { type: String, default: '全部' },
  roundKeyword: { type: String, default: '' },
  selectedEntryUuids: { type: Array, default: () => [] },
  roundValidationIssues: { type: Array, required: true },
  publishDisabledReason: { type: String, default: '' },
  canPublish: Boolean,
  competitionType: { type: String, default: 'AWARD' },
  getJudge: { type: Function, required: true },
  getJudgeInitial: { type: Function, required: true },
  getJudgeAssignmentSummary: { type: Function, required: true },
  isAssigned: { type: Function, required: true },
  isJudgeActive: { type: Function, required: true },
  getRoundEntryAssignment: { type: Function, required: true },
  getRoundTableIssues: { type: Function, required: true },
  getRoundTableConflictWarnings: { type: Function, default: () => [] },
})

const emit = defineEmits([
  'update:allocationMode',
  'selectRound',
  'update:judgeKeyword',
  'update:judgeRoleFilter',
  'update:roundKeyword',
  'update:roundCategoryFilter',
  'selectAssignmentTarget',
  'addJudgeToTarget',
  'addJudgeTable',
  'removeJudgeTable',
  'removeAssignment',
  'saveJudgeDraft',
  'startJudgeDrag',
  'startAssignmentDrag',
  'dropOnRole',
  'clearDrag',
  'dropRoundJudge',
  'generateFirstRound',
  'toggleEntrySelection',
  'addSelectedToTable',
  'addEntryToSelectedTable',
  'startEntryDrag',
  'openEntryAutoAssign',
  'selectRoundTable',
  'dropEntryOnRoundTable',
  'removeEntryFromRoundTable',
  'updateTableCaptain',
  'updateRoundTableName',
  'updateTableScope',
  'updateTableTarget',
  'setRoundCaptain',
  'updateRoundTargetMode',
  'addRoundTable',
  'removeRoundTable',
  'addRoundParticipant',
  'removeRoundParticipant',
  'syncRoundCandidates',
  'publishCurrentRound',
])

const judgeKeywordModel = computed({
  get: () => props.judgeKeyword,
  set: (value) => emit('update:judgeKeyword', value),
})

const roundKeywordModel = computed({
  get: () => props.roundKeyword,
  set: (value) => emit('update:roundKeyword', value),
})

function isRoundTableNameEditable(table) {
  return Boolean(table && props.currentRound?.status === 'DRAFT')
}

function updateRoundTableName(table, value) {
  if (!isRoundTableNameEditable(table)) return
  emit('updateRoundTableName', table.id, value)
}

function commitRoundTableName(table, value) {
  if (!isRoundTableNameEditable(table)) return
  emit('updateRoundTableName', table.id, value.trim(), { commit: true })
}

const selectedRankingRole = ref(null)
const scoreRoundMemberRoles = [
  { value: 'PROFESSIONAL', label: '专业评审' },
  { value: 'CROSS', label: '跨界评审' },
]
const firstRoundExists = computed(() => props.rounds.some((round) => round.roundNo === 1))
const isFeedbackOnlyScoreRound = computed(() => props.competitionType === 'FEEDBACK_ONLY' && props.currentRound?.type === 'SCORE')
const usesRoundJudgeEditor = computed(() => props.currentRound?.type === 'RANKING'
  || (props.currentRound?.type === 'SCORE' && !props.currentRound?.isPreparationDraft))
const displayRounds = computed(() => {
  if (props.currentRound?.isPreparationDraft && !firstRoundExists.value) return [props.currentRound]
  return props.rounds
})
const canRunPrimaryRoundAction = computed(() => {
  return props.canPublish
})
const canSyncRoundCandidates = computed(() => (
  props.currentRound?.type === 'RANKING'
    && props.currentRound?.status === 'DRAFT'
    && props.currentRound?.sourceLocked
    && !props.currentRound?.candidatesSynced
))
const primaryRoundActionLabel = computed(() => {
  return props.currentRound?.type === 'SCORE' ? '发布给评审' : '发布给桌长和参与评审'
})
const overviewBaseValidationIssues = computed(() => (usesRoundJudgeEditor.value ? [] : props.validationIssues))
const overviewStatusText = computed(() => {
  if (props.canPublish) return '可发布'
  if (props.currentRound?.isPreparationDraft && props.roundValidationIssues.length === 0 && overviewBaseValidationIssues.value.length === 0) return '预排草稿'
  if (props.publishDisabledReason && props.roundValidationIssues.length === 0 && overviewBaseValidationIssues.value.length === 0) return '预排草稿'
  return '需处理'
})
const overviewStatusClass = computed(() => (props.canPublish ? 'ok' : 'warning'))
const roundTargetOptions = computed(() => {
  if (currentRoundTargetMode.value === 'CHAMPION' || canUseChampionTarget.value) {
    return [
      {
        value: 'TOP_N',
        label: '普通排序轮',
        hint: '继续筛选，锁定后可创建下一轮',
      },
      {
        value: 'MEDALS',
        label: '组别金银铜轮',
        hint: '金奖、银奖、铜奖为可用名额，可按评审结果留空',
      },
      {
        value: 'CHAMPION',
        label: '决赛轮',
        hint: '从各组别金奖中选出 1 款全场总冠军',
      },
    ]
  }
  return [
    {
      value: 'TOP_N',
      label: '普通排序轮',
      hint: '继续筛选，锁定后可创建下一轮',
    },
    {
      value: 'MEDALS',
      label: '组别金银铜轮',
      hint: '金奖、银奖、铜奖为可用名额，可按评审结果留空',
    },
  ]
})
const canUseChampionTarget = computed(() => {
  const sourceRound = props.rounds.find((round) => round.id === props.currentRound?.sourceRoundId)
  return Boolean(sourceRound?.tables?.some((table) => table.targetMode === 'MEDALS'))
})
const currentRoundTargetMode = computed(() => {
  const modes = new Set(props.currentRoundTables.map((table) => table.targetMode).filter(Boolean))
  if (modes.size === 1) return [...modes][0]
  return 'TOP_N'
})
const currentRoundTargetHint = computed(() => {
  const option = roundTargetOptions.value.find((item) => item.value === currentRoundTargetMode.value)
  if (props.currentRound?.status !== 'DRAFT') return option ? `${option.label}已发布，不能在这里修改` : '当前轮次已发布，不能在这里修改'
  return option?.hint || '草稿阶段可以修改轮次目标'
})
const tableScopeOptions = computed(() => {
  const categories = new Map()
  props.currentPoolEntries.forEach((entry) => {
    if (entry.categoryId == null || !entry.categoryName) return
    categories.set(entry.categoryId, entry.categoryName)
  })
  props.currentRoundTables.forEach((table) => {
    if (table.categoryId == null || !table.categoryName) return
    categories.set(table.categoryId, table.categoryName)
  })
  return [
    { value: 'EMPTY', label: '未指定' },
    { value: 'MIXED', label: '混合' },
    ...[...categories.entries()].map(([id, name]) => ({ value: `CATEGORY:${id}`, label: name })),
  ]
})

const overviewMetrics = computed(() => {
  const assignedEntries = new Set(props.currentRoundTables.flatMap((table) => table.entryUuids)).size
  const assignedJudges = usesRoundJudgeEditor.value
    ? new Set(props.currentRoundTables.flatMap((table) => getRoundJudgeAssignments(table).map((assignment) => assignment.judgePublicId)).filter(Boolean)).size
    : props.currentRound?.type === 'SCORE'
      ? props.judgeTableForm.reduce((sum, table) => sum + props.assignmentsForTable(table).length, 0)
      : props.currentRoundTables.reduce((sum, table) => sum + getRoundMembers(table).length, 0)
  const issueCount = props.roundValidationIssues.length + overviewBaseValidationIssues.value.length + currentRoundConflictWarnings.value.length
  return {
    assignedEntries,
    assignedJudges,
    unassignedEntries: Math.max(props.currentPoolEntries.length - assignedEntries, 0),
    issueCount,
  }
})
const currentRoundConflictWarnings = computed(() => {
  const warnings = props.currentRoundTables.flatMap((table) => props.getRoundTableConflictWarnings(table))
  return [...new Set(warnings)]
})

function getRoundMembers(roundTable) {
  return roundTable?.members || []
}

function selectRankingTable(tableId) {
  selectedRankingRole.value = null
  emit('selectRoundTable', tableId)
}

function selectRankingRole(tableId, role) {
  selectedRankingRole.value = role
  emit('selectRoundTable', tableId)
}

function isSelectedRankingRole(tableId, role) {
  return props.selectedRoundTableId === tableId && selectedRankingRole.value === role
}

function addJudgeToRankingTarget(judge) {
  const judgePublicId = typeof judge === 'string' ? judge : judge?.publicId
  if (selectedRankingRole.value === 'CAPTAIN') {
    emit('setRoundCaptain', judgePublicId)
    return
  }
  if (props.currentRound?.type === 'SCORE' && ['PROFESSIONAL', 'CROSS'].includes(selectedRankingRole.value)) {
    emit('addRoundParticipant', judgePublicId, selectedRankingRole.value)
    return
  }
  emit('addRoundParticipant', judgePublicId, resolveRoundMemberRole(judge))
}

function dropRankingJudge(tableId, role) {
  selectRankingRole(tableId, role)
  emit('dropRoundJudge', tableId, role)
}

function getRankingParticipants(roundTable) {
  return getRoundMembers(roundTable).filter((member) => member.role !== 'CAPTAIN')
}

function getScoreRoleMembers(roundTable, role) {
  return getRoundMembers(roundTable)
    .filter((member) => member.role !== 'CAPTAIN' && normalizeRoundScoreMemberRole(member.role) === role)
}

function isRoundJudgeAssigned(judgePublicId) {
  return props.currentRoundTables.some((table) => (
    table.captainPublicId === judgePublicId
      || getRoundMembers(table).some((member) => member.judgePublicId === judgePublicId)
  ))
}

function getRoundJudgeAssignmentSummary(judgePublicId) {
  const table = props.currentRoundTables.find((item) => (
    item.captainPublicId === judgePublicId
      || getRoundMembers(item).some((member) => member.judgePublicId === judgePublicId)
  ))
  if (!table) return ''
  if (table.captainPublicId === judgePublicId) return `已在 ${table.name} · 桌长`
  const member = getRoundMembers(table).find((item) => item.judgePublicId === judgePublicId)
  return `已在 ${table.name} · ${props.currentRound?.type === 'SCORE' ? formatRoundMemberRole(member?.role) : '参与评审'}`
}

function canAddRoundJudge(judge) {
  return props.currentRound?.status === 'DRAFT'
    && props.selectedRoundTableId
    && props.isJudgeActive(judge)
    && !isRoundJudgeAssigned(judge.publicId)
}

function getRoundJudgeActionLabel(judge) {
  if (!props.isJudgeActive(judge)) return '已停用'
  if (isRoundJudgeAssigned(judge.publicId)) return '已分配'
  return '加入'
}

function getRoundEntryStatusLabel(uuid) {
  const tableName = props.getRoundEntryAssignment(uuid)
  return tableName ? `已分配到 ${tableName}` : '未分配'
}

function getRoundEntryActionLabel(uuid) {
  return props.getRoundEntryAssignment(uuid) ? '已分配' : '加入'
}

function resolveRoundMemberRole(judge) {
  if (props.currentRound?.type !== 'SCORE') return 'PROFESSIONAL'
  if (props.judgeRoleFilter === 'CROSS') return 'CROSS'
  if (props.judgeRoleFilter === 'PROFESSIONAL') return 'PROFESSIONAL'
  const roles = inferJudgeRoles(judge || {})
  return roles.includes('CROSS') && !roles.includes('PROFESSIONAL') ? 'CROSS' : 'PROFESSIONAL'
}

function inferJudgeRoles(judge) {
  const qualification = `${judge?.qualification || ''}${judge?.name || ''}`.toLowerCase()
  const roles = []
  if (qualification.includes('专业') || qualification.includes('酿酒') || qualification.includes('bjcp') || qualification.includes('judge')) roles.push('PROFESSIONAL')
  if (qualification.includes('跨界') || qualification.includes('媒体') || qualification.includes('餐饮') || qualification.includes('大众')) roles.push('CROSS')
  return roles.length ? roles : ['PROFESSIONAL']
}

function formatRoundMemberRole(role) {
  if (role === 'CROSS') return '跨界评审'
  return '专业评审'
}

function normalizeRoundScoreMemberRole(role) {
  return role === 'CROSS' ? 'CROSS' : 'PROFESSIONAL'
}

function getEntry(uuid) {
  return props.currentPoolEntries.find((entry) => entry.uuid === uuid)
}

function formatEntryName(entry) {
  return entry?.name || entry?.uuid || '-'
}

function formatEntryBrief(uuid) {
  const entry = getEntry(uuid)
  const name = formatEntryName(entry)
  const shortCode = entry?.shortCode
  return shortCode ? `${name} ·${shortCode}` : name
}

function getEntryHoverRows(uuid) {
  const entry = getEntry(uuid)
  if (!entry) {
    return [
      { label: '编号', value: uuid },
      { label: '状态', value: '未找到酒款信息' },
    ]
  }
  return [
    { label: '组别', value: entry.categoryName || '未归类' },
    { label: '风格', value: entry.style || '未填写风格' },
    { label: '厂牌', value: entry.breweryCompanyName || '未关联厂牌' },
    { label: '状态', value: formatEntryState(entry) },
    { label: '来源', value: entry.sourceTable || '本轮分配' },
  ]
}

function formatEntryState(entry) {
  if (entry?.status === 'CANCELED') return '已取消'
  if (entry?.refundStatus === 'SUCCESS' || entry?.paymentStatus === 'REFUNDED') return '已退款'
  if (entry?.stored || entry?.status === 'STORED') return '已入库'
  if (entry?.deliveryStatus === 'RECEIVED') return '已收到样品'
  if (entry?.deliveryStatus === 'SUBMITTED') return '已提交送样'
  if (entry?.paymentStatus === 'PAID' || entry?.status === 'REGISTERED') return '已支付'
  if (entry?.status === 'PENDING_PAYMENT' || entry?.paymentStatus === 'UNPAID') return '待支付'
  return entry?.status || '-'
}

function getTargetCountLabel(table) {
  if (isFeedbackOnlyScoreRound.value) return '诊断'
  if (props.currentRound?.type === 'SCORE') return '晋级数量'
  if (table?.targetMode === 'MEDALS') return '奖项名额'
  if (table?.targetMode === 'CHAMPION') return '总冠军'
  return '晋级数量'
}

function isTargetCountFixed(table) {
  return table?.targetMode === 'MEDALS' || table?.targetMode === 'CHAMPION'
}

function getTableScopeValue(table) {
  if (table?.categoryMode === 'MIXED') return 'MIXED'
  if (table?.categoryId != null) return `CATEGORY:${table.categoryId}`
  return 'EMPTY'
}

function getJudgeTable(roundTable) {
  return props.judgeTableForm.find((table) => table.tableName === roundTable.name)
}

function getRoundJudgeAssignments(roundTable) {
  const assignments = []
  const seen = new Set()
  if (roundTable?.captainPublicId) {
    assignments.push({ judgePublicId: roundTable.captainPublicId, role: 'CAPTAIN' })
    seen.add(roundTable.captainPublicId)
  }
  getRoundMembers(roundTable).forEach((member) => {
    if (!member?.judgePublicId || seen.has(member.judgePublicId)) return
    assignments.push({
      judgePublicId: member.judgePublicId,
      role: member.role || 'PROFESSIONAL',
    })
    seen.add(member.judgePublicId)
  })
  return assignments
}

function getTableJudgeAssignments(roundTable) {
  if (usesRoundJudgeEditor.value) {
    return getRoundJudgeAssignments(roundTable)
  }
  const judgeTable = getJudgeTable(roundTable)
  if (judgeTable) return props.assignmentsForTable(judgeTable)
  return roundTable.captainPublicId ? [{ judgePublicId: roundTable.captainPublicId, role: 'CAPTAIN' }] : []
}

function getRoleLabel(role) {
  return props.roleOptions.find((option) => option.value === role)?.label || '评审'
}

function formatJudgeQualification(qualification) {
  const text = (qualification || '').trim()
  return text || '未填写资质'
}

function hasJudgeBreweryConflict(judge) {
  return Boolean(judge?.breweryConflictFlag && String(judge?.breweryConflictText || '').trim())
}

function formatJudgeBreweryConflict(judge) {
  const text = String(judge?.breweryConflictText || '').trim()
  return text ? `需回避：${text}` : ''
}

function getJudgeBreweryConflict(judgePublicId) {
  return formatJudgeBreweryConflict(props.getJudge(judgePublicId))
}

function getOverviewJudgeItems(roundTable) {
  return getTableJudgeAssignments(roundTable).map((assignment, index) => {
    const judge = props.getJudge(assignment.judgePublicId)
    const role = assignment.role || 'JUDGE'
    return {
      key: assignment.localId || `${assignment.judgePublicId || 'judge'}-${role}-${index}`,
      name: judge?.name || '未知评审',
      breweryConflict: formatJudgeBreweryConflict(judge),
      initial: props.getJudgeInitial(judge?.name),
      role,
      roleLabel: getRoleLabel(role),
    }
  })
}

function formatJudgeNames(roundTable) {
  const names = getTableJudgeAssignments(roundTable)
    .map((assignment) => props.getJudge(assignment.judgePublicId)?.name)
    .filter(Boolean)
  if (!names.length) return '未安排评审'
  return names.length > 4 ? `${names.slice(0, 4).join(' · ')} +${names.length - 4}` : names.join(' · ')
}

function formatRoleSummary(roundTable) {
  const assignments = getTableJudgeAssignments(roundTable)
  if (!assignments.length) return '桌长0 / 专业0 / 跨界0'
  const counts = assignments.reduce((map, assignment) => {
    map[assignment.role] = (map[assignment.role] || 0) + 1
    return map
  }, {})
  return `桌长${counts.CAPTAIN || 0} / 专业${counts.PROFESSIONAL || 0} / 跨界${counts.CROSS || 0}`
}

function formatEntryCodes(roundTable) {
  const codes = roundTable.entryUuids
    .map((uuid) => getEntry(uuid)?.shortCode || uuid)
    .filter(Boolean)
  if (!codes.length) return '未分配酒款'
  return codes.length > 6 ? `${codes.slice(0, 6).join(' · ')} +${codes.length - 6}` : codes.join(' · ')
}

function getOverviewEntryItems(roundTable) {
  return roundTable.entryUuids.map((uuid) => {
    const entry = getEntry(uuid)
    return {
      uuid,
      code: entry?.shortCode || uuid,
      categoryName: entry?.categoryName || '未归类',
      style: entry?.style || '未填写风格',
    }
  })
}

function formatCategorySummary(roundTable) {
  const map = new Map()
  roundTable.entryUuids.forEach((uuid) => {
    const entry = getEntry(uuid)
    if (!entry?.categoryName) return
    map.set(entry.categoryName, (map.get(entry.categoryName) || 0) + 1)
  })
  if (!map.size) return '暂无组别分布'
  return [...map.entries()].map(([category, count]) => `${category} ${count}`).join(' · ')
}

function getOverviewTableIssues(roundTable) {
  const issues = [...props.getRoundTableIssues(roundTable)]
  issues.push(...props.getRoundTableConflictWarnings(roundTable))
  if (props.currentRound?.type === 'SCORE' && props.currentRound?.isPreparationDraft) {
    const judgeTable = getJudgeTable(roundTable)
    if (judgeTable) issues.push(...props.tableValidationIssues(judgeTable))
    else issues.push(`${roundTable.name}缺少对应评审桌`)
  }
  return [...new Set(issues)]
}
</script>

<style scoped>
.allocation-workbench,
.allocation-grid,
.resource-panel,
.resource-list,
.table-board,
.check-panel,
.control-section,
.entry-list,
.overview-board,
.overview-grid {
  display: grid;
  gap: 10px;
}

.allocation-workbench {
  min-height: 0;
  height: 100%;
  max-height: calc(100vh - 254px);
  overflow: hidden;
}

.allocation-workbench.overview-workbench {
  height: auto;
  max-height: none;
  overflow: visible;
}

.allocation-grid {
  min-height: 0;
  height: 100%;
}

.resource-list,
.table-board,
.check-panel,
.overview-board {
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: rgba(216, 169, 53, 0.46) rgba(255, 255, 255, 0.04);
}

.resource-list::-webkit-scrollbar,
.table-board::-webkit-scrollbar,
.check-panel::-webkit-scrollbar,
.overview-board::-webkit-scrollbar {
  width: 8px;
}

.resource-list::-webkit-scrollbar-track,
.table-board::-webkit-scrollbar-track,
.check-panel::-webkit-scrollbar-track,
.overview-board::-webkit-scrollbar-track {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.035);
}

.resource-list::-webkit-scrollbar-thumb,
.table-board::-webkit-scrollbar-thumb,
.check-panel::-webkit-scrollbar-thumb,
.overview-board::-webkit-scrollbar-thumb {
  border: 1px solid rgba(9, 16, 20, 0.9);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(216, 169, 53, 0.64), rgba(216, 169, 53, 0.3));
}

.resource-list::-webkit-scrollbar-thumb:hover,
.table-board::-webkit-scrollbar-thumb:hover,
.check-panel::-webkit-scrollbar-thumb:hover,
.overview-board::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(224, 184, 74, 0.78), rgba(216, 169, 53, 0.42));
}

.round-switch,
.mode-switch,
.filter-row,
.bulk-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

button,
input,
select {
  font: inherit;
}

input[type="checkbox"] {
  position: relative;
  width: 16px;
  min-width: 16px;
  height: 16px;
  min-height: 16px;
  margin: 0;
  border: 1px solid rgba(219, 232, 237, 0.2);
  border-radius: 5px;
  appearance: none;
  outline: 0;
  background: rgba(7, 14, 17, 0.78);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
  transition: border-color 0.18s ease, background-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
  cursor: pointer;
}

input[type="checkbox"]:hover {
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(16, 25, 29, 0.9);
}

input[type="checkbox"]:focus-visible {
  border-color: rgba(224, 184, 74, 0.56);
  box-shadow: 0 0 0 3px rgba(216, 169, 53, 0.12);
}

input[type="checkbox"]:checked {
  border-color: rgba(216, 169, 53, 0.64);
  background: linear-gradient(180deg, rgba(216, 169, 53, 0.24), rgba(216, 169, 53, 0.48));
  box-shadow: inset 0 0 0 1px rgba(255, 248, 220, 0.08), 0 0 0 1px rgba(216, 169, 53, 0.14);
}

input[type="checkbox"]:checked::after {
  content: '';
  position: absolute;
  top: 1px;
  left: 4px;
  width: 4px;
  height: 8px;
  border: solid #fff5d7;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

button {
  min-height: 34px;
  padding: 0 10px;
  color: #e6edf0;
  border: 1px solid rgba(219, 232, 237, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.round-switch button.active,
.mode-switch button.active,
.filter-row button.active,
.main-action {
  color: #e0b84a;
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
  font-weight: 800;
}

.main-action {
  position: relative;
}

.main-action.blocked {
  opacity: 1;
  color: rgba(224, 184, 74, 0.42);
  border-color: rgba(216, 169, 53, 0.16);
  background: rgba(216, 169, 53, 0.045);
}

.allocation-grid {
  grid-template-columns: 320px minmax(0, 1fr) 300px;
  align-items: stretch;
}

.entry-allocation-grid {
  grid-template-columns: minmax(300px, 340px) minmax(420px, 1fr) minmax(280px, 300px);
}

.overview-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: stretch;
  gap: 10px;
  min-height: 0;
  height: 100%;
}

.overview-workbench .overview-shell {
  height: auto;
}

.resource-panel,
.desk-card,
.check-panel,
.overview-card {
  min-width: 0;
  padding: 12px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(22, 32, 36, 0.9);
}

.resource-panel,
.check-panel {
  min-height: 0;
}

.entry-allocation-grid .resource-panel {
  display: flex;
  flex-direction: column;
}

.entry-allocation-grid .table-board {
  min-width: 0;
  min-height: 0;
  grid-auto-rows: max-content;
  align-content: start;
}

.entry-allocation-grid .resource-list {
  flex: 1 1 auto;
  min-height: 0;
  align-content: start;
  grid-auto-rows: max-content;
}

.judge-resource-panel {
  grid-template-rows: auto auto minmax(0, 1fr);
}

.panel-head,
.desk-card header,
.role-lane header,
.resource-card,
.mini-card,
.desk-warning-line,
.check-panel p,
.desk-header-actions {
  display: flex;
  align-items: center;
}

.panel-head,
.desk-card header,
.role-lane header {
  justify-content: space-between;
  gap: 10px;
}

.desk-header-actions {
  flex: 0 0 auto;
  align-self: start;
  align-items: flex-start;
  gap: 8px;
}

.panel-head span,
small,
em,
.desk-card header span,
dt,
.stack-field span {
  color: #8da1aa;
}

.search-box {
  position: relative;
}

.entry-search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.entry-add-selected {
  min-width: 58px;
  min-height: 38px;
  padding: 0 12px;
  color: #e0b84a;
  border-color: rgba(216, 169, 53, 0.28);
  background: rgba(216, 169, 53, 0.08);
  font-weight: 850;
  white-space: nowrap;
}

.search-box :deep(svg) {
  position: absolute;
  top: 50%;
  left: 10px;
  width: 16px;
  height: 16px;
  color: #8da1aa;
  transform: translateY(-50%);
}

.search-box input,
.desk-card input,
.stack-field input,
.stack-field select {
  width: 100%;
  box-sizing: border-box;
  min-height: 38px;
  padding: 0 10px;
  color: #e6edf0;
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  outline: 0;
  background: rgba(7, 14, 17, 0.72);
}

.search-box input {
  padding-left: 32px;
}

.filter-row button,
.bulk-row button {
  min-height: 30px;
  color: #8da1aa;
  font-size: 12px;
}

.primary-filter-row {
  margin-bottom: 2px;
}

.filter-row button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.filter-row button b {
  min-width: 18px;
  color: inherit;
  font-size: 11px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
}

.resource-card {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  gap: 9px;
  padding: 8px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.resource-card.entry {
  grid-template-columns: 18px minmax(0, 1fr) auto;
  align-items: center;
  min-height: 66px;
}

.resource-card.assigned {
  border-color: rgba(219, 232, 237, 0.075);
  background: rgba(255, 255, 255, 0.014);
}

.resource-card.assigned .avatar,
.resource-card.assigned strong,
.resource-card.assigned small {
  opacity: 0.48;
}

.resource-card.assigned button,
.resource-card.assigned input[type='checkbox'] {
  opacity: 0.55;
}

.resource-card.assigned .assignment-status,
.resource-card.assigned .entry-meta-line em {
  opacity: 1;
}

.assignment-status,
.entry-meta-line em {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  max-width: 100%;
  min-height: 22px;
  padding: 0 7px;
  border: 1px solid rgba(111, 207, 122, 0.22);
  border-radius: 999px;
  background: rgba(111, 207, 122, 0.08);
  color: #7ee08a;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
  line-height: 20px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry-meta-line em.pending {
  color: #8da1aa;
  border-color: rgba(219, 232, 237, 0.1);
  background: rgba(255, 255, 255, 0.026);
}

.resource-body {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.resource-card-actions {
  display: grid;
  gap: 6px;
  align-content: center;
}

.resource-card-actions button {
  min-height: 30px;
  padding: 0 8px;
  white-space: nowrap;
}

.resource-body em {
  font-size: 12px;
  color: #6fcf7a;
}

.judge-conflict-line {
  position: relative;
  display: block;
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
  color: #f1bd79;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.desk-warning-line {
  gap: 7px;
  margin: 0;
  padding: 7px 8px;
  color: #f1bd79;
  border: 1px solid rgba(242, 153, 74, 0.22);
  border-radius: 8px;
  background: rgba(242, 153, 74, 0.08);
  font-size: 12px;
  font-weight: 750;
  line-height: 1.45;
}

.desk-warning-line :deep(svg) {
  width: 15px;
  height: 15px;
  flex: 0 0 auto;
}

.resource-card.selected {
  border-color: rgba(216, 169, 53, 0.34);
}

.resource-card.disabled {
  opacity: 0.52;
}

.avatar {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  color: #e0b84a;
  border: 1px solid rgba(216, 169, 53, 0.24);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.08);
  font-weight: 900;
}

.avatar.small {
  width: 28px;
  height: 28px;
}

.resource-card div,
.mini-card div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.entry-meta-line {
  display: flex;
  gap: 7px;
  align-items: center;
  min-width: 0;
}

.entry-meta-line small {
  min-width: 0;
  flex: 1 1 auto;
}

.entry-meta-line em {
  flex: 0 0 auto;
  max-width: 132px;
  min-height: 22px;
  padding: 0 7px;
  color: #7ee08a;
  border: 1px solid rgba(111, 207, 122, 0.22);
  border-radius: 999px;
  background: rgba(111, 207, 122, 0.08);
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
  line-height: 20px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry-meta-line em.pending {
  color: #8da1aa;
  border-color: rgba(219, 232, 237, 0.1);
  background: rgba(255, 255, 255, 0.026);
}

.resource-card strong,
.resource-card em,
.mini-card strong,
.entry-list span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.judge-qualification,
.judge-conflict-line {
  position: relative;
  display: block;
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.judge-qualification:hover,
.judge-conflict-line:hover {
  overflow: visible;
}

.judge-qualification:hover::after,
.judge-conflict-line:hover::after {
  content: attr(data-full);
  position: absolute;
  z-index: 20;
  left: 0;
  bottom: calc(100% + 7px);
  width: max-content;
  max-width: min(360px, 72vw);
  padding: 7px 9px;
  color: #e6edf0;
  border: 1px solid rgba(216, 169, 53, 0.3);
  border-radius: 8px;
  background: rgba(7, 14, 17, 0.98);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.35);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
  white-space: normal;
  overflow-wrap: anywhere;
  pointer-events: none;
}

.judge-qualification:hover::before,
.judge-conflict-line:hover::before {
  content: '';
  position: absolute;
  z-index: 21;
  left: 14px;
  bottom: calc(100% + 2px);
  width: 8px;
  height: 8px;
  border-right: 1px solid rgba(216, 169, 53, 0.3);
  border-bottom: 1px solid rgba(216, 169, 53, 0.3);
  background: rgba(7, 14, 17, 0.98);
  transform: rotate(45deg);
  pointer-events: none;
}

.table-board {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-content: start;
  align-items: start;
}

.entry-allocation-grid .table-board {
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
}

.judge-board {
  grid-template-columns: 1fr;
  grid-auto-rows: max-content;
}

.desk-card {
  display: grid;
  gap: 8px;
  align-self: start;
}

.desk-card.active {
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(216, 169, 53, 0.045);
}

.desk-card.danger {
  border-color: rgba(242, 153, 74, 0.26);
}

.desk-card h3,
.overview-card h3,
dl,
dd,
p {
  margin: 0;
}

.desk-card .round-table-name-input,
.overview-card .round-table-name-input {
  width: min(220px, 100%);
  max-width: 220px;
  min-width: 96px;
  min-height: 34px;
  box-sizing: border-box;
  padding: 0 9px;
  color: #f4f7f5;
  border: 1px solid rgba(216, 169, 53, 0.18);
  border-radius: 8px;
  outline: 0;
  background: rgba(7, 14, 17, 0.28);
  font-size: 22px;
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0;
}

.desk-card .round-table-name-input:hover,
.overview-card .round-table-name-input:hover {
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(7, 14, 17, 0.46);
}

.desk-card .round-table-name-input:focus,
.overview-card .round-table-name-input:focus {
  color: #fff8dc;
  border-color: rgba(224, 184, 74, 0.72);
  background: rgba(7, 14, 17, 0.68);
  box-shadow: 0 0 0 3px rgba(216, 169, 53, 0.12);
}

.desk-card .round-table-name-input::placeholder,
.overview-card .round-table-name-input::placeholder {
  color: #6f848d;
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  align-items: start;
}

.role-lane {
  display: grid;
  grid-auto-rows: max-content;
  align-content: start;
  align-self: start;
  gap: 6px;
  min-height: 0;
  padding: 8px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.022);
}

.role-lane.active {
  border-color: rgba(216, 169, 53, 0.34);
}

.mini-card {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  align-items: center;
  gap: 6px;
  min-height: 52px;
  padding: 6px 8px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.desk-summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: start;
}

.desk-summary-main {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.desk-summary-side {
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 8px;
}

.desk-name-field {
  display: grid;
  grid-template-columns: 52px minmax(0, 120px);
  gap: 8px;
  align-items: center;
}

.desk-name-field span {
  color: #8da1aa;
  font-size: 14px;
  font-weight: 800;
  white-space: nowrap;
}

.desk-status {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 8px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.026);
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
  white-space: nowrap;
}

.desk-status.ok {
  color: #6fcf7a;
  border-color: rgba(111, 207, 122, 0.2);
  background: rgba(111, 207, 122, 0.07);
}

.desk-status.warning {
  color: #f1bd79;
  border-color: rgba(242, 153, 74, 0.22);
  background: rgba(242, 153, 74, 0.08);
}

.role-empty {
  min-height: 52px;
  box-sizing: border-box;
  padding: 10px 2px 2px;
  color: #6f848d;
  font-size: 12px;
  line-height: 1.5;
}

.icon-action {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  padding: 0;
}

.desk-delete-action {
  flex: 0 0 auto;
  color: #f1bd79;
  border-color: rgba(242, 153, 74, 0.22);
  background: rgba(242, 153, 74, 0.06);
}

.desk-delete-action:hover {
  color: #ffd3a6;
  border-color: rgba(242, 153, 74, 0.38);
  background: rgba(242, 153, 74, 0.12);
}

.icon-action :deep(svg),
.check-panel :deep(svg) {
  width: 16px;
  height: 16px;
  flex: 0 0 auto;
}

.add-desk {
  color: #e0b84a;
  border-style: dashed;
}

.control-panel {
  align-content: start;
}

.control-section {
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(219, 232, 237, 0.08);
}

.control-section:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.control-section > strong {
  font-size: 14px;
}

.control-section > button,
.control-section .main-action {
  width: 100%;
}

.control-section .round-switch,
.control-section .mode-switch {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.control-section .round-switch {
  grid-template-columns: repeat(auto-fit, minmax(76px, 1fr));
}

.desk-meta,
.overview-card dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.desk-meta span,
.desk-category-line,
.inline-scope,
.inline-target,
.entry-list article,
.overview-card dl div {
  padding: 8px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.entry-desk-header {
  display: grid !important;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: start;
}

.entry-desk-title {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  align-items: center;
  min-width: 0;
}

.entry-desk-title h3 {
  flex: 0 0 auto;
  font-size: 22px;
  line-height: 1;
}

.entry-desk-title .round-table-name-input {
  flex: 0 1 220px;
}

.entry-desk-title > span {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  color: #a9bac2;
  font-size: 14px;
  font-weight: 800;
  white-space: nowrap;
}

.desk-config-row {
  display: grid;
  grid-template-columns: minmax(150px, 220px) minmax(94px, 120px);
  gap: 8px;
  align-items: center;
}

.ranking-config-row {
  grid-template-columns: minmax(248px, 300px);
}

.inline-target {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 58px;
  gap: 8px;
  align-items: center;
}

.inline-scope {
  display: grid;
  grid-template-columns: auto minmax(84px, 126px);
  gap: 6px;
  align-items: center;
}

.compact-target {
  grid-template-columns: minmax(0, 1fr) 46px;
  gap: 6px;
  min-height: 28px;
  padding: 3px 4px 3px 8px;
}

.compact-scope {
  min-height: 28px;
  padding: 3px 4px 3px 8px;
}

.desk-category-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.desk-category-line strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.desk-auto-action {
  min-height: 28px;
  padding: 0 10px;
  color: #e0b84a;
  border-color: rgba(216, 169, 53, 0.24);
  background: rgba(216, 169, 53, 0.08);
  font-size: 12px;
  font-weight: 700;
}

.inline-target span,
.inline-scope span {
  color: #8da1aa;
  white-space: nowrap;
}

.target-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.target-label > span {
  flex: 0 0 auto;
}

.inline-target input,
.inline-scope select,
.target-scope-select {
  width: 100%;
  box-sizing: border-box;
  min-height: 28px;
  padding: 0 6px;
  color: #e6edf0;
  border: 1px solid rgba(216, 169, 53, 0.24);
  border-radius: 8px;
  outline: 0;
  background: rgba(7, 14, 17, 0.72);
  font-weight: 800;
}

.target-scope-select {
  min-width: 0;
  flex: 1 1 104px;
  padding-right: 18px;
  text-overflow: ellipsis;
}

.inline-target input {
  text-align: center;
}

.inline-target input:disabled {
  color: #e0b84a;
  border-color: rgba(216, 169, 53, 0.28);
  background: rgba(216, 169, 53, 0.055);
  cursor: not-allowed;
  font-weight: 850;
}

.inline-scope select {
  min-width: 0;
  padding-right: 18px;
  text-overflow: ellipsis;
}

.inline-scope select:disabled {
  opacity: 1;
  cursor: default;
}

.target-scope-select:disabled {
  opacity: 1;
  cursor: default;
}

.entry-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.entry-list article {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  min-height: 34px;
  padding: 5px 8px;
  overflow: visible;
}

.assigned-entry-card {
  isolation: isolate;
}

.assigned-entry-card:hover,
.assigned-entry-card:focus-within {
  z-index: 40;
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.055);
}

.assigned-entry-name {
  min-width: 0;
}

.entry-hover-card {
  position: absolute;
  z-index: 45;
  top: calc(100% + 8px);
  left: -1px;
  display: grid;
  gap: 8px;
  width: min(320px, 72vw);
  padding: 10px;
  color: #e6edf0;
  border: 1px solid rgba(216, 169, 53, 0.34);
  border-radius: 8px;
  background: rgba(7, 14, 17, 0.98);
  box-shadow: 0 16px 34px rgba(0, 0, 0, 0.44);
  opacity: 0;
  pointer-events: none;
  transform: translateY(-3px);
  transition: opacity 0.14s ease, transform 0.14s ease;
}

.entry-list article:nth-child(2n) .entry-hover-card {
  right: -1px;
  left: auto;
}

.entry-hover-card::before {
  content: '';
  position: absolute;
  top: -5px;
  left: 18px;
  width: 8px;
  height: 8px;
  border-top: 1px solid rgba(216, 169, 53, 0.34);
  border-left: 1px solid rgba(216, 169, 53, 0.34);
  background: rgba(7, 14, 17, 0.98);
  transform: rotate(45deg);
}

.entry-list article:nth-child(2n) .entry-hover-card::before {
  right: 36px;
  left: auto;
}

.assigned-entry-card:hover .entry-hover-card,
.assigned-entry-card:focus-within .entry-hover-card {
  opacity: 1;
  transform: translateY(0);
}

.entry-hover-card header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: start;
}

.entry-hover-card header strong {
  color: #f4f8f9;
  font-size: 14px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.entry-hover-card header em {
  min-height: 22px;
  padding: 0 7px;
  color: #e0b84a;
  border: 1px solid rgba(216, 169, 53, 0.26);
  border-radius: 999px;
  background: rgba(216, 169, 53, 0.08);
  font-size: 12px;
  font-style: normal;
  font-weight: 850;
  line-height: 22px;
  white-space: nowrap;
}

.entry-hover-card dl {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 6px 10px;
  padding-top: 8px;
  border-top: 1px solid rgba(219, 232, 237, 0.09);
}

.entry-hover-card dt {
  color: #8da1aa;
  font-size: 12px;
  font-weight: 750;
  line-height: 1.45;
}

.entry-hover-card dd {
  min-width: 0;
  color: #d8e4e8;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.entry-list p {
  grid-column: 1 / -1;
}

.resource-empty,
.desk-empty-state {
  display: grid;
  place-items: center;
  min-height: 140px;
  border: 1px dashed rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  color: #8da1aa;
  background: rgba(255, 255, 255, 0.018);
  font-size: 13px;
  font-weight: 750;
  text-align: center;
}

.desk-empty-state {
  gap: 6px;
  min-height: 220px;
}

.desk-empty-state strong {
  color: #e6edf0;
  font-size: 18px;
}

.desk-empty-state span {
  color: #8da1aa;
}

.entry-list button {
  min-height: 26px;
  padding: 0 8px;
  color: #e0b84a;
}

.stack-field {
  display: grid;
  gap: 6px;
}

.check-panel {
  position: sticky;
  top: 0;
}

.check-panel p {
  gap: 8px;
  padding: 9px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.warning {
  color: #f1bd79;
}

.ok {
  color: #6fcf7a;
}

.overview-card {
  display: grid;
  gap: 0;
  align-self: start;
  padding: 0;
}

.overview-card.danger {
  border-color: rgba(242, 153, 74, 0.3);
}

.overview-board {
  align-content: start;
  grid-auto-rows: max-content;
}

.overview-workbench .overview-board {
  overflow: visible;
}

.overview-grid {
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  align-content: start;
  align-items: start;
}

.overview-card-header,
.overview-card-meta,
.overview-block-head,
.overview-member-card,
.overview-entry-card,
.overview-issue {
  display: flex;
  align-items: center;
}

.overview-card-header,
.overview-block-head {
  justify-content: space-between;
  gap: 10px;
}

.overview-card-header {
  padding: 12px 12px 10px;
  border-bottom: 1px solid rgba(219, 232, 237, 0.08);
  background: rgba(255, 255, 255, 0.018);
}

.overview-block-head span {
  flex: 0 0 auto;
}

.overview-block-head small {
  min-width: 0;
  text-align: right;
}

.overview-title-block {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.overview-title-block h3 {
  font-size: 20px;
  line-height: 1;
}

.overview-card-meta {
  flex-wrap: wrap;
  gap: 6px;
}

.overview-card-meta span {
  min-height: 22px;
  padding: 0 7px;
  color: #a9bac2;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.026);
  font-size: 12px;
  font-weight: 800;
  line-height: 22px;
}

.overview-card-header > em {
  flex: 0 0 auto;
  min-height: 26px;
  padding: 0 8px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
  font-style: normal;
  font-weight: 800;
  line-height: 26px;
}

.overview-summary {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 6px;
}

.overview-summary article,
.overview-block {
  min-width: 0;
}

.overview-summary article {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 8px;
  align-items: baseline;
  padding: 8px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.overview-summary span,
.overview-block span,
.overview-card small {
  color: #8da1aa;
}

.overview-summary strong {
  color: #e6edf0;
  font-size: 17px;
}

.overview-block {
  display: grid;
  align-content: start;
  gap: 8px;
}

.overview-content-grid {
  display: grid;
  grid-template-columns: minmax(230px, 0.42fr) minmax(360px, 1fr);
  gap: 18px;
  padding: 11px 12px 12px;
}

.overview-entries {
  padding-left: 18px;
  border-left: 1px solid rgba(219, 232, 237, 0.1);
}

.overview-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  padding: 8px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.overview-toolbar-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.overview-toolbar-group strong {
  color: #a9bac2;
  font-size: 13px;
}

.compact-switch button {
  min-height: 28px;
  padding: 0 10px;
}

.overview-member-list,
.overview-entry-list {
  display: grid;
  gap: 5px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.overview-entry-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 14px;
  row-gap: 5px;
}

.overview-member-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  min-width: 0;
  min-height: 30px;
  padding: 5px 0;
  border-top: 1px solid rgba(219, 232, 237, 0.08);
}

.overview-member-card div,
.overview-entry-card div {
  display: grid;
  gap: 1px;
  min-width: 0;
}

.overview-member-card strong,
.overview-entry-card small {
  color: #e6edf0;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.overview-member-card small,
.overview-block-head small {
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.overview-member-card em {
  align-self: center;
  min-height: 24px;
  padding: 0 7px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.026);
  color: #a9bac2;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
  line-height: 24px;
  white-space: nowrap;
}

.overview-member-card em.role-CAPTAIN {
  color: #e0b84a;
  border-color: rgba(216, 169, 53, 0.24);
  background: rgba(216, 169, 53, 0.08);
}

.overview-member-card em.role-PROFESSIONAL {
  color: #8ed6ff;
  border-color: rgba(98, 170, 215, 0.2);
  background: rgba(98, 170, 215, 0.08);
}

.overview-member-card em.role-CROSS {
  color: #9be6b0;
  border-color: rgba(111, 207, 122, 0.2);
  background: rgba(111, 207, 122, 0.07);
}

.overview-entry-card {
  display: grid;
  grid-template-columns: minmax(62px, auto) minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  min-width: 0;
  min-height: 30px;
  padding: 5px 0;
  border-top: 1px solid rgba(219, 232, 237, 0.08);
}

.overview-entry-card > strong {
  min-width: 62px;
  max-width: 92px;
  color: #e0b84a;
  font-size: 14px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.overview-empty {
  padding: 9px 0 4px;
  color: #6f848d;
  font-size: 13px;
}

.overview-issue {
  gap: 7px;
  padding: 9px 12px;
  border-top: 1px solid rgba(242, 153, 74, 0.2);
  background: rgba(242, 153, 74, 0.07);
  color: #f1bd79;
}

.overview-issue :deep(svg) {
  width: 16px;
  height: 16px;
  flex: 0 0 auto;
}

@media (max-width: 1180px) {
  .allocation-grid,
  .overview-shell,
  .table-board,
  .overview-grid,
  .overview-summary,
  .overview-content-grid,
  .role-grid {
    grid-template-columns: 1fr;
  }

  .overview-entries {
    padding-left: 0;
    border-left: 0;
  }

  .check-panel {
    position: static;
  }

  .entry-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1440px) {
  .desk-summary {
    grid-template-columns: 1fr;
  }

  .desk-summary-side,
  .desk-role-counts {
    justify-content: flex-start;
  }

  .desk-name-field {
    grid-template-columns: 52px minmax(0, 120px);
  }
}
</style>
