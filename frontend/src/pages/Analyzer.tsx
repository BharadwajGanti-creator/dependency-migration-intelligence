import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { runAnalysis, runExplanations } from '../api/client'
import type { AnalysisReport, FindingExplanation } from '../api/types'
import { FindingList } from '../components/FindingList'

const DEFAULT_REQUEST = {
  fixtureId: 'spring-migration-case',
  dependencyGroup: 'org.springframework.boot',
  dependencyArtifact: 'spring-boot-starter-parent',
  fromVersion: '2.7.18',
  toVersion: '3.2.12',
}

export function Analyzer() {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [report, setReport] = useState<AnalysisReport | null>(null)
  const [explanationsByFindingId, setExplanationsByFindingId] = useState<
    Record<string, FindingExplanation>
  >({})
  const [explanationLoadingId, setExplanationLoadingId] = useState<string | null>(null)
  const [explanationError, setExplanationError] = useState<string | null>(null)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const result = await runAnalysis(DEFAULT_REQUEST)
      setReport(result)
      setExplanationsByFindingId({})
      setExplanationError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Request failed')
      setReport(null)
    } finally {
      setLoading(false)
    }
  }

  async function onRequestExplanation(findingId: string) {
    if (!report) return
    setExplanationLoadingId(findingId)
    setExplanationError(null)
    try {
      const result = await runExplanations({
        analysis: DEFAULT_REQUEST,
        findingIds: [findingId],
      })
      const next: Record<string, FindingExplanation> = { ...explanationsByFindingId }
      for (const ex of result.explanations) {
        next[ex.findingId] = ex
      }
      setExplanationsByFindingId(next)
    } catch (err) {
      setExplanationError(err instanceof Error ? err.message : 'Explanation request failed')
    } finally {
      setExplanationLoadingId(null)
    }
  }

  return (
    <div className="page analyzer">
      <nav className="top-nav">
        <Link to="/">← Portfolio</Link>
      </nav>
      <h1>Spring Boot 2 → 3 analysis</h1>
      <p className="muted">
        Supported path: <code>spring-boot-starter-parent</code> 2.7.18 → 3.2.12 on fixture{' '}
        <code>spring-migration-case</code>.
      </p>

      <form className="analyze-form" onSubmit={onSubmit}>
        <button type="submit" className="button primary" disabled={loading}>
          {loading ? 'Analyzing…' : 'Run sync analysis'}
        </button>
      </form>

      {error && (
        <div className="alert error" role="alert">
          {error}
        </div>
      )}

      {report && (
        <section className="report">
          <header>
            <h2>Report {report.requestId.slice(0, 8)}</h2>
            <p className="muted">
              {report.before.group}:{report.before.artifact} {report.before.version} → {report.after.version}
            </p>
          </header>

          <FindingList
            findings={report.findings}
            explanationsByFindingId={explanationsByFindingId}
            onRequestExplanation={onRequestExplanation}
            explanationLoadingId={explanationLoadingId}
            explanationError={explanationError}
          />

          <div className="limitations">
            <h3>Limitations</h3>
            <ul>
              {report.limitations.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </div>
        </section>
      )}
    </div>
  )
}
