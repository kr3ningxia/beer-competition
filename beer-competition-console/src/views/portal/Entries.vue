<template>
  <div class="entries-page">
    <section class="toolbar brewer-card">
      <div>
        <h2 class="portal-section-title">我的酒款</h2>
        <p>按酒款查看报名记录，并处理支付、现场标签、样品入库和结果</p>
      </div>
      <div class="toolbar-actions">
        <el-input v-model="keyword" placeholder="搜索酒名 / 现场短编号" clearable />
        <el-select v-model="statusFilter" placeholder="状态" clearable>
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>
    </section>

    <section v-if="activeCompetitionFilter" class="filter-context brewer-card">
      <span>当前查看</span>
      <strong>{{ activeCompetitionFilter.name }}</strong>
      <button type="button" @click="clearCompetitionFilter">查看全部酒款</button>
    </section>

    <section class="entry-grid">
      <article
        v-for="entry in filteredEntries"
        :key="entry.id"
        class="entry-card"
        @click="selectEntry(entry)"
      >
        <div class="label-top">
          <span :class="['label-chip', `tone-${entryStatusMeta[entry.status].tone}`]">
            {{ entryStatusMeta[entry.status].label }}
          </span>
          <span class="abv">{{ formatAbvValue(entry.abv) }}</span>
        </div>
        <div class="competition-line">{{ competitionName(entry.competitionId) }}</div>
        <h3>{{ entry.name }}</h3>
        <p>{{ entry.categoryName }} · {{ entry.style }}</p>
        <div class="meta-grid">
          <span>
            <small>ABV</small>
            <strong>{{ formatAbvWithUnit(entry.abv) }}</strong>
          </span>
          <span>
            <small>报名费</small>
            <strong>{{ formatCurrency(entryPayAmount(entry)) }}</strong>
          </span>
          <span>
            <small>支付</small>
            <strong>{{ paymentStatusText(entry) }}</strong>
          </span>
        </div>
        <div class="uuid-band">
          <span v-if="entry.shortCode">现场短编号 {{ entry.shortCode }}</span>
          <span v-else>现场短编号待生成</span>
          <el-button size="small" text @click.stop="selectEntry(entry)">详情</el-button>
        </div>
        <div class="card-actions" @click.stop>
          <RouterLink v-if="cardPrimaryAction(entry).to" :to="cardPrimaryAction(entry).to">
            {{ cardPrimaryAction(entry).label }}
          </RouterLink>
          <button v-else class="primary-card-action" type="button" @click="selectEntry(entry)">
            {{ cardPrimaryAction(entry).label }}
          </button>
          <el-dropdown trigger="click" @command="(command) => handleMoreCommand(command, entry)">
            <button class="more-action" type="button">更多</button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="detail">查看酒款资料</el-dropdown-item>
                <el-dropdown-item command="event">赛事详情</el-dropdown-item>
                <el-dropdown-item v-if="canSubmitCompetitionEntry(entry)" command="submit">再报一款酒</el-dropdown-item>
                <el-dropdown-item command="refund">{{ refundMenuLabel(entry) }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </article>
    </section>

    <el-drawer v-model="drawerVisible" size="520px" class="entry-drawer" :with-header="false">
      <div v-if="selectedEntry" class="drawer-body">
        <div class="drawer-label">
          <span :class="['label-chip', `tone-${entryStatusMeta[selectedEntry.status].tone}`]">
            {{ entryStatusMeta[selectedEntry.status].label }}
          </span>
          <h2>{{ selectedEntry.name }}</h2>
          <p>现场短编号 {{ selectedEntry.shortCode || '待生成' }}</p>
        </div>

        <section class="drawer-section">
          <h3>关联赛事</h3>
          <div class="linked-event">
            <strong>{{ competitionName(selectedEntry.competitionId) }}</strong>
            <div class="linked-event-actions">
              <RouterLink :to="`/portal/events/${selectedEntry.competitionId}`">进入赛事详情</RouterLink>
              <RouterLink
                v-if="canSubmitCompetitionEntry(selectedEntry)"
                :to="`/portal/submit?competitionId=${selectedEntry.competitionId}`"
              >
                再报一款酒
              </RouterLink>
            </div>
          </div>
        </section>

        <section class="drawer-section">
          <h3>报名信息</h3>
          <dl>
            <div><dt>投递组别</dt><dd>{{ selectedEntry.categoryName }}</dd></div>
            <div><dt>基础风格</dt><dd>{{ selectedEntry.style }}</dd></div>
            <div><dt>ABV</dt><dd>{{ formatAbvWithUnit(selectedEntry.abv) }}</dd></div>
            <div><dt>现场短编号</dt><dd>{{ selectedEntry.shortCode }}</dd></div>
            <div><dt>提交时间</dt><dd>{{ selectedEntry.submittedAt }}</dd></div>
            <div><dt>支付状态</dt><dd>{{ paymentStatusText(selectedEntry) }}</dd></div>
            <div v-if="selectedEntry.refundStatus"><dt>退款状态</dt><dd>{{ refundStatusText(selectedEntry.refundStatus) }}</dd></div>
            <div><dt>入库状态</dt><dd>{{ isStored(selectedEntry) ? '已入库' : '待入库' }}</dd></div>
          </dl>
        </section>

        <section class="drawer-section">
          <h3>评审可见信息预览</h3>
          <div class="anonymous-preview">
            <strong>{{ selectedEntry.categoryName }} · {{ selectedEntry.style }}</strong>
            <p>{{ selectedEntry.categoryName }} · {{ selectedEntry.style }} · {{ formatAbvWithUnit(selectedEntry.abv) }}</p>
            <ul>
              <li v-for="field in selectedEntry.extraFields || []" :key="field.label">
                <span>{{ field.label }}</span>
                <b>{{ field.value }}</b>
              </li>
            </ul>
          </div>
          <p class="privacy-note">评审扫码时隐藏酒名、厂牌和联系人信息</p>
        </section>

        <section class="drawer-section">
          <h3>状态时间线</h3>
          <div class="timeline">
            <div v-for="item in timeline(selectedEntry)" :key="item.label" :class="{ done: item.done }">
              <span />
              <div>
                <strong>{{ item.label }}</strong>
                <p>{{ item.time || item.hint }}</p>
              </div>
            </div>
          </div>
        </section>

        <section class="drawer-section">
          <h3>下一步操作</h3>
          <div class="drawer-actions">
            <RouterLink v-if="cardPrimaryAction(selectedEntry).to" :to="cardPrimaryAction(selectedEntry).to">
              {{ cardPrimaryAction(selectedEntry).label }}
            </RouterLink>
            <button v-else class="primary-card-action" type="button" @click="selectEntry(selectedEntry)">
              {{ cardPrimaryAction(selectedEntry).label }}
            </button>
            <button v-if="selectedEntry.canRequestRefund" type="button" @click="submitRefundRequest(selectedEntry)">申请退款</button>
            <RouterLink :to="`/portal/events/${selectedEntry.competitionId}`">赛事详情</RouterLink>
            <RouterLink
              v-if="canSubmitCompetitionEntry(selectedEntry)"
              :to="`/portal/submit?competitionId=${selectedEntry.competitionId}`"
            >
              再报一款酒
            </RouterLink>
            <RouterLink v-if="isEntryResultPublished(selectedEntry)" :to="entryResultPath(selectedEntry)">查看这款酒的结果</RouterLink>
          </div>
        </section>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchPortalCompetitions, fetchPortalEntries, fetchPortalEntryDetail, requestPortalEntryRefund } from '@/api/portal'
