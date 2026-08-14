const ACTIVE_STATUS_PRIORITY = ['JUDGING', 'JUDGING_PREP']
const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000

export function selectDefaultLiveBoardCompetition(competitions = [], now = new Date()) {
  const candidates = Array.isArray(competitions) ? competitions.filter(Boolean) : []
  for (const status of ACTIVE_STATUS_PRIORITY) {
    const activeCompetition = candidates.find((competition) => competition.status === status)
    if (activeCompetition) return activeCompetition
  }

  const today = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate())
  return candidates
    .map((competition, index) => {
      const competitionDay = parseCompetitionDay(competition.competitionDate)
      return {
        competition,
        index,
        distance: competitionDay == null ? Number.POSITIVE_INFINITY : Math.abs(competitionDay - today) / MILLISECONDS_PER_DAY,
        isPast: competitionDay != null && competitionDay < today,
      }
    })
    .sort((left, right) => (
      left.distance - right.distance
      || Number(left.isPast) - Number(right.isPast)
      || left.index - right.index
    ))[0]?.competition || null
}

function parseCompetitionDay(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(value || ''))
  if (!match) return null
  return Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3]))
}