import { formatAbvValue, formatAbvWithUnit } from '@/utils/formatters'
import { canSubmitEntry, entryPayAmount, entryPrimaryAction, entryResultPath, entryStatusMeta, isEntryRefundActive, isEntryRefunded, isEntryResultPublished } from './portalViewModels'

const router = useRouter()
const route = useRoute()
const keyword = ref('')
const statusFilter = ref('')
const drawerVisible = ref(false)
const selectedEntry = ref(null)
const entries = ref([])
const competitions = ref([])
const currencyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
})

const statusOptions = Object.entries(entryStatusMeta).map(([value, meta]) => ({ value, label: meta.label }))
const competitionMap = computed(() => new Map(competitions.value.map((competition) => [competition.id, competition])))
const routeCompetitionId = computed(() => route.query.competitionId ? Number(route.query.competitionId) : null)
const routeEntryId = computed(() => route.query.entryId ? Number(route.query.entryId) : null)
const activeCompetitionFilter = computed(() => {
  if (!routeCompetitionId.value) return null
  const competition = competitionMap.value.get(routeCompetitionId.value)
  if (competition) return competition
  const entry = entries.value.find((item) => item.competitionId === routeCompetitionId.value)
  return entry ? { id: entry.competitionId, name: entry.competitionName } : null
})

const filteredEntries = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return entries.value.filter((entry) => {
    const hitCompetition = !routeCompetitionId.value || entry.competitionId === routeCompetitionId.value
    const hitStatus = !statusFilter.value || entry.status === statusFilter.value
    const hitWord = !word || `${entry.name} ${entry.uuid} ${entry.shortCode}`.toLowerCase().includes(word)
    return hitCompetition && hitStatus && hitWord
  })
})

async function selectEntry(entry) {
  ensureEntryQuery(entry)
  selectedEntry.value = await fetchPortalEntryDetail(entry.id)
  drawerVisible.value = true
}

async function openEntryFromQuery() {
  if (!routeEntryId.value || !entries.value.length) return
  if (selectedEntry.value?.id === routeEntryId.value && drawerVisible.value) return
  const matched = entries.value.find((entry) => entry.id === routeEntryId.value)
  if (!matched) return
  if (!routeCompetitionId.value) {
    router.replace({
      path: '/portal/entries',
      query: { ...route.query, competitionId: matched.competitionId, entryId: matched.id },
    })
  }
  selectedEntry.value = await fetchPortalEntryDetail(matched.id)
  drawerVisible.value = true
}

function ensureEntryQuery(entry) {
  const nextQuery = {
    ...route.query,
    competitionId: route.query.competitionId || entry.competitionId,
    entryId: entry.id,
  }
  router.replace({ path: '/portal/entries', query: nextQuery })
}

function clearEntryQuery() {
  if (!route.query.entryId) return
  const query = { ...route.query }
  delete query.entryId
  router.replace({ path: '/portal/entries', query })
}

function clearCompetitionFilter() {
  keyword.value = ''
  statusFilter.value = ''
  selectedEntry.value = null
  drawerVisible.value = false
  router.replace('/portal/entries')
}

function competitionName(competitionId) {
  return entries.value.find((entry) => entry.competitionId === competitionId)?.competitionName || '未关联赛事'
}

function primaryAction(entry) {
  return entryPrimaryAction(entry)
}

function cardPrimaryAction(entry) {
  const action = primaryAction(entry)
  if (!entry || action.to === '/portal/entries' || ['查看参赛进度', '查看酒款资料'].includes(action.label)) {
    return { label: '查看酒款资料', to: '' }
  }
  return action
}

function canSubmitCompetitionEntry(entry) {
  return canSubmitEntry(competitionMap.value.get(entry?.competitionId))
}

function handleMoreCommand(command, entry) {
  if (command === 'detail') {
    selectEntry(entry)
    return
  }
  if (command === 'event') {
    router.push(`/portal/events/${entry.competitionId}`)
    return
  }
  if (command === 'submit') {
    router.push(`/portal/submit?competitionId=${entry.competitionId}`)
    return
  }
  if (command === 'refund') {
    submitRefundRequest(entry)
  }
}

function refundMenuLabel(entry) {
  if (isEntryRefundActive(entry)) return '查看退款进度'
  if (isEntryRefunded(entry)) return '已退款'
  return '申请退款'
}

function timeline(entry) {
  const resultPublished = isEntryResultPublished(entry)
  const items = [
    { label: '提交资料', time: entry.submittedAt, done: true },
    { label: '支付报名费', hint: entry.paymentStatus === 'PAID' ? '已支付' : '待支付', done: entry.paymentStatus === 'PAID' },
    { label: '标签下载', hint: entry.canDownloadLabel ? '已生成现场标签' : '支付成功后开放下载', done: entry.canDownloadLabel },
    { label: '样品入库', hint: isStored(entry) ? '已入库' : '等待组委会确认入库', done: isStored(entry) },
    { label: '结果发布', hint: resultPublished ? '结果已发布' : '等待组委会发布结果', done: resultPublished },
  ]
  if (entry.refundStatus) {
    items.splice(2, 0, {
      label: isEntryRefunded(entry) ? '退款完成' : '退款申请',
      hint: isEntryRefunded(entry) ? '报名已取消' : '等待退款结果',
      time: entry.refundProcessedAt || entry.refundRequestedAt,
      done: isEntryRefunded(entry),
    })
  }
  return items
}

function isStored(entry) {
  return entry?.stored || entry?.status === 'STORED' || isEntryResultPublished(entry)
}

function formatCurrency(value) {
  if (value === null || value === undefined || value === '') return '¥0'
  return currencyFormatter.format(Number(value))
}

function paymentStatusText(entry) {
  if (isEntryRefundActive(entry)) return '退款处理中'
  if (isEntryRefunded(entry)) return '已退款'
  if (entry?.refundStatus === 'FAILED') return '退款失败'
  if (entry?.refundStatus === 'REJECTED') return '退款已驳回'
  if (entry?.paymentStatus === 'PAID') return '已支付'
  if (entry?.paymentStatus === 'CANCELED') return '已取消'
  return '待支付'
}

function refundStatusText(status) {
  return {
    REQUESTED: '待组委会处理',
    APPROVED: '退款处理中',
    PROCESSING: '退款处理中',
    SUCCESS: '已退款',
    FAILED: '退款失败',
    REJECTED: '已驳回',
  }[status] || status || '-'
}

async function submitRefundRequest(entry) {
  if (isEntryRefundActive(entry)) {
    router.push(`/portal/payment?entryId=${entry.id}`)
    return
  }
  if (!entry?.canRequestRefund) {
    ElMessage.warning(refundUnavailableText(entry))
    return
  }
  try {
    const { value } = await ElMessageBox.prompt(
      '提交后，这款酒将退出本场比赛；微信支付将原路退回，银行转账由组委会联系处理',
      '申请退款',
      {
        confirmButtonText: '提交退款申请',
        cancelButtonText: '取消',
        inputPlaceholder: '请填写退款原因',
        inputPattern: /.+/,
        inputErrorMessage: '请填写退款原因',
        type: 'warning',
      },
    )
    const detail = await requestPortalEntryRefund(entry.id, { reason: value.trim() })
    ElMessage.success('退款申请已提交')
    entries.value = await fetchPortalEntries()
    if (selectedEntry.value?.id === entry.id) selectedEntry.value = detail
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.warning(error?.message || '退款申请提交失败')
  }
}

function refundUnavailableText(entry) {
  if (isEntryRefunded(entry)) return '这款酒已完成退款'
  if (entry?.paymentStatus !== 'PAID') return '报名费支付完成后才能申请退款'
  if (isStored(entry)) return '样品已入库，不能申请退款'
  if (entry?.status === 'CANCELED') return '报名已取消，不能申请退款'
  return '当前状态不能申请退款'
}

onMounted(async () => {
  const [entryData, competitionData] = await Promise.all([
    fetchPortalEntries(),
    fetchPortalCompetitions(),
  ])
  entries.value = entryData
  competitions.value = competitionData
  await openEntryFromQuery()
})

watch(routeEntryId, () => {
  openEntryFromQuery()
})

watch(drawerVisible, (visible) => {
  if (!visible) clearEntryQuery()
})
</script>

<style scoped>
.entries-page {
  display: grid;
  gap: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 22px;
}

.toolbar p {
  margin: 0;
  color: #746a5f;
}

.toolbar-actions {
  display: grid;
  grid-template-columns: 260px 150px;
  gap: 12px;
  align-items: center;
}

.filter-context {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
}

.filter-context span {
  color: #746a5f;
  font-size: 13px;
  font-weight: 700;
}

.filter-context strong {
  color: #2b1d10;
  font-size: 16px;
}

.filter-context button {
  margin-left: auto;
  min-height: 32px;
  padding: 0 12px;
  color: #6b4710;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.14);
  border-radius: 8px;
  font-weight: 800;
  cursor: pointer;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.entry-card {
  display: flex;
  flex-direction: column;
  min-height: 290px;
  padding: 20px;
  cursor: pointer;
  color: #2b1d10;
  background:
    linear-gradient(180deg, #fffaf0 0%, #f4dca5 100%),
    repeating-linear-gradient(135deg, rgba(84, 52, 19, 0.05) 0 1px, transparent 1px 9px);
  border: 1px solid rgba(87, 58, 26, 0.16);
  border-radius: 8px;
  box-shadow: 0 16px 34px rgba(83, 51, 17, 0.09);
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.entry-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 22px 42px rgba(83, 51, 17, 0.14);
}

.label-top,
.uuid-band {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.abv {
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  color: #fff6df;
  background: #2b1d10;
  border-radius: 50%;
  font-weight: 900;
}

.entry-card h3 {
  min-height: 68px;
  margin: 24px 0 8px;
  font-size: 25px;
  line-height: 1.2;
}

.entry-card p {
  margin: 0;
  color: #746a5f;
}

.competition-line {
  margin-top: 16px;
  color: #8b5c19;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.4;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin: 22px 0;
}

.meta-grid span {
  padding: 10px;
  background: rgba(255, 250, 240, 0.52);
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 8px;
}

.meta-grid small,
.meta-grid strong {
  display: block;
}

.meta-grid small {
  color: #837565;
}

.meta-grid strong {
  margin-top: 5px;
}

.uuid-band {
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px dashed rgba(87, 58, 26, 0.22);
  font-weight: 800;
}

.card-actions,
.drawer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.card-actions a,
.card-actions button,
.drawer-actions button,
.drawer-actions a,
.linked-event a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 0 12px;
  color: #2b1d10;
  background: #e1a23d;
  border: 0;
  border-radius: 8px;
  font-weight: 800;
  text-decoration: none;
  cursor: pointer;
}

.card-actions .primary-card-action,
.drawer-actions .primary-card-action {
  color: #2b1d10;
  background: #e1a23d;
  border: 0;
}

.card-actions .more-action {
  color: #6b4710;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.14);
}

.card-actions a + a,
.card-actions button,
.drawer-actions button,
.drawer-actions a + a,
.linked-event a {
  color: #6b4710;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.14);
}

.drawer-body {
  min-height: 100%;
  padding: 28px;
  color: #2b1d10;
  background: #fff7e6;
}

.drawer-label {
  padding: 22px;
  color: #fff6df;
  background:
    linear-gradient(135deg, #2b1d10, #5a3414),
    repeating-linear-gradient(90deg, rgba(255, 250, 240, 0.07) 0 1px, transparent 1px 12px);
  border-radius: 8px;
}

.drawer-label h2 {
  margin: 18px 0 8px;
  font-size: 30px;
  line-height: 1.15;
}

.drawer-label p {
  margin: 0;
  color: #d8c9ac;
}

.drawer-section {
  margin-top: 22px;
}

.drawer-section h3 {
  margin: 0 0 12px;
  font-size: 18px;
}

dl {
  display: grid;
  gap: 10px;
  margin: 0;
}

.linked-event {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 14px;
  background: #fffdf7;
  border: 1px solid rgba(87, 58, 26, 0.13);
  border-radius: 8px;
}

.linked-event strong {
  line-height: 1.4;
}

.linked-event-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

dl div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(87, 58, 26, 0.1);
}

dt {
  color: #746a5f;
}

dd {
  margin: 0;
  font-weight: 700;
  text-align: right;
}

.anonymous-preview {
  padding: 18px;
  background: #fffdf7;
  border: 1px solid rgba(87, 58, 26, 0.13);
  border-radius: 8px;
}

.anonymous-preview > strong {
  display: block;
  font-size: 20px;
}

.anonymous-preview ul {
  display: grid;
  gap: 8px;
  padding: 0;
  margin: 14px 0 0;
  list-style: none;
}

.anonymous-preview li {
  display: grid;
  gap: 4px;
}

.anonymous-preview li span,
.privacy-note,
.timeline p {
  color: #746a5f;
}

.anonymous-preview li b {
  line-height: 1.5;
}

.privacy-note {
  margin: 10px 0 0;
  font-size: 13px;
}

.timeline {
  display: grid;
  gap: 12px;
}

.timeline > div {
  display: flex;
  gap: 12px;
}

.timeline > div > span {
  width: 12px;
  height: 12px;
  margin-top: 5px;
  background: #d5c6ad;
  border-radius: 50%;
}

.timeline > div.done > span {
  background: #3d7d50;
}

.timeline strong,
.timeline p {
  display: block;
}

.timeline p {
  margin: 4px 0 0;
}

@media (max-width: 1180px) {
  .entry-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 780px) {
  .toolbar,
  .toolbar-actions {
    grid-template-columns: 1fr;
  }

  .toolbar {
    display: grid;
  }

  .entry-grid {
    grid-template-columns: 1fr;
  }
}
</style>
